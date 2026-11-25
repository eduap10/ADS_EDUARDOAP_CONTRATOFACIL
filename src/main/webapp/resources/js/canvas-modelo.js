/* Editor principal de modelos.
 * Cada elemento A4 tem posição/dimensão e conteúdo. Ao salvar, serializamos
 * para CFCANVAS1; a validação e o preenchimento são refeitos no servidor.
 * O navegador apenas edita e mostra prévia; nunca é a fonte de confiança.
 */
(function () {
"use strict";
const W=642,H=907,PREFIX="CFCANVAS1\n",MAX=100;
const $=id=>document.getElementById(id);
const stage=$("cfCanvas"),hidden=$("form:conteudo");
if(!stage||!hidden)return;
const LABEL={colaborador:"Colaborador",gestor:"Gestor",testemunha1:"Testemunha 1",testemunha2:"Testemunha 2",outro:"Outro participante"};
const clamp=(v,min,max)=>Math.max(min,Math.min(max,Math.round(Number(v)||0)));
let selected=null,drag=null,focusEditable=null,storedRange=null,examplesOn=true,timer;
let loading=true,history=[],historyIndex=-1,historyTask,clipboard=null,lastSelectionIndex=-1;
function enc(txt){const b=new TextEncoder().encode(String(txt));let o="";for(let i=0;i<b.length;i+=8192)o+=String.fromCharCode(...b.slice(i,i+8192));return btoa(o);}
function dec(txt){return new TextDecoder().decode(Uint8Array.from(atob(txt),c=>c.charCodeAt(0)));}
// Rótulos são apenas visuais: o servidor continua recebendo {{chave}}.
 const tagPattern=/^\{\{[a-zA-Z][a-zA-Z0-9_.]*\}\}$/;
 function campoTag(tag){return Array.from(document.querySelectorAll("#catalogoCampos .botao-campo")).find(b=>b.dataset.tag===tag);}
 function etiqueta(tag){
  const campo=campoTag(tag);if(!campo)return document.createTextNode(tag);
  const chip=document.createElement("span");chip.className="cf-tag-chip";chip.dataset.cfTag=tag;
  chip.dataset.label=campo.querySelector(".nome-campo")?.textContent.trim()||tag;
  chip.title=chip.dataset.label+" · "+tag;chip.contentEditable="false";chip.setAttribute("aria-label",chip.dataset.label);
  const key=document.createElement("span");key.className="cf-tag-key";key.textContent=tag;chip.append(key);return chip;
 }
 function mostrarTags(edit){
  const walk=document.createTreeWalker(edit,NodeFilter.SHOW_TEXT),nodes=[];
  while(walk.nextNode())nodes.push(walk.currentNode);
  for(const n of nodes){
   if(n.parentElement?.closest(".cf-tag-chip,script,style"))continue;
   const value=n.nodeValue,matches=Array.from(value.matchAll(/\{\{[a-zA-Z][a-zA-Z0-9_.]*\}\}/g));
   if(!matches.length)continue;
   const frag=document.createDocumentFragment();let pos=0;
   for(const m of matches){frag.append(value.slice(pos,m.index),etiqueta(m[0]));pos=m.index+m[0].length;}
   frag.append(value.slice(pos));n.replaceWith(frag);
  }
 }
 function rawTags(container){
  container.querySelectorAll(".cf-tag-chip").forEach(chip=>{
   const tag=chip.dataset.cfTag;chip.replaceWith(document.createTextNode(tagPattern.test(tag||"")?tag:chip.textContent));
  });return container;
 }
 function textoCelula(cell){return rawTags(cell.cloneNode(true)).textContent.replace(/[\r\n|\u001f]/g," ").slice(0,500);}
 function htmlTexto(edit){return rawTags(edit.cloneNode(true)).innerHTML;}
 function rect(el,s){el.style.left=s.x+"px";el.style.top=s.y+"px";el.style.width=s.w+"px";el.style.height=s.h+"px";}
function object(tipo,extra={}){
 if(stage.querySelectorAll(".cf-canvas-obj").length>=MAX){alert("Limite de 100 elementos por página.");return null;}
 const n=stage.querySelectorAll(".cf-canvas-obj").length;
 const dimensions={text:[530,130],line:[400,42],table:[510,140],sign:[340,100],image:[300,180]}[tipo];
 const s={tipo,x:35+n*8%150,y:35+n*75%760,w:dimensions[0],h:dimensions[1],...extra};
 s.w=clamp(s.w,40,W);s.h=clamp(s.h,20,H);s.x=clamp(s.x,0,W-s.w);s.y=clamp(s.y,0,H-s.h);
 if(tipo==="text")s.html=s.html??"<p>Digite seu texto aqui...</p>";
 if(tipo==="line"){s.orient=["horizontal","vertical","diagonal"].includes(s.orient)?s.orient:"horizontal";
   s.weight=clamp(s.weight||2,1,12);s.color=/^[a-fA-F0-9]{6}$/.test(s.color||"")?s.color:"334155";}
 if(tipo==="table"){
   s.rows=clamp(s.rows||2,1,12);s.cols=clamp(s.cols||2,1,8);
   s.header=s.header!==false;s.style=["grade","listrado","simples"].includes(s.style)?s.style:"grade";
   s.cells=Array.from({length:s.rows},(_,r)=>Array.from({length:s.cols},(_,c)=>s.cells?.[r]?.[c]??""));
 }
 if(tipo==="sign")s.role=Object.hasOwn(LABEL,s.role)?s.role:"colaborador";
 if(tipo==="image"){s.src=/^data:image\/(?:png|jpeg);base64,[A-Za-z0-9+/=]+$/.test(s.src||"")?s.src:"";s.background=s.background===true;}
 return s;
}
// O fundo é uma camada persistente. O clique atravessa a imagem.
function applyLayer(el){
 const fundo=el.cf.tipo==="image"&&el.cf.background===true;
 el.classList.toggle("cf-background",fundo);el.tabIndex=fundo?-1:0;
 if(fundo&&!loading)stage.prepend(el);
}
function layers(){
 const list=$("cfLayers");if(!list)return;
 const nomes={text:"Texto",line:"Linha",table:"Tabela",sign:"Assinatura",image:"Imagem"};
 list.replaceChildren(new Option("Selecionar objeto ou fundo…",""));
 Array.from(stage.children).forEach((el,i)=>{
  const nome=el.cf.background?"Fundo (bloqueado)":nomes[el.cf.tipo];
  list.add(new Option((i+1)+" · "+nome,String(i)));
 });
 list.value=selected?String(Array.from(stage.children).indexOf(selected)):"";
}
function add(s,select=true){
 if(!s)return null;
 const el=document.createElement("div");el.className="cf-canvas-obj cf-obj-"+s.tipo;
 el.cf=s;el.tabIndex=0;el.dataset.role=s.role||"";rect(el,s);
 const move=document.createElement("button"),resize=document.createElement("button"),inside=document.createElement("div");
 move.type="button";move.className="cf-move";move.textContent="⠿";move.title="Arrastar";
 resize.type="button";resize.className="cf-resize";resize.textContent="↘";resize.title="Redimensionar";
 inside.className="cf-obj-conteudo";el.append(move,inside,resize);
 for(const dir of ["nw","n","ne","e","se","s","sw","w"]){
  const handle=document.createElement("button");handle.type="button";handle.className="cf-resize-handle";
  handle.dataset.dir=dir;handle.title="Redimensionar pelo lado "+dir;handle.setAttribute("aria-label",handle.title);
  handle.addEventListener("pointerdown",e=>beginPointer(e,el,"size",dir));el.append(handle);
 }
 stage.append(el);applyLayer(el);render(el);
 el.addEventListener("pointerdown",e=>{
  if(e.target.closest(".cf-resize-handle,.cf-move,.cf-resize"))return;
  if(el.classList.contains("cf-editing")&&e.target.closest('[contenteditable="true"]')&&!e.altKey){
    choose(el);return;
  }
  const edge=edgeDirection(e,el);
  beginPointer(e,el,edge?"size":"move",edge||"se");
 });
 el.addEventListener("pointermove",e=>{
  if(drag || el.classList.contains("cf-editing"))return;
  const edge=edgeDirection(e,el);
  el.style.cursor=edge?cursorForEdge(edge):"move";
 });
 move.addEventListener("pointerdown",e=>beginPointer(e,el,"move"));
 resize.addEventListener("pointerdown",e=>beginPointer(e,el,"size","se"));
 el.addEventListener("dblclick",e=>{
  if(["text","table"].includes(s.tipo))enterEditing(el,e.target);
 });
 if(select)choose(el);else layers();
 updatePreview();if(!loading)markHistory();return el;
}
function render(el){
 const s=el.cf,area=el.querySelector(".cf-obj-conteudo");area.textContent="";
 if(s.tipo==="text"){
  const div=document.createElement("div");div.className="cf-texto";div.contentEditable=el.classList.contains("cf-editing")?"true":"false";div.tabIndex=0;div.innerHTML=s.html;mostrarTags(div);
  area.append(div);
  div.addEventListener("focus",()=>{focusEditable=div;choose(el);});
  div.addEventListener("click",()=>{focusEditable=div;choose(el);remember();});
  div.addEventListener("keyup",remember);div.addEventListener("mouseup",remember);
  div.addEventListener("input",()=>{s.html=htmlTexto(div);updatePreview();scheduleHistory();});
 }else if(s.tipo==="line"){
  const line=document.createElement("div");line.className="cf-real-line cf-"+s.orient;
  line.style.borderColor="#"+s.color;line.style.borderWidth=s.weight+"px";area.append(line);
 }else if(s.tipo==="sign"){
  const line=document.createElement("div");line.className="cf-sign-line";area.append(line);
  const label=document.createElement("div");label.className="cf-sign-name";
  label.textContent=LABEL[s.role]+": [preenchido na emissão]";area.append(label);
 }else if(s.tipo==="table"){
  const t=document.createElement("table");t.className="cf-edit-table cf-tabela-"+s.style;
  const body=document.createElement("tbody");
  for(let r=0;r<s.rows;r++){
    const tr=document.createElement("tr");
    for(let c=0;c<s.cols;c++){
      const td=document.createElement(s.header&&r===0?"th":"td");td.contentEditable=el.classList.contains("cf-editing")?"true":"false";td.tabIndex=0;
      td.dataset.r=r;td.dataset.c=c;td.textContent=s.cells[r][c];mostrarTags(td);
      td.addEventListener("focus",()=>{focusEditable=td;choose(el);});
      td.addEventListener("keyup",remember);td.addEventListener("mouseup",remember);
      td.addEventListener("input",()=>{s.cells[r][c]=textoCelula(td);updatePreview();scheduleHistory();});
      tr.append(td);
    }
    body.append(tr);
  }
  t.append(body);area.append(t);
 }else if(s.tipo==="image"){
  const img=document.createElement("img");img.src=s.src||"";img.alt="Imagem inserida no documento";img.draggable=false;area.append(img);
 }
}
function enterEditing(el,target){
 if(!el || !["text","table"].includes(el.cf.tipo))return;
 choose(el);el.classList.add("cf-editing");
 el.querySelectorAll(".cf-texto,.cf-edit-table td,.cf-edit-table th")
   .forEach(c=>c.contentEditable="true");
 const box=target?.closest?.(".cf-texto,.cf-edit-table td,.cf-edit-table th")||
   el.querySelector(".cf-texto,.cf-edit-table td,.cf-edit-table th");
 focusEditable=box||null;storedRange=null;if(box){box.focus();remember();ribbonState();}
}
function exitEditing(){
 if(!selected)return;
 selected.classList.remove("cf-editing");
 selected.querySelectorAll(".cf-texto,.cf-edit-table td,.cf-edit-table th")
   .forEach(c=>c.contentEditable="false");
 focusEditable=null;storedRange=null;selected.focus({preventScroll:true});ribbonState();
}
function choose(el){
 if(selected && selected!==el){
  selected.classList.remove("cf-selected","cf-editing");
  selected.querySelectorAll(".cf-texto,.cf-edit-table td,.cf-edit-table th")
    .forEach(c=>c.contentEditable="false");
  storedRange=null;
 }
 selected=el;if(selected){
  selected.classList.add("cf-selected");
  // Se o foco veio de uma célula específica, não devemos voltar para a primeira.
  // Recalcular a célula em todos os cliques fazia a formatação atingir outro local.
  const focoNoObjeto=focusEditable?.isConnected && selected.contains(focusEditable);
  focusEditable=focoNoObjeto?focusEditable:
   selected.cf.tipo==="text"?selected.querySelector(".cf-texto"):
   selected.cf.tipo==="table"?selected.querySelector(".cf-edit-table td,.cf-edit-table th"):null;
 }
 toolbar();
 if(typeof ribbonState==="function")ribbonState();
}
function toolbar(){
 layers();
 $("cfProperties").hidden=!selected;if(!selected)return;
 const s=selected.cf;
 $("cfObjectKind").textContent={text:"Texto",line:"Linha",table:"Tabela",sign:"Assinatura",image:"Imagem"}[s.tipo];
 for(const key of ["x","y","w","h"]){const input=$("cf"+key.toUpperCase());input.value=s[key];input.disabled=Boolean(s.background);}
 if($("cfUseBackground"))$("cfUseBackground").checked=Boolean(s.background);
 if($("cfFitBackground"))$("cfFitBackground").disabled=!s.background;
 $("cfFront").disabled=Boolean(s.background);$("cfBack").disabled=Boolean(s.background);
 document.querySelectorAll("[data-cf-panel]").forEach(node=>node.hidden=node.dataset.cfPanel!==s.tipo);
 if(s.tipo==="line"){$("cfLineColor").value="#"+s.color;$("cfLineWeight").value=s.weight;$("cfLineOrient").value=s.orient;}
 if(s.tipo==="table"){$("cfTableStyle").value=s.style;$("cfTableHeader").checked=s.header;}
 if(s.tipo==="sign")$("cfSignRole").value=s.role;
}
function change(props,redraw=true){
 if(!selected||selected.cf.background)return;
 const s=selected.cf;Object.assign(s,props);
 s.w=clamp(s.w,40,W);s.h=clamp(s.h,20,H);s.x=clamp(s.x,0,W-s.w);s.y=clamp(s.y,0,H-s.h);
 rect(selected,s);if(redraw)render(selected);toolbar();updatePreview();markHistory();
}
// Também considera os 12 pixels internos de cada borda como área de redimensionamento,
// sem depender de acertar visualmente uma pequena alça.
function edgeDirection(e,el){
 const r=el.getBoundingClientRect();
 if(!r.width || !r.height || e.clientX==null || e.clientY==null)return null;
 const tolerance=Math.max(8,Math.min(14,r.width/4,r.height/4));
 const x=e.clientX-r.left,y=e.clientY-r.top;
 if(x<0||y<0||x>r.width||y>r.height)return null;
 const horiz=x<=tolerance?"w":x>=r.width-tolerance?"e":"";
 const vert=y<=tolerance?"n":y>=r.height-tolerance?"s":"";
 return vert+horiz||null;
}
function cursorForEdge(dir){
 return ["nw","se"].includes(dir)?"nwse-resize":
   ["ne","sw"].includes(dir)?"nesw-resize":
   ["n","s"].includes(dir)?"ns-resize":"ew-resize";
}
let canvasFocus=false;
function beginPointer(e,el,action,dir="se"){
 if(el.cf.background||e.button!=null&&e.button!==0)return;
 e.preventDefault();e.stopPropagation();
 choose(el);
 if(el.classList.contains("cf-editing"))exitEditing();
 canvasFocus=true;
 el.focus({preventScroll:true});
 drag={id:e.pointerId,el,action,dir,mouseX:e.clientX,mouseY:e.clientY,moved:false,
  start:{x:el.cf.x,y:el.cf.y,w:el.cf.w,h:el.cf.h}};
 // Captura no objeto (não na alça): continua funcionando quando o ponteiro sai da borda.
 try{if(e.pointerId!=null)el.setPointerCapture(e.pointerId);}catch(_){}
}
function pointerMoved(e){
 if(!drag||drag.id!==e.pointerId)return;
 const scale=stage.getBoundingClientRect().width/W;
 if(!Number.isFinite(scale)||scale<=0)return;
 const dx=(e.clientX-drag.mouseX)/scale,dy=(e.clientY-drag.mouseY)/scale;
 if(Math.abs(dx)+Math.abs(dy)<2&&!drag.moved)return;
 drag.moved=true;
 const d=drag.el.cf,s=drag.start;
 if(drag.action==="move"){
  d.x=clamp(s.x+dx,0,W-d.w);
  d.y=clamp(s.y+dy,0,H-d.h);
 }else{
  const dir=drag.dir;
  if(dir.includes("e"))d.w=clamp(s.w+dx,40,W-s.x);
  if(dir.includes("s"))d.h=clamp(s.h+dy,20,H-s.y);
  if(dir.includes("w")){
   d.x=clamp(s.x+dx,0,s.x+s.w-40);d.w=s.w+s.x-d.x;
  }
  if(dir.includes("n")){
   d.y=clamp(s.y+dy,0,s.y+s.h-20);d.h=s.h+s.y-d.y;
  }
 }
 rect(drag.el,d);toolbar();
}
function endPointer(e){
 if(!drag || e?.pointerId!=null && drag.id!==e.pointerId)return;
 const activeDrag=drag;drag=null;
 try{if(activeDrag.id!=null)activeDrag.el.releasePointerCapture(activeDrag.id);}catch(_){}
 if(activeDrag.moved){updatePreview();markHistory();}
 if(activeDrag.el.isConnected && !activeDrag.el.classList.contains("cf-editing")){
  canvasFocus=true;
  activeDrag.el.focus({preventScroll:true});
 }
}
window.addEventListener("pointermove",pointerMoved,true);
window.addEventListener("pointerup",endPointer,true);
window.addEventListener("pointercancel",endPointer,true);
window.addEventListener("blur",()=>endPointer());
stage.addEventListener("pointerdown",e=>{
 canvasFocus=true;
 if(e.target!==stage)return;
 if(selected){
  selected.classList.remove("cf-selected","cf-editing");
  selected.querySelectorAll('[contenteditable="true"]').forEach(el=>el.contentEditable="false");
 }
 selected=null;focusEditable=null;storedRange=null;toolbar();
 stage.focus({preventScroll:true});
});
document.addEventListener("pointerdown",e=>{
 if(!stage.contains(e.target)){
  // A barra pode manter a seleção, mas teclas de remoção não devem afetar o documento
  // quando o usuário trabalha nos filtros, formulários ou em outros comandos.
  canvasFocus=false;
 }
},true);
stage.addEventListener("dblclick",e=>{
 if(e.target!==stage)return;
 const bounds=stage.getBoundingClientRect(),scale=bounds.width/W;
 const x=clamp((e.clientX-bounds.left)/scale,0,W-160),y=clamp((e.clientY-bounds.top)/scale,0,H-80);
 const el=add(object("text",{x,y,w:clamp(W-x-16,160,530),h:120,html:"<p></p>"}));
 if(el)enterEditing(el);
});
for(const key of ["x","y","w","h"])$("cf"+key.toUpperCase()).addEventListener("change",e=>change({[key]:Number(e.target.value)},false));
$("cfLineOrient").addEventListener("change",e=>change({orient:e.target.value}));
$("cfLineColor").addEventListener("input",e=>change({color:e.target.value.slice(1)}));
$("cfLineWeight").addEventListener("change",e=>change({weight:clamp(e.target.value,1,12)}));
$("cfTableStyle").addEventListener("change",e=>change({style:e.target.value}));
$("cfTableHeader").addEventListener("change",e=>change({header:e.target.checked}));
$("cfSignRole").addEventListener("change",e=>change({role:e.target.value}));
$("cfRemove").addEventListener("click",removeSelected);
$("cfFront").addEventListener("click",()=>{if(selected&&!selected.cf.background){stage.append(selected);layers();updatePreview();markHistory();}});
 $("cfBack").addEventListener("click",()=>{if(selected&&!selected.cf.background){
  const first=Array.from(stage.children).find(el=>!el.cf.background);
  if(first&&first!==selected)stage.insertBefore(selected,first);
  layers();updatePreview();markHistory();
 }});
 $("cfLayers")?.addEventListener("change",e=>{
  if(e.target.value===""){choose(null);focusEditable=null;storedRange=null;return;}
  const el=stage.children[Number(e.target.value)];if(el)choose(el);
 });
 $("cfUseBackground")?.addEventListener("change",e=>{
  if(selected?.cf.tipo!=="image")return;
  selected.cf.background=e.target.checked;
  applyLayer(selected);toolbar();updatePreview();markHistory();
 });
 $("cfFitBackground")?.addEventListener("click",()=>{
  if(!selected?.cf.background)return;
  Object.assign(selected.cf,{x:0,y:0,w:W,h:H});rect(selected,selected.cf);
  toolbar();updatePreview();markHistory();
 });
 $("cfTogglePreview")?.addEventListener("click",e=>{
  const wrapper=stage.closest(".editor-duas-colunas");if(!wrapper)return;
  const hidden=wrapper.classList.toggle("cf-preview-hidden");
  e.currentTarget.setAttribute("aria-expanded",String(!hidden));
  e.currentTarget.textContent=hidden?"Mostrar prévia":"Ocultar prévia";
 });
function grid(rows,cols){
 if(!selected||selected.cf.tipo!=="table")return;
 const s=selected.cf;rows=clamp(rows,1,12);cols=clamp(cols,1,8);
 const cells=Array.from({length:rows},(_,r)=>Array.from({length:cols},(_,c)=>s.cells?.[r]?.[c]||""));
 change({rows,cols,cells});
}
$("cfRowPlus").addEventListener("click",()=>grid(selected.cf.rows+1,selected.cf.cols));
$("cfRowMinus").addEventListener("click",()=>grid(selected.cf.rows-1,selected.cf.cols));
$("cfColPlus").addEventListener("click",()=>grid(selected.cf.rows,selected.cf.cols+1));
$("cfColMinus").addEventListener("click",()=>grid(selected.cf.rows,selected.cf.cols-1));
document.querySelectorAll("[data-cf-add]").forEach(b=>b.addEventListener("click",()=>add(object(b.dataset.cfAdd))));
function removeSelected(){
 if(!selected)return;
 lastSelectionIndex=Array.from(stage.children).indexOf(selected);
 selected.remove();selected=null;focusEditable=null;storedRange=null;
 canvasFocus=true;toolbar();stage.focus();updatePreview();markHistory();
}
function markHistory(){
 if(loading)return;
 const serialized=serialize();
 if(history[historyIndex]===serialized)return;
 history=history.slice(0,historyIndex+1);history.push(serialized);
 if(history.length>20)history.shift();historyIndex=history.length-1;
}
function scheduleHistory(){clearTimeout(historyTask);historyTask=setTimeout(markHistory,400);}
function restoreHistory(step){
 clearTimeout(historyTask);
 const now=serialize();if(history[historyIndex]!==now)markHistory();
 const index=historyIndex+step;if(index<0||index>=history.length)return;
 const selectionIndex=selected ? Array.from(stage.children).indexOf(selected):lastSelectionIndex;
 historyIndex=index;const prior=loading;loading=true;
 stage.replaceChildren();selected=null;focusEditable=null;storedRange=null;
 loadSaved(history[index]);loading=prior;
 const restored=stage.children[Math.min(Math.max(selectionIndex,0),stage.children.length-1)];
 if(restored){
  choose(restored);restored.focus({preventScroll:true});
  lastSelectionIndex=Array.from(stage.children).indexOf(restored);
 }else{
  toolbar();stage.focus({preventScroll:true});
 }
 canvasFocus=true;preview();
}
function duplicateSelected(item=selected){
 if(!item)return;
 const data=JSON.parse(JSON.stringify(item.cf));
 data.x=Math.min(W-data.w,data.x+16);data.y=Math.min(H-data.h,data.y+16);
 return add(object(data.tipo,data));
}
document.addEventListener("keydown",e=>{
 const target=e.target;
 const active=document.activeElement;
 const editingTarget=target?.closest?.('input,textarea,select,[contenteditable="true"],[role="textbox"]')||
  active?.closest?.('input,textarea,select,[contenteditable="true"],[role="textbox"]');
 if(!canvasFocus)return;
 if(e.key==="Escape"&&selected?.classList.contains("cf-editing")){
   e.preventDefault();exitEditing();return;
 }
 // Nunca interceptar Delete/backspace enquanto digita.
 if(selected?.classList.contains("cf-editing")||editingTarget)return;
 const mod=e.ctrlKey||e.metaKey;
 if(mod&&(e.key.toLowerCase()==="z"||e.key.toLowerCase()==="y")){
   e.preventDefault();restoreHistory(e.key.toLowerCase()==="z"&&!e.shiftKey?-1:1);return;
 }
 if(!selected||selected.cf.background)return;
 if(mod&&e.key.toLowerCase()==="d"){e.preventDefault();duplicateSelected();return;}
 if(mod&&e.key.toLowerCase()==="c"){e.preventDefault();clipboard=JSON.parse(JSON.stringify(selected.cf));return;}
 if(mod&&e.key.toLowerCase()==="v"&&clipboard){
   e.preventDefault();duplicateSelected({cf:clipboard});return;
 }
 if(e.key==="Delete"||e.key==="Backspace"){e.preventDefault();removeSelected();return;}
 if((e.key==="Enter"||e.key==="F2")&&["text","table"].includes(selected.cf.tipo)){
   e.preventDefault();enterEditing(selected);return;
 }
 const delta={ArrowLeft:[-1,0],ArrowRight:[1,0],ArrowUp:[0,-1],ArrowDown:[0,1]}[e.key];
 if(delta){
   e.preventDefault();const unit=e.shiftKey?10:1;
   change({x:selected.cf.x+delta[0]*unit,y:selected.cf.y+delta[1]*unit},false);
 }
});

let inserirFundo=false;
 $("cfAddImage")?.addEventListener("click",()=>{inserirFundo=false;$("cfImageFile")?.click();});
 $("cfAddBackground")?.addEventListener("click",()=>{inserirFundo=true;$("cfImageFile")?.click();});
$("cfImageFile")?.addEventListener("change",async e=>{
 const file=e.target.files?.[0],background=inserirFundo;e.target.value="";inserirFundo=false;
 if(!file)return;
 if(!["image/png","image/jpeg"].includes(file.type)||file.size>8_000_000){
   alert("Selecione PNG ou JPEG de até 8 MB.");return;
 }
 try{
   const uri=await new Promise((resolve,reject)=>{
     const reader=new FileReader();reader.onload=()=>resolve(reader.result);
     reader.onerror=()=>reject(new Error("Falha na leitura."));reader.readAsDataURL(file);
   });
   const img=await new Promise((resolve,reject)=>{
     const pic=new Image();pic.onload=()=>resolve(pic);
     pic.onerror=()=>reject(new Error("Imagem inválida."));pic.src=uri;
   });
   const ratio=Math.min(1,1200/Math.max(img.naturalWidth,img.naturalHeight));
   const canvas=document.createElement("canvas");
   canvas.width=Math.round(img.naturalWidth*ratio);canvas.height=Math.round(img.naturalHeight*ratio);
   if(!canvas.width||!canvas.height)throw new Error("Dimensões inválidas.");
   const ctx=canvas.getContext("2d");if(!ctx)throw new Error("Canvas de imagem indisponível.");
   ctx.drawImage(img,0,0,canvas.width,canvas.height);
   let result=canvas.toDataURL(file.type==="image/png"?"image/png":"image/jpeg",0.82);
   if(result.length>350000){
     ctx.globalCompositeOperation="destination-over";ctx.fillStyle="#fff";
     ctx.fillRect(0,0,canvas.width,canvas.height);ctx.globalCompositeOperation="source-over";
     result=canvas.toDataURL("image/jpeg",0.72);
   }
   if(result.length>350000)throw new Error("Reduza a resolução da imagem.");
   const factor=340/Math.max(img.naturalWidth,img.naturalHeight);
   const w=clamp(img.naturalWidth*factor,60,420),h=clamp(img.naturalHeight*factor,40,420);
   add(object("image",background?{x:0,y:0,w:W,h:H,src:result,background:true}:{x:60,y:110,w,h,src:result}));
 }catch(err){alert("Falha ao inserir imagem: "+err.message);}
});

// A faixa de formatação opera sobre a seleção DOM (não sobre o bloco inteiro).
// O intervalo é lembrado antes de o usuário tocar na barra, pois os selects
// e campos de cores retiram naturalmente o foco do contenteditable.
const FONTS=new Set(Array.from($("cfFontFamily").options).map(o=>o.value));
const safeFont=name=>FONTS.has(name)?name:"Arial";
function remember(){
 const sel=window.getSelection();
 if(sel?.rangeCount&&focusEditable?.contains(sel.anchorNode)
     && focusEditable?.contains(sel.focusNode)){
   storedRange=sel.getRangeAt(0).cloneRange();
 }
}
document.addEventListener("selectionchange",()=>{
 const sel=window.getSelection();
 if(sel?.rangeCount && focusEditable?.contains(sel.anchorNode)
     && focusEditable?.contains(sel.focusNode)) remember();
});
function editableReady(){
 if(focusEditable?.isConnected && selected?.contains(focusEditable)) return focusEditable;
 if(!selected || !["text","table"].includes(selected.cf.tipo)) return null;
 focusEditable=selected.querySelector(".cf-texto,[contenteditable=true]");
 storedRange=null;
 return focusEditable;
}
function getRange(edit){
 const sel=window.getSelection();
 let r=null;
 if(storedRange && edit.contains(storedRange.commonAncestorContainer)) r=storedRange.cloneRange();
 else if(sel?.rangeCount && edit.contains(sel.getRangeAt(0).commonAncestorContainer))
   r=sel.getRangeAt(0).cloneRange();
 return r;
}
function restoreRange(edit,r){
 if(!r)return;
 edit.focus();
 try{
   const sel=window.getSelection();
   sel.removeAllRanges();sel.addRange(r);
   storedRange=r.cloneRange();
 }catch(e){storedRange=null;}
}
function syncFormatted(edit){
 if(!selected)return;
 if(selected.cf.tipo==="text"){
   selected.cf.html=htmlTexto(selected.querySelector(".cf-texto"));
 }else if(selected.cf.tipo==="table"){
   selected.querySelectorAll("tr").forEach((tr,r)=>Array.from(tr.children).forEach((cell,c)=>{
     selected.cf.cells[r][c]=textoCelula(cell);
   }));
 }
 updatePreview();scheduleHistory();
}
function applyInline(property,value){
 const edit=editableReady();if(!edit)return;
 let r=getRange(edit);
 if(!r || r.collapsed){
   // Sem texto selecionado, modifica os parágrafos da caixa inteira.
   const blocks=Array.from(edit.children).filter(el=>el.matches("p,div,ul,ol,li,h1,h2,h3"));
   const targets=blocks.length?blocks:[edit];
   for(const el of targets){
     // Estilo fica no HTML persistido, não apenas no contêiner de edição.
     if(el===edit){
       const span=document.createElement("span");
       while(edit.firstChild)span.append(edit.firstChild);
       edit.append(span);span.style.setProperty(property,value);
     }else el.style.setProperty(property,value);
   }
   syncFormatted(edit);return;
 }
 const wrapper=document.createElement("span");
 wrapper.style.setProperty(property,value);
 try{
   const frag=r.extractContents();
   wrapper.append(frag);
   r.insertNode(wrapper);
   const next=document.createRange();next.selectNodeContents(wrapper);
   restoreRange(edit,next);
 }catch(e){
   console.warn("Não foi possível formatar a seleção",e);
 }
 syncFormatted(edit);
}
/*
 * Formatação de parágrafo
 *
 * O conteúdo da folha é um contenteditable. Clicar na barra leva o foco para o
 * botão, por isso recuperamos o intervalo (Range) salvo antes de editar.
 * Alinhamento, listas e recuo são comandos de parágrafo: uma palavra selecionada
 * já é suficiente para afetar o parágrafo inteiro, sem criar outro objeto A4.
 */
function selectionForParagraph(){
 const edit=editableReady();if(!edit||!selected)return null;
 const range=getRange(edit);
 if(!selected.classList.contains("cf-editing"))enterEditing(selected,edit);
 if(range && edit.contains(range.commonAncestorContainer)){
   restoreRange(edit,range);
 }else{
   edit.focus();
   const atEnd=document.createRange();
   atEnd.selectNodeContents(edit);atEnd.collapse(false);
   restoreRange(edit,atEnd);
 }
 return edit;
}
function currentParagraph(){
 const edit=editableReady();if(!edit)return null;
 const r=getRange(edit);
 let node=r?.startContainer||edit;
 if(node.nodeType===Node.TEXT_NODE)node=node.parentElement;
 const block=node?.closest?.("p,div,li,h1,h2,h3,blockquote");
 if(block && edit.contains(block) && block!==edit)return block;
 return edit.querySelector("p,div,h1,h2,h3,li,blockquote")||edit;
}
function executeParagraphCommand(command){
 const edit=selectionForParagraph();if(!edit)return false;
 const succeeded=document.execCommand(command,false,null);
 if(succeeded){
   remember();
   syncFormatted(edit);
   ribbonState();
 }else{
   console.warn("O navegador não executou o comando de parágrafo:",command);
 }
 return succeeded;
}
function paragraphStyle(property,value){
 if(property!=="text-align")return;
 const commands={
   left:"justifyLeft",center:"justifyCenter",
   right:"justifyRight",justify:"justifyFull"
 };
 const command=commands[value];
 if(!command)return;
 const edit=selectionForParagraph();if(!edit)return;
 // Um segundo clique no alinhamento ativo restaura o padrão (esquerda).
 const active=document.queryCommandState(command);
 if(document.execCommand(active && value!=="left"?"justifyLeft":command,false,null)){
   remember();syncFormatted(edit);ribbonState();
 }
}
function styleParagraph(type){
 const edit=selectionForParagraph();if(!edit)return;
 const el=currentParagraph();if(!el)return;
 const styles={
   normal:{size:"11pt",weight:"400",line:"1.45",spacing:"6px"},
   title:{size:"22pt",weight:"700",line:"1.2",spacing:"12px"},
   subtitle:{size:"14pt",weight:"400",line:"1.3",spacing:"8px"},
   heading1:{size:"18pt",weight:"700",line:"1.25",spacing:"10px"},
   heading2:{size:"14pt",weight:"700",line:"1.3",spacing:"8px"}
 };
 const style=styles[type]||styles.normal;
 el.style.fontSize=style.size;
 el.style.fontWeight=style.weight;
 el.style.lineHeight=style.line;
 el.style.marginBottom=style.spacing;
 syncFormatted(edit);ribbonState();
}
function makeList(type){
 const command=type==="ol"?"insertOrderedList":"insertUnorderedList";
 executeParagraphCommand(command);
}
function changeIndent(delta){
 const edit=selectionForParagraph();if(!edit)return;
 const p=currentParagraph();if(!p)return;
 // Recuo em passos de 24px. Guardamos no próprio parágrafo, não na
 // caixa A4, para que apenas o trecho editado seja deslocado.
 const current=parseInt(p.style.marginLeft,10)||0;
 p.style.marginLeft=Math.max(0,Math.min(192,current+delta))+"px";
 syncFormatted(edit);ribbonState();
}
function clearTextFormat(){
 const edit=editableReady();if(!edit)return;
 const r=getRange(edit);
 if(r && !r.collapsed){
   // Uma palavra pode estar dentro de strong, em e spans com cor. Substituir
   // apenas o texto deixa esses estilos herdados dos pais, então usamos
   // a remoção de formatação do próprio navegador para preservar a seleção.
   if(!selected.classList.contains("cf-editing"))enterEditing(selected,edit);
   restoreRange(edit,r);
   if(!document.execCommand("removeFormat",false,null)){
     console.warn("O navegador não conseguiu limpar a formatação selecionada.");
     return;
   }
   remember();
 }else{
   // Sem seleção, limpamos os estilos da caixa, mas mantemos os parágrafos.
   edit.querySelectorAll("span,strong,em,u,s,font,b,i,strike").forEach(el=>{
     if(el.closest(".cf-tag-chip"))return;
      const parent=el.parentNode;
     while(el.firstChild)parent.insertBefore(el.firstChild,el);
     el.remove();
   });
   edit.querySelectorAll("[style]").forEach(el=>el.removeAttribute("style"));
 }
 syncFormatted(edit);
 ribbonState();
}
function fontSize(pt){
 const n=clamp(pt,6,96);
 $("cfFontSize").value=n;
 applyInline("font-size",n+"pt");
}
function bindButton(id,handler){
 const b=$(id);if(!b)return;
 b.addEventListener("mousedown",e=>e.preventDefault());
 b.addEventListener("click",handler);
}
const TOGGLE_COMMANDS=new Set(["bold","italic","underline","strikeThrough"]);
function commandState(command){
 try{return Boolean(document.queryCommandState(command));}catch(_){return false;}
}
function ribbonState(){
 const sel=window.getSelection(),edit=focusEditable;
 const active=!!selected?.classList.contains("cf-editing") && !!edit?.isConnected &&
   !!sel?.rangeCount && edit.contains(sel.anchorNode) && edit.contains(sel.focusNode);
 document.querySelectorAll("[data-cf-format]").forEach(button=>{
   const pressed=active && commandState(button.dataset.cfFormat);
   button.setAttribute("aria-pressed",String(pressed));
   button.classList.toggle("cf-format-active",pressed);
 });
 document.querySelectorAll("[data-cf-align]").forEach(button=>{
   const commands={left:"justifyLeft",center:"justifyCenter",right:"justifyRight",justify:"justifyFull"};
   const pressed=active && commandState(commands[button.dataset.cfAlign]);
   button.setAttribute("aria-pressed",String(pressed));
   button.classList.toggle("cf-format-active",pressed);
 });
 document.querySelectorAll("[data-cf-list]").forEach(button=>{
   const p=active?currentParagraph():null;
   const list=p?.closest?.("ul,ol");
   const pressed=!!list && list.tagName.toLowerCase()===button.dataset.cfList;
   button.setAttribute("aria-pressed",String(pressed));
   button.classList.toggle("cf-format-active",pressed);
 });
}
function toggleInlineCommand(command){
 const edit=editableReady();
 if(!edit || !selected || !TOGGLE_COMMANDS.has(command))return;
 const range=getRange(edit);
 if(!selected.classList.contains("cf-editing"))enterEditing(selected,edit);
 if(range && edit.contains(range.commonAncestorContainer))
   restoreRange(edit,range);
 else{
   const end=document.createRange();
   end.selectNodeContents(edit);end.collapse(false);
   restoreRange(edit,end);
 }
 // O navegador alterna bold/italic/underline/strikeThrough para a seleção.
 // Também ajusta o estado da próxima digitação quando só há um cursor.
 const applied=document.execCommand(command,false,null);
 if(!applied)console.warn("Comando de formatação não disponível:",command);
 remember();
 syncFormatted(edit);
 ribbonState();
}
document.querySelectorAll("[data-cf-format]").forEach(button=>{
 button.setAttribute("aria-pressed","false");
 button.addEventListener("mousedown",e=>e.preventDefault());
 button.addEventListener("click",()=>toggleInlineCommand(button.dataset.cfFormat));
});
document.addEventListener("selectionchange",ribbonState);

$("cfFontFamily").addEventListener("change",e=>applyInline("font-family",safeFont(e.target.value)));
$("cfFontSize").addEventListener("change",e=>fontSize(e.target.value));
bindButton("cfFontGrow",()=>fontSize(Number($("cfFontSize").value)+1));
bindButton("cfFontShrink",()=>fontSize(Number($("cfFontSize").value)-1));
$("cfTextColor").addEventListener("change",e=>applyInline("color",e.target.value));
$("cfHighlightColor").addEventListener("change",e=>applyInline("background-color",e.target.value));
document.querySelectorAll("[data-cf-align]").forEach(b=>{
 b.addEventListener("mousedown",e=>e.preventDefault());
 b.addEventListener("click",()=>paragraphStyle("text-align",b.dataset.cfAlign));
});
document.querySelectorAll("[data-cf-list]").forEach(button=>{
 button.setAttribute("aria-pressed","false");
 // Impede que o clique na ferramenta troque o foco do texto.
 button.addEventListener("mousedown",event=>event.preventDefault());
 button.addEventListener("click",()=>makeList(button.dataset.cfList));
});
$("cfParagraphStyle").addEventListener("change",e=>styleParagraph(e.target.value));
bindButton("cfIndentLess",()=>changeIndent(-24));
bindButton("cfIndentMore",()=>changeIndent(24));
bindButton("cfFormatReset",clearTextFormat);
// O clique recupera o cursor; o drop usa a posição real do ponteiro.
 function insertTag(tag,drop=null){
  if(!tagPattern.test(tag||"")||!campoTag(tag))return;
  const target=drop?.target?.closest(".cf-texto,.cf-edit-table td,.cf-edit-table th");
  if(drop){
   const obj=target?.closest(".cf-canvas-obj");
   if(obj&&stage.contains(obj)){enterEditing(obj,target);}
   else{
    const bounds=stage.getBoundingClientRect();
    const x=(drop.clientX-bounds.left)*W/bounds.width,y=(drop.clientY-bounds.top)*H/bounds.height;
    const created=add(object("text",{x,y,w:Math.min(420,W-x),h:80,html:"<p></p>"}));
    if(!created)return;enterEditing(created);
   }
  }else if(!selected||!["text","table"].includes(selected.cf.tipo)){
   const created=add(object("text",{html:"<p></p>",w:420,h:100}));
   if(!created)return;enterEditing(created);
  }
  const edit=editableReady();if(!edit)return;
  if(!selected.classList.contains("cf-editing"))enterEditing(selected,edit);
  let range=getRange(edit);
  if(drop){
   range=null;
   if(target){
    const hit=document.caretRangeFromPoint?.(drop.clientX,drop.clientY);
    if(hit&&edit.contains(hit.commonAncestorContainer))range=hit;
    else if(document.caretPositionFromPoint){
     const p=document.caretPositionFromPoint(drop.clientX,drop.clientY);
     if(p&&edit.contains(p.offsetNode)){range=document.createRange();range.setStart(p.offsetNode,p.offset);range.collapse(true);}
    }
   }
  }
  if(!range){range=document.createRange();range.selectNodeContents(edit);range.collapse(false);}
  const inside=(range.startContainer.nodeType===1?range.startContainer:range.startContainer.parentElement)?.closest(".cf-tag-chip");
  if(inside&&edit.contains(inside)){range.setStartAfter(inside);range.collapse(true);}
  const node=etiqueta(tag);range.deleteContents();range.insertNode(node);
  const space=document.createTextNode(" ");node.after(space);
  range.setStartAfter(space);range.collapse(true);
  restoreRange(edit,range);syncFormatted(edit);markHistory();
 }
 const catalogo=$("catalogoCampos"),tipoArrasto="application/x-contratofacil-tag";
 catalogo.addEventListener("mousedown",e=>{if(e.target.closest(".botao-campo"))remember();});
 catalogo.addEventListener("click",e=>{const b=e.target.closest(".botao-campo");if(b)insertTag(b.dataset.tag);});
 catalogo.addEventListener("dragstart",e=>{
  const b=e.target.closest(".botao-campo");if(!b||!e.dataTransfer)return;
  e.dataTransfer.setData(tipoArrasto,b.dataset.tag);e.dataTransfer.effectAllowed="copy";stage.classList.add("cf-tag-drop-ready");
 });
 catalogo.addEventListener("dragend",()=>stage.classList.remove("cf-tag-drop-ready"));
 stage.addEventListener("dragover",e=>{
  if(!Array.from(e.dataTransfer?.types||[]).includes(tipoArrasto))return;
  e.preventDefault();e.dataTransfer.dropEffect="copy";stage.classList.add("cf-tag-drop-ready");
 });
 stage.addEventListener("drop",e=>{
  if(!Array.from(e.dataTransfer?.types||[]).includes(tipoArrasto))return;
  e.preventDefault();e.stopPropagation();stage.classList.remove("cf-tag-drop-ready");insertTag(e.dataTransfer.getData(tipoArrasto),e);
 });
 function filterTags(){
 const term=($("buscaCampo")?.value||"").toLocaleLowerCase("pt-BR"),cat=$("grupoCampo")?.value||"";
 let count=0;
 document.querySelectorAll("#listaCampos .grupo-catalogo").forEach(group=>{
  let num=0;
  group.querySelectorAll(".botao-campo").forEach(b=>{
   const text=(b.innerText+" "+b.dataset.tag+" "+b.dataset.descricao).toLocaleLowerCase("pt-BR");
   b.hidden=Boolean(term&&!text.includes(term)||cat&&cat!==b.dataset.grupo);
   if(!b.hidden){count++;num++;}
  });
  const empty=group.querySelector(".catalogo-vazio");
  if(empty)empty.hidden=Boolean(group.querySelectorAll(".botao-campo").length)||Boolean(term)||Boolean(cat&&cat!=="Personalizados");
  group.hidden=!num&&(!empty||empty.hidden);
 });
 $("contadorCampos").textContent=count+" campos";
}
$("buscaCampo").addEventListener("input",filterTags);
$("grupoCampo").addEventListener("change",filterTags);
$("catalogoCampos").addEventListener("mouseover",e=>{const b=e.target.closest(".botao-campo");if(b)$("descricaoCampo").textContent=b.dataset.descricao||b.dataset.tag;});
function serialize(){
 const lines=[PREFIX.trimEnd()];
 for(const el of stage.querySelectorAll(".cf-canvas-obj")){
  const s=el.cf,pre=[{text:"T",line:"L",table:"G",sign:"S",image:"I"}[s.tipo],s.x,s.y,s.w,s.h];
  if(s.tipo==="text")lines.push([...pre,enc(htmlTexto(el.querySelector(".cf-texto")))].join("|"));
  if(s.tipo==="line")lines.push([...pre,s.orient,s.weight,s.color].join("|"));
  if(s.tipo==="sign")lines.push([...pre,s.role].join("|"));
  if(s.tipo==="image"){
   const m=/^data:(image\/(?:png|jpeg));base64,([A-Za-z0-9+/=]+)$/.exec(s.src);
   if(!m)throw new Error("Imagem inválida.");
   lines.push([...pre,m[1],m[2],...(s.background?["fundo"]:[])].join("|"));
  }
  if(s.tipo==="table"){
   const values=Array.from(el.querySelectorAll("tr")).flatMap(tr=>Array.from(tr.children)
     .map(td=>textoCelula(td)));
   lines.push([...pre,s.style,s.header?"1":"0",s.rows,s.cols,enc(values.join("\u001f"))].join("|"));
  }
 }
 return lines.join("\n")+"\n";
}
function preview(){
 clearTimeout(timer);timer=setTimeout(()=>{
  const clone=rawTags(stage.cloneNode(true));clone.removeAttribute("id");
  clone.querySelectorAll(".cf-move,.cf-resize,.cf-resize-handle").forEach(n=>n.remove());
  clone.querySelectorAll("[contenteditable]").forEach(n=>n.removeAttribute("contenteditable"));
   clone.querySelectorAll(".cf-selected,.cf-editing").forEach(n=>n.classList.remove("cf-selected","cf-editing"));
  if(examplesOn){
   const map=new Map(Array.from(document.querySelectorAll(".botao-campo")).map(b=>[b.dataset.tag,b.dataset.exemplo||"[Exemplo]"]));
   map.set("{{empresa.logo}}","[Logo]");
   const walker=document.createTreeWalker(clone,NodeFilter.SHOW_TEXT);const nodes=[];
   while(walker.nextNode())nodes.push(walker.currentNode);
   for(const node of nodes)node.nodeValue=node.nodeValue.replace(/\{\{[a-zA-Z][a-zA-Z0-9_.]*\}\}/g,t=>map.get(t)||"[Campo]");
   clone.querySelectorAll(".cf-obj-sign").forEach(el=>{
    const s=el.querySelector(".cf-sign-name");
    const nomes={colaborador:"Ana Silva",gestor:"Gestor de exemplo",testemunha1:"Testemunha A",testemunha2:"Testemunha B",outro:"Pessoa Exemplo"};
    s.textContent=LABEL[el.dataset.role]+": "+nomes[el.dataset.role];
   });
  }
  const css="*{box-sizing:border-box}body{margin:0;color:#243247}.cf-canvas-stage{width:642px;height:907px;position:relative}"
    +".cf-canvas-obj{position:absolute;overflow:hidden;z-index:1}.cf-canvas-obj.cf-background{z-index:0}.cf-obj-conteudo{height:100%;width:100%}"
    +".cf-texto{font:14px/1.5 Arial,sans-serif;padding:5px}.cf-texto p{margin:0 0 8px}"
    +".cf-real-line{position:absolute;top:50%;width:100%;border-bottom-style:solid}.cf-vertical{border-bottom:0;border-left-style:solid;height:100%;left:50%;top:0;width:0}.cf-diagonal{transform:rotate(-11deg)}"
    +".cf-edit-table{border-collapse:collapse;table-layout:fixed;width:100%;height:100%;font:12px Arial}.cf-edit-table td,.cf-edit-table th{border:1px solid #64748b;padding:4px}.cf-edit-table th{background:#e2e8f0}.cf-tabela-listrado tr:nth-child(even){background:#f1f5f9}"
    +".cf-sign-line{height:38px;border-bottom:1px solid #334155}.cf-sign-name{text-align:center;font:12px Arial;margin:6px 0}"
    +".cf-obj-image img{display:block;width:100%;height:100%;object-fit:contain}";
  $("previaModelo").srcdoc='<!doctype html><html><head><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src &#39;none&#39;;style-src &#39;unsafe-inline&#39;;img-src data:"><style>'+css+'</style></head><body>'+clone.outerHTML+'</body></html>';
 },120);
}
function updatePreview(){preview();}
$("modoExemplo").addEventListener("click",()=>{examplesOn=true;$("modoExemplo").classList.add("ativo");$("modoTags").classList.remove("ativo");preview();});
$("modoTags").addEventListener("click",()=>{examplesOn=false;$("modoTags").classList.add("ativo");$("modoExemplo").classList.remove("ativo");preview();});
function loadSaved(raw){
 for(const line of raw.slice(PREFIX.length).split("\n")){
  if(!line.trim())continue;
  const f=line.split("|"),type={T:"text",L:"line",G:"table",S:"sign",I:"image"}[f[0]];
  if(!type||f.length<6)continue;
  const obj={x:Number(f[1]),y:Number(f[2]),w:Number(f[3]),h:Number(f[4])};
  if(type==="text")obj.html=dec(f[5]);
  if(type==="line")Object.assign(obj,{orient:f[5],weight:Number(f[6]),color:f[7]});
  if(type==="sign")obj.role=f[5];
  if(type==="image"){obj.src="data:"+f[5]+";base64,"+f[6];obj.background=f[7]==="fundo";}
  if(type==="table"){
   const rows=Number(f[7]),cols=Number(f[8]),values=dec(f[9]).split("\u001f");
   Object.assign(obj,{style:f[5],header:f[6]==="1",rows,cols,
    cells:Array.from({length:rows},(_,r)=>Array.from({length:cols},(_,c)=>values[r*cols+c]||""))});
  }
  add(object(type,obj),false);
 }
}
function importLegacy(raw){
 const container=document.createElement("div");container.innerHTML=raw||"";
 const children=Array.from(container.children);let y=35,block=[];
 function flush(){
  if(!block.length)return;
  const html=block.map(el=>el.outerHTML).join("\n"),h=clamp(55+html.length/12,75,300);
  add(object("text",{x:35,y:Math.min(y,H-h),w:570,h,html}),false);
  y+=h+12;block=[];
 }
 for(let i=0;i<children.length;i++){
  const value=children[i].textContent.trim();
  let m=value.match(/^\{\{visual\.linha\.(horizontal|vertical|diagonal|livre)(?:\..*)?\}\}$/);
  if(m){flush();add(object("line",{x:40,y:Math.min(y,H-45),w:510,h:40,orient:m[1]==="livre"?"horizontal":m[1]}),false);y+=55;continue;}
  m=value.match(/^\{\{assinatura\.(colaborador|gestor|testemunha1|testemunha2|outro)(?:\..*)?\}\}$/);
  if(m){flush();add(object("sign",{x:140,y:Math.min(y,H-110),w:360,h:95,role:m[1]}),false);y+=110;continue;}
  m=value.match(/^\{\{visual\.tabela\.inicio\.(\d+)x(\d+)\..*\}\}$/);
  if(m){
   const rows=clamp(m[1],1,12),cols=clamp(m[2],1,8);
   if(children[i+rows+1]?.textContent.trim()==="{{visual.tabela.fim}}"){
    flush();const cells=Array.from({length:rows},(_,r)=>children[i+1+r].textContent.split("|").map(x=>x.trim()));
    const h=clamp(rows*36+10,65,460);
    add(object("table",{x:40,y:Math.min(y,H-h),w:550,h,rows,cols,cells,header:false}),false);
    y+=h+12;i+=rows+1;continue;
   }
  }
  block.push(children[i]);
 }
 flush();
 if(!stage.querySelector(".cf-canvas-obj"))add(object("text",{x:35,y:40,w:570,h:220,html:"<p></p>"}),false);
}
try{
 if(hidden.value.startsWith(PREFIX))loadSaved(hidden.value);
 else importLegacy(hidden.value);
}catch(err){
 console.error("Falha ao carregar documento visual",err);
 $("cfSaveWarning").textContent="Erro ao abrir o conteúdo. O salvamento foi bloqueado para preservar o modelo.";
 window.CFCanvas={salvar:()=>false};return;
}
loading=false;markHistory();filterTags();preview();
if(!hidden.value.trim()){
 const inicial=stage.querySelector(".cf-obj-text");
 if(inicial)enterEditing(inicial);
}
window.CFCanvas={salvar(){
 if(!stage.querySelector(".cf-canvas-obj")){alert("Adicione ao menos um elemento.");return false;}
 try{clearTimeout(historyTask);markHistory();hidden.value=serialize();return true;}
 catch(err){alert("Não foi possível salvar: "+err.message);return false;}
}};
})();
