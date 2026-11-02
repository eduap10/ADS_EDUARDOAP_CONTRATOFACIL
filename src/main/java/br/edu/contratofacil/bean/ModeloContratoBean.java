package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.ModeloContratoDAO;
import br.edu.contratofacil.model.ModeloContrato;
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
import org.primefaces.PrimeFaces;

@Named
@ViewScoped
public class ModeloContratoBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ModeloContratoBean.class.getName());

    private String pesquisa = "";
    private ModeloContrato edicao = new ModeloContrato();
    private List<ModeloContrato> lista = new ArrayList<>();

    @PostConstruct
    public void iniciar() {
        pesquisar();
    }

    public void pesquisar() {
        try {
            lista = new ModeloContratoDAO().listar(pesquisa);
        } catch (SQLException e) {
            lista = new ArrayList<>();
            erroBanco(e);
        }
    }

    public void novo() {
        edicao = new ModeloContrato();
        PrimeFaces.current().resetInputs("form:campos");
    }

    public void editar(ModeloContrato selecionado) {
        edicao = new ModeloContrato();
        edicao.setId(selecionado.getId());
        edicao.setTitulo(selecionado.getTitulo());
        edicao.setConteudo(selecionado.getConteudo());
        PrimeFaces.current().resetInputs("form:campos");
    }

    public void salvar() {
        boolean salvo = false;
        try {
            if (edicao.getTitulo() == null || edicao.getTitulo().isBlank()
                    || edicao.getConteudo() == null || edicao.getConteudo().isBlank()) {
                mensagem(FacesMessage.SEVERITY_WARN, "Informe título e conteúdo.");
                return;
            }

            new ModeloContratoDAO().salvar(edicao);
            novo();
            pesquisar();
            salvo = true;
            mensagem(FacesMessage.SEVERITY_INFO, "Modelo salvo com sucesso.");
        } catch (SQLException e) {
            erroBanco(e);
        } finally {
            PrimeFaces.current().ajax().addCallbackParam("salvo", salvo);
        }
    }

    public void excluir(Long id) {
        try {
            new ModeloContratoDAO().excluir(id);
            pesquisar();
            mensagem(FacesMessage.SEVERITY_INFO, "Modelo excluído.");
        } catch (SQLException e) {
            erroBanco(e);
        }
    }

    private void erroBanco(SQLException e) {
        LOG.log(Level.WARNING, "Falha no cadastro de modelos. SQLState={0}", e.getSQLState());

        String texto;
        if (e.getMessage() != null && e.getMessage().contains("DB_PASSWORD nao definida")) {
            texto = "Senha do banco não configurada. Reinicie o Tomcat informando DB_PASSWORD.";
        } else if ("42P01".equals(e.getSQLState())) {
            texto = "Tabela de modelos não encontrada. Execute database/04_modelos_contratos.sql.";
        } else if ("28P01".equals(e.getSQLState())) {
            texto = "Usuário ou senha do banco inválidos.";
        } else if ("3D000".equals(e.getSQLState())) {
            texto = "Banco contratofacil não encontrado.";
        } else if ("42501".equals(e.getSQLState())) {
            texto = "Usuário do banco sem permissão para esta operação.";
        } else if ("22023".equals(e.getSQLState())) {
            texto = "Informe título e conteúdo do modelo.";
        } else if ("02000".equals(e.getSQLState())) {
            texto = "Modelo não encontrado. Atualize a lista e tente novamente.";
        } else {
            texto = "Não foi possível concluir a operação. Confira a conexão e o log do Tomcat.";
        }

        mensagem(FacesMessage.SEVERITY_ERROR, texto);
    }

    private void mensagem(FacesMessage.Severity tipo, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(tipo, texto, null));
    }

    public String getPesquisa() { return pesquisa; }
    public void setPesquisa(String pesquisa) { this.pesquisa = pesquisa; }
    public ModeloContrato getEdicao() { return edicao; }
    public void setEdicao(ModeloContrato edicao) { this.edicao = edicao; }
    public List<ModeloContrato> getLista() { return lista; }
}
