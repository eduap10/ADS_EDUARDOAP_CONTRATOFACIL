package br.edu.contratofacil.bean;
import br.edu.contratofacil.dao.*;
import br.edu.contratofacil.model.*;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Named;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.*;
import org.primefaces.PrimeFaces;
@Named @ViewScoped
public class ContratoBean implements Serializable {
 private static final Logger LOG=Logger.getLogger(ContratoBean.class.getName());
 private String pesquisa="";
 private Contrato edicao=new Contrato();
 private List<Contrato> lista=new ArrayList<>();
 private List<Colaborador> colaboradores=new ArrayList<>();
 private List<ModeloContrato> modelos=new ArrayList<>();
 @PostConstruct public void iniciar(){atualizarOpcoes();pesquisar();}
 private void atualizarOpcoes(){try{
  colaboradores=new ColaboradorDAO().listar("");
  modelos=new ModeloContratoDAO().listar("");
 }catch(SQLException e){erro(e);}}
 public void pesquisar(){try{lista=new ContratoDAO().listar(pesquisa);}catch(SQLException e){lista=new ArrayList<>();erro(e);}}
 public void novo(){edicao=new Contrato();atualizarOpcoes();PrimeFaces.current().resetInputs("form:campos");}
 public void editar(Contrato c){
  edicao=new Contrato();
  edicao.setId(c.getId());edicao.setColaboradorId(c.getColaboradorId());edicao.setModeloId(c.getModeloId());
  edicao.setTitulo(c.getTitulo());edicao.setConteudo(c.getConteudo());edicao.setSituacao(c.getSituacao());
  atualizarOpcoes();PrimeFaces.current().resetInputs("form:campos");
 }
 public void carregarModelo(){
  if(edicao.getModeloId()==null)return;
  try{
   ModeloContrato m=new ModeloContratoDAO().buscar(edicao.getModeloId());
   if(m!=null){edicao.setConteudo(m.getConteudo());if(edicao.getTitulo()==null||edicao.getTitulo().isBlank())edicao.setTitulo(m.getTitulo());}
  }catch(SQLException e){erro(e);}
 }
 public void salvar(){
  boolean salvo=false;
  try{
   if(edicao.getColaboradorId()==null||edicao.getTitulo()==null||edicao.getTitulo().isBlank()
      ||edicao.getConteudo()==null||edicao.getConteudo().isBlank()){
    aviso(FacesMessage.SEVERITY_WARN,"Informe colaborador, titulo e conteudo.");return;
   }
   edicao.setTitulo(edicao.getTitulo().strip());
   new ContratoDAO().salvar(edicao);novo();pesquisar();salvo=true;
   aviso(FacesMessage.SEVERITY_INFO,"Contrato salvo.");
  }catch(SQLException e){erro(e);}finally{PrimeFaces.current().ajax().addCallbackParam("salvo",salvo);}
 }
 public void excluir(Long id){try{new ContratoDAO().excluir(id);pesquisar();aviso(FacesMessage.SEVERITY_INFO,"Contrato excluido.");}catch(SQLException e){erro(e);}}
 private void erro(SQLException e){LOG.log(Level.SEVERE,"Erro em contratos",e);aviso(FacesMessage.SEVERITY_ERROR,"Falha no banco. Verifique as tabelas e o log do Tomcat.");}
 private void aviso(FacesMessage.Severity s,String m){FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(s,m,null));}
 public String getPesquisa(){return pesquisa;}public void setPesquisa(String v){pesquisa=v;}
 public Contrato getEdicao(){return edicao;}public void setEdicao(Contrato v){edicao=v;}
 public List<Contrato> getLista(){return lista;}
 public List<Colaborador> getColaboradores(){return colaboradores;}
 public List<ModeloContrato> getModelos(){return modelos;}
 public SituacaoContrato[] getSituacoes(){return SituacaoContrato.values();}
}
