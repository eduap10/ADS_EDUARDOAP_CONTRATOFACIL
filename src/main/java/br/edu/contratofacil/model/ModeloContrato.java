package br.edu.contratofacil.model;

import java.io.Serializable;

public class ModeloContrato implements Serializable {
    private Long id;
    private String titulo;
    private String conteudo;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
}
