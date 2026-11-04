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
        String caminho = req.getRequestURI().substring(contexto.length());

        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");

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
            resp.sendRedirect(contexto + "/login.xhtml");
            return;
        }

        // Somente administradores podem abrir as páginas de manutenção e contratos.
        if ((caminho.equals("/colaboradores.xhtml") || caminho.equals("/modelos.xhtml")
                || caminho.equals("/contratos.xhtml")) && usuario.getPerfil() != PerfilUsuario.ADMIN) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        cadeia.doFilter(requisicao, resposta);
    }

    private boolean ehLocal(String endereco) {
        try {
            return InetAddress.getByName(endereco).isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
