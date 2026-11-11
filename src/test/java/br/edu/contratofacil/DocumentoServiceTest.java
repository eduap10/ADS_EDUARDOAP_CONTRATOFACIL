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
        assertTrue(html.contains("Ana &amp; João &lt;Teste&gt;"));
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
        assertTrue(html.contains("não informado"));
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
