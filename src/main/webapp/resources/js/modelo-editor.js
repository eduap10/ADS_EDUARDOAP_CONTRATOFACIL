(function () {
    "use strict";

    let ultimaSelecao = null;
    let modoExemplo = true;
    let timerPrevia = null;

    function editorAtual() {
        if (typeof PF !== "function") return null;
        const widget = PF("modeloEditor");
        return widget && widget.editor ? widget.editor : null;
    }

    function campos() {
        return Array.from(document.querySelectorAll("#listaCampos .botao-campo"));
    }

    function textoSeguro(no, valor) {
        // TextNode converte os exemplos em texto, não em HTML executável.
        no.nodeValue = valor;
    }

    function substituirExemplos(raiz) {
        const exemplos = new Map(campos().map(botao => [botao.dataset.tag, botao.dataset.exemplo]));
        const blocos = new Set(["P", "DIV", "H1", "H2", "H3", "H4", "H5", "H6", "LI", "BLOCKQUOTE"]);
        const grupos = new Map();
        const walker = document.createTreeWalker(raiz, NodeFilter.SHOW_TEXT);
        while (walker.nextNode()) {
            const no = walker.currentNode;
            let bloco = no.parentNode;
            while (bloco && bloco !== raiz && !blocos.has(bloco.nodeName)) bloco = bloco.parentNode;
            if (!grupos.has(bloco)) grupos.set(bloco, []);
            grupos.get(bloco).push(no);
        }
        for (const nos of grupos.values()) {
            const inicios = [];
            let conteudo = "";
            for (const no of nos) {
                inicios.push(conteudo.length);
                conteudo += no.nodeValue;
            }
            const exp = /\{\{([a-zA-Z][a-zA-Z0-9_.]*)\}\}/g;
            const matches = Array.from(conteudo.matchAll(exp)).reverse();
            const indice = (pos) => {
                for (let i = nos.length - 1; i >= 0; i--) {
                    if (pos >= inicios[i] && pos < inicios[i] + nos[i].nodeValue.length) return i;
                }
                return -1;
            };
            for (const match of matches) {
                const inicio = match.index, fim = inicio + match[0].length;
                const a = indice(inicio), b = indice(fim - 1);
                if (a < 0 || b < 0) continue;
                const valor = exemplos.get(match[0]) || "[Campo não reconhecido]";
                const di = inicio - inicios[a], df = fim - inicios[b];
                if (a === b) {
                    const texto = nos[a].nodeValue;
                    textoSeguro(nos[a], texto.slice(0, di) + valor + texto.slice(df));
                } else {
                    textoSeguro(nos[a], nos[a].nodeValue.slice(0, di) + valor);
                    for (let i = a + 1; i < b; i++) textoSeguro(nos[i], "");
                    textoSeguro(nos[b], nos[b].nodeValue.slice(df));
                }
            }
        }
    }

    function atualizarPrevia() {
        const frame = document.getElementById("previaModelo");
        const editor = editorAtual();
        if (!frame || !editor) return;
        const copia = editor.root.cloneNode(true);
        if (modoExemplo) substituirExemplos(copia);
        const estilo = "body{font:15px/1.6 Arial,sans-serif;padding:28px;color:#1e293b;overflow-wrap:anywhere;}"
            + ".ql-align-center{text-align:center}.ql-align-right{text-align:right}"
            + ".ql-align-justify{text-align:justify}.ql-size-small{font-size:12px}"
            + ".ql-size-large{font-size:22px}.ql-size-huge{font-size:29px}"
            + ".ql-font-serif{font-family:Georgia,serif}.ql-font-monospace{font-family:monospace}";
        // Iframe sem permissões de script/origem, com CSP restritiva.
        frame.srcdoc = '<!doctype html><html><head><meta charset="utf-8">'
            + '<meta http-equiv="Content-Security-Policy" content="default-src &#39;none&#39;; style-src &#39;unsafe-inline&#39;; img-src data:">'
            + '<style>' + estilo + '</style></head><body>' + copia.innerHTML + '</body></html>';
    }

    function inserir(tag) {
        const editor = editorAtual();
        if (!editor) return;
        const range = editor.getSelection() || ultimaSelecao;
        const pos = range ? range.index : editor.getLength() - 1;
        const comprimento = range ? range.length : 0;
        // A tag é inserida inteira. Não se deve formatar somente parte de suas chaves.
        if (comprimento) editor.deleteText(pos, comprimento, "user");
        editor.insertText(pos, tag, "user");
        editor.setSelection(pos + tag.length, 0, "user");
        ultimaSelecao = {index: pos + tag.length, length: 0};
        atualizarPrevia();
    }

    function filtrar() {
        const termo = (document.getElementById("buscaCampo")?.value || "").toLocaleLowerCase("pt-BR").trim();
        const grupo = document.getElementById("grupoCampo")?.value || "";
        let encontrados = 0;
        for (const campo of campos()) {
            const busca = (campo.innerText + " " + campo.dataset.descricao + " "
                + campo.dataset.tag).toLocaleLowerCase("pt-BR");
            const mostra = (!termo || busca.includes(termo)) && (!grupo || campo.dataset.grupo === grupo);
            campo.hidden = !mostra;
            if (mostra) encontrados++;
        }
        const contador = document.getElementById("contadorCampos");
        if (contador) contador.textContent = encontrados + " campo(s)";
    }

    function configurarCampos() {
        const catalogo = document.getElementById("catalogoCampos");
        if (!catalogo) return;
        document.getElementById("buscaCampo")?.addEventListener("input", filtrar);
        document.getElementById("grupoCampo")?.addEventListener("change", filtrar);
        catalogo.addEventListener("click", function (evento) {
            const alvo = evento.target.closest(".botao-campo");
            if (alvo) inserir(alvo.dataset.tag);
        });
        const explicacao = document.getElementById("descricaoCampo");
        const mostrarAjuda = (evento) => {
            const alvo = evento.target.closest(".botao-campo");
            if (alvo && explicacao) {
                explicacao.textContent = alvo.innerText.trim() + ": " + alvo.dataset.descricao
                    + " Exemplo: " + alvo.dataset.exemplo + ". Tag: " + alvo.dataset.tag;
            }
        };
        catalogo.addEventListener("mouseover", mostrarAjuda);
        catalogo.addEventListener("focusin", mostrarAjuda);
        filtrar();
    }

    function configurarPrevia() {
        const exemplo = document.getElementById("modoExemplo");
        const tags = document.getElementById("modoTags");
        function alternar(usaExemplo) {
            modoExemplo = usaExemplo;
            exemplo?.classList.toggle("ativo", usaExemplo);
            tags?.classList.toggle("ativo", !usaExemplo);
            exemplo?.setAttribute("aria-pressed", String(usaExemplo));
            tags?.setAttribute("aria-pressed", String(!usaExemplo));
            const aviso = document.getElementById("avisoPrevia");
            if (aviso) aviso.textContent = usaExemplo
                ? "Dados ilustrativos. Na emissão serão usados os dados reais da ficha selecionada."
                : "Tags do modelo original. O preenchimento acontecerá na geração do contrato.";
            atualizarPrevia();
        }
        exemplo?.addEventListener("click", () => alternar(true));
        tags?.addEventListener("click", () => alternar(false));
    }

    function iniciar(tentativas) {
        const editor = editorAtual();
        if (!editor) {
            if (tentativas > 0) window.setTimeout(() => iniciar(tentativas - 1), 200);
            return;
        }
        editor.on("selection-change", range => { if (range) ultimaSelecao = range; });
        editor.on("text-change", () => {
            window.clearTimeout(timerPrevia);
            timerPrevia = window.setTimeout(atualizarPrevia, 140);
        });
        configurarCampos();
        configurarPrevia();
        atualizarPrevia();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => iniciar(25));
    } else iniciar(25);
}());
