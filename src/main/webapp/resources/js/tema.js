/* A preferência é visual e local ao navegador, não faz parte da sessão nem dos documentos. */
(function(){
 "use strict";
 const key="contratofacil.tema";
 const root=document.documentElement;
 let saved=null;
 try{saved=localStorage.getItem(key);}catch(e){}
 const media=window.matchMedia("(prefers-color-scheme: dark)");
 let dark=saved==="dark"||(saved!=="light"&&media.matches);
 function render(){
  root.dataset.cfTheme=dark?"dark":"light";
  document.querySelectorAll("[data-cf-theme-toggle]").forEach(btn=>{
   btn.setAttribute("aria-pressed",String(dark));
   btn.setAttribute("title",dark?"Ativar modo claro":"Ativar modo noturno");
   const label=btn.querySelector(".cf-theme-label");if(label)label.textContent=dark?"Modo claro":"Modo noturno";
   const icon=btn.querySelector(".pi");if(icon)icon.className="pi "+(dark?"pi-sun":"pi-moon");
  });
 }
 render();
 document.addEventListener("DOMContentLoaded",render);
 document.addEventListener("click",e=>{
  if(!e.target.closest("[data-cf-theme-toggle]"))return;
  dark=!dark;saved=dark?"dark":"light";
  try{localStorage.setItem(key,saved);}catch(err){}
  render();
 });
 media.addEventListener("change",e=>{if(saved!=="light"&&saved!=="dark"){dark=e.matches;render();}});
 window.addEventListener("storage",e=>{if(e.key===key){saved=e.newValue;dark=saved==="dark"||(saved!=="light"&&media.matches);render();}});
})();
