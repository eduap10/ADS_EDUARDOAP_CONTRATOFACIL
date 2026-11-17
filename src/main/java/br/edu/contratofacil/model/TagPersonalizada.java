package br.edu.contratofacil.model;

import java.io.Serializable;

/**
 * Modelo de domínio.
 * Representa uma tag definida pelo usuário, disponibilizada no catálogo
 *  * do editor e resolvida no momento da emissão.
 */
public class TagPersonalizada implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String codigo;
    private String nome;
    private String descricao;
    private String tipo = "TEXTO_FIXO";
    private String referencia;
    private String valorFixo;
    private boolean ativo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getValorFixo() { return valorFixo; }
    public void setValorFixo(String valorFixo) { this.valorFixo = valorFixo; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public String getTag() { return "{{personalizada." + codigo + "}}"; }
}
