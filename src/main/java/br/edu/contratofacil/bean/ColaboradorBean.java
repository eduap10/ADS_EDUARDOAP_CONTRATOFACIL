package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.ColaboradorDAO;
import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.Colaborador;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.PrimeFaces;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            statusBanco = "Conexao indisponivel";
            erro("consultar colaboradores", e);
        }
    }

    public void testarConexao() {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement("SELECT 1 FROM colaboradores LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            statusBanco = "Conectado";
            mensagem(FacesMessage.SEVERITY_INFO, "Banco PostgreSQL e tabela colaboradores acessiveis.");
            pesquisar();
        } catch (SQLException e) {
            statusBanco = "Conexao indisponivel";
            erro("testar banco e tabela", e);
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
            erro("salvar colaborador", e);
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
            if ("23503".equals(e.getSQLState())) {
                mensagem(FacesMessage.SEVERITY_WARN, "Colaborador possui contratos vinculados e nao pode ser excluido.");
            } else {
                erro("excluir colaborador", e);
            }
        }
    }

    private void erro(String operacao, SQLException e) {
        // Causa completa nos logs do Tomcat; a interface nao revela credenciais.
        LOG.log(Level.SEVERE, "Falha ao " + operacao + " (SQLState=" + e.getSQLState() + ")", e);
        String estado = e.getSQLState();
        String orientacao;
        if ("23505".equals(estado)) orientacao = "A matricula informada ja existe.";
        else if ("42P01".equals(estado)) orientacao = "Tabela nao encontrada. Execute database/01_schema.sql.";
        else if ("42501".equals(estado)) orientacao = "O usuario do PostgreSQL nao tem permissao na tabela.";
        else if (estado != null && estado.startsWith("28")) orientacao = "Credenciais invalidas no servidor.";
        else if ("3D000".equals(estado)) orientacao = "Banco contratofacil nao encontrado.";
        else if (estado != null && estado.startsWith("08")) orientacao = "Conexao com o PostgreSQL indisponivel. Confira DB_PASSWORD e o Tomcat.";
        else orientacao = "Falha no banco. Consulte o log do Tomcat para ver o SQLState.";
        mensagem(FacesMessage.SEVERITY_ERROR, orientacao);
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
