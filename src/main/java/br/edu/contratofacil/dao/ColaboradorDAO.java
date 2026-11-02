package br.edu.contratofacil.dao;

import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.Colaborador;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ColaboradorDAO {

    public List<Colaborador> listar(String pesquisa) throws SQLException {
        List<Colaborador> lista = new ArrayList<>();
        String termo = pesquisa == null ? "" : pesquisa.strip();
        String sql = "SELECT id, matricula, nome, cargo, cpf, email "
                + "FROM public.colaboradores "
                + "WHERE (? = '' OR nome ILIKE ? OR matricula ILIKE ?) "
                + "ORDER BY nome, id";

        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, termo);
            ps.setString(2, "%" + termo + "%");
            ps.setString(3, "%" + termo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public void salvar(Colaborador colaborador) throws SQLException {
        validar(colaborador);
        if (colaborador.getId() == null) {
            inserir(colaborador);
        } else {
            atualizar(colaborador);
        }
    }

    private void inserir(Colaborador colaborador) throws SQLException {
        String sql = "INSERT INTO public.colaboradores (matricula, nome, cargo, cpf, email) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            preencher(ps, colaborador);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    colaborador.setId(rs.getLong("id"));
                } else {
                    throw new SQLException("Não foi possível inserir o colaborador.");
                }
            }
        }
    }

    private void atualizar(Colaborador colaborador) throws SQLException {
        String sql = "UPDATE public.colaboradores SET matricula=?, nome=?, cargo=?, cpf=?, email=? WHERE id=?";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            preencher(ps, colaborador);
            ps.setLong(6, colaborador.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Colaborador não encontrado.", "02000");
            }
        }
    }

    public void excluir(Long id) throws SQLException {
        if (id == null) {
            throw new SQLException("Colaborador não informado.", "22023");
        }
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(
                     "DELETE FROM public.colaboradores WHERE id=?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Colaborador não encontrado.", "02000");
            }
        }
    }

    private void validar(Colaborador c) throws SQLException {
        if (c == null || c.getNome() == null || c.getNome().isBlank()
                || c.getMatricula() == null || c.getMatricula().isBlank()) {
            throw new SQLException("Matrícula e nome são obrigatórios.", "22023");
        }
        c.setNome(c.getNome().strip());
        c.setMatricula(c.getMatricula().strip());
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
