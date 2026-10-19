package br.edu.contratofacil.dao;

import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.Colaborador;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ColaboradorDAO {
    public List<Colaborador> listar(String pesquisa) throws SQLException {
        List<Colaborador> resultado = new ArrayList<>();
        String sql = "SELECT id, matricula, nome, cargo, cpf, email FROM colaboradores "
                + "WHERE (? = '' OR nome ILIKE ? OR matricula ILIKE ?) ORDER BY nome";
        String termo = pesquisa == null ? "" : pesquisa.strip();
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, termo);
            ps.setString(2, "%" + termo + "%");
            ps.setString(3, "%" + termo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) resultado.add(mapear(rs));
            }
        }
        return resultado;
    }

    public void salvar(Colaborador colaborador) throws SQLException {
        if (colaborador.getId() == null) inserir(colaborador);
        else atualizar(colaborador);
    }

    private void inserir(Colaborador c) throws SQLException {
        String sql = "INSERT INTO colaboradores (matricula, nome, cargo, cpf, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection conexao = Conexao.abrir(); PreparedStatement ps = conexao.prepareStatement(sql)) {
            preencher(ps, c);
            ps.executeUpdate();
        }
    }

    private void atualizar(Colaborador c) throws SQLException {
        String sql = "UPDATE colaboradores SET matricula=?, nome=?, cargo=?, cpf=?, email=? WHERE id=?";
        try (Connection conexao = Conexao.abrir(); PreparedStatement ps = conexao.prepareStatement(sql)) {
            preencher(ps, c);
            ps.setLong(6, c.getId());
            if (ps.executeUpdate() == 0) throw new SQLException("Colaborador nao encontrado.");
        }
    }

    public void excluir(Long id) throws SQLException {
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM colaboradores WHERE id=?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private void preencher(PreparedStatement ps, Colaborador c) throws SQLException {
        ps.setString(1, c.getMatricula());
        ps.setString(2, c.getNome());
        ps.setString(3, c.getCargo());
        ps.setString(4, c.getCpf());
        ps.setString(5, c.getEmail());
    }

    private Colaborador mapear(ResultSet rs) throws SQLException {
        Colaborador c = new Colaborador();
        c.setId(rs.getLong("id"));
        c.setMatricula(rs.getString("matricula"));
        c.setNome(rs.getString("nome"));
        c.setCargo(rs.getString("cargo"));
        c.setCpf(rs.getString("cpf"));
        c.setEmail(rs.getString("email"));
        return c;
    }
}
