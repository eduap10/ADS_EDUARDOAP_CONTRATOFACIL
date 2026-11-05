package br.edu.contratofacil.filter;

import br.edu.contratofacil.dao.UsuarioDAO;
import br.edu.contratofacil.model.PerfilUsuario;
import br.edu.contratofacil.model.Usuario;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;

public class FiltroAcesso implements Filter {

    @Override
    public void doFilter(ServletRequest requisicao, ServletResponse resposta, FilterChain cadeia)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) requisicao;
        HttpServletResponse resp = (HttpServletResponse) resposta;

        String contexto = req.getContextPath();
        // O container normaliza o destino e remove parametros de caminho como ;jsessionid.
        String caminho = req.getServletPath();

        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("X-Frame-Options", "DENY");

        if (caminho.startsWith("/jakarta.faces.resource/")
                || caminho.startsWith("/javax.faces.resource/")
                || caminho.startsWith("/resources/") || caminho.equals("/favicon.ico")) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        if (caminho.equals("/primeiro-acesso.xhtml")) {
            // A configuração inicial só pode ser feita no próprio computador.
            if (!ehLocal(req.getRemoteAddr())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            try {
                if (new UsuarioDAO().possuiUsuarios()) {
                    resp.sendRedirect(contexto + "/login.xhtml");
                    return;
                }
            } catch (SQLException e) {
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                        "Execute database/06_usuarios.sql e confira a conexão com o banco.");
                return;
            }
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        if (caminho.equals("/login.xhtml")) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }

        HttpSession sessao = req.getSession(false);
        Usuario usuario = sessao == null ? null : (Usuario) sessao.getAttribute("usuarioLogado");
        if (usuario == null) {
            redirecionarLogin(req, resp);
            return;
        }

        // Toda funcao autenticada deste sistema e administrativa, inclusive novos downloads.
        if (usuario.getPerfil() != PerfilUsuario.ADMIN) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private void redirecionarLogin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String destino = req.getContextPath() + "/login.xhtml";
        if ("partial/ajax".equals(req.getHeader("Faces-Request"))) {
            resp.setContentType("text/xml;charset=UTF-8");
            resp.getWriter().write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<partial-response><redirect url=\"" + destino + "\"></redirect></partial-response>");
        } else {
            resp.sendRedirect(destino);
        }
    }

    private boolean ehLocal(String endereco) {
        try {
            return InetAddress.getByName(endereco).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
