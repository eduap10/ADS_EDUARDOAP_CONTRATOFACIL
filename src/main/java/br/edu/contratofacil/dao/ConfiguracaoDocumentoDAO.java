package br.edu.contratofacil.dao;

import br.edu.contratofacil.db.Conexao;
import java.sql.*;

public class ConfiguracaoDocumentoDAO {
    public record Logo(byte[] dados, String mime) { }

    public Logo buscarLogo() throws SQLException {
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(
                "SELECT logo, mime FROM public.configuracao_documento WHERE id=1");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            byte[] bytes = rs.getBytes("logo");
            return bytes == null ? null : new Logo(bytes, rs.getString("mime"));
        }
    }

    public void salvarLogo(byte[] imagem, String mime) throws SQLException {
        String sql = "INSERT INTO public.configuracao_documento (id, logo, mime) VALUES (1, ?, ?) "
                   + "ON CONFLICT (id) DO UPDATE SET logo=EXCLUDED.logo, mime=EXCLUDED.mime";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBytes(1, imagem);
            ps.setString(2, mime);
            ps.executeUpdate();
        }
    }
}
