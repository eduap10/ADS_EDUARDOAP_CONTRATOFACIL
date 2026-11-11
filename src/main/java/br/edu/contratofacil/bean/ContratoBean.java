package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.*;
import br.edu.contratofacil.model.*;
import br.edu.contratofacil.service.DocumentoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("contratoBean")
@ViewScoped
public class ContratoBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ContratoBean.class.getName());

    private String pesquisa = "";
    private Long idEdicao;
    private Contrato edicao = new Contrato();
    private List<Contrato> lista = new ArrayList<>();
    private List<Colaborador> colaboradores = new ArrayList<>();
    private List<ModeloContrato> modelos = new ArrayList<>();
    private List<TipoContrato> tipos = new ArrayList<>();
    private String htmlPrevia = "";

    @PostConstruct
    public void iniciar() {
        carregarOpcoes();
        pesquisar();
    }

    private void carregarOpcoes() {
        try {
            colaboradores = new ColaboradorDAO().listar("");
            modelos = new ModeloContratoDAO().listar("");
            tipos = new TipoContratoDAO().listar(false);
        } catch (SQLException e) { erro(e); }
    }

    public void pesquisar() {
        try { lista = new ContratoDAO().listar(pesquisa); }
        catch (SQLException e) {
            lista = new ArrayList<>();
            erro(e);
        }
    }

    public void carregarEdicao() {
        if (idEdicao == null) return;
        try {
            Contrato encontrado = new ContratoDAO().buscar(idEdicao);
            if (encontrado == null) {
                aviso(FacesMessage.SEVERITY_ERROR, "Contrato não encontrado.");
                return;
            }
            edicao = encontrado;
            atualizarPrevia();
        } catch (SQLException e) { erro(e); }
    }

    public void selecionarTipo() {
        edicao.setModeloId(null);
        htmlPrevia = "";
    }

    public void atualizarPrevia() {
        htmlPrevia = "";
        try {
            String conteudo = null;
            if (edicao.getModeloId() != null) {
                ModeloContrato modelo = new ModeloContratoDAO().buscar(edicao.getModeloId());
                if (modelo != null && Objects.equals(modelo.getTipoId(), edicao.getTipoId())) {
                    conteudo = modelo.getConteudo();
                }
            } else if (edicao.getId() != null) {
                // Contratos antigos sem modelo continuam disponíveis.
                conteudo = edicao.getConteudo();
            }
            if (conteudo == null) return;
            Colaborador colaborador = edicao.getColaboradorId() == null
                ? null : new ColaboradorDAO().buscar(edicao.getColaboradorId());
            if (colaborador == null) {
                htmlPrevia = DocumentoService.sanitizar(conteudo);
            } else {
                htmlPrevia = DocumentoService.preencher(conteudo, colaborador,
                        new ConfiguracaoDocumentoDAO().buscarLogo());
            }
        } catch (SQLException e) { erro(e); }
    }

    public String gerar() {
        if (edicao.getColaboradorId() == null) {
            aviso(FacesMessage.SEVERITY_WARN, "Selecione o colaborador.");
            return null;
        }
        try {
            Colaborador colaborador = new ColaboradorDAO().buscar(edicao.getColaboradorId());
            if (colaborador == null) {
                aviso(FacesMessage.SEVERITY_WARN, "Colaborador não encontrado.");
                return null;
            }
            if (edicao.getModeloId() != null) {
                ModeloContrato modelo = new ModeloContratoDAO().buscar(edicao.getModeloId());
                if (modelo == null || !Objects.equals(modelo.getTipoId(), edicao.getTipoId())) {
                    aviso(FacesMessage.SEVERITY_WARN, "Selecione um modelo válido para esse tipo.");
                    return null;
                }
                // O documento emitido guarda uma cópia do modelo neste momento.
                // Se o modelo for alterado depois, o contrato salvo não muda.
                edicao.setConteudo(DocumentoService.sanitizar(modelo.getConteudo()));
                edicao.setCategoria(modelo.getCategoria());
                if (edicao.getTitulo() == null || edicao.getTitulo().isBlank()) {
                    edicao.setTitulo(modelo.getTitulo());
                }
            } else if (edicao.getId() == null || edicao.getConteudo() == null
                    || edicao.getConteudo().isBlank()) {
                aviso(FacesMessage.SEVERITY_WARN, "Selecione um modelo para gerar o contrato.");
                return null;
            }
            if (edicao.getTitulo() == null || edicao.getTitulo().isBlank()) {
                aviso(FacesMessage.SEVERITY_WARN, "Informe o título.");
                return null;
            }
            edicao.setTitulo(edicao.getTitulo().strip());
            new ContratoDAO().salvar(edicao);
            return "/visualizar-contrato.xhtml?faces-redirect=true&id=" + edicao.getId();
        } catch (SQLException e) {
            erro(e);
            return null;
        }
    }

    public void excluir(Long id) {
        try {
            new ContratoDAO().excluir(id);
            pesquisar();
            aviso(FacesMessage.SEVERITY_INFO, "Contrato excluído.");
        } catch (SQLException e) { erro(e); }
    }

    public String nomeColaborador(Long id) {
        for (Colaborador c : colaboradores) if (Objects.equals(c.getId(), id)) return c.getNome();
        return "Colaborador indisponível";
    }

    public String nomeTipo(Long id) {
        for (TipoContrato t : tipos) if (Objects.equals(t.getId(), id)) return t.getNome();
        return "Outros";
    }

    private void erro(SQLException e) {
        LOG.log(Level.WARNING, "Falha em contratos. SQLState={0}", e.getSQLState());
        aviso(FacesMessage.SEVERITY_ERROR,
                "Não foi possível concluir a operação. Confira as migrações e a conexão do PostgreSQL.");
    }

    private void aviso(FacesMessage.Severity nivel, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(nivel, texto, null));
    }

    public String getPesquisa() { return pesquisa; }
    public void setPesquisa(String pesquisa) { this.pesquisa = pesquisa; }
    public Long getIdEdicao() { return idEdicao; }
    public void setIdEdicao(Long idEdicao) { this.idEdicao = idEdicao; }
    public Contrato getEdicao() { return edicao; }
    public void setEdicao(Contrato edicao) { this.edicao = edicao; }
    public List<Contrato> getLista() { return lista; }
    public List<Colaborador> getColaboradores() { return colaboradores; }
    public List<ModeloContrato> getModelosDoTipo() {
        List<ModeloContrato> resultado = new ArrayList<>();
        for (ModeloContrato m : modelos) {
            if (Objects.equals(m.getTipoId(), edicao.getTipoId())) resultado.add(m);
        }
        return resultado;
    }
    public List<TipoContrato> getTipos() { return tipos; }
    public SituacaoContrato[] getSituacoes() { return SituacaoContrato.values(); }
    public String getHtmlPrevia() { return htmlPrevia; }
}
