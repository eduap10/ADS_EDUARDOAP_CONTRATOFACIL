package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.TipoContratoDAO;
import br.edu.contratofacil.model.TipoContrato;
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

@Named("tipoContratoBean")
@ViewScoped
public class TipoContratoBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TipoContratoBean.class.getName());
    private TipoContrato edicao = new TipoContrato();
    private List<TipoContrato> lista = new ArrayList<>();

    @PostConstruct
    public void iniciar() { atualizar(); }

    public void atualizar() {
        try { lista = new TipoContratoDAO().listar(false); }
        catch (SQLException e) {
            lista = new ArrayList<>();
            erro(e, "Não foi possível consultar os tipos. Execute database/11_tipos_contrato.sql.");
        }
    }

    public void novo() { edicao = new TipoContrato(); }

    public void editar(TipoContrato selecionado) {
        edicao = new TipoContrato();
        edicao.setId(selecionado.getId());
        edicao.setNome(selecionado.getNome());
        edicao.setDescricao(selecionado.getDescricao());
        edicao.setAtivo(selecionado.isAtivo());
    }

    public void salvar() {
        try {
            new TipoContratoDAO().salvar(edicao);
            atualizar();
            novo();
            mensagem(FacesMessage.SEVERITY_INFO, "Tipo salvo com sucesso.");
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState()))
                mensagem(FacesMessage.SEVERITY_WARN, "Já existe um tipo com esse nome.");
            else if ("22023".equals(e.getSQLState()))
                mensagem(FacesMessage.SEVERITY_WARN, e.getMessage());
            else erro(e, "Não foi possível salvar o tipo.");
        }
    }

    public void excluir(Long id) {
        try {
            new TipoContratoDAO().excluir(id);
            atualizar();
            mensagem(FacesMessage.SEVERITY_INFO, "Tipo excluído.");
        } catch (SQLException e) {
            if ("23503".equals(e.getSQLState()))
                mensagem(FacesMessage.SEVERITY_WARN,
                    "Este tipo já é utilizado. Desative-o em vez de excluir.");
            else erro(e, "Não foi possível excluir o tipo.");
        }
    }

    private void erro(SQLException e, String texto) {
        LOG.log(Level.WARNING, "Falha em tipos de contrato. SQLState={0}", e.getSQLState());
        mensagem(FacesMessage.SEVERITY_ERROR, texto);
    }

    private void mensagem(FacesMessage.Severity nivel, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(nivel, texto, null));
    }

    public TipoContrato getEdicao() { return edicao; }
    public void setEdicao(TipoContrato edicao) { this.edicao = edicao; }
    public List<TipoContrato> getLista() { return lista; }
}
