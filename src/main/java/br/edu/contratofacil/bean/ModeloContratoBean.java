package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.ModeloContratoDAO;
import br.edu.contratofacil.dao.TipoContratoDAO;
import br.edu.contratofacil.model.ModeloContrato;
import br.edu.contratofacil.model.TipoContrato;
import br.edu.contratofacil.service.DocumentoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Named("modeloContratoBean")
@ViewScoped
public class ModeloContratoBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ModeloContratoBean.class.getName());

    private String pesquisa = "";
    private Long idEdicao;
    private ModeloContrato edicao = new ModeloContrato();
    private List<ModeloContrato> lista = new ArrayList<>();
    private List<TipoContrato> tipos = new ArrayList<>();

    @PostConstruct
    public void iniciar() {
        pesquisar();
        try { tipos = new TipoContratoDAO().listar(false); }
        catch (SQLException e) { erroBanco(e); }
    }

    public void pesquisar() {
        try { lista = new ModeloContratoDAO().listar(pesquisa); }
        catch (SQLException e) { lista = new ArrayList<>(); erroBanco(e); }
    }

    public void carregarEdicao() {
        if (idEdicao == null) return;
        try {
            ModeloContrato encontrado = new ModeloContratoDAO().buscar(idEdicao);
            if (encontrado == null) {
                mensagem(FacesMessage.SEVERITY_ERROR, "Modelo não encontrado.");
                return;
            }
            edicao = encontrado;
        } catch (SQLException e) { erroBanco(e); }
    }

    public String salvarERetornar() {
        if (edicao.getTipoId() == null || edicao.getTitulo() == null
                || edicao.getTitulo().isBlank() || edicao.getConteudo() == null) {
            mensagem(FacesMessage.SEVERITY_WARN, "Informe tipo, título e conteúdo do modelo.");
            return null;
        }
        String seguro = DocumentoService.sanitizar(edicao.getConteudo());
        if (org.jsoup.Jsoup.parseBodyFragment(seguro).text().isBlank()
                && !seguro.contains("{{empresa.logo}}")) {
            mensagem(FacesMessage.SEVERITY_WARN, "Escreva o conteúdo do modelo.");
            return null;
        }
        try {
            TipoContrato t = new TipoContratoDAO().buscar(edicao.getTipoId());
            if (t == null || (!t.isAtivo() && edicao.getId() == null)) {
                mensagem(FacesMessage.SEVERITY_WARN, "Selecione um tipo de contrato ativo.");
                return null;
            }
            edicao.setConteudo(seguro);
            new ModeloContratoDAO().salvar(edicao);
            return "/modelos.xhtml?faces-redirect=true";
        } catch (SQLException e) {
            erroBanco(e);
            return null;
        }
    }

    public void excluir(Long id) {
        try {
            new ModeloContratoDAO().excluir(id);
            pesquisar();
            mensagem(FacesMessage.SEVERITY_INFO, "Modelo excluído. Contratos já gerados preservam seu texto.");
        } catch (SQLException e) { erroBanco(e); }
    }

    public String nomeTipo(Long id) {
        for (TipoContrato t : tipos) if (t.getId().equals(id)) return t.getNome();
        return "Outros";
    }

    private void erroBanco(SQLException e) {
        LOG.log(Level.WARNING, "Falha em modelos. SQLState={0}", e.getSQLState());
        mensagem(FacesMessage.SEVERITY_ERROR,
                "Não foi possível consultar ou salvar modelos. Verifique o banco e a migração 11.");
    }

    private void mensagem(FacesMessage.Severity tipo, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(tipo, texto, null));
    }

    public String getPesquisa() { return pesquisa; }
    public void setPesquisa(String pesquisa) { this.pesquisa = pesquisa; }
    public Long getIdEdicao() { return idEdicao; }
    public void setIdEdicao(Long idEdicao) { this.idEdicao = idEdicao; }
    public ModeloContrato getEdicao() { return edicao; }
    public void setEdicao(ModeloContrato edicao) { this.edicao = edicao; }
    public List<ModeloContrato> getLista() { return lista; }
    public List<TipoContrato> getTipos() { return tipos; }
}
