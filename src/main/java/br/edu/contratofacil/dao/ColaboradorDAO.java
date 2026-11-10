package br.edu.contratofacil.dao;

import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.Colaborador;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ColaboradorDAO {
    private static final String COLUNAS = "id, matricula, nome, cargo, cpf, email, data_nascimento, sexo, "
       + "raca_cor, estado_civil, nacionalidade, escolaridade, genero, cep, logradouro, numero, complemento, "
       + "bairro, cidade, uf, pais, data_admissao, centro_custo_codigo, centro_custo_descricao, salario";

    public List<Colaborador> listar(String pesquisa) throws SQLException {
        List<Colaborador> lista = new ArrayList<>();
        String termo = pesquisa == null ? "" : pesquisa.strip();
        String sql = "SELECT " + COLUNAS + " FROM public.colaboradores "
                   + "WHERE (? = '' OR nome ILIKE ? OR matricula ILIKE ?) ORDER BY nome, id";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, termo);
            ps.setString(2, "%" + termo + "%");
            ps.setString(3, "%" + termo + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    public Colaborador buscar(Long id) throws SQLException {
        if (id == null) return null;
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUNAS + " FROM public.colaboradores WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    public void salvar(Colaborador dados) throws SQLException {
        validar(dados);
        final String campos = "matricula,nome,cargo,cpf,email,data_nascimento,sexo,raca_cor,estado_civil,"
            + "nacionalidade,escolaridade,genero,cep,logradouro,numero,complemento,bairro,cidade,uf,pais,"
            + "data_admissao,centro_custo_codigo,centro_custo_descricao,salario";
        String[] nomes = campos.split(",");
        StringBuilder sql = new StringBuilder();
        boolean novo = dados.getId() == null;
        if (novo) {
            sql.append("INSERT INTO public.colaboradores (").append(campos).append(") VALUES (");
            for (int i = 0; i < nomes.length; i++) sql.append(i == 0 ? "?" : ",?");
            sql.append(") RETURNING id");
        } else {
            sql.append("UPDATE public.colaboradores SET ");
            for (int i = 0; i < nomes.length; i++) {
                if (i != 0) sql.append(",");
                sql.append(nomes[i]).append("=?");
            }
            sql.append(" WHERE id=?");
        }
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setString(i++, dados.getMatricula());
            ps.setString(i++, dados.getNome());
            ps.setString(i++, dados.getCargo());
            ps.setString(i++, dados.getCpf());
            ps.setString(i++, dados.getEmail());
            data(ps, i++, dados.getDataNascimento());
            ps.setString(i++, dados.getSexo());
            ps.setString(i++, dados.getRacaCor());
            ps.setString(i++, dados.getEstadoCivil());
            ps.setString(i++, dados.getNacionalidade());
            ps.setString(i++, dados.getEscolaridade());
            ps.setString(i++, dados.getGenero());
            ps.setString(i++, dados.getCep());
            ps.setString(i++, dados.getLogradouro());
            ps.setString(i++, dados.getNumero());
            ps.setString(i++, dados.getComplemento());
            ps.setString(i++, dados.getBairro());
            ps.setString(i++, dados.getCidade());
            ps.setString(i++, dados.getUf());
            ps.setString(i++, dados.getPais());
            data(ps, i++, dados.getDataAdmissao());
            ps.setString(i++, dados.getCentroCustoCodigo());
            ps.setString(i++, dados.getCentroCustoDescricao());
            if (dados.getSalario() == null) ps.setNull(i++, Types.NUMERIC);
            else ps.setBigDecimal(i++, dados.getSalario());
            if (novo) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Falha ao incluir colaborador.");
                    dados.setId(rs.getLong("id"));
                }
            } else {
                ps.setLong(i, dados.getId());
                if (ps.executeUpdate() == 0) throw new SQLException("Colaborador não encontrado.", "02000");
            }
        }
    }

    public void excluir(Long id) throws SQLException {
        if (id == null) throw new SQLException("ID obrigatório.", "22023");
        try (Connection c = Conexao.abrir();
             PreparedStatement ps = c.prepareStatement("DELETE FROM public.colaboradores WHERE id=?")) {
            ps.setLong(1, id);
            if (ps.executeUpdate() == 0) throw new SQLException("Colaborador não encontrado.", "02000");
        }
    }

    private void validar(Colaborador c) throws SQLException {
        if (c == null || c.getNome() == null || c.getNome().isBlank()
            || c.getMatricula() == null || c.getMatricula().isBlank()) {
            throw new SQLException("Informe nome e matrícula.", "22023");
        }
        c.setNome(c.getNome().strip());
        c.setMatricula(c.getMatricula().strip());
        if (c.getSalario() != null && c.getSalario().signum() < 0) {
            throw new SQLException("Salário inválido.", "22023");
        }
        if (c.getDataNascimento() != null && c.getDataNascimento().isAfter(LocalDate.now())) {
            throw new SQLException("Data de nascimento futura.", "22023");
        }
        if (c.getDataAdmissao() != null && c.getDataAdmissao().isBefore(c.getDataNascimento() == null
                ? LocalDate.MIN : c.getDataNascimento())) {
            throw new SQLException("Admissão anterior ao nascimento.", "22023");
        }
    }

    private void data(PreparedStatement ps, int indice, LocalDate valor) throws SQLException {
        if (valor == null) ps.setNull(indice, Types.DATE);
        else ps.setObject(indice, valor);
    }

    private Colaborador mapear(ResultSet r) throws SQLException {
        Colaborador c = new Colaborador();
        c.setId(r.getLong("id"));
        c.setMatricula(r.getString("matricula"));
        c.setNome(r.getString("nome"));
        c.setCargo(r.getString("cargo"));
        c.setCpf(r.getString("cpf"));
        c.setEmail(r.getString("email"));
        c.setDataNascimento(r.getObject("data_nascimento", LocalDate.class));
        c.setSexo(r.getString("sexo"));
        c.setRacaCor(r.getString("raca_cor"));
        c.setEstadoCivil(r.getString("estado_civil"));
        c.setNacionalidade(r.getString("nacionalidade"));
        c.setEscolaridade(r.getString("escolaridade"));
        c.setGenero(r.getString("genero"));
        c.setCep(r.getString("cep"));
        c.setLogradouro(r.getString("logradouro"));
        c.setNumero(r.getString("numero"));
        c.setComplemento(r.getString("complemento"));
        c.setBairro(r.getString("bairro"));
        c.setCidade(r.getString("cidade"));
        c.setUf(r.getString("uf"));
        c.setPais(r.getString("pais"));
        c.setDataAdmissao(r.getObject("data_admissao", LocalDate.class));
        c.setCentroCustoCodigo(r.getString("centro_custo_codigo"));
        c.setCentroCustoDescricao(r.getString("centro_custo_descricao"));
        c.setSalario(r.getBigDecimal("salario"));
        return c;
    }
}
