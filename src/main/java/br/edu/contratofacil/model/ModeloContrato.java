package br.edu.contratofacil.model;

import java.io.Serializable;

public class ModeloContrato implements Serializable {
    private Long id;
    private String titulo;
    private String conteudo;
    private CategoriaDocumento categoria = CategoriaDocumento.OUTROS;
    public CategoriaDocumento getCategoria() { return categoria; }
    public void setCategoria(CategoriaDocumento v) { categoria = v == null ? CategoriaDocumento.OUTROS : v; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getConteudo() { return conteudo; }
    public void setConteudo(String conteudo) { this.conteudo = conteudo; }
}
