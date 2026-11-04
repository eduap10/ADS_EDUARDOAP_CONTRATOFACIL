package br.edu.contratofacil.dao;

import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.PerfilUsuario;
import br.edu.contratofacil.model.Usuario;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class UsuarioDAO {
    private static final int ITERACOES = 210_000;
    private static final int TAMANHO_HASH = 256;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public boolean possuiUsuarios() throws SQLException {
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(
                     "SELECT EXISTS (SELECT 1 FROM public.usuarios)")) {
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }

    public Usuario autenticar(String login, String senha) throws SQLException {
        if (login == null || senha == null) {
            return null;
        }
        String sql = "SELECT id, login, senha_hash, senha_salt, perfil "
                + "FROM public.usuarios WHERE login = ? AND ativo = TRUE";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, normalizarLogin(login));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                byte[] salt = HexFormat.of().parseHex(rs.getString("senha_salt"));
                byte[] esperado = HexFormat.of().parseHex(rs.getString("senha_hash"));
                char[] caracteres = senha.toCharArray();
                try {
                    byte[] calculado = hash(caracteres, salt);
                    if (!MessageDigest.isEqual(calculado, esperado)) {
                        return null;
                    }
                } finally {
                    Arrays.fill(caracteres, '\0');
                }
                return new Usuario(rs.getLong("id"), rs.getString("login"),
                        PerfilUsuario.valueOf(rs.getString("perfil")));
            }
        }
    }

    public boolean criarAdministradorInicial(String login, String senha) throws SQLException {
        String usuario = normalizarLogin(login);
        if (!usuario.matches("[a-z0-9._-]{3,60}")
                || senha == null || senha.length() < 10
                || !senha.matches("(?s).*[A-Za-z].*")
                || !senha.matches("(?s).*\\d.*")) {
            throw new SQLException("Informe usuário válido e senha com ao menos 10 caracteres, letra e número.",
                    "22023");
        }

        byte[] salt = new byte[16];
        ALEATORIO.nextBytes(salt);
        char[] caracteres = senha.toCharArray();
        String senhaHash;
        try {
            senhaHash = HexFormat.of().formatHex(hash(caracteres, salt));
        } finally {
            Arrays.fill(caracteres, '\0');
        }

        try (Connection conexao = Conexao.abrir()) {
            conexao.setAutoCommit(false);
            try (Statement lock = conexao.createStatement()) {
                // Garante que apenas a primeira pessoa possa criar o administrador.
                lock.execute("LOCK TABLE public.usuarios IN EXCLUSIVE MODE");
                try (PreparedStatement consulta = conexao.prepareStatement(
                        "SELECT EXISTS (SELECT 1 FROM public.usuarios)");
                     ResultSet rs = consulta.executeQuery()) {
                    rs.next();
                    if (rs.getBoolean(1)) {
                        conexao.rollback();
                        return false;
                    }
                }
                try (PreparedStatement inserir = conexao.prepareStatement(
                        "INSERT INTO public.usuarios (login, senha_hash, senha_salt, perfil) "
                                + "VALUES (?, ?, ?, 'ADMIN')")) {
                    inserir.setString(1, usuario);
                    inserir.setString(2, senhaHash);
                    inserir.setString(3, HexFormat.of().formatHex(salt));
                    inserir.executeUpdate();
                }
                conexao.commit();
                return true;
            } catch (SQLException e) {
                conexao.rollback();
                throw e;
            }
        }
    }

    private String normalizarLogin(String login) {
        return login.strip().toLowerCase(Locale.ROOT);
    }

    private byte[] hash(char[] caracteres, byte[] salt) throws SQLException {
        PBEKeySpec especificacao = new PBEKeySpec(caracteres, salt, ITERACOES, TAMANHO_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacao).getEncoded();
        } catch (java.security.GeneralSecurityException e) {
            throw new SQLException("Não foi possível proteger a senha.", "58000", e);
        } finally {
            especificacao.clearPassword();
        }
    }
}
