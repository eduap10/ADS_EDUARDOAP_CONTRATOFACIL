package br.edu.contratofacil.dao;

import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.Colaborador;
import br.edu.contratofacil.model.TagPersonalizada;
import br.edu.contratofacil.service.CatalogoCampos;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Camada de persistência (DAO, Data Access Object).
 * Armazena tags personalizadas para uso no corpo dos modelos.
 *  * Mantém as operações de banco separadas das regras de catálogo e preenchimento.
 */
public class TagPersonalizadaDAO {
    private static final Set<String> TIPOS = Set.of(
        "TEXTO_FIXO","CAMPO_FICHA","DATA_ATUAL","HORA_ATUAL","DATA_HORA_ATUAL","ANO_ATUAL");
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    public List<TagPersonalizada> listar(boolean somenteAtivas) throws SQLException {
        String sql = "SELECT id,codigo,nome,descricao,tipo,referencia,valor_fixo,ativo "
                   + "FROM public.tags_personalizadas "
                   + (somenteAtivas ? "WHERE ativo=TRUE " : "") + "ORDER BY nome,codigo";
        List<TagPersonalizada> resultado = new ArrayList<>();
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resultado.add(mapear(rs));
        }
        return resultado;
    }

    public void salvar(TagPersonalizada tag) throws SQLException {
        if (tag.getCodigo() == null || !tag.getCodigo().matches("[a-z][a-z0-9_]{2,39}"))
            throw new SQLException("Código inválido: use letras minúsculas, números e _ (3 a 40 caracteres).", "22023");
        if (tag.getNome() == null || tag.getNome().isBlank() || tag.getNome().length() > 100)
            throw new SQLException("Informe o nome do campo com até 100 caracteres.", "22023");
        if (!TIPOS.contains(tag.getTipo()))
            throw new SQLException("Tipo de valor não permitido.", "22023");
        if ("CAMPO_FICHA".equals(tag.getTipo()) && CatalogoCampos.todos().stream()
                .noneMatch(c -> c.getChave().equals(tag.getReferencia()) && c.getChave().startsWith("colaborador.")))
            throw new SQLException("Selecione um campo válido da ficha cadastral.", "22023");
        if ("TEXTO_FIXO".equals(tag.getTipo())
                && (tag.getValorFixo() == null || tag.getValorFixo().length() > 500))
            throw new SQLException("Informe o texto fixo (até 500 caracteres).", "22023");
        boolean novo = tag.getId() == null;
        // Depois do primeiro cadastro, a chave integra o texto dos modelos.
        // Alterá-la quebraria as referências antigas. A condição no UPDATE
        // protege o código também contra requisições HTTP manipuladas.
        String sql = novo
            ? "INSERT INTO public.tags_personalizadas(codigo,nome,descricao,tipo,referencia,valor_fixo,ativo) "
              + "VALUES(?,?,?,?,?,?,?) RETURNING id"
            : "UPDATE public.tags_personalizadas SET nome=?,descricao=?,tipo=?,referencia=?,"
              + "valor_fixo=?,ativo=? WHERE id=? AND codigo=?";
        try (Connection c = Conexao.abrir(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (novo) {
                ps.setString(1, tag.getCodigo());
                ps.setString(2, tag.getNome().strip());
                ps.setString(3, tag.getDescricao());
                ps.setString(4, tag.getTipo());
                ps.setString(5, tag.getReferencia());
                ps.setString(6, tag.getValorFixo());
                ps.setBoolean(7, tag.isAtivo());
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("A tag não foi criada.");
                    tag.setId(rs.getLong(1));
                }
            } else {
                ps.setString(1, tag.getNome().strip());
                ps.setString(2, tag.getDescricao());
                ps.setString(3, tag.getTipo());
                ps.setString(4, tag.getReferencia());
                ps.setString(5, tag.getValorFixo());
                ps.setBoolean(6, tag.isAtivo());
                ps.setLong(7, tag.getId());
                ps.setString(8, tag.getCodigo());
                if (ps.executeUpdate() == 0)
                    throw new SQLException("Tag não encontrada ou código alterado. A chave é permanente.", "22023");
            }
        }
    }

    public Map<String,String> valores(Colaborador colaborador) throws SQLException {
        Map<String,String> base = CatalogoCampos.valores(colaborador);
        Map<String,String> resultado = new LinkedHashMap<>();
        ZonedDateTime agora = ZonedDateTime.now(FUSO);
        for (TagPersonalizada tag : listar(false)) {
            String valor = switch (tag.getTipo()) {
                case "TEXTO_FIXO" -> tag.getValorFixo();
                case "CAMPO_FICHA" -> base.getOrDefault(tag.getReferencia(), "Não informado");
                case "DATA_ATUAL" -> agora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                case "HORA_ATUAL" -> agora.format(DateTimeFormatter.ofPattern("HH:mm"));
                case "DATA_HORA_ATUAL" -> agora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                case "ANO_ATUAL" -> Integer.toString(agora.getYear());
                default -> "Não informado";
            };
            resultado.put("personalizada." + tag.getCodigo(), valor == null ? "Não informado" : valor);
        }
        return resultado;
    }

    private TagPersonalizada mapear(ResultSet rs) throws SQLException {
        TagPersonalizada t = new TagPersonalizada();
        t.setId(rs.getLong("id"));
        t.setCodigo(rs.getString("codigo"));
        t.setNome(rs.getString("nome"));
        t.setDescricao(rs.getString("descricao"));
        t.setTipo(rs.getString("tipo"));
        t.setReferencia(rs.getString("referencia"));
        t.setValorFixo(rs.getString("valor_fixo"));
        t.setAtivo(rs.getBoolean("ativo"));
        return t;
    }
}
