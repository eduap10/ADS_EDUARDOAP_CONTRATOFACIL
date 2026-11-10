package br.edu.contratofacil.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Colaborador implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String matricula, nome, cargo, cpf, email;
    private String sexo, racaCor, estadoCivil, nacionalidade, escolaridade, genero;
    private String cep, logradouro, numero, complemento, bairro, cidade, uf, pais;
    private String centroCustoCodigo, centroCustoDescricao;
    private LocalDate dataNascimento, dataAdmissao;
    private BigDecimal salario;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String v) { matricula = v; }
    public String getNome() { return nome; }
    public void setNome(String v) { nome = v; }
    public String getCargo() { return cargo; }
    public void setCargo(String v) { cargo = v; }
    public String getCpf() { return cpf; }
    public void setCpf(String v) { cpf = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getSexo() { return sexo; }
    public void setSexo(String v) { sexo = v; }
    public String getRacaCor() { return racaCor; }
    public void setRacaCor(String v) { racaCor = v; }
    public String getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(String v) { estadoCivil = v; }
    public String getNacionalidade() { return nacionalidade; }
    public void setNacionalidade(String v) { nacionalidade = v; }
    public String getEscolaridade() { return escolaridade; }
    public void setEscolaridade(String v) { escolaridade = v; }
    public String getGenero() { return genero; }
    public void setGenero(String v) { genero = v; }
    public String getCep() { return cep; }
    public void setCep(String v) { cep = v; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String v) { logradouro = v; }
    public String getNumero() { return numero; }
    public void setNumero(String v) { numero = v; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String v) { complemento = v; }
    public String getBairro() { return bairro; }
    public void setBairro(String v) { bairro = v; }
    public String getCidade() { return cidade; }
    public void setCidade(String v) { cidade = v; }
    public String getUf() { return uf; }
    public void setUf(String v) { uf = v; }
    public String getPais() { return pais; }
    public void setPais(String v) { pais = v; }
    public String getCentroCustoCodigo() { return centroCustoCodigo; }
    public void setCentroCustoCodigo(String v) { centroCustoCodigo = v; }
    public String getCentroCustoDescricao() { return centroCustoDescricao; }
    public void setCentroCustoDescricao(String v) { centroCustoDescricao = v; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate v) { dataNascimento = v; }
    public LocalDate getDataAdmissao() { return dataAdmissao; }
    public void setDataAdmissao(LocalDate v) { dataAdmissao = v; }
    public BigDecimal getSalario() { return salario; }
    public void setSalario(BigDecimal v) { salario = v; }
}
