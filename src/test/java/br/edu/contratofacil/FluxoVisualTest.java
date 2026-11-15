package br.edu.contratofacil;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

class FluxoVisualTest {
    private void xhtmlValido(String caminho) throws Exception {
        File arquivo = new File("src/main/webapp/" + caminho);
        assertTrue(arquivo.isFile(), "Página inexistente: " + arquivo);
        var fabrica = DocumentBuilderFactory.newInstance();
        fabrica.setNamespaceAware(true);
        fabrica.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
        fabrica.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        fabrica.newDocumentBuilder().parse(arquivo);
    }

    @Test void telasDoFluxoSaoXhtmlBemFormado() throws Exception {
        xhtmlValido("tipos.xhtml");
        xhtmlValido("modelos.xhtml");
        xhtmlValido("editar-modelo.xhtml");
        String editor = java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/webapp/editar-modelo.xhtml"));
        assertTrue(editor.contains("canvas-modelo.js"));
        assertTrue(editor.contains("cfCanvas"));
        assertTrue(editor.contains("cfLineColor"));
        assertTrue(editor.contains("cfTableStyle"));
        assertTrue(editor.contains("cfTableHeader"));
        assertTrue(editor.contains("cfSignRole"));
        assertTrue(editor.contains("data-cf-add=\"text\""));
        assertTrue(editor.contains("CFCanvas.salvar()"));
        assertTrue(editor.contains("data-catalogo-grupo=\"personalizado\""));
        xhtmlValido("contratos.xhtml");
        xhtmlValido("editar-contrato.xhtml");
        xhtmlValido("visualizar-contrato.xhtml");
        xhtmlValido("colaboradores.xhtml");
    }

    @Test void nenhumaTelaUsaOperadorModComoNomeDeVariavel() throws Exception {
        for (String nome : new String[]{"modelos.xhtml", "editar-modelo.xhtml",
                "contratos.xhtml", "editar-contrato.xhtml"}) {
            String texto = java.nio.file.Files.readString(
                java.nio.file.Path.of("src/main/webapp", nome));
            assertFalse(texto.contains("var=\"mod\""), nome);
            assertFalse(texto.contains("#{mod."), nome);
        }
    }
}
