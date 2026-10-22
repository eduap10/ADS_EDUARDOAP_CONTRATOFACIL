package br.edu.contratofacil.dao;
import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.*;
import java.sql.*;
import java.util.*;
public class ContratoDAO {
 public List<Contrato> listar(String pesquisa) throws SQLException {
  List<Contrato> lista=new ArrayList<>();
  String termo=pesquisa==null?"":pesquisa.strip();
  String sql="SELECT c.id,c.colaborador_id,c.modelo_id,c.titulo,c.conteudo,c.situacao FROM contratos c "
   +"JOIN colaboradores p ON p.id=c.colaborador_id WHERE c.titulo ILIKE ? OR p.nome ILIKE ? OR p.matricula ILIKE ? ORDER BY c.id DESC";
  try(Connection c=Conexao.abrir();PreparedStatement p=c.prepareStatement(sql)){
   for(int i=1;i<=3;i++)p.setString(i,"%"+termo+"%");
   try(ResultSet r=p.executeQuery()){while(r.next())lista.add(mapear(r));}
  }return lista;
 }
 public void salvar(Contrato contrato) throws SQLException {
  boolean novo=contrato.getId()==null;
  String sql=novo
    ?"INSERT INTO contratos (colaborador_id,modelo_id,titulo,conteudo,situacao) VALUES (?,?,?,?,?)"
    :"UPDATE contratos SET colaborador_id=?,modelo_id=?,titulo=?,conteudo=?,situacao=? WHERE id=?";
  try(Connection c=Conexao.abrir();PreparedStatement p=c.prepareStatement(sql)){
   p.setLong(1,contrato.getColaboradorId());
   if(contrato.getModeloId()==null)p.setNull(2,Types.BIGINT);else p.setLong(2,contrato.getModeloId());
   p.setString(3,contrato.getTitulo());p.setString(4,contrato.getConteudo());
   p.setString(5,contrato.getSituacao().name());
   if(!novo)p.setLong(6,contrato.getId());
   if(p.executeUpdate()==0)throw new SQLException("Contrato nao encontrado");
  }
 }
 public void excluir(Long id) throws SQLException {
  try(Connection c=Conexao.abrir();PreparedStatement p=c.prepareStatement("DELETE FROM contratos WHERE id=?")){
   p.setLong(1,id);if(p.executeUpdate()==0)throw new SQLException("Contrato nao encontrado");
  }
 }
 private Contrato mapear(ResultSet r)throws SQLException{
  Contrato c=new Contrato();c.setId(r.getLong("id"));c.setColaboradorId(r.getLong("colaborador_id"));
  c.setModeloId(r.getObject("modelo_id",Long.class));c.setTitulo(r.getString("titulo"));
  c.setConteudo(r.getString("conteudo"));c.setSituacao(SituacaoContrato.valueOf(r.getString("situacao")));return c;
 }
}
