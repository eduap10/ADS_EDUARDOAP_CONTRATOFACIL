package br.edu.contratofacil;

import br.edu.contratofacil.dao.*;
import br.edu.contratofacil.model.*;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PersistenciaIntegracaoTest {
    @Test void criaModeloEGeraContratoComTipoEColaborador() throws Exception {
        Assumptions.assumeTrue("true".equals(System.getenv("RUN_DB_INTEGRATION")),
                "Teste de banco executado somente no job PostgreSQL da integração.");

        List<TipoContrato> tipos = new TipoContratoDAO().listar(true);
        assertFalse(tipos.isEmpty(), "A migração de tipos precisa estar aplicada.");
        Long tipoId = tipos.get(0).getId();

        Colaborador colaborador = new Colaborador();
        colaborador.setNome("Colaborador de Teste CI");
        colaborador.setMatricula("CI" + System.nanoTime() % 100000000000000000L);
        colaborador.setCargo("Analista");
        colaborador.setCidade("Passo Fundo");
        colaborador.setEstadoCivil("Solteiro(a)");
        colaborador.setComplemento("Apto 42");
        colaborador.setSalario(new java.math.BigDecimal("3250.75"));
        ModeloContrato modelo = new ModeloContrato();
        modelo.setTitulo("Modelo da integração");
        modelo.setConteudo("<p>Olá {{colaborador.nome}}. Cidade: {{colaborador.cidade}}. "
            + "Estado civil: {{colaborador.estadoCivil}}. "
            + "Complemento: {{colaborador.complemento}}. "
            + "Salário: {{colaborador.salario}}</p>");
        modelo.setTipoId(tipoId);
        Contrato contrato = new Contrato();
        contrato.setGestorNome("Gestora Teste");
        contrato.setTestemunha1Nome("Testemunha Teste");
        try {
            new ColaboradorDAO().salvar(colaborador);
            assertNotNull(colaborador.getId());
            Colaborador ficha = new ColaboradorDAO().buscar(colaborador.getId());
            assertEquals("Analista", ficha.getCargo());
            assertEquals("Passo Fundo", ficha.getCidade());
            String preenchido = br.edu.contratofacil.service.DocumentoService.preencher(
                modelo.getConteudo(), ficha);
            String texto = org.jsoup.Jsoup.parseBodyFragment(preenchido).text();
            assertTrue(texto.contains("Colaborador de Teste CI"), texto);
            assertTrue(texto.contains("Passo Fundo"), texto);
            assertTrue(texto.contains("Solteiro(a)"), texto);
            assertTrue(texto.contains("Apto 42"), texto);
            assertTrue(texto.contains("3.250,75"), texto);
            assertFalse(texto.contains("{{colaborador."), texto);

            new ModeloContratoDAO().salvar(modelo);
            assertNotNull(modelo.getId());
            assertEquals(tipoId, new ModeloContratoDAO().buscar(modelo.getId()).getTipoId());

            contrato.setColaboradorId(colaborador.getId());
            contrato.setModeloId(modelo.getId());
            contrato.setTipoId(tipoId);
            contrato.setTitulo("Documento de teste");
            contrato.setConteudo(modelo.getConteudo());
            new ContratoDAO().salvar(contrato);
            assertNotNull(contrato.getId());
            assertEquals(modelo.getConteudo(), new ContratoDAO().buscar(contrato.getId()).getConteudo());
            Contrato persistido = new ContratoDAO().buscar(contrato.getId());
            assertEquals("Gestora Teste", persistido.getGestorNome());
            assertEquals("Testemunha Teste", persistido.getTestemunha1Nome());
            String assinaturas = br.edu.contratofacil.service.DocumentoService.preencher(
                "<p>{{assinatura.colaborador}}</p><p>{{assinatura.gestor}}</p>",
                colaborador, br.edu.contratofacil.service.AssinaturaService.combinar(
                    java.util.Map.of(), persistido, colaborador));
            assertTrue(assinaturas.contains("Gestora Teste"));

            modelo.setConteudo("<p>Texto novo que não pode alterar contrato antigo</p>");
            new ModeloContratoDAO().salvar(modelo);
            assertEquals(contrato.getConteudo(),
                    new ContratoDAO().buscar(contrato.getId()).getConteudo());

            colaborador.setCargo("Coordenador");
            new ColaboradorDAO().salvar(colaborador);
            assertEquals("Coordenador", new ColaboradorDAO().buscar(colaborador.getId()).getCargo());

        } finally {
            if (contrato.getId() != null) new ContratoDAO().excluir(contrato.getId());
            if (modelo.getId() != null) new ModeloContratoDAO().excluir(modelo.getId());
            if (colaborador.getId() != null) new ColaboradorDAO().excluir(colaborador.getId());
        }
    }
}
