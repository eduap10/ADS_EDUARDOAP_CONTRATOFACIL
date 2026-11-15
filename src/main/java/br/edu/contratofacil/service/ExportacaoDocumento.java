package br.edu.contratofacil.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.poi.xwpf.usermodel.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.nodes.Entities;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public final class ExportacaoDocumento {
    private ExportacaoDocumento() { }

    public static void pdf(String titulo, String html, OutputStream saida) throws Exception {
        Document doc = Jsoup.parseBodyFragment(html);
        doc.outputSettings().syntax(Document.OutputSettings.Syntax.xml)
            .escapeMode(Entities.EscapeMode.xhtml);
        String conteudo = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>"
           + "<meta charset=\"UTF-8\"/><style>"
           + "@page{size:A4;margin:21mm 19mm;} body{font-family:Arial,sans-serif;font-size:11pt;color:#222;}"
           + "h1,h2,h3{text-align:center;} p{line-height:1.45;}"
           + ".ql-align-center{text-align:center}.ql-align-right{text-align:right}"
           + ".ql-align-justify{text-align:justify}"
           + ".ql-size-small{font-size:9pt}.ql-size-large{font-size:15pt}"
           + ".ql-size-huge{font-size:20pt}"
           + "table.cf-tabela{border-collapse:collapse;margin:10px 0;}"
           + "table.cf-tabela td{border:1px solid #64748b;padding:5px;}"
           + "p.cf-linha-horizontal{border-bottom:1px solid #333;}"
           + "p.cf-linha-vertical{border-left:1px solid #333;}"
           + ".cf-canvas-documento{page-break-inside:avoid;overflow:hidden;}"
           + ".cf-canvas-item{box-sizing:border-box;overflow:hidden;}"
           + ".cf-canvas-texto{font-size:10pt;line-height:1.3;}"
           + ".cf-canvas-texto p{margin:0 0 6px;}"
           + ".cf-canvas-G table{border-collapse:collapse;table-layout:fixed;}"
           + ".cf-canvas-G th,.cf-canvas-G td{padding:3px;border:1px solid #64748b;}"
           + ".cf-canvas-G th{background:#e2e8f0;font-weight:bold;}"
           + ".cf-canvas-assinatura p{font-size:10pt;text-align:center;}"

           + ".cf-linha{margin:8px 0 18px;min-height:12px;}"
           + ".cf-linha-horizontal,.cf-linha-livre{border-bottom-style:solid;}"
           + ".cf-linha-vertical{border-left-style:solid;height:90px;}"
           + ".cf-linha-diagonal{border-bottom-style:solid;transform:rotate(-9deg);}"
           + ".cf-assinatura{margin-top:25px;margin-bottom:18px;}"
           + ".cf-assinatura-linha{border-bottom:1px solid #334155;height:35px;}"
           + ".cf-assinatura-nome{text-align:center;font-size:10pt;margin-top:6px;}"
           + ".cf-tabela th{background:#e2e8f0;font-weight:bold;border:1px solid #64748b;padding:5px;}"
           + ".cf-tabela-listrado tr:nth-child(even){background:#f1f5f9;}"
           + ".cf-tabela-simples td,.cf-tabela-simples th{border:none;border-bottom:1px solid #d7e1e9;}"

           + "</style></head><body><h2>" + org.jsoup.nodes.Entities.escape(titulo)
           + "</h2>" + doc.body().html() + "</body></html>";
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.withHtmlContent(conteudo, null);
        builder.toStream(saida);
        builder.run();
    }

    public static void docx(String titulo, String html, OutputStream saida) throws IOException {
        try (XWPFDocument documento = new XWPFDocument()) {
            XWPFParagraph cabecalho = documento.createParagraph();
            cabecalho.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tituloRun = cabecalho.createRun();
            tituloRun.setBold(true);
            tituloRun.setFontSize(16);
            tituloRun.setText(titulo);
            Document pagina = Jsoup.parseBodyFragment(html);
            for (Node no : pagina.body().childNodes()) {
                adicionarBloco(documento, no);
            }
            documento.write(saida);
        }
    }

    private static void adicionarBloco(XWPFDocument doc, Node no) {
        if (no instanceof TextNode texto) {
            if (!texto.getWholeText().isBlank()) {
                XWPFParagraph p = doc.createParagraph();
                p.createRun().setText(texto.getWholeText());
            }
            return;
        }
        if (!(no instanceof Element el)) return;
        String tag = el.normalName();
        if (el.hasClass("cf-canvas-documento")) {
            // Word permanece editável. Os blocos são ordenados pela posição vertical;
            // deslocamentos em pixels não possuem equivalência exata em DOCX fluido.
            java.util.List<Element> objetos = new java.util.ArrayList<>();
            for (Element filho : el.children()) if (filho.hasClass("cf-canvas-item")) objetos.add(filho);
            objetos.sort(java.util.Comparator.comparingDouble(x -> obterMm(x.attr("style"), "top")));
            for (Element objeto : objetos) {
                if (objeto.hasClass("cf-canvas-G")) {
                    Element grade = objeto.selectFirst("table");
                    if (grade != null) adicionarBloco(doc, grade);
                } else if (objeto.hasClass("cf-canvas-S")) {
                    XWPFParagraph linha = doc.createParagraph();
                    linha.setAlignment(ParagraphAlignment.CENTER);
                    linha.createRun().setText("_____________________________________");
                    XWPFParagraph identificacao = doc.createParagraph();
                    identificacao.setAlignment(ParagraphAlignment.CENTER);
                    identificacao.createRun().setText(objeto.select(".cf-canvas-assinatura p").text());
                } else if (objeto.hasClass("cf-canvas-L")) {
                    XWPFParagraph linha = doc.createParagraph();
                    linha.createRun().setText("_____________________________________");
                } else {
                    Element texto = objeto.selectFirst(".cf-canvas-texto");
                    if (texto != null) for (Node filho : texto.childNodes()) adicionarBloco(doc, filho);
                }
            }
            return;
        }
        if (tag.equals("table")) {
            java.util.List<Element> linhas = el.select("tr");
            int colunas = linhas.stream().mapToInt(l -> l.select("td,th").size()).max().orElse(1);
            if (!linhas.isEmpty() && colunas > 0) {
                XWPFTable tabela = doc.createTable(linhas.size(), colunas);
                java.util.regex.Matcher largura = java.util.regex.Pattern.compile("width:(\\d{2,3})%")
                    .matcher(el.attr("style"));
                tabela.setWidth(largura.find() ? largura.group(1) + "%" : "100%");
                for (int i = 0; i < linhas.size(); i++) {
                    java.util.List<Element> celulas = linhas.get(i).select("td,th");
                    for (int j = 0; j < celulas.size(); j++) {
                        Element origem = celulas.get(j);
                        XWPFTableCell destino = tabela.getRow(i).getCell(j);
                        destino.setText(origem.text());
                        if (origem.normalName().equals("th")) {
                            destino.setColor("E2E8F0");
                            for (XWPFParagraph paragrafo : destino.getParagraphs()) {
                                for (XWPFRun run : paragrafo.getRuns()) run.setBold(true);
                            }
                        } else if (el.hasClass("cf-tabela-listrado") && i % 2 == 1) {
                            destino.setColor("F1F5F9");
                        }
                    }
                }
            }
            return;
        }
        if (el.hasClass("cf-assinatura")) {
            XWPFParagraph linha = doc.createParagraph();
            linha.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun tracejado = linha.createRun();
            tracejado.setText("____________________________________________");
            tracejado.setFontSize(12);
            XWPFParagraph nome = doc.createParagraph();
            nome.setAlignment(ParagraphAlignment.CENTER);
            nome.createRun().setText(el.select(".cf-assinatura-nome").text());
            return;
        }
        if (el.hasClass("cf-linha")) {
            XWPFParagraph linha = doc.createParagraph();
            String estilo = el.attr("style");
            java.util.regex.Matcher cor = java.util.regex.Pattern.compile("border-color:#([a-fA-F0-9]{6})")
                .matcher(estilo);
            java.util.regex.Matcher grossura = java.util.regex.Pattern.compile("border-width:(\\d{1,2})px")
                .matcher(estilo);
            XWPFRun traco = linha.createRun();
            traco.setText(el.hasClass("cf-linha-vertical") ? "│\n│\n│"
                : el.hasClass("cf-linha-diagonal") ? "╱           ╱"
                : "________________________________________");
            if (cor.find()) traco.setColor(cor.group(1));
            if (grossura.find() && Integer.parseInt(grossura.group(1)) > 4) traco.setBold(true);
            return;
        }
        if (tag.equals("ul") || tag.equals("ol")) {
            int indice = 1;
            for (Element filho : el.children()) {
                if (filho.normalName().equals("li")) {
                    XWPFParagraph p = doc.createParagraph();
                    p.setIndentationLeft(420);
                    p.createRun().setText(tag.equals("ol") ? (indice++) + ". " : "• ");
                    adicionarConteudo(p, filho, false, false, false);
                }
            }
            return;
        }
        XWPFParagraph p = doc.createParagraph();
        if (el.hasClass("ql-align-center") || "center".equals(el.attr("align"))) p.setAlignment(ParagraphAlignment.CENTER);
        else if (el.hasClass("ql-align-right")) p.setAlignment(ParagraphAlignment.RIGHT);
        else if (el.hasClass("ql-align-justify")) p.setAlignment(ParagraphAlignment.BOTH);
        p.setSpacingAfter(140);
        if (tag.equals("h1") || tag.equals("h2") || tag.equals("h3")) {
            p.setAlignment(ParagraphAlignment.CENTER);
        }
        adicionarConteudo(p, el, tag.matches("h[123]"), false, false);
    }

    private static double obterMm(String style, String propriedade) {
        var exp = java.util.regex.Pattern.compile("(?:^|;)\\s*" + propriedade + ":([0-9.]+)mm")
            .matcher(style);
        if (!exp.find()) return 0;
        try { return Double.parseDouble(exp.group(1)); }
        catch (NumberFormatException e) { return 0; }
    }

    private static void adicionarConteudo(XWPFParagraph p, Node no,
            boolean forte, boolean italico, boolean sublinhado) {
        if (no instanceof TextNode texto) {
            String valor = texto.getWholeText();
            if (!valor.isEmpty()) {
                XWPFRun run = p.createRun();
                run.setText(valor);
                run.setFontFamily("Arial");
                run.setFontSize(forte ? 14 : 11);
                run.setBold(forte);
                run.setItalic(italico);
                if (sublinhado) run.setUnderline(UnderlinePatterns.SINGLE);
            }
            return;
        }
        if (!(no instanceof Element el)) return;
        String tag = el.normalName().toLowerCase(Locale.ROOT);
        if (tag.equals("img") && el.attr("src").startsWith("data:image/")) {
            try {
                String fonte = el.attr("src");
                int corte = fonte.indexOf(',');
                if (corte > 0) {
                    byte[] bytes = java.util.Base64.getDecoder().decode(fonte.substring(corte + 1));
                    if (bytes.length <= 1_000_000) {
                        int tipo = fonte.startsWith("data:image/png;")
                                ? XWPFDocument.PICTURE_TYPE_PNG : XWPFDocument.PICTURE_TYPE_JPEG;
                        try (java.io.ByteArrayInputStream entrada = new java.io.ByteArrayInputStream(bytes)) {
                            p.createRun().addPicture(entrada, tipo, "logo",
                                    org.apache.poi.util.Units.toEMU(160),
                                    org.apache.poi.util.Units.toEMU(65));
                        }
                    }
                }
            } catch (Exception ignored) {
                p.createRun().setText("[Imagem não disponível]");
            }
            return;
        }
        if (tag.equals("br")) {
            p.createRun().addBreak();
            return;
        }
        boolean b = forte || tag.equals("b") || tag.equals("strong");
        boolean i = italico || tag.equals("i") || tag.equals("em");
        boolean u = sublinhado || tag.equals("u");
        for (Node filho : el.childNodes()) adicionarConteudo(p, filho, b, i, u);
    }
}
