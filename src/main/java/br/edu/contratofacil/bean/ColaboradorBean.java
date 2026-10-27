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
            statusBanco = "Falha de conexão";
            erroBanco("consultar", e);
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
                erroBanco("salvar", e);
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
            else erroBanco("excluir", e);
        }
    }

    private void erroBanco(String operacao, SQLException erro) {
        LOG.log(java.util.logging.Level.WARNING,
                "Falha ao {0} colaboradores. SQLState={1}",
                new Object[]{operacao, erro.getSQLState()});

        String estado = erro.getSQLState();
        String texto;
        if (erro.getMessage() != null && erro.getMessage().contains("DB_PASSWORD nao definida")) {
            texto = "Senha do banco não configurada. Reinicie o Tomcat informando DB_PASSWORD.";
        } else if ("42P01".equals(estado)) {
            texto = "Tabela de colaboradores não encontrada. Execute database/01_schema.sql.";
        } else if ("28P01".equals(estado)) {
            texto = "Usuário ou senha do banco inválidos. Confira as credenciais locais.";
        } else if ("3D000".equals(estado)) {
            texto = "Banco contratofacil não encontrado. Confira a configuração do PostgreSQL.";
        } else if ("42501".equals(estado)) {
            texto = "Usuário do banco sem permissão para esta operação.";
        } else {
            texto = "Não foi possível " + operacao + " colaboradores. Confira a conexão e os logs do Tomcat.";
        }
        mensagem(FacesMessage.SEVERITY_ERROR, texto);
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
