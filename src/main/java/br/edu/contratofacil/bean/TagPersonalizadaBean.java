package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.TagPersonalizadaDAO;
import br.edu.contratofacil.model.TagPersonalizada;
import br.edu.contratofacil.service.CatalogoCampos;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

@Named("tagPersonalizadaBean")
@ViewScoped
/**
 * Responsabilidade da camada de apresentação.
 * Permite cadastrar tags personalizadas, independentes dos campos fixos da ficha.
 *  * Essas tags compõem o catálogo usado no editor e são resolvidas na emissão.
 */
public class TagPersonalizadaBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TagPersonalizadaBean.class.getName());
    private TagPersonalizada edicao = new TagPersonalizada();
    private List<TagPersonalizada> lista = new ArrayList<>();

    @PostConstruct public void iniciar() { atualizar(); }
    public void atualizar() {
        try { lista = new TagPersonalizadaDAO().listar(false); }
        catch (SQLException e) { lista = new ArrayList<>(); erro(e); }
    }
    public void novo() { edicao = new TagPersonalizada(); }
    public void editar(TagPersonalizada tag) {
        edicao = new TagPersonalizada();
        edicao.setId(tag.getId());
        edicao.setCodigo(tag.getCodigo());
        edicao.setNome(tag.getNome());
        edicao.setDescricao(tag.getDescricao());
        edicao.setTipo(tag.getTipo());
        edicao.setReferencia(tag.getReferencia());
        edicao.setValorFixo(tag.getValorFixo());
        edicao.setAtivo(tag.isAtivo());
    }
    public void salvar() {
        try {
            new TagPersonalizadaDAO().salvar(edicao);
            atualizar();
            novo();
            mensagem(FacesMessage.SEVERITY_INFO, "Campo salvo. Pode ser inserido no Editor de Modelos.");
        } catch (SQLException e) {
            if ("22023".equals(e.getSQLState()) || "23505".equals(e.getSQLState()))
                mensagem(FacesMessage.SEVERITY_WARN, "23505".equals(e.getSQLState())
                    ? "Já existe uma tag com esse código." : e.getMessage());
            else erro(e);
        }
    }
    private void erro(SQLException e) {
        LOG.warning("Falha em tags personalizadas: " + e.getSQLState());
        mensagem(FacesMessage.SEVERITY_ERROR, "Erro ao acessar tags. Confira a migração 13 e a conexão com o banco.");
    }
    private void mensagem(FacesMessage.Severity tipo, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(tipo, texto, null));
    }
    public TagPersonalizada getEdicao() { return edicao; }
    public void setEdicao(TagPersonalizada edicao) { this.edicao = edicao; }
    public List<TagPersonalizada> getLista() { return lista; }
    public List<CatalogoCampos.Campo> getCamposDaFicha() {
        return CatalogoCampos.todos().stream()
            .filter(c -> c.getChave().startsWith("colaborador.")).toList();
    }
}
