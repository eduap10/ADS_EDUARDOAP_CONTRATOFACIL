package br.edu.contratofacil.service;

import br.edu.contratofacil.model.Colaborador;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.Entities;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.*;

public final class DocumentoService {
    private DocumentoService() { }
    private static final Pattern TAG = Pattern.compile("\\{\\{([a-zA-Z][a-zA-Z0-9_.]*)\\}\\}");
    private static final Pattern HTML = Pattern.compile("(?is)<\\s*(p|div|span|strong|em|u|br|h[1-6]|ul|ol|li|blockquote)\\b");
    private static final PolicyFactory POLITICA = new HtmlPolicyBuilder()
        .allowCommonBlockElements().allowCommonInlineFormattingElements()
        .allowElements("span","div","h1","h2","h3","br","blockquote")
        .allowStyling()
        .allowAttributes("class").matching(Pattern.compile("(?:ql-(?:align-(?:center|right|justify)|size-(?:small|large|huge)|indent-[1-8]|font-(?:serif|monospace))\\s*)+"))
        .onElements("p","div","span","li","h1","h2","h3").toFactory();

    public static String sanitizar(String entrada) {
        if (entrada == null || entrada.isBlank()) return "";
        String texto = HTML.matcher(entrada).find() ? entrada :
            "<p>" + Entities.escape(entrada).replace("\r\n","\n").replace("\n","<br/>") + "</p>";
        return POLITICA.sanitize(texto);
    }

    public static String preencher(String entrada, Colaborador colaborador) {
        Document d = Jsoup.parseBodyFragment(sanitizar(entrada));
        Map<String,String> valores = valores(colaborador);
        List<TextNode> textos = new ArrayList<>();
        coletar(d.body(), textos);
        for (TextNode no : textos) {
            Matcher m = TAG.matcher(no.getWholeText());
            StringBuffer substituido = new StringBuffer();
            while(m.find()){
                String valor=valores.getOrDefault(m.group(1),"["+m.group(1)+" não informado]");
                m.appendReplacement(substituido,Matcher.quoteReplacement(valor));
            }
            m.appendTail(substituido);
            no.text(substituido.toString());
        }
        return d.body().html();
    }

    public static String preencher(String conteudo, Colaborador colaborador,
            br.edu.contratofacil.dao.ConfiguracaoDocumentoDAO.Logo logo) {
        String html = preencher(conteudo, colaborador);
        String marcador = "[empresa.logo não informado]";
        if (logo == null) return html.replace(marcador, "[Logo da empresa não cadastrado]");
        String imagem = "<img alt=\"Logo da empresa\" style=\"width:160px;height:auto;\" src=\"data:"
                + logo.mime() + ";base64," + java.util.Base64.getEncoder().encodeToString(logo.dados()) + "\"/>";
        return html.replace(marcador, imagem);
    }

    private static void coletar(Node no, List<TextNode> textos) {
        if(no instanceof TextNode texto) textos.add(texto);
        for(Node filho:no.childNodes()) coletar(filho,textos);
    }

    public static String textoSimples(String html){
        Document d=Jsoup.parseBodyFragment(html);
        for(org.jsoup.nodes.Element e:d.select("br")) e.after("\n");
        for(org.jsoup.nodes.Element e:d.select("p,div,h1,h2,h3,li")) e.appendText("\n");
        return d.body().wholeText().replaceAll("\n{3,}","\n\n").strip();
    }

    private static Map<String,String> valores(Colaborador c){
        Map<String,String> m=new HashMap<>();
        if(c==null)return m;
        m.put("colaborador.nome", texto(c.getNome()));
        m.put("colaborador.matricula", texto(c.getMatricula()));
        m.put("colaborador.cpf", texto(c.getCpf()));
        m.put("colaborador.email", texto(c.getEmail()));
        m.put("colaborador.cargo", texto(c.getCargo()));
        m.put("colaborador.dataNascimento", data(c.getDataNascimento()));
        m.put("colaborador.dataAdmissao", data(c.getDataAdmissao()));
        m.put("colaborador.centroCustoCodigo",texto(c.getCentroCustoCodigo()));
        m.put("colaborador.centroCustoDescricao",texto(c.getCentroCustoDescricao()));
        m.put("colaborador.cep",texto(c.getCep()));
        m.put("colaborador.logradouro",texto(c.getLogradouro()));
        m.put("colaborador.numero",texto(c.getNumero()));
        m.put("colaborador.bairro",texto(c.getBairro()));
        m.put("colaborador.cidade",texto(c.getCidade()));
        m.put("colaborador.uf",texto(c.getUf()));
        m.put("colaborador.pais",texto(c.getPais()));
        m.put("colaborador.salario",c.getSalario()==null?"Não informado":NumberFormat.getCurrencyInstance(Locale.of("pt","BR")).format(c.getSalario()));
        m.put("documento.dataAtual",data(LocalDate.now()));
        m.put("documento.assinatura","________________________________________ Assinatura de "+texto(c.getNome()));
        return m;
    }
    private static String texto(String s){return s==null||s.isBlank()?"Não informado":s;}
    private static String data(LocalDate d){return d==null?"Não informado":d.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
}
