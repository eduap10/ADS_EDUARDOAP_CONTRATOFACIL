(function () {
    "use strict";
    function localizar() {
        if (typeof PF !== "function") return null;
        const w = PF("modeloEditor");
        return w && w.editor ? w.editor : null;
    }
    function atualizar() {
        const frame = document.getElementById("previaModelo");
        const editor = localizar();
        if (!frame || !editor) return;
        const html = editor.root.innerHTML;
        const estilo = "body{font:15px/1.6 Arial,sans-serif;padding:28px;color:#1e293b;overflow-wrap:anywhere;}"
          + ".ql-align-center{text-align:center}.ql-align-right{text-align:right}"
          + ".ql-align-justify{text-align:justify}.ql-size-small{font-size:12px}"
          + ".ql-size-large{font-size:22px}.ql-size-huge{font-size:29px}"
          + ".ql-font-serif{font-family:Georgia,serif}.ql-font-monospace{font-family:monospace}";
        // O conteúdo do editor não executa scripts e não compartilha a origem da aplicação.
        frame.srcdoc = '<!doctype html><html><head><meta charset="utf-8">'
            + '<meta http-equiv="Content-Security-Policy" content="default-src &#39;none&#39;; '
            + 'style-src &#39;unsafe-inline&#39;; img-src data:"><style>'
            + estilo + "</style></head><body>" + html + "</body></html>";
    }
    window.inserirTagModelo = function (tag) {
        const editor = localizar();
        if (!editor) return;
        const selecao = editor.getSelection();
        const posicao = selecao ? selecao.index : editor.getLength() - 1;
        editor.insertText(posicao, tag, "user");
        editor.setSelection(posicao + tag.length, 0, "user");
        atualizar();
    };
    function iniciar(tentativas) {
        const editor = localizar();
        if (!editor) {
            if (tentativas > 0) window.setTimeout(function () { iniciar(tentativas - 1); }, 200);
            return;
        }
        editor.on("text-change", function () { window.clearTimeout(iniciar.timer);
            iniciar.timer = window.setTimeout(atualizar, 150); });
        atualizar();
    }
    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", function () { iniciar(25); });
    } else iniciar(25);
}());
