package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.ConfiguracaoDocumentoDAO;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.file.UploadedFile;
import java.io.Serializable;
import java.sql.SQLException;

@Named("configuracaoDocumentoBean")
@ViewScoped
public class ConfiguracaoDocumentoBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int LIMITE = 1_000_000;

    public void enviar(FileUploadEvent evento) {
        UploadedFile arquivo = evento.getFile();
        byte[] dados = arquivo == null ? null : arquivo.getContent();
        if (dados == null || dados.length < 8 || dados.length > LIMITE) {
            mensagem(FacesMessage.SEVERITY_ERROR, "Selecione uma imagem PNG ou JPEG de até 1 MB.");
            return;
        }
        boolean png = dados[0] == (byte)137 && dados[1] == 80 && dados[2] == 78 && dados[3] == 71
                   && dados[4] == 13 && dados[5] == 10 && dados[6] == 26 && dados[7] == 10;
        boolean jpeg = dados[0] == (byte)255 && dados[1] == (byte)216 && dados[2] == (byte)255;
        if (!png && !jpeg) {
            mensagem(FacesMessage.SEVERITY_ERROR, "Arquivo inválido. Aceitos somente PNG ou JPEG.");
            return;
        }
        try {
            new ConfiguracaoDocumentoDAO().salvarLogo(dados, png ? "image/png" : "image/jpeg");
            mensagem(FacesMessage.SEVERITY_INFO, "Logo salvo. Insira a tag {{empresa.logo}} no documento.");
        } catch (SQLException e) {
            mensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível salvar o logo. Confira database/09_logo_empresa.sql.");
        }
    }

    private void mensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }
}
