package br.edu.contratofacil.model;

import java.io.Serializable;

public class Colaborador implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String matricula;
    private String nome;
    private String cargo;
    private String cpf;
    private String email;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
