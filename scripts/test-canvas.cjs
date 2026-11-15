/* Teste DOM isolado do editor canvas, sem dependências de produção. */
const assert=require("node:assert/strict");
const fs=require("node:fs");
const {JSDOM}=require("jsdom");
const code=fs.readFileSync("src/main/webapp/resources/js/canvas-modelo.js","utf8");
const labels=["cfX","cfY","cfW","cfH","cfLineOrient","cfLineColor","cfLineWeight",
 "cfTableStyle","cfTableHeader","cfSignRole","cfRemove","cfFront","cfBack",
 "cfRowPlus","cfRowMinus","cfColPlus","cfColMinus","cfTextColor","cfFontSize","cfAlign"];
function fixture(initial){
  const html='<!doctype html><html><body><div id="cfCanvas"></div>'
   +'<input type="hidden" id="form:conteudo"><div id="cfProperties" hidden>'
   +'<strong id="cfObjectKind"></strong>'
   +labels.map(id=>'<input id="'+id+'" value="2">').join("")
   +'<div data-cf-panel="line"></div><div data-cf-panel="text"></div>'
   +'<div data-cf-panel="table"></div><div data-cf-panel="sign"></div></div>'
   +'<button type="button" data-cf-add="text"></button>'
   +'<button type="button" data-cf-add="line"></button>'
   +'<button type="button" data-cf-add="table"></button>'
   +'<button type="button" data-cf-add="sign"></button>'
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
const editor=doc.querySelector(".cf-obj-text .cf-texto");
editor.focus();
doc.querySelector(".botao-campo").click();
assert.ok(editor.textContent.includes("{{colaborador.nome}}"),"Inserção de tags no texto visual falhou");
press(reopened,"line");
assert.equal(count(reopened,"line"),2);
assert.equal(reopened.window.CFCanvas.salvar(),true);
console.log("Editor canvas: importação legada, criação, drag, resize, tags, salvamento e reabertura OK.");
dom.window.close();reopened.window.close();
