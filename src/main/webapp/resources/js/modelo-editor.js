/* Editor anterior baseado em Quill/PrimeFaces.
 * Mantido por compatibilidade com telas legadas; o novo canvas A4 é gerenciado
 * por canvas-modelo.js. Não confundir os dois fluxos ao estudar o projeto.
 */
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
        exemplos.set("{{assinatura.colaborador}}", "____________________________ Colaborador: Ana Silva");
        exemplos.set("{{assinatura.gestor}}", "____________________________ Gestor: João Gestor");
        exemplos.set("{{assinatura.testemunha1}}", "____________________________ Testemunha 1: Pessoa Teste");
        exemplos.set("{{assinatura.testemunha2}}", "____________________________ Testemunha 2: Pessoa Teste");
        exemplos.set("{{assinatura.outro}}", "____________________________ Outro participante: Pessoa Teste");
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
                const valor = exemplos.get(match[0]) || (match[1].startsWith("visual.")
                    || match[1].startsWith("assinatura.") ? match[0] : "[Campo não reconhecido]");
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
        montarComponentesPreview(copia);
        const estilo = "body{font:15px/1.6 Arial,sans-serif;padding:28px;color:#1e293b;overflow-wrap:anywhere;}" 
            + ".cf-visual-bloco{position:relative;margin:14px 0 18px;}"
            + ".cf-visual-bloco .cf-bloco-alca{display:none}"
            + ".cf-desenho-linha{border-bottom-style:solid;height:24px}"
            + ".cf-orient-vertical{width:18px;height:90px;border-left-style:solid;border-bottom:0}"
            + ".cf-orient-diagonal{transform:rotate(-9deg)}"
            + ".cf-tabela{border-collapse:collapse}.cf-tabela td,.cf-tabela th{border:1px solid #64748b;padding:5px}"
            + ".cf-tabela-listrado tr:nth-child(even){background:#f1f5f9}"
            + ".cf-tabela-simples td,.cf-tabela-simples th{border:0}"
            + ".cf-tabela th{background:#e2e8f0;font-weight:bold}"
            + ".cf-assinatura-linha{height:38px;border-bottom:1px solid #334155}"
            + ".cf-assinatura-nome{text-align:center;margin:4px 0;}"
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
        for (const secao of document.querySelectorAll("#listaCampos .grupo-catalogo")) {
            const visiveis = Array.from(secao.querySelectorAll(".botao-campo"))
                .filter(botao => !botao.hidden);
            secao.hidden = visiveis.length === 0;
            if (secao.dataset.catalogoGrupo === "personalizado") {
                const vazio = document.getElementById("semTags");
                const cadastradas = secao.querySelectorAll(".botao-campo").length;
                if (vazio) vazio.hidden = cadastradas !== 0 || Boolean(termo) || Boolean(grupo && grupo !== "Personalizados");
                if (!cadastradas && !termo && (!grupo || grupo === "Personalizados")) secao.hidden = false;
            }
        }
        const contador = document.getElementById("contadorCampos");
        if (contador) contador.textContent = encontrados + " campo(s)";
    }


    function montarComponentesPreview(root) {
        const linhaRegex = /^\{\{visual\.linha\.(horizontal|vertical|diagonal|livre)\}\}$/;
        const tabelaRegex = /^\{\{visual\.tabela\.inicio\.(\d{1,2})x(\d{1,2})\.w(\d{2,3})\.h(\d{2,3})\}\}$/;
        for (const inicio of Array.from(root.children)) {
            if (inicio.parentElement !== root) continue;
            const texto = inicio.textContent.trim();
            const lm = texto.match(linhaRegex);
            if (lm) {
                const p = document.createElement("p");
                p.className = "cf-linha-" + lm[1];
                if (lm[1] === "vertical") p.innerHTML = "|<br/>|<br/>|<br/>|";
                else p.textContent = lm[1] === "diagonal" ? "╱           ╱           ╱"
                    : lm[1] === "livre" ? "________________________"
                    : "________________________________________";
                inicio.replaceWith(p);
                continue;
            }
            const m = texto.match(tabelaRegex);
            if (!m) continue;
            const nlin = Number(m[1]), ncol = Number(m[2]), largura = Number(m[3]), altura = Number(m[4]);
            if (nlin < 1 || nlin > 12 || ncol < 1 || ncol > 8 ||
                    largura < 40 || largura > 100 || altura < 20 || altura > 90) continue;
            const linhas = [];
            let cursor = inicio.nextElementSibling;
            while (cursor && linhas.length < nlin && cursor.textContent.trim() !== "{{visual.tabela.fim}}") {
                linhas.push(cursor);
                cursor = cursor.nextElementSibling;
            }
            if (!cursor || cursor.textContent.trim() !== "{{visual.tabela.fim}}" || linhas.length !== nlin) continue;
            const tabela = document.createElement("table");
            tabela.className = "cf-tabela";
            tabela.style.width = largura + "%";
            tabela.style.borderCollapse = "collapse";
            for (const linha of linhas) {
                const tr = tabela.insertRow();
                const valores = linha.textContent.split("|");
                for (let coluna = 0; coluna < ncol; coluna++) {
                    const celula = tr.insertCell();
                    celula.textContent = (valores[coluna] || "").trim();
                    celula.style.cssText = "border:1px solid #64748b;padding:5px;vertical-align:top;height:" + altura + "px";
                }
            }
            inicio.replaceWith(tabela);
            linhas.forEach(l => l.remove());
            cursor.remove();
        }
    }

    function configurarComponentes() {
        const abrir = (id) => document.getElementById(id)?.showModal();
        document.getElementById("abrirLinha")?.addEventListener("click", () => abrir("dialogLinha"));
        document.getElementById("abrirTabela")?.addEventListener("click", () => {
            window.CFBlocos?.novoTabela();
            criarGrade();
            abrir("dialogTabela");
        });
        document.getElementById("abrirAssinatura")?.addEventListener("click", () => abrir("dialogAssinatura"));
        document.querySelectorAll(".cancelar-componente").forEach(el => el.addEventListener("click",
            () => el.closest("dialog")?.close()));
        document.getElementById("confirmarLinha")?.addEventListener("click", () => {
            const tipo = document.getElementById("tipoLinha").value;
            if (!["horizontal","vertical","diagonal","livre"].includes(tipo)) return;
            if (!window.CFBlocos?.inserirLinha(tipo)) return;
            document.getElementById("dialogLinha").close();
        });
        for (const id of ["linhasTabela","colunasTabela"]) {
            document.getElementById(id)?.addEventListener("input", criarGrade);
        }
        document.getElementById("confirmarTabela")?.addEventListener("click", () => {
            const int = (id, min, max) => {
                const v = Number(document.getElementById(id)?.value);
                return Number.isInteger(v) && v >= min && v <= max ? v : null;
            };
            const linhas = int("linhasTabela",1,12), colunas = int("colunasTabela",1,8),
                largura = int("larguraTabela",40,100), altura = int("alturaTabela",20,90);
            if (!linhas || !colunas || !largura || !altura) {
                window.alert("Confira os limites de linhas, colunas e tamanhos.");
                return;
            }
            const linhasTexto = [];
            for (let i = 0; i < linhas; i++) {
                const celulas = [];
                for (let j = 0; j < colunas; j++) {
                    const input = document.querySelector('[data-celula="' + i + '-' + j + '"]');
                    celulas.push((input?.value || "").replace(/[|\r\n]/g," ").trim());
                }
                linhasTexto.push(celulas.join(" | "));
            }
            window.CFBlocos?.inserirTabela({
                linhas, colunas, largura, altura,
                estilo: document.getElementById("estiloTabela").value,
                cabecalho: document.getElementById("cabecalhoTabela").checked,
                celulas:linhasTexto.map(texto => texto.split(" | "))
            });
            document.getElementById("dialogTabela").close();
        });
        document.getElementById("confirmarAssinatura")?.addEventListener("click", () => {
            const papel = document.getElementById("papelAssinante").value;
            if (!["colaborador","gestor","testemunha1","testemunha2","outro"].includes(papel)) return;
            if (!window.CFBlocos?.inserirAssinatura(papel)) return;
            document.getElementById("dialogAssinatura").close();
        });
    }

    function criarGrade() {
        const linhas = Math.max(1,Math.min(12,Number(document.getElementById("linhasTabela")?.value)||2));
        const colunas = Math.max(1,Math.min(8,Number(document.getElementById("colunasTabela")?.value)||2));
        const root = document.getElementById("gradeEntrada");
        if (!root) return;
        const salvos = new Map(Array.from(root.querySelectorAll("[data-celula]"))
            .map(e => [e.dataset.celula,e.value]));
        root.textContent = "";
        root.style.gridTemplateColumns = "repeat(" + colunas + ", minmax(85px, 1fr))";
        for (let i = 0; i < linhas; i++) {
            for (let j = 0; j < colunas; j++) {
                const input = document.createElement("input");
                input.type = "text";
                input.dataset.celula = i + "-" + j;
                input.setAttribute("aria-label","Linha " + (i+1) + ", coluna " + (j+1));
                input.placeholder = (i+1) + " × " + (j+1);
                input.maxLength = 250;
                input.value = salvos.get(input.dataset.celula) || "";
                root.append(input);
            }
        }
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
        if (!window.CFBlocos?.iniciar(editor, atualizarPrevia)) {
            console.warn("Editor visual não inicializado, mantendo apenas campos de texto.");
        }
        configurarCampos();
        configurarComponentes();
        configurarPrevia();
        atualizarPrevia();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", () => iniciar(25));
    } else iniciar(25);
}());
