package br.edu.contratofacil.service;

import br.edu.contratofacil.model.Colaborador;
import br.edu.contratofacil.dao.ConfiguracaoDocumentoDAO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Element;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import java.util.*;
import java.util.regex.*;

public final class DocumentoService {
    private DocumentoService() { }
    private static final Pattern TAG = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9_.]*)\\}\\}");
    private static final Pattern HTML = Pattern.compile("(?is)<\\s*(p|div|span|strong|em|u|br|h[1-6]|ul|ol|li|blockquote)\\b");

    private static final Pattern MARCADOR_LINHA = Pattern.compile(
        "\\{\\{visual\\.linha\\.(horizontal|vertical|diagonal|livre)(?:\\.w(\\d{2,3})\\.t(\\d{1,2})\\.c([a-fA-F0-9]{6})(?:\\.x(\\d{1,2}))?)?\\}\\}");
    private static final Pattern MARCADOR_ASSINATURA = Pattern.compile(
        "\\{\\{assinatura\\.(colaborador|gestor|testemunha1|testemunha2|outro)\\.w(\\d{2,3})(?:\\.x(\\d{1,2}))?\\}\\}");
    private static final Pattern MARCADOR_TABELA = Pattern.compile(
        "\\{\\{visual\\.tabela\\.inicio\\.(\\d{1,2})x(\\d{1,2})\\.w(\\d{2,3})\\.h(\\d{2,3})(?:\\.s(grade|listrado|simples)\\.k([01])(?:\\.x(\\d{1,2}))?)?\\}\\}");
    private static final String FIM_TABELA = "{{visual.tabela.fim}}";
    private static final Set<String> BLOCOS = Set.of("p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "li", "blockquote");
    private static final PolicyFactory POLITICA = new HtmlPolicyBuilder()
        .allowCommonBlockElements().allowCommonInlineFormattingElements()
        .allowElements("span","div","h1","h2","h3","br","blockquote")
        .allowStyling()
        .allowAttributes("class")
        .matching(Pattern.compile("(?:ql-(?:align-(?:center|right|justify)|size-(?:small|large|huge)|indent-[1-8]|font-(?:serif|monospace))\\s*)+"))
        .onElements("p","div","span","li","h1","h2","h3").toFactory();

    public static String sanitizar(String entrada) {
        if (entrada == null || entrada.isBlank()) return "";
        String texto = HTML.matcher(entrada).find() ? entrada :
            "<p>" + Entities.escape(entrada).replace("\r\n","\n").replace("\n","<br/>") + "</p>";
        // Componentes visuais do Quill viram marcadores internos validados.
        // O HTML final sempre passa pela política OWASP antes de ir ao banco.
        texto = normalizarComponentesVisuais(texto);
        return POLITICA.sanitize(texto).replace("{<!-- -->{", "{{");
    }

    public static String preencher(String entrada, Colaborador colaborador) {
        return preencher(entrada, colaborador, Map.of());
    }

    public static String preencher(String entrada, Colaborador colaborador, Map<String,String> extras) {
        Document d = Jsoup.parseBodyFragment(sanitizar(entrada));
        Map<String,String> valores = CatalogoCampos.valores(colaborador);
        if (extras != null) {
            for (Map.Entry<String,String> entradaExtra : extras.entrySet()) {
                if (entradaExtra.getKey().startsWith("personalizada.")
                        || entradaExtra.getKey().startsWith("assinatura.")) {
                    valores.put(entradaExtra.getKey(), entradaExtra.getValue());
                }
            }
        }
        Map<Node,List<TextNode>> grupos = new LinkedHashMap<>();
        List<TextNode> textos = new ArrayList<>();
        coletar(d.body(), textos);
        for (TextNode no : textos) {
            Node bloco = blocoMaisProximo(no, d.body());
            grupos.computeIfAbsent(bloco, k -> new ArrayList<>()).add(no);
        }
        for (List<TextNode> grupo : grupos.values()) preencherGrupo(grupo, valores);
        montarComponentes(d, valores);
        return d.body().html();
    }

    public static String preencher(String conteudo, Colaborador colaborador,
            ConfiguracaoDocumentoDAO.Logo logo) {
        return preencher(conteudo, colaborador, logo, Map.of());
    }

    public static String preencher(String conteudo, Colaborador colaborador,
            ConfiguracaoDocumentoDAO.Logo logo, Map<String,String> extras) {
        String html = preencher(conteudo, colaborador, extras);
        String marcador = "[empresa.logo não informado]";
        if (logo == null) return html.replace(marcador, "[Logo da empresa não cadastrado]");
        if (!"image/png".equals(logo.mime()) && !"image/jpeg".equals(logo.mime())) {
            return html.replace(marcador, "[Logo inválido]");
        }
        String imagem = "<img alt=\"Logo da empresa\" style=\"width:160px;height:auto;\" src=\"data:"
                + logo.mime() + ";base64," + Base64.getEncoder().encodeToString(logo.dados()) + "\"/>";
        return html.replace(marcador, imagem);
    }

    /**
     * Campos podem ser formatados parcialmente no editor (ex.: o nome em negrito).
     * A substituição é feita pelos offsets dos nós de texto, mantendo o HTML
     * seguro e a formatação, sem interpretar o conteúdo do colaborador como HTML.
     */
    private static void preencherGrupo(List<TextNode> textos, Map<String,String> valores) {
        if (textos.isEmpty()) return;
        List<Integer> inicios = new ArrayList<>();
        StringBuilder unido = new StringBuilder();
        for (TextNode n : textos) {
            inicios.add(unido.length());
            unido.append(n.getWholeText());
        }
        Matcher matcher = TAG.matcher(unido);
        List<int[]> limites = new ArrayList<>();
        List<String> substituicoes = new ArrayList<>();
        while (matcher.find()) {
            limites.add(new int[]{matcher.start(), matcher.end()});
            String chave = matcher.group(1);
            String valor = valores.get(chave);
            // Marcadores de layout só são transformados após o preenchimento da ficha.
            if (valor == null && (chave.startsWith("visual.")
                    || MARCADOR_ASSINATURA.matcher("{{" + chave + "}}").matches()))
                valor = "{{" + chave + "}}";
            if (valor == null) valor = "[" + chave + " não informado]";
            substituicoes.add(valor);
        }
        for (int k = limites.size() - 1; k >= 0; k--) {
            int inicio = limites.get(k)[0], fim = limites.get(k)[1];
            int primeiro = indice(textos, inicios, inicio);
            int ultimo = indice(textos, inicios, fim - 1);
            TextNode a = textos.get(primeiro), b = textos.get(ultimo);
            int deslocInicio = inicio - inicios.get(primeiro);
            int deslocFim = fim - inicios.get(ultimo);
            String valor = substituicoes.get(k);
            if (primeiro == ultimo) {
                String original = a.getWholeText();
                a.text(original.substring(0, deslocInicio) + valor + original.substring(deslocFim));
            } else {
                a.text(a.getWholeText().substring(0, deslocInicio) + valor);
                for (int i = primeiro + 1; i < ultimo; i++) textos.get(i).text("");
                b.text(b.getWholeText().substring(deslocFim));
            }
        }
    }

    private static int numero(String valor, int padrao, int minimo, int maximo) {
        try {
            int n = Integer.parseInt(valor);
            return n >= minimo && n <= maximo ? n : padrao;
        } catch (NumberFormatException e) { return padrao; }
    }

    /**
     * Lê apenas os atributos permitidos dos blocos visuais. O conteúdo não é
     * interpretado como HTML livre e o resultado retorna à política OWASP.
     */
    private static String normalizarComponentesVisuais(String html) {
        if (!html.contains("cf-visual-bloco")) return html;
        Document documento = Jsoup.parseBodyFragment(html);
        for (Element visual : new ArrayList<>(documento.select("div.cf-visual-bloco"))) {
            String tipo = visual.attr("data-cf-kind");
            int largura = numero(visual.attr("data-cf-width"), 80, 40, 100);
            int deslocamento = Math.min(numero(visual.attr("data-cf-offset"), 0, 0, 60), 100 - largura);
            String nome;
            if ("linha".equals(tipo)) {
                String orientacao = visual.attr("data-cf-orient");
                if (!Set.of("horizontal", "vertical", "diagonal", "livre").contains(orientacao)) {
                    visual.remove(); continue;
                }
                int espessura = numero(visual.attr("data-cf-thick"), 2, 1, 12);
                String cor = visual.attr("data-cf-color");
                if (!cor.matches("[0-9a-fA-F]{6}")) cor = "334155";
                nome = "{{visual.linha." + orientacao + ".w" + largura
                    + ".t" + espessura + ".c" + cor.toUpperCase(Locale.ROOT) + ".x" + deslocamento + "}}";
                visual.replaceWith(new Element("p").text(nome));
            } else if ("assinatura".equals(tipo)) {
                String papel = visual.attr("data-cf-role");
                if (!Set.of("colaborador","gestor","testemunha1","testemunha2","outro").contains(papel)) {
                    visual.remove(); continue;
                }
                visual.replaceWith(new Element("p").text("{{assinatura." + papel + ".w" + largura + ".x" + deslocamento + "}}"));
            } else if ("tabela".equals(tipo)) {
                String estilo = visual.attr("data-cf-style");
                if (!Set.of("grade","listrado","simples").contains(estilo)) estilo = "grade";
                int altura = numero(visual.attr("data-cf-height"), 36, 20, 90);
                boolean cabecalho = "1".equals(visual.attr("data-cf-header"));
                List<Element> linhas = visual.select("table tr");
                if (linhas.isEmpty() || linhas.size() > 12) { visual.remove(); continue; }
                int colunas = linhas.get(0).select("td,th").size();
                if (colunas < 1 || colunas > 8) { visual.remove(); continue; }
                List<Element> marcadores = new ArrayList<>();
                marcadores.add(new Element("p").text("{{visual.tabela.inicio."
                    + linhas.size() + "x" + colunas + ".w" + largura + ".h" + altura
                    + ".s" + estilo + ".k" + (cabecalho ? "1" : "0") + ".x" + deslocamento + "}}"));
                for (Element linha : linhas) {
                    List<Element> celulas = linha.select("td,th");
                    List<String> textos = new ArrayList<>();
                    for (int i = 0; i < colunas; i++) {
                        String celula = i < celulas.size() ? celulas.get(i).text() : "";
                        textos.add(celula.replace('|', ' ').replace('\n', ' ').strip());
                    }
                    marcadores.add(new Element("p").text(String.join(" | ", textos)));
                }
                marcadores.add(new Element("p").text(FIM_TABELA));
                for (Element p : marcadores) visual.before(p);
                visual.remove();
            } else {
                visual.remove();
            }
        }
        return documento.body().html();
    }

    private static void montarComponentes(Document documento, Map<String, String> valores) {
        for (Element inicio : new ArrayList<>(documento.body().children())) {
            if (inicio.parent() != documento.body()) continue;
            String marcador = inicio.text().strip();
            Matcher linha = MARCADOR_LINHA.matcher(marcador);
            if (linha.matches()) {
                int largura = numero(linha.group(2), 80, 40, 100);
                int espessura = numero(linha.group(3), 2, 1, 12);
                String cor = linha.group(4) == null ? "334155" : linha.group(4);
                int deslocamento = Math.min(numero(linha.group(5), 0, 0, 60), 100 - largura);
                Element separador = new Element("div").addClass("cf-linha");
                separador.addClass("cf-linha-" + linha.group(1));
                String tamanho = "width:" + largura + "%;margin-left:" + deslocamento + "%;";
                if (linha.group(1).equals("vertical")) tamanho += "height:90px;";
                separador.attr("style", tamanho + "border-color:#" + cor
                    + ";border-width:" + espessura + "px;");
                inicio.replaceWith(separador);
                continue;
            }
            Matcher assinatura = MARCADOR_ASSINATURA.matcher(marcador);
            if (assinatura.matches()) {
                String papel = assinatura.group(1);
                int largura = numero(assinatura.group(2), 70, 40, 100);
                int deslocamento = Math.min(numero(assinatura.group(3), 0, 0, 60), 100 - largura);
                String rotulo = switch (papel) {
                    case "colaborador" -> "Colaborador";
                    case "gestor" -> "Gestor";
                    case "testemunha1" -> "Testemunha 1";
                    case "testemunha2" -> "Testemunha 2";
                    default -> "Outro participante";
                };
                String texto = valores.getOrDefault("assinatura." + papel, "");
                String nome = texto.contains(":") ? texto.substring(texto.indexOf(':') + 1).strip()
                    : "Não informado";
                Element bloco = new Element("div").addClass("cf-assinatura")
                    .attr("style", "width:" + largura + "%;margin-left:" + deslocamento + "%;margin-top:24px;text-align:center;");
                bloco.appendElement("div").addClass("cf-assinatura-linha")
                    .attr("style", "border-bottom:1px solid #334155;height:36px;");
                bloco.appendElement("p").addClass("cf-assinatura-nome").text(rotulo + ": " + nome);
                inicio.replaceWith(bloco);
                continue;
            }
            Matcher tabela = MARCADOR_TABELA.matcher(marcador);
            if (!tabela.matches()) continue;
            int linhas = Integer.parseInt(tabela.group(1));
            int colunas = Integer.parseInt(tabela.group(2));
            int largura = Integer.parseInt(tabela.group(3));
            int altura = Integer.parseInt(tabela.group(4));
            String estilo = tabela.group(5) == null ? "grade" : tabela.group(5);
            boolean cabecalho = "1".equals(tabela.group(6));
            int deslocamento = Math.min(numero(tabela.group(7), 0, 0, 60), 100 - largura);
            if (linhas < 1 || linhas > 12 || colunas < 1 || colunas > 8
                    || largura < 40 || largura > 100 || altura < 20 || altura > 90) continue;
            List<Element> originais = new ArrayList<>();
            Element cursor = inicio.nextElementSibling();
            while (cursor != null && originais.size() < linhas
                    && !FIM_TABELA.equals(cursor.text().strip())) {
                originais.add(cursor);
                cursor = cursor.nextElementSibling();
            }
            if (originais.size() != linhas || cursor == null
                    || !FIM_TABELA.equals(cursor.text().strip())) continue;
            Element resultado = new Element("table").addClass("cf-tabela")
                .addClass("cf-tabela-" + estilo)
                .attr("style", "border-collapse:collapse;width:" + largura + "%;margin-left:" + deslocamento + "%;");
            Element corpo = resultado.appendElement("tbody");
            for (int i = 0; i < originais.size(); i++) {
                Element tr = corpo.appendElement("tr");
                String[] conteudos = originais.get(i).text().split("\\|", -1);
                for (int j = 0; j < colunas; j++) {
                    Element celula = tr.appendElement(i == 0 && cabecalho ? "th" : "td");
                    String estilos = "height:" + altura + "px;padding:5px;vertical-align:top;";
                    if (!estilo.equals("simples")) estilos += "border:1px solid #64748b;";
                    if (cabecalho && i == 0) estilos += "font-weight:bold;background:#e2e8f0;";
                    else if (estilo.equals("listrado") && i % 2 == 1) estilos += "background:#f1f5f9;";
                    celula.attr("style", estilos);
                    celula.text(j < conteudos.length ? conteudos[j].strip() : "");
                }
            }
            inicio.replaceWith(resultado);
            for (Element linhaTexto : originais) linhaTexto.remove();
            cursor.remove();
        }
    }

    private static int indice(List<TextNode> textos, List<Integer> inicios, int posicao) {
        for (int i = textos.size() - 1; i >= 0; i--) {
            if (posicao >= inicios.get(i) && posicao < inicios.get(i) + textos.get(i).getWholeText().length()) {
                return i;
            }
        }
        throw new IllegalStateException("Índice fora do fragmento do documento.");
    }

    private static Node blocoMaisProximo(Node no, Node raiz) {
        Node atual = no.parent();
        while (atual != null && atual != raiz) {
            if (atual instanceof Element el && BLOCOS.contains(el.normalName())) return atual;
            atual = atual.parent();
        }
        return raiz;
    }

    private static void coletar(Node no, List<TextNode> textos) {
        if (no instanceof TextNode texto) textos.add(texto);
        for (Node filho : no.childNodes()) coletar(filho,textos);
    }

    public static String textoSimples(String html) {
        Document d=Jsoup.parseBodyFragment(html);
        for (Element e:d.select("br")) e.after("\n");
        for (Element e:d.select("p,div,h1,h2,h3,li")) e.appendText("\n");
        return d.body().wholeText().replaceAll("\n{3,}","\n\n").strip();
    }
}
