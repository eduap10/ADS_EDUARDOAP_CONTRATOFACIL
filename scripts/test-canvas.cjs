/* Teste DOM isolado do editor canvas, sem dependências de produção. */
const assert=require("node:assert/strict");
const fs=require("node:fs");
const {JSDOM}=require("jsdom");
const code=fs.readFileSync("src/main/webapp/resources/js/canvas-modelo.js","utf8");
const labels=["cfX","cfY","cfW","cfH","cfLineOrient","cfLineColor","cfLineWeight",
 "cfTableStyle","cfTableHeader","cfSignRole","cfRemove","cfFront","cfBack",
 "cfRowPlus","cfRowMinus","cfColPlus","cfColMinus","cfTextColor","cfFontSize",
 "cfHighlightColor","cfParagraphStyle","cfFontGrow","cfFontShrink","cfFormatReset",
 "cfIndentLess","cfIndentMore"];
function fixture(initial){
  const html='<!doctype html><html><body><div id="cfCanvas" tabindex="0"></div>'
   +'<input type="hidden" id="form:conteudo"><div id="cfProperties" hidden>'
   +'<strong id="cfObjectKind"></strong>'
   +labels.map(id=>'<input id="'+id+'" value="11">').join("")
   +'<select id="cfFontFamily"><option value="Arial">Arial</option><option value="Georgia">Georgia</option><option value="Calibri">Calibri</option></select>'
   +'<button data-cf-format="bold"></button><button data-cf-format="italic"></button><button data-cf-format="underline"></button><button data-cf-format="strikeThrough"></button>'
   +'<button data-cf-align="center"></button><button data-cf-align="justify"></button>'
   +'<button data-cf-list="ul"></button><button data-cf-list="ol"></button>'
   +'<div data-cf-panel="line"></div><div data-cf-panel="text"></div>'
   +'<div data-cf-panel="table"></div><div data-cf-panel="sign"></div></div>'
   +'<button type="button" data-cf-add="text"></button>'
   +'<button type="button" data-cf-add="line"></button>'
   +'<button type="button" data-cf-add="table"></button>'
   +'<button type="button" data-cf-add="sign"></button>'
   +'<button id="cfAddImage"></button><input id="cfImageFile" type="file">'
   +'<button id="modoExemplo"></button><button id="modoTags"></button>'
   +'<input id="buscaCampo"><input id="grupoCampo">'
   +'<div id="catalogoCampos"><div id="listaCampos">'
   +'<section class="grupo-catalogo"><button class="botao-campo" data-tag="{{colaborador.nome}}" data-exemplo="Ana Teste" data-grupo="Identificação" data-descricao="Nome">Nome</button></section>'
   +'</div></div><div id="contadorCampos"></div><div id="descricaoCampo"></div>'
   +'<iframe id="previaModelo"></iframe><div id="cfSaveWarning"></div>'
   +'</body></html>';
  const dom=new JSDOM(html,{url:"http://localhost/",runScripts:"dangerously"});
  const win=dom.window;
  win.TextEncoder=TextEncoder;win.TextDecoder=TextDecoder;win.alert=()=>{};
  Object.defineProperty(win.HTMLElement.prototype,"innerText",{
    configurable:true,get(){return this.textContent;},set(v){this.textContent=v;}
  });
  win.HTMLElement.prototype.setPointerCapture=function(){};
  win.document.getElementById("cfCanvas").getBoundingClientRect=()=>({width:642,height:907,left:0,top:0});
  win.document.getElementById("form:conteudo").value=initial||"";
  win.eval(code);
  return dom;
}
function press(dom,kind){
 const b=dom.window.document.querySelector('[data-cf-add="'+kind+'"]');
 b.click();
}
function count(dom,kind){
 return dom.window.document.querySelectorAll(".cf-obj-"+kind).length;
}
const old="<p>{{visual.linha.horizontal}}</p>"
 +"<p>{{visual.tabela.inicio.2x2.w100.h36}}</p>"
 +"<p>Nome | Cargo</p><p> | </p><p>{{visual.tabela.fim}}</p>"
 +"<p>{{assinatura.colaborador}}</p>";
const dom=fixture(old);
assert.equal(count(dom,"line"),1,"Linha legada precisa virar objeto real na área de edição");
assert.equal(count(dom,"table"),1,"Tabela legada precisa virar grade visual editável");
assert.equal(count(dom,"sign"),1,"Assinatura legada precisa virar objeto");
press(dom,"text");
assert.equal(count(dom,"text"),1);
assert.equal(dom.window.CFCanvas.salvar(),true);
const saved=dom.window.document.getElementById("form:conteudo").value;
assert.ok(saved.startsWith("CFCANVAS1\n"));
assert.match(saved,/\nL\|/);
assert.match(saved,/\nG\|/);
assert.match(saved,/\nS\|/);
assert.match(saved,/\nT\|/);
const reopened=fixture(saved);
assert.equal(count(reopened,"line"),1,"Reabertura perdeu linha");
assert.equal(count(reopened,"table"),1,"Reabertura perdeu tabela");
assert.equal(count(reopened,"sign"),1,"Reabertura perdeu assinatura");
assert.equal(count(reopened,"text"),1,"Reabertura perdeu texto");
// Movimento e redimensionamento com ponteiro, persistidos como coordenadas A4.
const doc=reopened.window.document;
const item=doc.querySelector(".cf-obj-line");
const iniX=item.cf.x,iniY=item.cf.y;
const move=item.querySelector(".cf-move");
move.dispatchEvent(new reopened.window.MouseEvent("pointerdown",{bubbles:true,clientX:100,clientY:100}));
doc.dispatchEvent(new reopened.window.MouseEvent("pointermove",{bubbles:true,clientX:148,clientY:133}));
doc.dispatchEvent(new reopened.window.MouseEvent("pointerup",{bubbles:true,clientX:148,clientY:133}));
assert.equal(item.cf.x,iniX+48);
assert.equal(item.cf.y,iniY+33);
const iniW=item.cf.w,iniH=item.cf.h;
const handle=item.querySelector(".cf-resize");
handle.dispatchEvent(new reopened.window.MouseEvent("pointerdown",{bubbles:true,clientX:200,clientY:200}));
doc.dispatchEvent(new reopened.window.MouseEvent("pointermove",{bubbles:true,clientX:230,clientY:245}));
doc.dispatchEvent(new reopened.window.MouseEvent("pointerup",{bubbles:true,clientX:230,clientY:245}));
assert.equal(item.cf.w,iniW+30);
assert.equal(item.cf.h,iniH+45);
assert.equal(item.querySelectorAll(".cf-resize-handle").length,8);
const area=item.querySelector(".cf-obj-conteudo");
const px=item.cf.x,py=item.cf.y;
area.dispatchEvent(new reopened.window.MouseEvent("pointerdown",{bubbles:true,clientX:100,clientY:100}));
doc.dispatchEvent(new reopened.window.MouseEvent("pointermove",{bubbles:true,clientX:128,clientY:115}));
doc.dispatchEvent(new reopened.window.MouseEvent("pointerup",{bubbles:true,clientX:128,clientY:115}));
assert.equal(item.cf.x,Math.min(px+28,642-item.cf.w),"Objeto não pode sair da folha.");
assert.equal(item.cf.y,Math.min(py+15,907-item.cf.h));
const priorWidth=item.cf.w,priorLeft=item.cf.x;
const handleWest=item.querySelector('[data-dir="w"]');
handleWest.dispatchEvent(new reopened.window.MouseEvent("pointerdown",{bubbles:true,clientX:100,clientY:100}));
doc.dispatchEvent(new reopened.window.MouseEvent("pointermove",{bubbles:true,clientX:115,clientY:100}));
doc.dispatchEvent(new reopened.window.MouseEvent("pointerup",{bubbles:true,clientX:115,clientY:100}));
assert.equal(item.cf.x,priorLeft+15);
assert.equal(item.cf.w,priorWidth-15);
const editor=doc.querySelector(".cf-obj-text .cf-texto");
editor.focus();
doc.querySelector(".botao-campo").click();
assert.ok(editor.textContent.includes("{{colaborador.nome}}"),"Inserção de tags no texto visual falhou");
// Estilo aplicado apenas à seleção, não a todo o documento.
const paragraph=editor.querySelector("p");
const textNode=paragraph.firstChild;
const range=doc.createRange();
range.setStart(textNode,0);
range.setEnd(textNode,Math.min(5,textNode.textContent.length));
const sel=reopened.window.getSelection();sel.removeAllRanges();sel.addRange(range);
editor.dispatchEvent(new reopened.window.MouseEvent("mouseup",{bubbles:true}));
const family=doc.getElementById("cfFontFamily");
family.value="Georgia";
family.dispatchEvent(new reopened.window.Event("change",{bubbles:true}));
assert.match(editor.innerHTML,/font-family:\s*Georgia/i,"Fonte não aplicada à seleção");
const size=doc.getElementById("cfFontSize");size.value="18";
size.dispatchEvent(new reopened.window.Event("change",{bubbles:true}));
assert.match(editor.innerHTML,/font-size:\s*18pt/i,"Tamanho numérico não persistiu");
const highlight=doc.getElementById("cfHighlightColor");highlight.value="#ffee00";
highlight.dispatchEvent(new reopened.window.Event("change",{bubbles:true}));
assert.match(editor.innerHTML,/background-color:\s*rgb\(255, 238, 0\)|background-color:\s*#ffee00/i,"Marca-texto não aplicado");
press(reopened,"line");
assert.equal(count(reopened,"line"),2);
assert.equal(reopened.window.CFCanvas.salvar(),true);
const newLine=Array.from(doc.querySelectorAll(".cf-obj-line")).at(-1);
newLine.focus();
doc.dispatchEvent(new reopened.window.KeyboardEvent("keydown",{key:"Delete",bubbles:true}));
assert.equal(count(reopened,"line"),1);
doc.dispatchEvent(new reopened.window.KeyboardEvent("keydown",{key:"z",ctrlKey:true,bubbles:true}));
assert.equal(count(reopened,"line"),2);
const styleSaved=reopened.window.document.getElementById("form:conteudo").value;
const styleReopened=fixture(styleSaved);
assert.match(styleReopened.window.document.querySelector(".cf-obj-text .cf-texto").innerHTML,/font-family:\s*Georgia/i);
assert.match(styleReopened.window.document.querySelector(".cf-obj-text .cf-texto").innerHTML,/font-size:\s*18pt/i);
styleReopened.window.close();
const png="iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGPkCtjCwMDAxMDAwMDAAAAMKAESBnDZDAAAAABJRU5ErkJggg==";
const imageFixture=fixture("CFCANVAS1\nI|40|60|120|100|image/png|"+png+"\n");
assert.equal(count(imageFixture,"image"),1);
assert.equal(imageFixture.window.CFCanvas.salvar(),true);
const imageSaved=fixture(imageFixture.window.document.getElementById("form:conteudo").value);
assert.equal(count(imageSaved,"image"),1);
assert.match(imageSaved.window.document.querySelector(".cf-obj-image img").src,/^data:image\/png;base64,/);
imageSaved.window.close();imageFixture.window.close();
console.log("Editor canvas: importação legada, criação, drag, resize, tags, salvamento e reabertura OK.");
dom.window.close();reopened.window.close();
