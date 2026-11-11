package br.edu.contratofacil.dao;

import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.ModeloContrato;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ModeloContratoDAO {

    public List<ModeloContrato> listar(String pesquisa) throws SQLException {
        List<ModeloContrato> lista = new ArrayList<>();
        String termo = pesquisa == null ? "" : pesquisa.strip();
        String sql = "SELECT id, titulo, conteudo, categoria, tipo_id FROM public.modelos_contrato "
                + "WHERE titulo ILIKE ? ORDER BY titulo, id";

        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, "%" + termo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    public ModeloContrato buscar(Long id) throws SQLException {
        if (id == null) {
            return null;
        }
        String sql = "SELECT id, titulo, conteudo, categoria, tipo_id FROM public.modelos_contrato WHERE id=?";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void salvar(ModeloContrato modelo) throws SQLException {
        validar(modelo);
        if (modelo.getId() == null) {
            inserir(modelo);
        } else {
            atualizar(modelo);
        }
    }

    private void inserir(ModeloContrato modelo) throws SQLException {
        String sql = "INSERT INTO public.modelos_contrato (titulo, conteudo, categoria, tipo_id) "
                + "VALUES (?, ?, ?, ?) RETURNING id";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, modelo.getTitulo());
            ps.setString(2, modelo.getConteudo());
            ps.setString(3, modelo.getCategoria().name());
            if (modelo.getTipoId() == null) ps.setNull(4, java.sql.Types.BIGINT);
            else ps.setLong(4, modelo.getTipoId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    modelo.setId(rs.getLong("id"));
                } else {
                    throw new SQLException("Não foi possível inserir o modelo.");
                }
            }
        }
    }

    private void atualizar(ModeloContrato modelo) throws SQLException {
        String sql = "UPDATE public.modelos_contrato SET titulo=?, conteudo=?, categoria=?, tipo_id=? WHERE id=?";
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(sql)) {
            ps.setString(1, modelo.getTitulo());
            ps.setString(2, modelo.getConteudo());
            ps.setString(3, modelo.getCategoria().name());
            if (modelo.getTipoId() == null) ps.setNull(4, java.sql.Types.BIGINT);
            else ps.setLong(4, modelo.getTipoId());
            ps.setLong(5, modelo.getId());
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Modelo não encontrado.", "02000");
            }
        }
    }

    public void excluir(Long id) throws SQLException {
        if (id == null) {
            throw new SQLException("Modelo não informado.", "22023");
        }
        try (Connection conexao = Conexao.abrir();
             PreparedStatement ps = conexao.prepareStatement(
                     "DELETE FROM public.modelos_contrato WHERE id=?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("Modelo não encontrado.", "02000");
            }
        }
    }

    private void validar(ModeloContrato modelo) throws SQLException {
        if (modelo == null || modelo.getTitulo() == null || modelo.getTitulo().isBlank()
                || modelo.getConteudo() == null || modelo.getConteudo().isBlank()) {
            throw new SQLException("Título e conteúdo são obrigatórios.", "22023");
        }
        modelo.setTitulo(modelo.getTitulo().strip());
    }

    private ModeloContrato mapear(ResultSet rs) throws SQLException {
        ModeloContrato modelo = new ModeloContrato();
        modelo.setId(rs.getLong("id"));
        modelo.setTitulo(rs.getString("titulo"));
        modelo.setConteudo(rs.getString("conteudo"));
        modelo.setCategoria(br.edu.contratofacil.model.CategoriaDocumento.valueOf(rs.getString("categoria")));
        modelo.setTipoId(rs.getObject("tipo_id", Long.class));
        return modelo;
    }
}
