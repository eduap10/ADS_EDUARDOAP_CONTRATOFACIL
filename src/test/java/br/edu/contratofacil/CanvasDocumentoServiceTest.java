package br.edu.contratofacil;

import br.edu.contratofacil.model.*;
import br.edu.contratofacil.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Verifica o formato A4, as tags, a segurança do HTML e os dados
 *  * preservados nas exportações de documentos.
 */
class CanvasDocumentoServiceTest {
    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }
    private static Colaborador pessoa() {
        Colaborador c = new Colaborador();
        c.setNome("Maria Teste");
        c.setCargo("Analista");
        return c;
    }
    private static String exemplo() {
        return CanvasDocumentoService.CABECALHO
            + "T|32|34|530|120|" + b64("<p><strong>{{colaborador.nome}}</strong> trabalha como {{colaborador.cargo}}.</p>") + "\n"
            + "L|45|190|450|35|horizontal|3|112233\n"
            + "G|35|285|510|140|listrado|1|2|2|"
            + b64(String.join("\u001f","Nome","Cargo","{{colaborador.nome}}","{{colaborador.cargo}}")) + "\n"
            + "S|160|670|330|100|colaborador\n";
    }
    @Test void salvaReabreGeraPreviewEPreservaCampos() {
        String salvo = DocumentoService.sanitizar(exemplo());
        assertTrue(salvo.startsWith(CanvasDocumentoService.CABECALHO));
        assertEquals(salvo,DocumentoService.sanitizar(salvo),"Salvar novamente não deve alterar conteúdo.");
        Contrato contrato=new Contrato();
        String html=DocumentoService.preencher(salvo,pessoa(),
            AssinaturaService.combinar(Map.of(),contrato,pessoa()));
        var d=org.jsoup.Jsoup.parseBodyFragment(html);
        assertEquals(1,d.select(".cf-canvas-documento").size());
        assertEquals(4,d.select(".cf-canvas-item").size());
        assertTrue(d.select(".cf-canvas-texto").text().contains("Maria Teste"));
        assertEquals(2,d.select(".cf-canvas-G th, .cf-canvas-G td").stream().filter(n->n.normalName().equals("th")).count());
        assertTrue(d.select("table").text().contains("Analista"));
        assertTrue(d.select(".cf-canvas-assinatura").text().contains("Maria Teste"));
        assertTrue(d.select(".cf-canvas-L").attr("style").contains("left:"));
    }
    @Test void rejeitaElementosForaDaPaginaEAtributosInvalidos() {
        assertThrows(IllegalArgumentException.class, () ->
            DocumentoService.sanitizar(CanvasDocumentoService.CABECALHO+"L|640|900|100|60|horizontal|3|112233\n"));
        assertThrows(IllegalArgumentException.class, () ->
            DocumentoService.sanitizar(CanvasDocumentoService.CABECALHO+"L|1|2|100|60|horizontal|3|ZZZZZZ\n"));
    }
    @Test void preservaSegurancaEmTextoRico() {
        String raw=CanvasDocumentoService.CABECALHO+
            "T|12|14|400|100|"+b64("<p>Olá {{colaborador.nome}}</p><script>alert('perigo')</script>")+"\n";
        String seguro=DocumentoService.sanitizar(raw);
        String html=DocumentoService.preencher(seguro,pessoa());
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("alert("));
        assertTrue(html.contains("Maria Teste"));
    }
    @Test void exportaArquivoDocxComTabelaEditavel() throws Exception {
        String html=DocumentoService.preencher(DocumentoService.sanitizar(exemplo()),pessoa(),
            AssinaturaService.combinar(Map.of(),new Contrato(),pessoa()));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        ExportacaoDocumento.docx("Contrato",html,bytes);
        try(var word=new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertEquals(1,word.getTables().size());
            assertEquals("Nome",word.getTables().get(0).getRow(0).getCell(0).getText());
            assertTrue(word.getParagraphs().stream().anyMatch(p->p.getText().contains("Maria Teste")));
        }
    }
    @Test void exportaPdfComObjetosAbsolutos() throws Exception {
        String html=DocumentoService.preencher(DocumentoService.sanitizar(exemplo()),pessoa(),
            AssinaturaService.combinar(Map.of(),new Contrato(),pessoa()));
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        ExportacaoDocumento.pdf("Contrato",html,bytes);
        assertTrue(bytes.size()>100);
        assertEquals("%PDF-",new String(bytes.toByteArray(),0,5,StandardCharsets.US_ASCII));
    }
    @Test void estilosTipograficosSãoPreservadosNoCicloCanvasEWord() throws Exception {
        String conteudo = "<p style=\"text-align:center;margin-left:24px;\">"
            + "<span style=\"font-family:Georgia;font-size:18pt;color:#336699;"
            + "background-color:#FFF176;font-weight:700;text-decoration:underline;\">"
            + "{{colaborador.nome}}</span></p>";
        String texto=CanvasDocumentoService.CABECALHO+
            "T|20|25|550|100|"+b64(conteudo)+"\n";
        String guardado=DocumentoService.sanitizar(texto);
        String html=DocumentoService.preencher(guardado,pessoa(),Map.of());
        var doc=org.jsoup.Jsoup.parseBodyFragment(html);
        var span=doc.selectFirst(".cf-canvas-texto span");
        assertNotNull(span,"Sanitização não deve apagar o trecho formatado.");
        String style=span.attr("style").toLowerCase(java.util.Locale.ROOT);
        assertTrue(style.contains("font-family") && style.contains("georgia"),style);
        assertTrue(style.contains("font-size") && style.contains("18pt"),style);
        assertTrue(style.contains("color"),style);
        assertTrue(style.contains("background-color"),style);
        assertTrue(doc.selectFirst(".cf-canvas-texto p").attr("style").contains("text-align"),html);

        ByteArrayOutputStream stream=new ByteArrayOutputStream();
        ExportacaoDocumento.docx("Teste",html,stream);
        try(var word=new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(stream.toByteArray()))) {
            var p=word.getParagraphs().stream()
                .filter(x->x.getText().contains("Maria Teste")).findFirst().orElseThrow();
            assertEquals(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER,p.getAlignment());
            var run=p.getRuns().stream().filter(x->x.getText(0)!=null&&x.getText(0).contains("Maria Teste"))
                .findFirst().orElseThrow();
            assertEquals("Georgia",run.getFontFamily());
            assertEquals(18,run.getFontSize());
            assertTrue(run.isBold());
            assertEquals("336699",run.getColor());
        }
    }

    private static final String PNG_2X2 =
        "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAAFklEQVR4nGPkCtjCwMDAxMDAwMDAAAAMKAESBnDZDAAAAABJRU5ErkJggg==";

    @Test void imagemPngPermaneceNoModeloPreviewPdfEDocx() throws Exception {
        String modelo=CanvasDocumentoService.CABECALHO+
            "I|40|80|280|150|image/png|"+PNG_2X2+"\n";
        String seguro=DocumentoService.sanitizar(modelo);
        assertEquals(seguro,DocumentoService.sanitizar(seguro));
        String html=DocumentoService.preencher(seguro,pessoa(),Map.of());
        var d=org.jsoup.Jsoup.parseBodyFragment(html);
        assertEquals(1,d.select("img.cf-canvas-imagem").size());
        assertEquals("data:image/png;base64,"+PNG_2X2,
                     d.selectFirst("img.cf-canvas-imagem").attr("src"));

        ByteArrayOutputStream pdf=new ByteArrayOutputStream();
        ExportacaoDocumento.pdf("Imagem de contrato",html,pdf);
        assertTrue(pdf.size()>200);
        assertEquals("%PDF-",new String(pdf.toByteArray(),0,5,StandardCharsets.US_ASCII));

        ByteArrayOutputStream docx=new ByteArrayOutputStream();
        ExportacaoDocumento.docx("Imagem de contrato",html,docx);
        try(var word=new org.apache.poi.xwpf.usermodel.XWPFDocument(
                new ByteArrayInputStream(docx.toByteArray()))) {
            assertEquals(1,word.getAllPictures().size());
        }
    }

    @Test void fundoPreservaOrdemEProporcaoNaEmissaoEWord() throws Exception {
        String texto="T|35|40|530|130|"+b64("<p>Contrato sobre o fundo</p>")+"\n";
        String fundo="I|0|0|642|907|image/png|"+PNG_2X2+"|fundo\n";
        String bruto=CanvasDocumentoService.CABECALHO+texto+fundo;
        String salvo=DocumentoService.sanitizar(bruto);
        assertTrue(salvo.startsWith(CanvasDocumentoService.CABECALHO+"I|"));
        assertEquals(salvo,DocumentoService.sanitizar(salvo));
        String html=DocumentoService.preencher(salvo,pessoa());
        var documento=org.jsoup.Jsoup.parseBodyFragment(html);
        assertTrue(documento.selectFirst(".cf-canvas-item").hasClass("cf-canvas-fundo"));
        assertTrue(documento.selectFirst(".cf-canvas-fundo").attr("style").contains("z-index:0"));
        assertTrue(documento.selectFirst(".cf-canvas-T").attr("style").contains("z-index:1"));
        String img=documento.selectFirst(".cf-canvas-fundo img").attr("style");
        assertTrue(img.contains("width:170.00mm;height:170.00mm"),img);
        assertThrows(IllegalArgumentException.class,()->DocumentoService.sanitizar(bruto.replace("|fundo","|invalido")));
        ByteArrayOutputStream pdf=new ByteArrayOutputStream();
        ExportacaoDocumento.pdf("Contrato",html,pdf);
        assertTrue(pdf.size()>200);
        ByteArrayOutputStream docx=new ByteArrayOutputStream();
        ExportacaoDocumento.docx("Contrato",html,docx);
        try(var word=new org.apache.poi.xwpf.usermodel.XWPFDocument(new ByteArrayInputStream(docx.toByteArray()))){
            assertEquals(1,word.getAllPictures().size());
            var desenho=word.getParagraphs().get(0).getRuns().stream()
                .flatMap(r->Arrays.stream(r.getCTR().getDrawingArray())).findFirst().orElseThrow();
            assertEquals(0,desenho.sizeOfInlineArray());
            assertEquals(1,desenho.sizeOfAnchorArray());
            assertTrue(desenho.getAnchorArray(0).getBehindDoc());
            assertTrue(word.getParagraphs().stream().anyMatch(p->p.getText().contains("Contrato sobre o fundo")));
        }
    }

    @Test void imagensPerigosasEOuCorrompidasSaoRecusadas() {
        String prefixo=CanvasDocumentoService.CABECALHO+"I|0|0|100|100|";
        assertThrows(IllegalArgumentException.class,()->
            DocumentoService.sanitizar(prefixo+"image/svg+xml|"+b64("<svg onload=alert(1)/>")+"\n"));
        assertThrows(IllegalArgumentException.class,()->
            DocumentoService.sanitizar(prefixo+"image/png|"+b64("<script>alert(1)</script>")+"\n"));
        assertThrows(IllegalArgumentException.class,()->
            DocumentoService.sanitizar(prefixo+"image/jpeg|"+PNG_2X2+"\n"));
    }

    @Test void simulaCaixasA4ComCoordenadasLimitesETextoAcentuado() {
        // Casos reproduzíveis com semente fixa: testa canto da folha, tamanho,
        // Unicode, tags, reabertura e rejeição de elementos fora da página.
        java.util.Random sorteio = new java.util.Random(20261118L);
        for (int i = 0; i < 160; i++) {
            int largura = 20 + sorteio.nextInt(623);
            int altura = 10 + sorteio.nextInt(898);
            int x = sorteio.nextInt(643 - largura);
            int y = sorteio.nextInt(908 - altura);
            String texto = "<p>Edição " + i + ": cláusula áéíóú, R$ 100,00, 😀 "
                + "<strong>{{colaborador.nome}}</strong></p>";
            String campos = "|" + y + "|" + largura + "|" + altura + "|" + b64(texto) + "\n";
            String valido = CanvasDocumentoService.CABECALHO + "T|" + x + campos;
            String salvo = DocumentoService.sanitizar(valido);
            assertEquals(salvo, DocumentoService.sanitizar(salvo), "Round-trip " + i);
            String exibido = DocumentoService.preencher(salvo, pessoa());
            assertTrue(exibido.contains("cf-canvas-documento"), "Prévia " + i);
            assertTrue(org.jsoup.Jsoup.parseBodyFragment(exibido).text().contains("Maria Teste"),
                "Preenchimento das tags " + i);
            int xForaDaFolha = 643 - largura;
            String invalido = CanvasDocumentoService.CABECALHO + "T|" + xForaDaFolha + campos;
            assertThrows(IllegalArgumentException.class, () -> DocumentoService.sanitizar(invalido),
                "Limite horizontal do elemento " + i);
        }
    }

    @Test void rejeitaImagemComCabecalhoPngMasSemImagemReal() {
        // Só os oito bytes da assinatura PNG não constituem um arquivo decodificável.
        byte[] falso = new byte[]{(byte) 0x89, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 0};
        String documento = CanvasDocumentoService.CABECALHO
            + "I|0|0|100|100|image/png|" + Base64.getEncoder().encodeToString(falso) + "\n";
        assertThrows(IllegalArgumentException.class, () -> DocumentoService.sanitizar(documento),
            "Imagens malformadas devem ser bloqueadas ao salvar, não apenas na exportação.");
    }

    @Test void paragrafosListasERecuoSobrevivemAoSalvarEPreencher() {
        // A edição é visual, mas o servidor precisa manter apenas HTML permitido.
        String html = "<p style=\"text-align:right;margin-left:24px;\">"
            + "Descrição do cargo</p><ul><li>Atividade principal</li></ul>"
            + "<ol><li>Etapa um</li></ol>";
        String saved=DocumentoService.sanitizar(CanvasDocumentoService.CABECALHO+
            "T|20|30|500|280|"+b64(html)+"\n");
        String filled=DocumentoService.preencher(saved,pessoa());
        var document=org.jsoup.Jsoup.parseBodyFragment(filled);
        assertEquals(1,document.select(".cf-canvas-texto ul li").size(),
            "A lista com marcadores deve continuar no modelo.");
        assertEquals(1,document.select(".cf-canvas-texto ol li").size(),
            "A lista numerada deve continuar no modelo.");
        var paragraph=document.selectFirst(".cf-canvas-texto p");
        assertNotNull(paragraph);
        assertTrue(paragraph.attr("style").contains("text-align"),
            "O alinhamento precisa continuar após a sanitização.");
        assertTrue(paragraph.attr("style").contains("margin-left"),
            "O recuo não pode desaparecer no salvamento.");
    }

}
