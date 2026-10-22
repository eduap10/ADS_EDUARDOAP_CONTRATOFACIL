package br.edu.contratofacil.dao;
import br.edu.contratofacil.db.Conexao;
import br.edu.contratofacil.model.ModeloContrato;
import java.sql.*;
import java.util.*;
public class ModeloContratoDAO {
 public List<ModeloContrato> listar(String pesquisa) throws SQLException {
  List<ModeloContrato> lista=new ArrayList<>();
  String termo = pesquisa==null?"":pesquisa.strip();
  try(Connection c=Conexao.abrir(); PreparedStatement p=c.prepareStatement(
   "SELECT id,titulo,conteudo FROM modelos_contrato WHERE titulo ILIKE ? ORDER BY titulo")) {
   p.setString(1,"%"+termo+"%");
   try(ResultSet r=p.executeQuery()){while(r.next())lista.add(mapear(r));}
  } return lista;
 }
 public ModeloContrato buscar(Long id) throws SQLException {
  try(Connection c=Conexao.abrir();PreparedStatement p=c.prepareStatement("SELECT id,titulo,conteudo FROM modelos_contrato WHERE id=?")){
   p.setLong(1,id);try(ResultSet r=p.executeQuery()){return r.next()?mapear(r):null;}
  }
 }
 public void salvar(ModeloContrato m) throws SQLException {
  String sql=m.getId()==null
    ?"INSERT INTO modelos_contrato (titulo,conteudo) VALUES (?,?)"
    :"UPDATE modelos_contrato SET titulo=?,conteudo=? WHERE id=?";
  try(Connection c=Conexao.abrir();PreparedStatement p=c.prepareStatement(sql)){
   p.setString(1,m.getTitulo());p.setString(2,m.getConteudo());
   if(m.getId()!=null)p.setLong(3,m.getId());
   if(p.executeUpdate()==0)throw new SQLException("Modelo nao encontrado");
  }
 }
 public void excluir(Long id) throws SQLException {
  try(Connection c=Conexao.abrir();PreparedStatement p=c.prepareStatement("DELETE FROM modelos_contrato WHERE id=?")){
   p.setLong(1,id);if(p.executeUpdate()==0)throw new SQLException("Modelo nao encontrado");
  }
 }
 private ModeloContrato mapear(ResultSet r)throws SQLException{
  ModeloContrato m=new ModeloContrato();m.setId(r.getLong("id"));m.setTitulo(r.getString("titulo"));m.setConteudo(r.getString("conteudo"));return m;
 }
}
