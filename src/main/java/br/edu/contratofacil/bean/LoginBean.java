package br.edu.contratofacil.bean;

import br.edu.contratofacil.dao.UsuarioDAO;
import br.edu.contratofacil.model.Usuario;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.sql.SQLException;

@Named
@RequestScoped
public class LoginBean {
    private String login;
    private String senha;

    public String entrar() {
        try {
            Usuario usuario = new UsuarioDAO().autenticar(login, senha);
            senha = null;
            if (usuario == null) {
                mensagem("Usuário ou senha inválidos.");
                return null;
            }

            HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance()
                    .getExternalContext().getRequest();
            HttpSession sessao = req.getSession(true);
            req.changeSessionId();
            sessao.setAttribute("usuarioLogado", usuario);
            return "/index.xhtml?faces-redirect=true";
        } catch (SQLException e) {
            senha = null;
            mensagem("Não foi possível autenticar. Confira a conexão e a tabela de usuários.");
            return null;
        }
    }

    public String sair() {
        FacesContext contexto = FacesContext.getCurrentInstance();
        HttpSession sessao = (HttpSession) contexto.getExternalContext().getSession(false);
        if (sessao != null) {
            sessao.invalidate();
        }
        return "/login.xhtml?faces-redirect=true";
    }

    private void mensagem(String texto) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, texto, null));
    }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
