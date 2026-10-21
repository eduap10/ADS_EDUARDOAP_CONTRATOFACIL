package br.edu.contratofacil.model;
import java.io.Serializable;
public class Contrato implements Serializable {
 private Long id, colaboradorId, modeloId;
 private String titulo, conteudo;
 private SituacaoContrato situacao = SituacaoContrato.RASCUNHO;
 public Long getId(){return id;} public void setId(Long v){id=v;}
 public Long getColaboradorId(){return colaboradorId;} public void setColaboradorId(Long v){colaboradorId=v;}
 public Long getModeloId(){return modeloId;} public void setModeloId(Long v){modeloId=v;}
 public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
 public String getConteudo(){return conteudo;} public void setConteudo(String v){conteudo=v;}
 public SituacaoContrato getSituacao(){return situacao;} public void setSituacao(SituacaoContrato v){situacao=v;}
}
