package br.edu.contratofacil;

import br.edu.contratofacil.dao.UsuarioDAO;
import br.edu.contratofacil.db.Conexao;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "CF_TEST_URL", matches = "http://localhost:8081/contratofacil/")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AutenticacaoIT {
    private final String prefix = "teste_" + UUID.randomUUID().toString().replace("-", "");
    private final String password = UUID.randomUUID() + "A1";
    private boolean firstAdmin;

    @BeforeAll void preparar() throws Exception {
        try (var c = Conexao.abrir()) { assertEquals("contratofacil", c.getCatalog()); }
        firstAdmin = !new UsuarioDAO().possuiUsuarios();
        if (firstAdmin) {
            var response = new HttpTeste().form("primeiro-acesso.xhtml", "formInicial", Map.of(
                    "formInicial:login", prefix, "formInicial:senha", password, "formInicial:confirmacao", password));
            assertEquals(302, response.statusCode(), "Cadastro inicial deve redirecionar ao login");
            assertNotNull(new UsuarioDAO().autenticar(prefix, password));
        } else { inserir(prefix, "ADMIN", true); }
        inserir(prefix + "_user", "USUARIO", true);
        inserir(prefix + "_inativo", "ADMIN", false);
    }

    private void inserir(String login, String profile, boolean active) throws Exception {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        var spec = new PBEKeySpec(password.toCharArray(), salt, 210_000, 256);
        byte[] hash;
        try { hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
        try (var c = Conexao.abrir(); var p = c.prepareStatement(
                "INSERT INTO public.usuarios(login,senha_hash,senha_salt,perfil,ativo) VALUES (?,?,?,?,?)")) {
            p.setString(1, login); p.setString(2, HexFormat.of().formatHex(hash));
            p.setString(3, HexFormat.of().formatHex(salt)); p.setString(4, profile); p.setBoolean(5, active); p.executeUpdate();
        }
    }

    @AfterAll void limparSomenteUsuariosDoTeste() throws Exception {
        try (var c = Conexao.abrir(); var p = c.prepareStatement("DELETE FROM public.usuarios WHERE login IN (?,?,?)")) {
            p.setString(1, prefix); p.setString(2, prefix + "_user"); p.setString(3, prefix + "_inativo"); p.executeUpdate();
        }
    }

    @Test void loginCorretoRenovaSessaoELogoutInvalida() throws Exception {
        var web = new HttpTeste();
        assertEquals(200, web.get("login.xhtml").statusCode());
        String before = web.sessionId();
        assertEquals(302, web.login(prefix, password).statusCode());
        assertNotEquals(before, web.sessionId(), "Login deve renovar identificador da sessao");
        assertEquals(200, web.get("colaboradores.xhtml").statusCode());
        var form = org.jsoup.Jsoup.parse(web.get("index.xhtml").body()).getElementById("formSair");
        String linkId = form.selectFirst("a[id]").id();
        var response = web.form("index.xhtml", "formSair", Map.of(linkId, linkId));
        assertEquals(302, response.statusCode());
        assertEquals(302, web.get("colaboradores.xhtml").statusCode());
    }

    @Test void senhaIncorretaEUsuarioInativoNaoAutenticam() throws Exception {
        var web = new HttpTeste();
        var response = web.login(prefix, UUID.randomUUID().toString());
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("inválidos"));
        assertEquals(302, web.get("modelos.xhtml").statusCode());
        assertEquals(200, web.login(prefix + "_inativo", password).statusCode());
        assertEquals(302, web.get("contratos.xhtml").statusCode());
    }

    @Test void acessoDiretoSemSessaoRedireciona() throws Exception {
        var web = new HttpTeste();
        for (String path : new String[]{"colaboradores.xhtml", "modelos.xhtml", "contratos.xhtml", "resources/../colaboradores.xhtml", "contrato-editor.xhtml", "documento.xhtml", "exportar?id=1&formato=pdf"}) {
            var response = web.get(path);
            assertEquals(302, response.statusCode(), path);
            assertTrue(response.headers().firstValue("location").orElse("").endsWith("/login.xhtml"));
        }
    }

    @Test void usuarioComumNaoAcessaAdministracaoNemVariaçãoDaUrl() throws Exception {
        var web = new HttpTeste();
        assertEquals(302, web.login(prefix + "_user", password).statusCode());
        for (String path : new String[]{"colaboradores.xhtml", "modelos.xhtml", "contratos.xhtml", "colaboradores.xhtml;teste=1", "contrato-editor.xhtml", "documento.xhtml", "exportar?id=1&formato=pdf"}) {
            assertEquals(403, web.get(path).statusCode(), path);
        }
    }

    @Test void primeiroAcessoFechaAposCadastro() throws Exception {
        assertEquals(302, new HttpTeste().get("primeiro-acesso.xhtml").statusCode());
        assertFalse(new UsuarioDAO().criarAdministradorInicial(prefix + "_segundo", password));
    }

    @Test void sessaoExpiradaEmAjaxRecebeRedirecionamentoJsf() throws Exception {
        var response = new HttpTeste().post("contratos.xhtml", Map.of("jakarta.faces.partial.ajax", "true"), Map.of("Faces-Request", "partial/ajax"));
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("<partial-response><redirect"));
        assertTrue(response.body().contains("/login.xhtml"));
    }
}
