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
        ModeloContrato modelo = new ModeloContrato();
        modelo.setTitulo("Modelo da integração");
        modelo.setConteudo("<p>Olá {{colaborador.nome}}</p>");
        modelo.setTipoId(tipoId);
        Contrato contrato = new Contrato();
        try {
            new ColaboradorDAO().salvar(colaborador);
            assertNotNull(colaborador.getId());
            assertEquals("Analista", new ColaboradorDAO().buscar(colaborador.getId()).getCargo());

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

            modelo.setConteudo("<p>Texto novo que não pode alterar contrato antigo</p>");
            new ModeloContratoDAO().salvar(modelo);
            assertEquals("<p>Olá {{colaborador.nome}}</p>",
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
