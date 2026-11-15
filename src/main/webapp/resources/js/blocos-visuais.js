/**
 * Blocos visuais do ContratoFácil.
 * Integração com o Quill do PrimeFaces sem bibliotecas adicionais.
 * O servidor converte os blots para um formato interno validado antes de salvar.
 */
(function () {
    "use strict";

    let quill = null;
    let Quill = null;
    let selecionado = null;
    let arrastado = null;
    let aoAlterar = null;

    const papeis = {
        colaborador: "Colaborador",
        gestor: "Gestor",
        testemunha1: "Testemunha 1",
        testemunha2: "Testemunha 2",
        outro: "Outro participante"
    };
    const numero = (v, padrao, minimo, maximo) => {
        const n = Number(v);
        return Number.isFinite(n) && n >= minimo && n <= maximo
            ? Math.round(n) : padrao;
    };
    const validColor = v => /^[0-9a-f]{6}$/i.test(String(v)) ? String(v).toUpperCase() : "334155";

    function normalizar(info) {
        const tipo = ["linha","tabela","assinatura"].includes(info?.tipo) ? info.tipo : "linha";
        const v = {
            tipo,
            largura: numero(info?.largura, 80, 40, 100)
        };
        v.deslocamento = Math.min(numero(info?.deslocamento, 0, 0, 60), 100 - v.largura);
        if (tipo === "linha") {
            v.orientacao = ["horizontal","vertical","diagonal","livre"].includes(info?.orientacao)
                ? info.orientacao : "horizontal";
            v.espessura = numero(info?.espessura, 2, 1, 12);
            v.cor = validColor(info?.cor);
        } else if (tipo === "assinatura") {
            v.papel = Object.hasOwn(papeis, info?.papel) ? info.papel : "colaborador";
        } else {
            v.altura = numero(info?.altura, 36, 20, 90);
            v.estilo = ["grade","listrado","simples"].includes(info?.estilo) ? info.estilo : "grade";
            v.cabecalho = Boolean(info?.cabecalho);
            v.linhas = numero(info?.linhas, 2, 1, 12);
            v.colunas = numero(info?.colunas, 2, 1, 8);
            v.celulas = Array.from({length:v.linhas}, (_, i) =>
                Array.from({length:v.colunas}, (_, j) =>
                    String(info?.celulas?.[i]?.[j] || "").replace(/[|\r\n]/g," ").slice(0,250)));
        }
        return v;
    }

    function construirNode(node, original) {
        const v = normalizar(original);
        node.className = "cf-visual-bloco cf-bloco-" + v.tipo;
        node.setAttribute("contenteditable", "false");
        node.setAttribute("draggable", "true");
        node.dataset.cfKind = v.tipo;
        node.dataset.cfWidth = String(v.largura);
        node.dataset.cfOffset = String(v.deslocamento);
        node.style.width = v.largura + "%";
        node.style.marginLeft = v.deslocamento + "%";
        node.style.maxWidth = "100%";
        node.textContent = "";
        if (v.tipo === "linha") {
            node.dataset.cfOrient = v.orientacao;
            node.dataset.cfThick = String(v.espessura);
            node.dataset.cfColor = v.cor;
            const shape = document.createElement("div");
            shape.className = "cf-desenho-linha cf-orient-" + v.orientacao;
            shape.style.borderColor = "#" + v.cor;
            shape.style.borderWidth = v.espessura + "px";
            shape.title = "Linha " + v.orientacao;
            node.appendChild(shape);
        } else if (v.tipo === "assinatura") {
            node.dataset.cfRole = v.papel;
            const linha = document.createElement("div");
            linha.className = "cf-assinatura-linha";
            node.appendChild(linha);
            const nome = document.createElement("p");
            nome.className = "cf-assinatura-nome";
            nome.textContent = papeis[v.papel] + ": [preenchido na emissão]";
            node.appendChild(nome);
        } else {
            node.dataset.cfHeight = String(v.altura);
            node.dataset.cfStyle = v.estilo;
            node.dataset.cfHeader = v.cabecalho ? "1" : "0";
            const table = document.createElement("table");
            table.className = "cf-tabela cf-tabela-" + v.estilo;
            table.style.width = "100%";
            const body = document.createElement("tbody");
            for (let i = 0; i < v.linhas; i++) {
                const row = body.insertRow();
                for (let j = 0; j < v.colunas; j++) {
                    const cell = document.createElement(i === 0 && v.cabecalho ? "th" : "td");
                    cell.style.height = v.altura + "px";
                    cell.textContent = v.celulas[i][j] || " ";
                    row.appendChild(cell);
                }
            }
            table.appendChild(body);
            node.appendChild(table);
        }
        const mover = document.createElement("span");
        mover.className = "cf-bloco-alca";
        mover.textContent = "⠿";
        mover.title = "Arraste para mover o bloco entre parágrafos";
        node.appendChild(mover);
    }

    function valorDe(node) {
        const t = node.dataset.cfKind;
        const v = {tipo: t, largura: Number(node.dataset.cfWidth),
            deslocamento:Number(node.dataset.cfOffset || 0)};
        if (t === "linha") {
            v.orientacao = node.dataset.cfOrient;
            v.espessura = Number(node.dataset.cfThick);
            v.cor = node.dataset.cfColor;
        } else if (t === "assinatura") {
            v.papel = node.dataset.cfRole;
        } else if (t === "tabela") {
            v.altura = Number(node.dataset.cfHeight);
            v.estilo = node.dataset.cfStyle;
            v.cabecalho = node.dataset.cfHeader === "1";
            const trs = Array.from(node.querySelectorAll("table tr"));
            v.linhas = trs.length;
            v.colunas = trs[0]?.children.length || 2;
            v.celulas = trs.map(tr => Array.from(tr.children).map(td => td.textContent));
        }
        return normalizar(v);
    }

    function registrarBlot(editor) {
        Quill = editor.constructor;
        const BlockEmbed = Quill.import?.("blots/block/embed");
        if (!BlockEmbed) return false;
        class BlocoVisual extends BlockEmbed {
            static blotName = "cfbloco";
            static tagName = "div";
            static className = "cf-visual-bloco";
            static create(value) {
                const node = super.create();
                construirNode(node, value);
                return node;
            }
            static value(node) { return valorDe(node); }
        }
        Quill.register(BlocoVisual, true);
        return true;
    }

    function indice(node) {
        if (!quill || !node || !node.isConnected) return -1;
        const blot = Quill.find(node);
        return blot ? quill.getIndex(blot) : -1;
    }

    function avisar() {
        if (typeof aoAlterar === "function") aoAlterar();
    }

    function inserir(valor, em) {
        if (!quill) return false;
        const range = quill.getSelection() || {index:quill.getLength() - 1, length:0};
        let i = em == null ? range.index : em;
        if (em == null && range.length) quill.deleteText(i, range.length, "user");
        quill.insertEmbed(i, "cfbloco", normalizar(valor), "user");
        quill.setSelection(i + 1, 0, "silent");
        avisar();
        return true;
    }

    function selecionar(node) {
        if (selecionado) selecionado.classList.remove("cf-selecionado");
        selecionado = node && node.isConnected ? node : null;
        if (selecionado) selecionado.classList.add("cf-selecionado");
        atualizarBarra();
    }

    function atualizarBloco(patch) {
        if (!selecionado) return;
        const i = indice(selecionado);
        if (i < 0) return;
        const original = valorDe(selecionado);
        const atualizado = normalizar({...original, ...patch});
        selecionar(null);
        quill.deleteText(i, 1, "user");
        quill.insertEmbed(i, "cfbloco", atualizado, "user");
        const [blot] = quill.getLeaf(i);
        selecionar(blot?.domNode?.closest?.(".cf-visual-bloco") || null);
        avisar();
    }

    function atualizarBarra() {
        const barra = document.getElementById("barraBloco");
        if (!barra) return;
        barra.hidden = !selecionado;
        if (!selecionado) return;
        const v = valorDe(selecionado);
        document.getElementById("blocoDescricao").textContent =
            v.tipo === "linha" ? "Linha selecionada" : v.tipo === "tabela" ?
                "Tabela selecionada" : "Assinatura selecionada";
        const largura = document.getElementById("blocoLargura");
        largura.value = v.largura;
        largura.title = "Largura: " + v.largura + "%";
        const posicao = document.getElementById("blocoPosicao");
        posicao.value = v.deslocamento;
        posicao.max = 100 - v.largura;
        document.getElementById("acoesLinha").hidden = v.tipo !== "linha";
        document.getElementById("acoesTabela").hidden = v.tipo !== "tabela";
        document.getElementById("acoesAssinatura").hidden = v.tipo !== "assinatura";
        if (v.tipo === "linha") {
            document.getElementById("blocoCor").value = "#" + v.cor;
            document.getElementById("blocoEspessura").value = v.espessura;
            document.getElementById("blocoOrientacao").value = v.orientacao;
        }
        if (v.tipo === "tabela") {
            document.getElementById("blocoEstilo").value = v.estilo;
            document.getElementById("blocoCabecalho").checked = v.cabecalho;
        }
        if (v.tipo === "assinatura") document.getElementById("blocoPapel").value = v.papel;
    }

    function moverVertical(direcao) {
        if (!selecionado) return;
        const pos = indice(selecionado);
        if (pos < 0) return;
        const txt = quill.getText();
        const alvo = direcao < 0 ? Math.max(0, txt.lastIndexOf("\n", Math.max(0,pos-2)) + 1)
            : Math.min(quill.getLength()-1, txt.indexOf("\n",pos+1) + 1);
        if (alvo === pos || alvo < 0) return;
        const v = valorDe(selecionado);
        selecionar(null);
        quill.deleteText(pos, 1, "user");
        quill.insertEmbed(alvo > pos ? alvo-1 : alvo, "cfbloco", v, "user");
        avisar();
    }

    function localizarDrop(evento) {
        const alvo = evento.target.closest(".cf-visual-bloco,p,div,li");
        const bloco = Quill.find(alvo, true);
        return bloco ? quill.getIndex(bloco) : -1;
    }

    function instalarEventos() {
        const root = quill.root;
        root.addEventListener("click", e => {
            const node = e.target.closest(".cf-visual-bloco");
            selecionar(node);
        });
        root.addEventListener("dragstart", e => {
            const node = e.target.closest(".cf-visual-bloco");
            if (!node) return;
            arrastado = {index:indice(node), valor:valorDe(node)};
            e.dataTransfer.effectAllowed = "move";
            e.dataTransfer.setData("text/plain", "cf-bloco");
        });
        root.addEventListener("dragover", e => { if (arrastado) e.preventDefault(); });
        root.addEventListener("drop", e => {
            if (!arrastado) return;
            e.preventDefault();
            const alvo = localizarDrop(e);
            const item = arrastado; arrastado = null;
            if (alvo < 0 || item.index < 0 || alvo === item.index) return;
            quill.deleteText(item.index, 1, "user");
            const rect = root.getBoundingClientRect();
            const percentual = Math.round((e.clientX - rect.left) / Math.max(1, rect.width) * 100);
            item.valor.deslocamento = Math.max(0,Math.min(100 - item.valor.largura, percentual));
            quill.insertEmbed(alvo > item.index ? alvo - 1 : alvo, "cfbloco", item.valor, "user");
            selecionar(null);
            avisar();
        });
        root.addEventListener("dragend", () => {arrastado = null;});
        // Resize com a alça nativa do CSS. A largura fica no conteúdo salvo.
        root.addEventListener("mouseup", e => {
            const node = e.target.closest(".cf-visual-bloco");
            if (!node || !node.isConnected) return;
            const larguraAtual = Math.round(node.getBoundingClientRect().width /
                Math.max(1, root.getBoundingClientRect().width) * 100);
            if (Math.abs(larguraAtual - Number(node.dataset.cfWidth)) > 1) {
                selecionar(node);
                atualizarBloco({largura:numero(larguraAtual,80,40,100)});
            }
        });
        const binds = [
            ["blocoLargura", "change", e => atualizarBloco({largura:Number(e.target.value)})],
            ["blocoPosicao", "change", e => atualizarBloco({deslocamento:Number(e.target.value)})],
            ["blocoCor", "change", e => atualizarBloco({cor:e.target.value.replace("#","")})],
            ["blocoEspessura", "change", e => atualizarBloco({espessura:Number(e.target.value)})],
            ["blocoOrientacao", "change", e => atualizarBloco({orientacao:e.target.value})],
            ["blocoEstilo", "change", e => atualizarBloco({estilo:e.target.value})],
            ["blocoCabecalho", "change", e => atualizarBloco({cabecalho:e.target.checked})],
            ["blocoPapel", "change", e => atualizarBloco({papel:e.target.value})]
        ];
        for (const [id,nome,handler] of binds) document.getElementById(id)?.addEventListener(nome,handler);
        document.getElementById("blocoExcluir")?.addEventListener("click",() => {
            if (!selecionado) return;
            const pos = indice(selecionado);
            selecionar(null);
            if (pos >= 0) quill.deleteText(pos, 1, "user");
            avisar();
        });
        document.getElementById("blocoSubir")?.addEventListener("click",()=>moverVertical(-1));
        document.getElementById("blocoDescer")?.addEventListener("click",()=>moverVertical(1));
        document.getElementById("blocoEditarTabela")?.addEventListener("click",()=>{
            if (selecionado?.dataset.cfKind !== "tabela") return;
            const v = valorDe(selecionado);
            for (const [id,valor] of [["linhasTabela",v.linhas],["colunasTabela",v.colunas],
                    ["larguraTabela",v.largura],["alturaTabela",v.altura],
                    ["estiloTabela",v.estilo]]) document.getElementById(id).value=valor;
            document.getElementById("cabecalhoTabela").checked=v.cabecalho;
            const grade = document.getElementById("gradeEntrada");
            grade.textContent="";
            grade.style.gridTemplateColumns="repeat("+v.colunas+", minmax(85px,1fr))";
            for(let i=0;i<v.linhas;i++)for(let j=0;j<v.colunas;j++) {
                const input=document.createElement("input");
                input.type="text";input.dataset.celula=i+"-"+j;
                input.maxLength=250;input.value=v.celulas[i][j];grade.append(input);
            }
            document.getElementById("dialogTabela").showModal();
            document.getElementById("dialogTabela").dataset.editando="1";
        });
    }

    function reconhecerLinha(linha) {
        let m = linha.match(/^\{\{visual\.linha\.(horizontal|vertical|diagonal|livre)(?:\.w(\d{2,3})\.t(\d{1,2})\.c([a-fA-F0-9]{6})(?:\.x(\d{1,2}))?)?\}\}$/);
        if (m) return normalizar({tipo:"linha",orientacao:m[1],largura:Number(m[2]||80),
            espessura:Number(m[3]||2),cor:m[4]||"334155",deslocamento:Number(m[5]||0)});
        m = linha.match(/^\{\{assinatura\.(colaborador|gestor|testemunha1|testemunha2|outro)(?:\.w(\d{2,3})(?:\.x(\d{1,2}))?)?\}\}$/);
        if (m) return normalizar({tipo:"assinatura",papel:m[1],largura:Number(m[2]||70),deslocamento:Number(m[3]||0)});
        return null;
    }

    function hidratarMarcadores() {
        const linhas = quill.getText().split("\n");
        const operacoes = [];
        let indiceAtual=0;
        const tabExp=/^\{\{visual\.tabela\.inicio\.(\d{1,2})x(\d{1,2})\.w(\d{2,3})\.h(\d{2,3})(?:\.s(grade|listrado|simples)\.k([01])(?:\.x(\d{1,2}))?)?\}\}$/;
        for(let i=0;i<linhas.length;i++){
            const texto=linhas[i].trim();
            const tab=texto.match(tabExp);
            if (tab) {
                const nlin=Number(tab[1]), ncol=Number(tab[2]);
                if(nlin>=1 && nlin<=12 && ncol>=1 && ncol<=8 &&
                    linhas[i+nlin+1]?.trim()==="{{visual.tabela.fim}}") {
                    const celulas=linhas.slice(i+1,i+1+nlin).map(l =>
                        l.split("|").map(t=>t.trim()).slice(0,ncol));
                    const comprimento=linhas.slice(i,i+nlin+2).reduce((sum,l)=>sum+l.length+1,0);
                    operacoes.push({index:indiceAtual,length:comprimento,
                        valor:{tipo:"tabela",linhas:nlin,colunas:ncol,largura:Number(tab[3]),
                        altura:Number(tab[4]),estilo:tab[5]||"grade",cabecalho:tab[6]==="1",
                        deslocamento:Number(tab[7]||0),celulas}});
                    indiceAtual+=comprimento;i+=nlin+1;continue;
                }
            }
            const bloco=reconhecerLinha(texto);
            if(bloco)operacoes.push({index:indiceAtual,length:linhas[i].length+1,valor:bloco});
            indiceAtual+=linhas[i].length+1;
        }
        for (const op of operacoes.reverse()) {
            quill.deleteText(op.index,op.length,"silent");
            quill.insertEmbed(op.index,"cfbloco",op.valor,"silent");
        }
        if(operacoes.length)quill.update("silent");
    }

    return {
        iniciar(editor, callback) {
            if (quill === editor) return true;
            if (!registrarBlot(editor)) return false;
            quill=editor;aoAlterar=callback;
            hidratarMarcadores();
            instalarEventos();
            return true;
        },
        inserirLinha(orientacao) { return inserir({tipo:"linha",orientacao,largura:80,cor:"334155",espessura:2}); },
        inserirAssinatura(papel) { return inserir({tipo:"assinatura",papel,largura:70}); },
        inserirTabela(v) {
            if (document.getElementById("dialogTabela")?.dataset.editando === "1"
                && selecionado?.dataset.cfKind === "tabela") atualizarBloco(v);
            else inserir({...v,tipo:"tabela"});
            const modal=document.getElementById("dialogTabela");
            if(modal)modal.dataset.editando="0";
        },
        novoTabela() {
            const modal=document.getElementById("dialogTabela");
            if(modal)modal.dataset.editando="0";
        },
        atualizarPrevia(){avisar();}
    };
}());
