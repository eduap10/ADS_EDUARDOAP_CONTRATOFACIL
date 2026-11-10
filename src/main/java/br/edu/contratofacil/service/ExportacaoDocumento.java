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
