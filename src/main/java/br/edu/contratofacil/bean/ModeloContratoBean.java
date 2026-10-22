package br.edu.contratofacil.bean;
import br.edu.contratofacil.dao.ModeloContratoDAO;
import br.edu.contratofacil.model.ModeloContrato;
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
public class ModeloContratoBean implements Serializable {
 private static final Logger LOG=Logger.getLogger(ModeloContratoBean.class.getName());
 private String pesquisa="";
 private ModeloContrato edicao=new ModeloContrato();
 private List<ModeloContrato> lista=new ArrayList<>();
 @PostConstruct public void iniciar(){pesquisar();}
 public void pesquisar(){try{lista=new ModeloContratoDAO().listar(pesquisa);}catch(SQLException e){lista=new ArrayList<>();erro(e);}}
 public void novo(){edicao=new ModeloContrato();PrimeFaces.current().resetInputs("form:campos");}
 public void editar(ModeloContrato m){edicao=new ModeloContrato();edicao.setId(m.getId());edicao.setTitulo(m.getTitulo());edicao.setConteudo(m.getConteudo());PrimeFaces.current().resetInputs("form:campos");}
 public void salvar(){
  boolean salvo=false;
  try{
   if(edicao.getTitulo()==null||edicao.getTitulo().isBlank()||edicao.getConteudo()==null||edicao.getConteudo().isBlank()){
    aviso(FacesMessage.SEVERITY_WARN,"Informe titulo e conteudo.");return;
   }
   edicao.setTitulo(edicao.getTitulo().strip());
   new ModeloContratoDAO().salvar(edicao);novo();pesquisar();salvo=true;
   aviso(FacesMessage.SEVERITY_INFO,"Modelo salvo.");
  }catch(SQLException e){erro(e);}finally{PrimeFaces.current().ajax().addCallbackParam("salvo",salvo);}
 }
 public void excluir(Long id){try{new ModeloContratoDAO().excluir(id);pesquisar();aviso(FacesMessage.SEVERITY_INFO,"Modelo excluido.");}catch(SQLException e){erro(e);}}
 private void erro(SQLException e){LOG.log(Level.SEVERE,"Erro em modelos",e);aviso(FacesMessage.SEVERITY_ERROR,"Falha no banco. Execute 04_modelos_contratos.sql e consulte o log do Tomcat.");}
 private void aviso(FacesMessage.Severity s,String m){FacesContext.getCurrentInstance().addMessage(null,new FacesMessage(s,m,null));}
 public String getPesquisa(){return pesquisa;} public void setPesquisa(String v){pesquisa=v;}
 public ModeloContrato getEdicao(){return edicao;}public void setEdicao(ModeloContrato v){edicao=v;}
 public List<ModeloContrato> getLista(){return lista;}
}
