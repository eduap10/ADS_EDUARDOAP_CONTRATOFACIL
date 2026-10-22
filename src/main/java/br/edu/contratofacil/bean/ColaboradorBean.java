package br.edu.contratofacil.bean;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.primefaces.PrimeFaces;

import br.edu.contratofacil.dao.ColaboradorDAO;
import br.edu.contratofacil.model.Colaborador;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;

@Named
@ViewScoped
public class ColaboradorBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ColaboradorBean.class.getName());
    private String pesquisa = "";
    private String statusBanco = "Nao verificado";
    private Colaborador edicao = new Colaborador();
    private List<Colaborador> lista = new ArrayList<>();

    @PostConstruct
    public void iniciar() { pesquisar(); }

    public void pesquisar() {
        try {
            lista = new ColaboradorDAO().listar(pesquisa);
            statusBanco = "Conectado";
        } catch (SQLException e) {
            lista = new ArrayList<>();
            mensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível consultar os colaboradores. Verifique o banco.");
        }
    }

    public void novo() {
        edicao = new Colaborador();
        PrimeFaces.current().resetInputs("form:campos");
    }

    public void editar(Colaborador selecionado) {
        edicao = new Colaborador();
        edicao.setId(selecionado.getId());
        edicao.setMatricula(selecionado.getMatricula());
        edicao.setNome(selecionado.getNome());
        edicao.setCargo(selecionado.getCargo());
        edicao.setCpf(selecionado.getCpf());
        edicao.setEmail(selecionado.getEmail());
        PrimeFaces.current().resetInputs("form:campos");
    }

    public void salvar() {
        boolean salvo = false;
        try {
            if (edicao.getMatricula() == null || edicao.getMatricula().isBlank()
                    || edicao.getNome() == null || edicao.getNome().isBlank()) {
                mensagem(FacesMessage.SEVERITY_WARN, "Preencha matricula e nome.");
                return;
            }
            edicao.setMatricula(edicao.getMatricula().strip());
            edicao.setNome(edicao.getNome().strip());
            new ColaboradorDAO().salvar(edicao);
            novo();
            salvo = true;
            pesquisar();
            mensagem(FacesMessage.SEVERITY_INFO, "Colaborador salvo com sucesso.");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()))
                mensagem(FacesMessage.SEVERITY_WARN, "Já existe um colaborador com essa matrícula.");
            else
                mensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível salvar. Verifique a conexão.");
        } finally {
            PrimeFaces.current().ajax().addCallbackParam("salvo", salvo);
        }
    }

    public void excluir(Long id) {
        try {
            new ColaboradorDAO().excluir(id);
            pesquisar();
            mensagem(FacesMessage.SEVERITY_INFO, "Colaborador excluido.");
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState()))
                mensagem(FacesMessage.SEVERITY_WARN, "Colaborador possui contratos vinculados.");
            else mensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível excluir o colaborador.");
        }
    }

    private void mensagem(FacesMessage.Severity tipo, String texto) {
        FacesContext contexto = FacesContext.getCurrentInstance();
        if (contexto != null) contexto.addMessage(null, new FacesMessage(tipo, texto, null));
    }

    public String getPesquisa() { return pesquisa; }
    public void setPesquisa(String pesquisa) { this.pesquisa = pesquisa; }
    public String getStatusBanco() { return statusBanco; }
    public Colaborador getEdicao() { return edicao; }
    public void setEdicao(Colaborador edicao) { this.edicao = edicao; }
    public List<Colaborador> getLista() { return lista; }
}
