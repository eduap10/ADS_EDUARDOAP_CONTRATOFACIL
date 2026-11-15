(function () {
"use strict";
const W=642,H=907,PREFIX="CFCANVAS1\n",MAX=100;
const $=id=>document.getElementById(id);
const stage=$("cfCanvas"),hidden=$("form:conteudo");
if(!stage||!hidden)return;
const LABEL={colaborador:"Colaborador",gestor:"Gestor",testemunha1:"Testemunha 1",testemunha2:"Testemunha 2",outro:"Outro participante"};
const clamp=(v,min,max)=>Math.max(min,Math.min(max,Math.round(Number(v)||0)));
let selected=null,drag=null,focusEditable=null,storedRange=null,examplesOn=true,timer;
function enc(txt){const b=new TextEncoder().encode(String(txt));let o="";for(let i=0;i<b.length;i+=8192)o+=String.fromCharCode(...b.slice(i,i+8192));return btoa(o);}
function dec(txt){return new TextDecoder().decode(Uint8Array.from(atob(txt),c=>c.charCodeAt(0)));}
function rect(el,s){el.style.left=s.x+"px";el.style.top=s.y+"px";el.style.width=s.w+"px";el.style.height=s.h+"px";}
function object(tipo,extra={}){
 if(stage.querySelectorAll(".cf-canvas-obj").length>=MAX){alert("Limite de 100 elementos por página.");return null;}
 const n=stage.querySelectorAll(".cf-canvas-obj").length;
 const dimensions={text:[530,130],line:[400,42],table:[510,140],sign:[340,100]}[tipo];
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
 return s;
}
function add(s,select=true){
 if(!s)return;
 const el=document.createElement("div");el.className="cf-canvas-obj cf-obj-"+s.tipo;el.cf=s;el.dataset.role=s.role||"";
 rect(el,s);
 const move=document.createElement("button"),resize=document.createElement("button"),inside=document.createElement("div");
 move.type="button";move.className="cf-move";move.textContent="⠿";move.title="Arrastar livremente";
 resize.type="button";resize.className="cf-resize";resize.textContent="↘";resize.title="Arrastar para redimensionar";
 inside.className="cf-obj-conteudo";el.append(move,inside,resize);stage.append(el);
 render(el);
 el.addEventListener("pointerdown",()=>choose(el));
 move.addEventListener("pointerdown",e=>beginPointer(e,el,"move"));
 resize.addEventListener("pointerdown",e=>beginPointer(e,el,"size"));
 if(select)choose(el);
 updatePreview();
 return el;
}
function render(el){
 const s=el.cf,area=el.querySelector(".cf-obj-conteudo");area.textContent="";
 if(s.tipo==="text"){
  const div=document.createElement("div");div.className="cf-texto";div.contentEditable="true";div.tabIndex=0;div.innerHTML=s.html;
  area.append(div);
  div.addEventListener("focus",()=>{focusEditable=div;choose(el);});
  div.addEventListener("click",()=>{focusEditable=div;choose(el);remember();});
  div.addEventListener("keyup",remember);div.addEventListener("mouseup",remember);
  div.addEventListener("input",()=>{s.html=div.innerHTML;updatePreview();});
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
      const td=document.createElement(s.header&&r===0?"th":"td");td.contentEditable="true";td.tabIndex=0;
      td.dataset.r=r;td.dataset.c=c;td.textContent=s.cells[r][c];
      td.addEventListener("focus",()=>{focusEditable=td;choose(el);});
      td.addEventListener("keyup",remember);td.addEventListener("mouseup",remember);
      td.addEventListener("input",()=>{s.cells[r][c]=td.innerText.replace(/[\r\n|\u001f]/g," ").slice(0,500);updatePreview();});
      tr.append(td);
    }
    body.append(tr);
  }
  t.append(body);area.append(t);
 }
}
function choose(el){
 if(selected&&selected!==el)selected.classList.remove("cf-selected");
 selected=el;if(selected)selected.classList.add("cf-selected");toolbar();
}
function toolbar(){
 $("cfProperties").hidden=!selected;
 if(!selected)return;
 const s=selected.cf;
 $("cfObjectKind").textContent={text:"Texto",line:"Linha",table:"Tabela",sign:"Assinatura"}[s.tipo];
 for(const key of ["x","y","w","h"])$("cf"+key.toUpperCase()).value=s[key];
 document.querySelectorAll("[data-cf-panel]").forEach(node=>node.hidden=node.dataset.cfPanel!==s.tipo);
 if(s.tipo==="line"){$("cfLineColor").value="#"+s.color;$("cfLineWeight").value=s.weight;$("cfLineOrient").value=s.orient;}
 if(s.tipo==="table"){$("cfTableStyle").value=s.style;$("cfTableHeader").checked=s.header;}
 if(s.tipo==="sign")$("cfSignRole").value=s.role;
}
function change(props,redraw=true){
 if(!selected)return;
 const s=selected.cf;Object.assign(s,props);
 s.w=clamp(s.w,40,W);s.h=clamp(s.h,20,H);s.x=clamp(s.x,0,W-s.w);s.y=clamp(s.y,0,H-s.h);
 rect(selected,s);if(redraw)render(selected);toolbar();updatePreview();
}
function beginPointer(e,el,action){
 e.preventDefault();e.stopPropagation();choose(el);
 drag={id:e.pointerId,el,action,mouseX:e.clientX,mouseY:e.clientY,
  start:{x:el.cf.x,y:el.cf.y,w:el.cf.w,h:el.cf.h}};
 e.currentTarget.setPointerCapture(e.pointerId);
}
document.addEventListener("pointermove",e=>{
 if(!drag||drag.id!==e.pointerId)return;
 const scale=stage.getBoundingClientRect().width/W;
 const dx=(e.clientX-drag.mouseX)/scale,dy=(e.clientY-drag.mouseY)/scale;
 const d=drag.el.cf,s=drag.start;
 if(drag.action==="move"){d.x=clamp(s.x+dx,0,W-d.w);d.y=clamp(s.y+dy,0,H-d.h);}
 else{d.w=clamp(s.w+dx,40,W-d.x);d.h=clamp(s.h+dy,20,H-d.y);}
 rect(drag.el,d);toolbar();
});
document.addEventListener("pointerup",e=>{if(drag?.id===e.pointerId){drag=null;updatePreview();}});
stage.addEventListener("pointerdown",e=>{if(e.target===stage){selected?.classList.remove("cf-selected");selected=null;toolbar();}});
for(const key of ["x","y","w","h"])$("cf"+key.toUpperCase()).addEventListener("change",e=>change({[key]:Number(e.target.value)},false));
$("cfLineOrient").addEventListener("change",e=>change({orient:e.target.value}));
$("cfLineColor").addEventListener("input",e=>change({color:e.target.value.slice(1)}));
$("cfLineWeight").addEventListener("change",e=>change({weight:clamp(e.target.value,1,12)}));
$("cfTableStyle").addEventListener("change",e=>change({style:e.target.value}));
$("cfTableHeader").addEventListener("change",e=>change({header:e.target.checked}));
$("cfSignRole").addEventListener("change",e=>change({role:e.target.value}));
$("cfRemove").addEventListener("click",()=>{if(selected){selected.remove();selected=null;toolbar();updatePreview();}});
$("cfFront").addEventListener("click",()=>{if(selected){stage.append(selected);updatePreview();}});
$("cfBack").addEventListener("click",()=>{if(selected){stage.prepend(selected);updatePreview();}});
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
   selected.cf.html=selected.querySelector(".cf-texto").innerHTML;
 }else if(selected.cf.tipo==="table"){
   selected.querySelectorAll("tr").forEach((tr,r)=>Array.from(tr.children).forEach((cell,c)=>{
     selected.cf.cells[r][c]=cell.textContent.replace(/[\u001f\r\n|]/g," ").slice(0,500);
   }));
 }
 updatePreview();
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
function currentParagraph(){
 const edit=editableReady();if(!edit)return null;
 const r=getRange(edit);
 let node=r?.startContainer||edit;
 if(node.nodeType===Node.TEXT_NODE)node=node.parentElement;
 const block=node?.closest?.("p,div,li,h1,h2,h3");
 if(block && edit.contains(block) && block!==edit)return block;
 return edit.querySelector("p,div,h1,h2,h3,li")||edit;
}
function paragraphStyle(property,value){
 const el=currentParagraph();if(!el)return;
 el.style.setProperty(property,value);
 syncFormatted(editableReady());
}
function styleParagraph(type){
 const el=currentParagraph();if(!el)return;
 const styles={
  normal:{size:"11pt",weight:"400",line:"1.45",spacing:"6px"},
  title:{size:"22pt",weight:"700",line:"1.2",spacing:"12px"},
  subtitle:{size:"14pt",weight:"400",line:"1.3",spacing:"8px"},
  heading1:{size:"18pt",weight:"700",line:"1.25",spacing:"10px"},
  heading2:{size:"14pt",weight:"700",line:"1.3",spacing:"8px"}
 };
 const s=styles[type]||styles.normal;
 el.style.fontSize=s.size;
 el.style.fontWeight=s.weight;
 el.style.lineHeight=s.line;
 el.style.marginBottom=s.spacing;
 syncFormatted(editableReady());
}
function makeList(type){
 const edit=editableReady(),el=currentParagraph();if(!edit||!el)return;
 const listTag=type==="ol"?"ol":"ul";
 const li=el.closest("li");
 if(li && edit.contains(li)){
   const old=li.parentElement;
   if(old && /^(UL|OL)$/.test(old.tagName)) {
     if(old.tagName.toLowerCase()!==listTag){
       const list=document.createElement(listTag);
       while(old.firstChild)list.append(old.firstChild);
       old.replaceWith(list);
     }
     syncFormatted(edit);return;
   }
 }
 if(el===edit)return;
 const list=document.createElement(listTag),entry=document.createElement("li");
 while(el.firstChild)entry.append(el.firstChild);
 list.append(entry);el.replaceWith(list);
 syncFormatted(edit);
}
function changeIndent(delta){
 const block=currentParagraph();if(!block)return;
 const old=parseInt(block.style.marginLeft,10)||0;
 block.style.marginLeft=Math.max(0,Math.min(192,old+delta))+"px";
 syncFormatted(editableReady());
}
function clearTextFormat(){
 const edit=editableReady();if(!edit)return;
 const r=getRange(edit);
 if(r && !r.collapsed){
   const text=r.toString();
   r.deleteContents();
   const node=document.createTextNode(text);r.insertNode(node);
   const next=document.createRange();next.selectNode(node);
   restoreRange(edit,next);
 }else{
   // Limpar a caixa toda, mantendo o conteúdo textual e os parágrafos.
   edit.querySelectorAll("span,strong,em,u,s,font").forEach(el=>{
     const parent=el.parentNode;
     while(el.firstChild)parent.insertBefore(el.firstChild,el);
     el.remove();
   });
   edit.querySelectorAll("[style]").forEach(el=>el.removeAttribute("style"));
 }
 syncFormatted(edit);
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
document.querySelectorAll("[data-cf-format]").forEach(b=>{
 b.addEventListener("mousedown",e=>e.preventDefault());
 b.addEventListener("click",()=>{
   const key=b.dataset.cfFormat;
   const styles={bold:["font-weight","700"],italic:["font-style","italic"],
      underline:["text-decoration","underline"],strikeThrough:["text-decoration","line-through"]};
   if(styles[key])applyInline(...styles[key]);
 });
});
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
document.querySelectorAll("[data-cf-list]").forEach(b=>{
 b.addEventListener("mousedown",e=>e.preventDefault());
 b.addEventListener("click",()=>makeList(b.dataset.cfList));
});
$("cfParagraphStyle").addEventListener("change",e=>styleParagraph(e.target.value));
bindButton("cfIndentLess",()=>changeIndent(-24));
bindButton("cfIndentMore",()=>changeIndent(24));
bindButton("cfFormatReset",clearTextFormat);
function insertTag(tag){
 if(!selected || !["text","table"].includes(selected.cf.tipo))add(object("text",{html:"<p></p>",w:420,h:100}));
 let edit=focusEditable;
 if(!edit?.isConnected||!selected.contains(edit)){edit=selected.querySelector("[contenteditable=true]");focusEditable=edit;storedRange=null;}
 if(!edit)return;
 edit.focus();const sel=window.getSelection();
 if(storedRange && edit.contains(storedRange.commonAncestorContainer)){sel.removeAllRanges();sel.addRange(storedRange);}
 const local=sel.rangeCount && edit.contains(sel.getRangeAt(0).commonAncestorContainer);
 if(local){
  const r=sel.getRangeAt(0);r.deleteContents();const txt=document.createTextNode(tag);
  r.insertNode(txt);r.setStartAfter(txt);r.collapse(true);sel.removeAllRanges();sel.addRange(r);
 } else {
  const txt=document.createTextNode(tag);edit.append(txt);
  const r=document.createRange();r.setStartAfter(txt);r.collapse(true);
  sel.removeAllRanges();sel.addRange(r);
 }
 remember();
 if(selected.cf.tipo==="text")selected.cf.html=edit.innerHTML;
 else selected.cf.cells[Number(edit.dataset.r)][Number(edit.dataset.c)]=edit.innerText;
 updatePreview();
}
$("catalogoCampos").addEventListener("click",e=>{const b=e.target.closest(".botao-campo");if(b)insertTag(b.dataset.tag);});
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
  const s=el.cf,pre=[{text:"T",line:"L",table:"G",sign:"S"}[s.tipo],s.x,s.y,s.w,s.h];
  if(s.tipo==="text")lines.push([...pre,enc(el.querySelector(".cf-texto").innerHTML)].join("|"));
  if(s.tipo==="line")lines.push([...pre,s.orient,s.weight,s.color].join("|"));
  if(s.tipo==="sign")lines.push([...pre,s.role].join("|"));
  if(s.tipo==="table"){
   const values=Array.from(el.querySelectorAll("tr")).flatMap(tr=>Array.from(tr.children)
     .map(td=>td.innerText.replace(/[\r\n|\u001f]/g," ").slice(0,500)));
   lines.push([...pre,s.style,s.header?"1":"0",s.rows,s.cols,enc(values.join("\u001f"))].join("|"));
  }
 }
 return lines.join("\n")+"\n";
}
function preview(){
 clearTimeout(timer);timer=setTimeout(()=>{
  const clone=stage.cloneNode(true);clone.removeAttribute("id");
  clone.querySelectorAll(".cf-move,.cf-resize").forEach(n=>n.remove());
  clone.querySelectorAll("[contenteditable]").forEach(n=>n.removeAttribute("contenteditable"));
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
    +".cf-canvas-obj{position:absolute;overflow:hidden}.cf-obj-conteudo{height:100%;width:100%}"
    +".cf-texto{font:14px/1.5 Arial,sans-serif;padding:5px}.cf-texto p{margin:0 0 8px}"
    +".cf-real-line{position:absolute;top:50%;width:100%;border-bottom-style:solid}.cf-vertical{border-bottom:0;border-left-style:solid;height:100%;left:50%;top:0;width:0}.cf-diagonal{transform:rotate(-11deg)}"
    +".cf-edit-table{border-collapse:collapse;table-layout:fixed;width:100%;height:100%;font:12px Arial}.cf-edit-table td,.cf-edit-table th{border:1px solid #64748b;padding:4px}.cf-edit-table th{background:#e2e8f0}.cf-tabela-listrado tr:nth-child(even){background:#f1f5f9}"
    +".cf-sign-line{height:38px;border-bottom:1px solid #334155}.cf-sign-name{text-align:center;font:12px Arial;margin:6px 0}";
  $("previaModelo").srcdoc='<!doctype html><html><head><meta charset="utf-8"><meta http-equiv="Content-Security-Policy" content="default-src &#39;none&#39;;style-src &#39;unsafe-inline&#39;"><style>'+css+'</style></head><body>'+clone.outerHTML+'</body></html>';
 },120);
}
function updatePreview(){preview();}
$("modoExemplo").addEventListener("click",()=>{examplesOn=true;$("modoExemplo").classList.add("ativo");$("modoTags").classList.remove("ativo");preview();});
$("modoTags").addEventListener("click",()=>{examplesOn=false;$("modoTags").classList.add("ativo");$("modoExemplo").classList.remove("ativo");preview();});
function loadSaved(raw){
 for(const line of raw.slice(PREFIX.length).split("\n")){
  if(!line.trim())continue;
  const f=line.split("|"),type={T:"text",L:"line",G:"table",S:"sign"}[f[0]];
  if(!type||f.length<6)continue;
  const obj={x:Number(f[1]),y:Number(f[2]),w:Number(f[3]),h:Number(f[4])};
  if(type==="text")obj.html=dec(f[5]);
  if(type==="line")Object.assign(obj,{orient:f[5],weight:Number(f[6]),color:f[7]});
  if(type==="sign")obj.role=f[5];
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
 if(!stage.querySelector(".cf-canvas-obj"))add(object("text",{x:35,y:40,w:570,h:130,html:"<p>Digite seu contrato aqui...</p>"}),false);
}
try{
 if(hidden.value.startsWith(PREFIX))loadSaved(hidden.value);
 else importLegacy(hidden.value);
}catch(err){
 console.error("Falha ao carregar documento visual",err);
 $("cfSaveWarning").textContent="Erro ao abrir o conteúdo. O salvamento foi bloqueado para preservar o modelo.";
 window.CFCanvas={salvar:()=>false};return;
}
filterTags();preview();
window.CFCanvas={salvar(){
 if(!stage.querySelector(".cf-canvas-obj")){alert("Adicione ao menos um elemento.");return false;}
 try{hidden.value=serialize();return true;}
 catch(err){alert("Não foi possível salvar: "+err.message);return false;}
}};
})();
