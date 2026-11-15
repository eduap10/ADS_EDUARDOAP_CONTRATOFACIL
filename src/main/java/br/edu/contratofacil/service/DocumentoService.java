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
        // O sanitizador pode inserir um comentário entre as duas chaves iniciais.
        // Remover apenas esse marcador seguro mantém a tag intacta após salvar e reabrir.
        return POLITICA.sanitize(texto).replace("{<!-- -->{", "{{");
    }

    public static String preencher(String entrada, Colaborador colaborador) {
        Document d = Jsoup.parseBodyFragment(sanitizar(entrada));
        Map<String,String> valores = CatalogoCampos.valores(colaborador);
        Map<Node,List<TextNode>> grupos = new LinkedHashMap<>();
        List<TextNode> textos = new ArrayList<>();
        coletar(d.body(), textos);
        for (TextNode no : textos) {
            Node bloco = blocoMaisProximo(no, d.body());
            grupos.computeIfAbsent(bloco, k -> new ArrayList<>()).add(no);
        }
        for (List<TextNode> grupo : grupos.values()) preencherGrupo(grupo, valores);
        return d.body().html();
    }

    public static String preencher(String conteudo, Colaborador colaborador,
            ConfiguracaoDocumentoDAO.Logo logo) {
        String html = preencher(conteudo, colaborador);
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
            substituicoes.add(valores.getOrDefault(chave, "[" + chave + " não informado]"));
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
