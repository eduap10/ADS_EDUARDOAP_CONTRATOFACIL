package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.UsuarioDAO;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;

import java.sql.SQLException;

@Named
@RequestScoped
public class PrimeiroAcessoBean {
    private String login;
    private String senha;
    private String confirmacao;

    public String criar() {
        if (senha == null || !senha.equals(confirmacao)) {
            mensagem(FacesMessage.SEVERITY_WARN, "As senhas não conferem.");
            return null;
        }
        try {
            boolean criado = new UsuarioDAO().criarAdministradorInicial(login, senha);
            senha = null;
            confirmacao = null;
            if (!criado) {
                mensagem(FacesMessage.SEVERITY_WARN, "Administrador já cadastrado.");
                return "/login.xhtml?faces-redirect=true";
            }
            return "/login.xhtml?faces-redirect=true";
        } catch (SQLException e) {
            senha = null;
            confirmacao = null;
            mensagem(FacesMessage.SEVERITY_ERROR, "Confira os campos e a configuração do banco. "
                    + "O usuário deve ter 3 a 60 caracteres; a senha, 10 caracteres, letra e número.");
            return null;
        }
    }

    private void mensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(severidade, texto, null));
    }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmacao() { return confirmacao; }
    public void setConfirmacao(String confirmacao) { this.confirmacao = confirmacao; }
}
