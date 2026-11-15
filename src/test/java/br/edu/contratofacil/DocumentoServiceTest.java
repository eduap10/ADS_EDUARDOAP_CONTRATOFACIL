package br.edu.contratofacil;

import br.edu.contratofacil.model.Colaborador;
import br.edu.contratofacil.service.DocumentoService;
import br.edu.contratofacil.service.ExportacaoDocumento;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

class DocumentoServiceTest {
    private Colaborador colaborador() {
        Colaborador c = new Colaborador();
        c.setNome("Ana & João <Teste>");
        c.setMatricula("T123");
        c.setCargo("Analista");
        return c;
    }

    @Test void substituiTagsSemExecutarHtmlDoColaborador() {
        String html = DocumentoService.preencher(
            "<p><strong>{{colaborador.nome}}</strong> - {{colaborador.cargo}}</p>", colaborador());
        assertTrue(org.jsoup.Jsoup.parseBodyFragment(html).text().contains("Ana & João <Teste>"), html);
        assertTrue(html.contains("Analista"));
        assertFalse(html.contains("<Teste>"));
        assertFalse(html.contains("{{colaborador.nome}}"));
    }

    @Test void htmlPerigosoNaoEArmazenado() {
        String seguro = DocumentoService.sanitizar(
            "<p>Contrato</p><script>alert(1)</script><img src='x' onerror='alert(2)'/>");
        assertFalse(seguro.contains("<script"));
        assertFalse(seguro.contains("onerror"));
        assertFalse(seguro.contains("alert(1)"));
        assertTrue(seguro.contains("Contrato"));
    }

    @Test void tagsDesconhecidasSaoSinalizadas() {
        String html = DocumentoService.preencher("<p>{{colaborador.inexistente}}</p>", colaborador());
        assertTrue(org.jsoup.Jsoup.parseBodyFragment(html).text().contains("não informado"), html);
    }

    @Test void modeloSalvoMantemTagsAoGerarContrato() {
        String salvo = DocumentoService.sanitizar(
            "<p>Olá, <strong>{{colaborador.nome}}</strong>!</p>");
        String gerado = DocumentoService.preencher(salvo, colaborador());
        String texto = org.jsoup.Jsoup.parseBodyFragment(gerado).text();
        assertTrue(texto.contains("Ana & João <Teste>"), "Falha no ciclo sanitizar/gravar/preencher: " + gerado);
        assertFalse(texto.contains("{{colaborador.nome}}"));
    }


    @Test void catalogoIncluiTodosCamposDaFicha() {
        var campos = br.edu.contratofacil.service.CatalogoCampos.todos();
        var mapa = br.edu.contratofacil.service.CatalogoCampos.valores(colaborador());
        assertEquals(campos.size(), mapa.size());
        for (var campo : campos) {
            assertTrue(mapa.containsKey(campo.getChave()), campo.getChave());
            assertFalse(campo.getDescricao().isBlank());
            assertFalse(campo.getExemplo().isBlank());
            assertEquals("{{" + campo.getChave() + "}}", campo.getTag());
        }
        assertTrue(mapa.containsKey("colaborador.racaCor"));
        assertTrue(mapa.containsKey("colaborador.estadoCivil"));
        assertTrue(mapa.containsKey("colaborador.genero"));
        assertTrue(mapa.containsKey("colaborador.complemento"));
    }

    @Test void resolveTagsMesmoDivididasPelaFormatacao() {
        String html = "<p>Nome: <strong>{{colaborador.</strong><em>nome}}</em>"
            + " / <span>{{colaborador.cargo}}</span></p>";
        String renderizado = DocumentoService.preencher(html, colaborador());
        String texto = org.jsoup.Jsoup.parseBodyFragment(renderizado).text();
        assertTrue(texto.contains("Ana & João <Teste>"), renderizado);
        assertTrue(texto.contains("Analista"), renderizado);
        assertFalse(texto.contains("{{"), renderizado);
    }

    @Test void informaQuandoUmDadoOpcionalNaoFoiPreenchido() {
        String html = DocumentoService.preencher(
            "<p>{{colaborador.complemento}} - {{colaborador.escolaridade}}</p>", colaborador());
        assertTrue(org.jsoup.Jsoup.parseBodyFragment(html).text().contains("Não informado"));
    }


    @Test void geraTabelaEditavelNaPreviaEEmpacotaDocx() throws Exception {
        String modelo = "<p>{{visual.tabela.inicio.2x2.w80.h30}}</p>"
            + "<p>Nome | Cargo</p><p>{{colaborador.nome}} | {{colaborador.cargo}}</p>"
            + "<p>{{visual.tabela.fim}}</p>";
        String html = DocumentoService.preencher(modelo, colaborador());
        var doc = org.jsoup.Jsoup.parseBodyFragment(html);
        assertEquals(1, doc.select("table.cf-tabela").size(), html);
        assertEquals(4, doc.select("table.cf-tabela td").size(), html);
        assertTrue(doc.select("table.cf-tabela").text().contains("Analista"));
        ByteArrayOutputStream word = new ByteArrayOutputStream();
        ExportacaoDocumento.docx("Tabela", html, word);
        try (var zip = new ZipInputStream(new java.io.ByteArrayInputStream(word.toByteArray()))) {
            java.util.zip.ZipEntry e;
            String xml = "";
            while ((e = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(e.getName())) {
                    xml = new String(zip.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            }
            assertTrue(xml.contains("<w:tbl>"), "DOCX precisa manter a grade como tabela: " + xml.substring(0,Math.min(300,xml.length())));
        }
    }

    @Test void separadoresSaoConvertidosEmElementosVisuais() {
        String html = DocumentoService.preencher("<p>{{visual.linha.horizontal}}</p><p>{{visual.linha.vertical}}</p>",
            colaborador());
        assertTrue(html.contains("cf-linha-horizontal"), html);
        assertTrue(html.contains("cf-linha-vertical"), html);
    }


    @Test void assinaturasSaoInseridasComNomeEPapelSemExecutarHtml() {
        var contrato = new br.edu.contratofacil.model.Contrato();
        contrato.setGestorNome("Gestor <Teste>");
        String html = DocumentoService.preencher(
            "<p>{{assinatura.colaborador}}</p><p>{{assinatura.gestor}}</p>",
            colaborador(), br.edu.contratofacil.service.AssinaturaService.combinar(
                java.util.Map.of(), contrato, colaborador()));
        assertTrue(org.jsoup.Jsoup.parseBodyFragment(html).text().contains("Gestor <Teste>"));
        assertFalse(html.contains("<Teste>"), "Os nomes não podem ser executados como HTML.");
    }

    @Test void blocosVisuaisPersistemSemHtmlPerigoso() throws Exception {
        String html = "<p>Documento</p>"
            + "<div class='cf-visual-bloco' data-cf-kind='linha' "
            + "data-cf-orient='horizontal' data-cf-width='75' data-cf-thick='4' data-cf-color='112233'>"
            + "<script>alert(1)</script><span>linha</span></div>"
            + "<div class='cf-visual-bloco' data-cf-kind='assinatura' data-cf-role='gestor' data-cf-width='70'></div>"
            + "<div class='cf-visual-bloco' data-cf-kind='tabela' data-cf-width='90' "
            + "data-cf-height='36' data-cf-style='listrado' data-cf-header='1'>"
            + "<table><tr><th>Nome</th><th>Cargo</th></tr>"
            + "<tr><td>{{colaborador.nome}}</td><td>{{colaborador.cargo}}</td></tr></table></div>";
        String salvo = DocumentoService.sanitizar(html);
        assertFalse(salvo.contains("<script"), salvo);
        assertTrue(salvo.contains("visual.linha.horizontal.w75.t4.c112233"), salvo);
        assertTrue(salvo.contains("assinatura.gestor.w70"), salvo);
        assertTrue(salvo.contains("visual.tabela.inicio.2x2.w90.h36.slistrado.k1"), salvo);
        var contrato = new br.edu.contratofacil.model.Contrato();
        contrato.setGestorNome("Gestora Teste");
        String renderizado = DocumentoService.preencher(salvo, colaborador(),
            br.edu.contratofacil.service.AssinaturaService.combinar(
                java.util.Map.of(), contrato, colaborador()));
        var doc = org.jsoup.Jsoup.parseBodyFragment(renderizado);
        assertEquals(1, doc.select("table.cf-tabela-listrado").size(), renderizado);
        assertEquals(2, doc.select("table th").size(), renderizado);
        assertEquals(1, doc.select(".cf-assinatura-nome").size(), renderizado);
        assertTrue(doc.select(".cf-assinatura-nome").text().contains("Gestora Teste"));
        assertEquals(1, doc.select(".cf-linha-horizontal").size(), renderizado);
        assertTrue(doc.select("table").text().contains("Analista"));
        ByteArrayOutputStream arquivo = new ByteArrayOutputStream();
        ExportacaoDocumento.docx("Teste",renderizado,arquivo);
        try (var word = new org.apache.poi.xwpf.usermodel.XWPFDocument(
                new java.io.ByteArrayInputStream(arquivo.toByteArray()))) {
            assertFalse(word.getTables().isEmpty());
            assertEquals("Nome",word.getTables().get(0).getRow(0).getCell(0).getText());
            assertTrue(word.getParagraphs().stream().anyMatch(p -> p.getText().contains("Gestora Teste")));
        }
    }

    @Test void exportacaoPdfGeraCabecalhoPdfValido() throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        String html = DocumentoService.preencher(
            "<p>Contrato de {{colaborador.nome}}</p>", colaborador());
        ExportacaoDocumento.pdf("Teste", html, saida);
        assertTrue(saida.size() > 100, "PDF deve conter dados renderizados.");
        assertEquals("%PDF-", new String(saida.toByteArray(), 0, 5,
            java.nio.charset.StandardCharsets.US_ASCII));
    }

    @Test void exportacaoDocxGeraArquivoZipValido() throws Exception {
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        ExportacaoDocumento.docx("Contrato de teste", "<p>Olá <strong>mundo</strong></p>", saida);
        boolean possuiDocumentoXml = false;
        try (ZipInputStream zip = new ZipInputStream(new java.io.ByteArrayInputStream(saida.toByteArray()))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entrada.getName())) possuiDocumentoXml = true;
            }
        }
        assertTrue(possuiDocumentoXml, "O arquivo deve conter um documento Word real.");
    }
}
