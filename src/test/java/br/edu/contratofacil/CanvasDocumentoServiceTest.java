package br.edu.contratofacil;

import br.edu.contratofacil.model.*;
import br.edu.contratofacil.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
}
