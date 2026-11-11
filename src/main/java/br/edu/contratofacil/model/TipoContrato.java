package br.edu.contratofacil.model;

import java.io.Serializable;

public class TipoContrato implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String nome;
    private String descricao;
    private boolean ativo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
