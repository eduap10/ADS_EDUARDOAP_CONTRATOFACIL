package br.edu.contratofacil.dao;

import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.TipoContrato;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TipoContratoDAO {
    public List<TipoContrato> listar(boolean apenasAtivos) throws SQLException {
        List<TipoContrato> lista = new ArrayList<>();
        String sql = "SELECT id,nome,descricao,ativo FROM public.tipos_contrato "
                   + (apenasAtivos ? "WHERE ativo=TRUE " : "") + "ORDER BY nome";
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    public TipoContrato buscar(Long id) throws SQLException {
        if (id == null) return null;
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT id,nome,descricao,ativo FROM public.tipos_contrato WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void salvar(TipoContrato t) throws SQLException {
        if (t == null || t.getNome() == null || t.getNome().isBlank()) {
            throw new SQLException("O nome é obrigatório.", "22023");
        }
        t.setNome(t.getNome().strip());
        if (t.getNome().length() > 100) throw new SQLException("Nome muito longo.", "22023");
        boolean novo = t.getId() == null;
        String sql = novo
            ? "INSERT INTO public.tipos_contrato (nome,descricao,ativo) VALUES (?,?,?) RETURNING id"
            : "UPDATE public.tipos_contrato SET nome=?,descricao=?,ativo=? WHERE id=?";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getNome());
            ps.setString(2, t.getDescricao());
            ps.setBoolean(3, t.isAtivo());
            if (novo) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Falha ao cadastrar tipo.");
                    t.setId(rs.getLong(1));
                }
            } else {
                ps.setLong(4, t.getId());
                if (ps.executeUpdate() == 0) throw new SQLException("Tipo não encontrado.", "02000");
            }
        }
    }

    public void excluir(Long id) throws SQLException {
        if (id == null) throw new SQLException("Tipo não informado.", "22023");
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM public.tipos_contrato WHERE id=?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Tipo não encontrado.", "02000");
        }
    }

    private TipoContrato mapear(ResultSet rs) throws SQLException {
        TipoContrato t = new TipoContrato();
        t.setId(rs.getLong("id"));
        t.setNome(rs.getString("nome"));
        t.setDescricao(rs.getString("descricao"));
        t.setAtivo(rs.getBoolean("ativo"));
        return t;
    }
}
