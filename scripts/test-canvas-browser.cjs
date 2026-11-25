/* Smoke E2E real com Chrome, reutilizando JS e CSS do aplicativo. */
const fs=require("node:fs");
const assert=require("node:assert/strict");
const {chromium}=require("playwright-core");
const script=fs.readFileSync("src/main/webapp/resources/js/canvas-modelo.js","utf8");
const css=fs.readFileSync("src/main/webapp/resources/css/estilo.css","utf8");
const ids=["cfX","cfY","cfW","cfH","cfLineOrient","cfLineColor","cfLineWeight",
"cfTableStyle","cfTableHeader","cfSignRole","cfRemove","cfFront","cfBack",
"cfRowPlus","cfRowMinus","cfColPlus","cfColMinus","cfTextColor","cfFontSize",
"cfHighlightColor","cfParagraphStyle","cfFontGrow","cfFontShrink","cfFormatReset"];
const inputs=ids.map(id=>'<input id="'+id+'" value="11">').join("");
const html='<!doctype html><html><head><meta charset="utf-8"><style>'+css+'</style>'+
'<style>body{margin:0;padding:30px;background:#fff}.cf-stage-scroll{width:900px;height:1000px}</style></head><body>'+
'<div class="cf-toolbar-sticky"><select id="cfFontFamily"><option>Arial</option><option>Georgia</option></select>'+inputs+
'<button id="cfIndentLess"></button><button id="cfIndentMore"></button>'+
'<button data-cf-format="bold"></button><button data-cf-format="italic"></button>'+
'<button data-cf-format="underline"></button><button data-cf-format="strikeThrough"></button>'+
'<button data-cf-align="left"></button><button data-cf-align="center"></button>'+
'<button data-cf-align="right"></button><button data-cf-align="justify"></button>'+
'<button data-cf-list="ul"></button><button data-cf-list="ol"></button></div>'+
'<div class="cf-property-slot"><div id="cfProperties" class="cf-properties" hidden><strong id="cfObjectKind"></strong>'+
'<div data-cf-panel="text"></div><div data-cf-panel="line"></div>'+
'<div data-cf-panel="table"></div><div data-cf-panel="sign"></div></div><div class="cf-properties-empty">Selecione um objeto</div></div>'+
'<button data-cf-add="text">Texto</button><button data-cf-add="line">Linha</button>'+
'<button data-cf-add="table">Tabela</button><button data-cf-add="sign">Assinatura</button>'+
'<button id="cfAddImage">Imagem</button><button id="cfAddBackground">Fundo</button><input id="cfImageFile" type="file">'+
 '<select id="cfLayers"></select><input id="cfUseBackground" type="checkbox"><button id="cfFitBackground">Ajustar fundo</button>'+
'<div class="cf-stage-scroll"><div id="cfCanvas" class="cf-canvas-stage" tabindex="0"></div></div>'+
'<input id="form:conteudo" type="hidden">'+
'<div id="catalogoCampos"><div id="listaCampos"><section class="grupo-catalogo">'+
 '<button type="button" draggable="true" class="botao-campo" data-tag="{{colaborador.nome}}" data-exemplo="Ana Silva" data-grupo="Identificação" data-descricao="Nome completo"><span class="nome-campo">Nome completo</span></button>'+
 '<button type="button" draggable="true" class="botao-campo" data-tag="{{personalizada.teste}}" data-exemplo="Valor especial" data-grupo="Personalizados" data-descricao="Tag de teste"><span class="nome-campo">Minha tag de teste</span></button></section></div></div>'+
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
  // Regression: ao selecionar a segunda célula de uma tabela, a barra
  // deve editar aquela célula, e não saltar silenciosamente para a primeira.
  await page.locator('[data-cf-add="table"]').click();
  const grade=page.locator(".cf-obj-table").last();
  await grade.dblclick();
  const celulas=grade.locator(".cf-edit-table td,.cf-edit-table th");
  await celulas.nth(0).evaluate(el=>el.textContent="Primeira célula");
  await celulas.nth(1).evaluate(el=>{
    el.textContent="Segunda célula";
    el.focus();
    const range=document.createRange();range.selectNodeContents(el);
    const sel=window.getSelection();sel.removeAllRanges();sel.addRange(range);
    el.dispatchEvent(new MouseEvent("mouseup",{bubbles:true}));
  });
  await page.locator("#cfTextColor").fill("#2244cc");
  await page.locator("#cfTextColor").press("Tab");
  assert.match(await celulas.nth(1).innerHTML(),/color:/i,
    "A formatação deve ser aplicada à segunda célula em foco.");
  assert.doesNotMatch(await celulas.nth(0).innerHTML(),/color:/i,
    "A formatação foi aplicada na primeira célula por engano.");
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
  // Uma cor e um marca-texto precisam afetar só a palavra selecionada.
  await page.locator("#cfTextColor").fill("#cc2244");
  await page.locator("#cfHighlightColor").fill("#ffee00");
  // Em inputs de texto do fixture, change só dispara quando o foco sai.
  await page.locator("#cfHighlightColor").press("Tab");
  const colorHtml=await editable.innerHTML();
  assert.match(colorHtml,/color:/i,"Cor da fonte não gerou estilo HTML.");
  assert.match(colorHtml,/background-color:/i,"Marca-texto não gerou estilo HTML.");
  assert.equal(await editable.evaluate(()=>window.getSelection().toString()),"Texto",
    "Controles de cor perderam a seleção.");
  await page.waitForTimeout(180);
  assert.match(await page.locator("#previaModelo").getAttribute("srcdoc"),/background-color:/i,
    "Marca-texto não apareceu na prévia.");

  // Reproduz o caso relatado: estilos aninhados e borracha na mesma seleção.
  await page.locator('[data-cf-format="bold"]').click();
  await page.locator('[data-cf-format="italic"]').click();
  await page.locator('[data-cf-format="underline"]').click();
  await page.locator('[data-cf-format="strikeThrough"]').click();
  await page.locator("#cfFormatReset").click();
  const reset=await editable.evaluate(el=>({
    html:el.innerHTML,selected:window.getSelection().toString(),
    bold:document.queryCommandState("bold"),
    italic:document.queryCommandState("italic"),
    underline:document.queryCommandState("underline"),
    strike:document.queryCommandState("strikeThrough"),
    text:el.textContent
  }));
  assert.equal(reset.selected,"Texto","Borracha perdeu a seleção.");
  assert.equal(reset.text,"Texto de homologação","Borracha mudou o conteúdo do texto.");
  assert.equal(reset.bold||reset.italic||reset.underline||reset.strike,false,
    "Borracha manteve um formato de caractere ativo: "+reset.html);
  assert.doesNotMatch(reset.html,/background-color:|color:|<(strong|em|u|s|strike|b|i)([\\s>])/i,
    "Borracha deixou estilos herdados no trecho selecionado: "+reset.html);
  assert.equal(await page.locator('[data-cf-format="bold"]').getAttribute("aria-pressed"),"false");
  // Reposiciona o cursor como um usuário faria antes dos comandos de parágrafo.
  async function caretAtStart(){
    await editable.evaluate(el=>{
      const line=el.querySelector("p,div,li")||el;
      const r=document.createRange();r.selectNodeContents(line);r.collapse(true);
      const selection=window.getSelection();selection.removeAllRanges();selection.addRange(r);
      el.dispatchEvent(new MouseEvent("mouseup",{bubbles:true}));
    });
  }
  for(const [kind,css] of [
    ["center","center"],["right","right"],["left","left"],["justify","justify"]
  ]){
    await caretAtStart();
    const button=page.locator('[data-cf-align="'+kind+'"]');
    await button.click();
    const alignment=await editable.evaluate(el=>{
      const p=el.querySelector("p,div,li")||el;
      return getComputedStyle(p).textAlign;
    });
    assert.equal(alignment,css,"Alinhamento "+kind+" não foi aplicado ao parágrafo: "+await editable.innerHTML());
    assert.equal(await button.getAttribute("aria-pressed"),"true","Botão não mostra alinhamento selecionado: "+kind);
  }
  await page.locator('[data-cf-align="justify"]').click();
  assert.equal(await editable.evaluate(el=>getComputedStyle(el.querySelector("p,div,li")||el).textAlign),"left",
    "Segundo clique em justificar deve restaurar o alinhamento à esquerda.");
  for(const type of ["ul","ol"]){
    await caretAtStart();
    const button=page.locator('[data-cf-list="'+type+'"]');
    await button.click();
    assert.equal(await editable.locator(type+">li").count(),1,
      "Lista "+type+" não foi inserida no editor.");
    assert.equal(await button.getAttribute("aria-pressed"),"true",
      "A lista "+type+" não marcou estado ativo.");
    await button.click();
    assert.equal(await editable.locator(type).count(),0,
      "Segundo clique deve retirar a lista "+type+".");
  }
  await caretAtStart();
  const indent=page.locator("#cfIndentMore");
  await indent.click();
  assert.equal(await editable.evaluate(el=>
    parseInt((el.querySelector("p,div,li")||el).style.marginLeft,10)),24,"Recuo +24px falhou.");
  await indent.click();
  assert.equal(await editable.evaluate(el=>
    parseInt((el.querySelector("p,div,li")||el).style.marginLeft,10)),48,"Segundo recuo falhou.");
  await page.locator("#cfIndentLess").click();
  assert.equal(await editable.evaluate(el=>
    parseInt((el.querySelector("p,div,li")||el).style.marginLeft,10)),24,"Redução do recuo falhou.");
  // Não basta mudar o canvas: o HTML usado na prévia também deve conservar os atributos.
  assert.match(await editable.innerHTML(),/margin-left:\s*24px/);
  // Salvar/reabrir após alternar não deve ressuscitar estilos desligados.
  const saved=await page.evaluate(()=>{window.CFCanvas.salvar();return document.getElementById("form:conteudo").value;});
  assert(saved.startsWith("CFCANVAS1\n"));

  // Regressão do papel timbrado: conteúdo real editável sobre uma imagem
  // que cobre a folha. Testamos a camada, o clique, a reabertura e o desbloqueio.
  const png="iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGPkCtjCwMDAxMDAwMDAAAAMKAESBnDZDAAAAABJRU5ErkJggg==";
  const txt=Buffer.from("<p>Contrato sobre fundo</p>").toString("base64");
  const raw="CFCANVAS1\nT|35|40|530|130|"+txt+"\nI|0|0|642|907|image/png|"+png+"|fundo\n";
  const initial=raw=>html.replace("<script>","<script>document.getElementById('form:conteudo').value="+JSON.stringify(raw)+";");
  const folha=await browser.newPage({viewport:{width:1280,height:1100}});
  folha.on("pageerror",e=>failures.push(e.message));
  await folha.setContent(initial(raw));
  const fundo=folha.locator(".cf-background");
  assert.equal(await fundo.count(),1);
  assert.equal(await fundo.evaluate(el=>getComputedStyle(el).pointerEvents),"none");
  const caixa=folha.locator(".cf-obj-text").first();
  assert(Number(await caixa.evaluate(el=>getComputedStyle(el).zIndex))>Number(await fundo.evaluate(el=>getComputedStyle(el).zIndex)));
  const pos=await fundo.evaluate(el=>[el.cf.x,el.cf.y,el.cf.w,el.cf.h]);
  const folhaAntes=await folha.locator("#cfCanvas").boundingBox();
  await caixa.dblclick({position:{x:45,y:30}});
  assert.equal(await caixa.locator(".cf-texto").getAttribute("contenteditable"),"true","Duplo clique não iniciou a edição.");
  assert.equal((await folha.locator("#cfCanvas").boundingBox()).y,folhaAntes.y,"A seleção deslocou a folha.");
  await caixa.locator(".cf-texto").fill("Conteúdo digitado sobre o papel timbrado");
  await folha.locator("#cfCanvas").dblclick({position:{x:620,y:500}});
  assert.equal(await folha.locator(".cf-obj-text").count(),2,"Fundo bloqueou a criação de texto na folha.");
  assert.deepEqual(await fundo.evaluate(el=>[el.cf.x,el.cf.y,el.cf.w,el.cf.h]),pos);
  const indice=await fundo.evaluate(el=>String(Array.from(el.parentNode.children).indexOf(el)));
  await folha.locator("#cfLayers").selectOption(indice);
  assert.equal(await folha.locator("#cfUseBackground").isChecked(),true);
  assert.equal(await folha.locator("#cfX").isDisabled(),true);
  await folha.locator("#cfUseBackground").uncheck();
  assert.equal(await folha.locator(".cf-background").count(),0);
  assert.equal(await folha.locator("#cfX").isDisabled(),false);
  await folha.locator("#cfUseBackground").check();
  await folha.locator("#cfFitBackground").click();
  assert.deepEqual(await folha.locator(".cf-background").evaluate(el=>[el.cf.x,el.cf.y,el.cf.w,el.cf.h]),[0,0,642,907]);
  const salvo=await folha.evaluate(()=>{window.CFCanvas.salvar();return document.getElementById("form:conteudo").value;});
  assert.match(salvo,/\|fundo\n/);
  await folha.waitForTimeout(180);
  assert.match(await folha.locator("#previaModelo").getAttribute("srcdoc"),/cf-background/);
  const reaberto=await browser.newPage();
  reaberto.on("pageerror",e=>failures.push(e.message));
  await reaberto.setContent(initial(salvo));
  assert.equal(await reaberto.locator(".cf-background").count(),1);
  assert.match(await reaberto.locator(".cf-obj-text").first().innerText(),/Conteúdo digitado/);
  assert.equal(await reaberto.locator(".cf-background").evaluate(el=>getComputedStyle(el).pointerEvents),"none");
  await reaberto.close();await folha.close();

  // Reutiliza CSS e JS reais com marcação representativa do PrimeFaces.
  // O servidor JSF não é iniciado neste teste; a integração HTTP é separada.
  const tema=fs.readFileSync("src/main/webapp/resources/js/tema.js","utf8");
  const ui=await browser.newPage({viewport:{width:1280,height:800},colorScheme:"light"});
  ui.on("pageerror",e=>failures.push(e.message));
  const mock='<!doctype html><html><head><style>'+css+'</style><script>'+tema+'</script></head><body class="cf-auth"><main class="cf-auth-shell"><aside class="cf-auth-brand">ContratoFácil</aside><section class="cf-auth-main"><div class="cf-auth-top"><button type="button" data-cf-theme-toggle="true"><span class="cf-theme-label"></span></button></div><div class="acesso"><section class="painel cf-auth-card"><h2>Entrar</h2><button class="ui-button botao-acesso" type="submit"><span class="ui-button-text">Entrar</span></button></section></div></section></main></body></html>';
  await ui.route("http://cf-test.local/**",route=>route.fulfill({contentType:"text/html",body:mock}));
  await ui.goto("http://cf-test.local/login");
  assert.equal(await ui.locator("html").getAttribute("data-cf-theme"),"light");
  const cor=await ui.locator(".botao-acesso").evaluate(el=>[getComputedStyle(el).backgroundColor,getComputedStyle(el).color]);
  assert.deepEqual(cor,["rgb(23, 111, 169)","rgb(255, 255, 255)"]);
  await ui.locator("[data-cf-theme-toggle]").click();
  assert.equal(await ui.locator("html").getAttribute("data-cf-theme"),"dark");
  assert.equal(await ui.locator("[data-cf-theme-toggle]").getAttribute("aria-pressed"),"true");
  await ui.reload();
  assert.equal(await ui.locator("html").getAttribute("data-cf-theme"),"dark","Preferência perdida ao recarregar.");
  await ui.setViewportSize({width:390,height:844});
  assert(await ui.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth),"Login móvel transbordou.");
  await ui.evaluate(()=>{
    document.body.className="cf-app";
    document.body.innerHTML='<main><section class="painel"><div class="campos"><div><label>Data</label><span class="ui-datepicker"><input class="ui-inputfield"><button class="ui-datepicker-trigger ui-button">D</button></span></div></div><div class="cf-canvas-stage"></div><section class="folha-a4">Documento</section></section></main>';
  });
  const input=await ui.locator(".ui-datepicker input").boundingBox();
  const trigger=await ui.locator(".ui-datepicker-trigger").boundingBox();
  assert(Math.abs(input.y-trigger.y)<2 && trigger.x>=input.x+input.width-2,"Calendário quebrou de linha.");
  assert.equal(await ui.locator(".cf-canvas-stage").evaluate(el=>getComputedStyle(el).backgroundColor),"rgb(255, 255, 255)");
  assert.equal(await ui.locator(".folha-a4").evaluate(el=>getComputedStyle(el).backgroundColor),"rgb(255, 255, 255)");
  await ui.close();
  console.log("UI: contraste, persistência do tema, largura móvel, calendário e folha branca verificados.");
  // Arrastar um campo real deve preservar o identificador original, inclusive
  // na reabertura e na prévia preenchida. A etiqueta não é enviada ao Java.
  const campos=await browser.newPage({viewport:{width:1280,height:1100}});
  campos.on("pageerror",e=>failures.push(e.message));
  await campos.setContent(initial("CFCANVAS1\nT|35|40|530|130|"+Buffer.from("<p>Nome: </p>").toString("base64")+"\n"));
  const nome=campos.locator('.botao-campo[data-tag="{{colaborador.nome}}"]');
  const edit=campos.locator(".cf-obj-text .cf-texto").first();
  await nome.dragTo(edit,{targetPosition:{x:90,y:25}});
  assert.equal(await edit.locator(".cf-tag-chip").count(),1);
  assert.equal(await edit.locator(".cf-tag-chip").getAttribute("data-label"),"Nome completo");
  assert.equal(await edit.locator(".cf-tag-key").textContent(),"{{colaborador.nome}}");
  const extra=campos.locator('.botao-campo[data-tag="{{personalizada.teste}}"]');
  await extra.dragTo(campos.locator("#cfCanvas"),{targetPosition:{x:180,y:440}});
  assert.equal(await campos.locator(".cf-obj-text").count(),2);
  assert.equal(await campos.locator(".cf-tag-chip[data-cf-tag='{{personalizada.teste}}']").count(),1);
  await nome.click();
  assert.equal(await campos.locator(".cf-tag-chip").count(),3,"Clique deixou de inserir tag.");
  const registro=await campos.evaluate(()=>{window.CFCanvas.salvar();return document.getElementById("form:conteudo").value;});
  const decodificado=registro.split("\n").filter(l=>l.startsWith("T|")).map(l=>Buffer.from(l.split("|")[5],"base64").toString("utf8")).join(" ");
  assert.match(decodificado,/\{\{colaborador.nome\}\}/);
  assert.match(decodificado,/\{\{personalizada.teste\}\}/);
  assert.doesNotMatch(decodificado,/cf-tag-chip|data-cf-tag|cf-tag-key/);
  await campos.waitForTimeout(200);
  const previa=await campos.locator("#previaModelo").getAttribute("srcdoc");
  assert.match(previa,/Ana Silva/);assert.match(previa,/Valor especial/);
  await campos.setContent(initial(registro));
  assert.equal(await campos.locator(".cf-tag-chip").count(),3);
  await campos.close();
  console.log("Campos: arrastar, clique, etiquetas, prévia e reabertura aprovados.");
  assert.equal(failures.length,0,failures.join("; "));
  console.log("Chrome: mouse, 8 alças, Delete, Backspace, setas, Shift+setas, Ctrl+D e Ctrl+Z aprovados.");
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
