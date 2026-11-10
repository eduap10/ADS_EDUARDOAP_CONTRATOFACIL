package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.ContratoDAO;
import br.edu.contratofacil.dao.ColaboradorDAO;
import br.edu.contratofacil.model.Contrato;
import br.edu.contratofacil.model.Colaborador;
import br.edu.contratofacil.service.DocumentoService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;

@Named("visualizacaoBean")
@ViewScoped
public class VisualizacaoBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Contrato contrato;
    private String html = "";

    public void carregar() {
        if (id == null) return;
        try {
            contrato = new ContratoDAO().buscar(id);
            if (contrato == null) throw new SQLException("Contrato não encontrado.");
            Colaborador colaborador = new ColaboradorDAO().buscar(contrato.getColaboradorId());
            if (colaborador == null) throw new SQLException("Colaborador não encontrado.");
            html = DocumentoService.preencher(contrato.getConteudo(), colaborador,
                    new br.edu.contratofacil.dao.ConfiguracaoDocumentoDAO().buscarLogo());
        } catch (SQLException e) {
            contrato = null;
            html = "";
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Documento indisponível. Verifique os registros.", null));
        }
    }
    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Contrato getContrato(){return contrato;}
    public String getHtml(){return html;}
}
