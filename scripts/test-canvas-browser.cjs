/* Smoke E2E real com Chrome, reutilizando JS e CSS do aplicativo. */
const fs=require("node:fs");
const assert=require("node:assert/strict");
const {chromium}=require("playwright-core");
const script=fs.readFileSync("src/main/webapp/resources/js/canvas-modelo.js","utf8");
const css=fs.readFileSync("src/main/webapp/resources/css/estilo.css","utf8");
const ids=["cfX","cfY","cfW","cfH","cfLineOrient","cfLineColor","cfLineWeight",
"cfTableStyle","cfTableHeader","cfSignRole","cfRemove","cfFront","cfBack",
"cfRowPlus","cfRowMinus","cfColPlus","cfColMinus","cfTextColor","cfFontSize",
"cfHighlightColor","cfParagraphStyle","cfFontGrow","cfFontShrink","cfFormatReset",
"cfIndentLess","cfIndentMore"];
const inputs=ids.map(id=>'<input id="'+id+'" value="11">').join("");
const html='<!doctype html><html><head><meta charset="utf-8"><style>'+css+'</style>'+
'<style>body{margin:0;padding:30px;background:#fff}.cf-stage-scroll{width:900px;height:1000px}</style></head><body>'+
'<div class="cf-toolbar-sticky"><select id="cfFontFamily"><option>Arial</option><option>Georgia</option></select>'+inputs+
'<button data-cf-format="bold"></button><button data-cf-format="italic"></button>'+
'<button data-cf-format="underline"></button><button data-cf-format="strikeThrough"></button>'+
'<button data-cf-align="justify"></button><button data-cf-align="left"></button>'+
'<button data-cf-list="ul"></button><button data-cf-list="ol"></button></div>'+
'<div id="cfProperties" class="cf-properties" hidden><strong id="cfObjectKind"></strong>'+
'<div data-cf-panel="text"></div><div data-cf-panel="line"></div>'+
'<div data-cf-panel="table"></div><div data-cf-panel="sign"></div></div>'+
'<button data-cf-add="text">Texto</button><button data-cf-add="line">Linha</button>'+
'<button data-cf-add="table">Tabela</button><button data-cf-add="sign">Assinatura</button>'+
'<button id="cfAddImage">Imagem</button><input id="cfImageFile" type="file">'+
'<div class="cf-stage-scroll"><div id="cfCanvas" class="cf-canvas-stage" tabindex="0"></div></div>'+
'<input id="form:conteudo" type="hidden">'+
'<div id="catalogoCampos"><div id="listaCampos"></div></div>'+
'<input id="buscaCampo"><input id="grupoCampo"><div id="contadorCampos"></div><div id="descricaoCampo"></div>'+
'<button id="modoExemplo"></button><button id="modoTags"></button>'+
'<iframe id="previaModelo"></iframe><div id="cfSaveWarning"></div>'+
'<script>'+script.replace(/<\/script/gi,"<\\/script")+'</script></body></html>';
(async()=>{
 const browser=await chromium.launch({channel:"chrome",headless:true,args:["--no-sandbox"]});
 const page=await browser.newPage({viewport:{width:1280,height:1100}});
 const failures=[];page.on("pageerror",e=>failures.push(e.message));
 try{
  await page.setContent(html);
  await page.locator('[data-cf-add="line"]').click();
  const line=page.locator(".cf-obj-line");
  assert.equal(await line.count(),1);
  const start=await line.evaluate(el=>({...el.cf}));
  // Clique na BORDA direita, como um usuário, sem depender de alça.
  let rect=await line.boundingBox();
  await page.mouse.move(rect.x+rect.width-4,rect.y+rect.height/2);
  await page.mouse.down();
  await page.mouse.move(rect.x+rect.width+60,rect.y+rect.height/2,{steps:6});
  await page.mouse.up();
  const after=await line.evaluate(el=>({...el.cf}));
  assert(after.w>start.w+35,"Borda direita não alterou largura: "+start.w+" => "+after.w);
  // Reduz pela borda esquerda.
  rect=await line.boundingBox();
  await page.mouse.move(rect.x+3,rect.y+rect.height/2);
  await page.mouse.down();
  await page.mouse.move(rect.x+25,rect.y+rect.height/2,{steps:5});
  await page.mouse.up();
  const west=await line.evaluate(el=>({...el.cf}));
  assert(west.w<after.w,"Borda esquerda não reduziu largura.");
  assert(west.x>after.x,"Borda esquerda não alterou X.");
  // Move clicando na superfície central, em vez de no ícone.
  rect=await line.boundingBox();
  await page.mouse.move(rect.x+rect.width/2,rect.y+rect.height/2);
  await page.mouse.down();
  await page.mouse.move(rect.x+rect.width/2+30,rect.y+rect.height/2+28,{steps:6});
  await page.mouse.up();
  const moved=await line.evaluate(el=>({...el.cf}));
  assert(moved.y>=west.y+25,"Movimento pelo corpo falhou.");
  assert.equal(await line.locator(".cf-resize-handle:visible").count(),8);
  // Perda de foco para BODY não pode impedir Delete no objeto selecionado.
  await page.evaluate(()=>document.body.focus());
  await page.keyboard.press("Delete");
  assert.equal(await page.locator(".cf-obj-line").count(),0,"Delete ignorado.");
  await page.keyboard.press("Control+z");
  assert.equal(await page.locator(".cf-obj-line").count(),1,"Desfazer ignorado.");
  const restored=page.locator(".cf-obj-line");
  const beforeKeys=await restored.evaluate(el=>({...el.cf}));
  await page.keyboard.press("ArrowRight");
  await page.keyboard.press("Shift+ArrowDown");
  const afterKeys=await restored.evaluate(el=>({...el.cf}));
  assert.equal(afterKeys.x,beforeKeys.x+1,"Seta direita não moveu 1 unidade.");
  assert.equal(afterKeys.y,beforeKeys.y+10,"Shift+seta não moveu 10 unidades.");
  await page.keyboard.press("Control+d");
  assert.equal(await page.locator(".cf-obj-line").count(),2,"Ctrl+D não duplicou.");
  await page.keyboard.press("Backspace");
  assert.equal(await page.locator(".cf-obj-line").count(),1,"Backspace não apagou cópia.");
  // Texto real selecionado: clicar duas vezes deve ativar e depois desativar.
  await page.locator('[data-cf-add="text"]').click();
  const textBox=page.locator(".cf-obj-text").last();
  await textBox.dblclick();
  const editable=textBox.locator(".cf-texto");
  await editable.evaluate(el=>{
    el.innerHTML="<p>Texto de homologação</p>";
    el.dispatchEvent(new InputEvent("input",{bubbles:true,inputType:"insertText"}));
    const node=el.querySelector("p").firstChild;
    const selection=window.getSelection(),range=document.createRange();
    range.setStart(node,0);range.setEnd(node,5);
    selection.removeAllRanges();selection.addRange(range);
  });
  for(const command of ["bold","italic","underline","strikeThrough"]){
    const button=page.locator('[data-cf-format="'+command+'"]');
    await button.click();
    const on=await editable.evaluate((el,cmd)=>{
      const text=Array.from(el.querySelectorAll("*")).find(e=>e.textContent?.startsWith("Texto")&&e.children.length===0);
      const st=text?getComputedStyle(text):null;
      const selected=window.getSelection().toString();
      return {selected,html:el.innerHTML,active:document.queryCommandState(cmd),
       weight:st?.fontWeight,style:st?.fontStyle,decoration:st?.textDecorationLine};
    },command);
    assert.equal(on.selected,"Texto","Seleção foi perdida ao clicar "+command);
    assert.equal(on.active,true,"Comando não ligou: "+command+" "+on.html);
    assert.equal(await button.getAttribute("aria-pressed"),"true","Botão não marcou estado ligado: "+command);
    await button.click();
    const off=await editable.evaluate((el,cmd)=>({
      active:document.queryCommandState(cmd),html:el.innerHTML,selected:window.getSelection().toString()
    }),command);
    assert.equal(off.active,false,"Comando não desligou: "+command+" "+off.html);
    assert.equal(off.selected,"Texto","Seleção foi perdida no segundo clique de "+command);
    assert.equal(await button.getAttribute("aria-pressed"),"false","Botão não voltou ao estado desligado: "+command);
  }
  const align=page.locator('[data-cf-align="justify"]');
  await align.click();
  assert.match(await editable.evaluate(el=>el.innerHTML),/text-align:\s*justify/i);
  await align.click();
  assert.doesNotMatch(await editable.evaluate(el=>el.innerHTML),/text-align:\s*justify/i);
  // Salvar/reabrir após alternar não deve ressuscitar estilos desligados.
  const saved=await page.evaluate(()=>{window.CFCanvas.salvar();return document.getElementById("form:conteudo").value;});
  assert(saved.startsWith("CFCANVAS1\n"));

  assert.equal(failures.length,0,failures.join("; "));
  console.log("Chrome: mouse, 8 alças, Delete, Backspace, setas, Shift+setas, Ctrl+D e Ctrl+Z aprovados.");
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
