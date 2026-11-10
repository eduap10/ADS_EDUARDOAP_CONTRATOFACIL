package br.edu.contratofacil.model;

public enum CategoriaDocumento {
    DESCRICAO_CARGO("Descrição de cargo"),
    CONSENTIMENTO_POLITICAS("Consentimento com políticas da empresa"),
    VALE_TRANSPORTE("Termo de uso de vale-transporte"),
    CONTRATO_TRABALHO("Contrato de trabalho"),
    SEGURANCA_INFORMACAO("Termo de segurança da informação"),
    OUTROS("Outros");

    private final String rotulo;

    CategoriaDocumento(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
