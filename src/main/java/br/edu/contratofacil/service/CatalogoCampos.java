package br.edu.contratofacil.service;

import br.edu.contratofacil.model.Colaborador;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Fonte única das tags: nome visível, ajuda, exemplo e valor proveniente da ficha.
 * Não usa reflection nem permite expressões ou SQL definidas pelo usuário.
 */
public final class CatalogoCampos {
    private static final ZoneId FUSO_DOCUMENTO = ZoneId.of("America/Sao_Paulo");
    private CatalogoCampos() { }

    public static final class Campo {
        private final String chave;
        private final String rotulo;
        private final String grupo;
        private final String descricao;
        private final String exemplo;
        private final Function<Colaborador, String> extrator;

        private Campo(String chave, String rotulo, String grupo,
                      String descricao, String exemplo, Function<Colaborador, String> extrator) {
            this.chave = chave;
            this.rotulo = rotulo;
            this.grupo = grupo;
            this.descricao = descricao;
            this.exemplo = exemplo;
            this.extrator = extrator;
        }
        public String getChave() { return chave; }
        public String getTag() { return "{{" + chave + "}}"; }
        public String getRotulo() { return rotulo; }
        public String getGrupo() { return grupo; }
        public String getDescricao() { return descricao; }
        public String getExemplo() { return exemplo; }
        private String resolver(Colaborador c) {
            if (c == null && chave.startsWith("colaborador.")) return "Não informado";
            String valor = extrator.apply(c);
            return valor == null || valor.isBlank() ? "Não informado" : valor;
        }
    }

    private static Campo campo(String chave, String rotulo, String grupo,
                               String descricao, String exemplo, Function<Colaborador, String> extrator) {
        return new Campo(chave, rotulo, grupo, descricao, exemplo, extrator);
    }

    private static final List<Campo> TODOS = List.of(
        campo("colaborador.nome", "Nome completo", "Identificação", "Nome completo da ficha cadastral.", "Ana Silva", Colaborador::getNome),
        campo("colaborador.matricula", "Matrícula", "Identificação", "Código ou matrícula cadastrada para o colaborador.", "10025", Colaborador::getMatricula),
        campo("colaborador.cpf", "CPF", "Identificação", "CPF informado na ficha do colaborador.", "000.000.000-00", Colaborador::getCpf),
        campo("colaborador.email", "E-mail", "Identificação", "Endereço de e-mail do colaborador.", "ana@exemplo.test", Colaborador::getEmail),
        campo("colaborador.dataNascimento", "Data de nascimento", "Dados pessoais", "Data de nascimento formatada como dia/mês/ano.", "15/03/1995", c -> data(c.getDataNascimento())),
        campo("colaborador.sexo", "Sexo", "Dados pessoais", "Opção de sexo selecionada na ficha.", "Feminino", Colaborador::getSexo),
        campo("colaborador.racaCor", "Raça/cor", "Dados pessoais", "Raça ou cor informada na ficha. Use apenas se necessária ao documento.", "Não informado", Colaborador::getRacaCor),
        campo("colaborador.estadoCivil", "Estado civil", "Dados pessoais", "Estado civil selecionado na ficha.", "Solteiro(a)", Colaborador::getEstadoCivil),
        campo("colaborador.nacionalidade", "Nacionalidade", "Dados pessoais", "Nacionalidade cadastrada.", "Brasileira", Colaborador::getNacionalidade),
        campo("colaborador.escolaridade", "Escolaridade", "Dados pessoais", "Escolaridade selecionada na ficha.", "Ensino superior", Colaborador::getEscolaridade),
        campo("colaborador.genero", "Gênero", "Dados pessoais", "Gênero informado na ficha. Use apenas quando relevante.", "Não informado", Colaborador::getGenero),
        campo("colaborador.cep", "CEP", "Endereço", "Código de endereçamento postal da residência.", "99000-000", Colaborador::getCep),
        campo("colaborador.logradouro", "Logradouro", "Endereço", "Rua, avenida ou outro logradouro.", "Rua Exemplo", Colaborador::getLogradouro),
        campo("colaborador.numero", "Número", "Endereço", "Número do imóvel.", "123", Colaborador::getNumero),
        campo("colaborador.complemento", "Complemento", "Endereço", "Apartamento, bloco ou complemento cadastrado.", "Apto. 12", Colaborador::getComplemento),
        campo("colaborador.bairro", "Bairro", "Endereço", "Bairro da residência.", "Centro", Colaborador::getBairro),
        campo("colaborador.cidade", "Cidade", "Endereço", "Cidade da residência.", "Passo Fundo", Colaborador::getCidade),
        campo("colaborador.uf", "UF", "Endereço", "Sigla do estado.", "RS", Colaborador::getUf),
        campo("colaborador.pais", "País", "Endereço", "País cadastrado.", "Brasil", Colaborador::getPais),
        campo("colaborador.dataAdmissao", "Data de admissão", "Dados contratuais", "Data de admissão formatada como dia/mês/ano.", "10/02/2024", c -> data(c.getDataAdmissao())),
        campo("colaborador.cargo", "Cargo", "Dados contratuais", "Cargo atual na ficha do colaborador.", "Analista", Colaborador::getCargo),
        campo("colaborador.centroCustoCodigo", "Código do centro de custo", "Dados contratuais", "Código do centro de custo vinculado ao colaborador.", "CC-101", Colaborador::getCentroCustoCodigo),
        campo("colaborador.centroCustoDescricao", "Descrição do centro de custo", "Dados contratuais", "Nome/descrição do centro de custo.", "Administrativo", Colaborador::getCentroCustoDescricao),
        campo("colaborador.salario", "Salário", "Dados contratuais", "Salário cadastrado, exibido em reais.", "R$ 3.250,00", c -> moeda(c.getSalario())),
        campo("documento.dataAtual", "Data atual", "Documento", "Data de geração do documento; não vem da ficha.", "15/11/2026", c -> data(LocalDate.now(FUSO_DOCUMENTO))),
        campo("documento.horaAtual", "Hora atual", "Documento", "Hora local de geração em Brasília.", "15:30", c -> java.time.LocalTime.now(FUSO_DOCUMENTO).format(DateTimeFormatter.ofPattern("HH:mm"))),
        campo("documento.dataHoraAtual", "Data e hora", "Documento", "Data e hora de geração em Brasília.", "15/11/2026 15:30", c -> java.time.ZonedDateTime.now(FUSO_DOCUMENTO).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))),
        campo("documento.anoAtual", "Ano atual", "Documento", "Ano corrente.", "2026", c -> Integer.toString(LocalDate.now(FUSO_DOCUMENTO).getYear())),
        campo("documento.assinatura", "Linha de assinatura", "Documento", "Insere uma linha para assinatura manual com o nome do colaborador. Não é assinatura digital.", "____________ Assinatura de Ana Silva", c -> "________________________________________ Assinatura de " + valor(c == null ? null : c.getNome())),
        campo("empresa.logo", "Logo da empresa", "Documento", "Coloca o logo cadastrado em Configurar logo. Se não houver imagem, mostra aviso.", "[Logo da empresa]", c -> "[empresa.logo não informado]")
    );

    public static List<Campo> todos() { return TODOS; }

    public static Map<String, String> valores(Colaborador colaborador) {
        Map<String, String> resultado = new LinkedHashMap<>();
        for (Campo campo : TODOS) resultado.put(campo.getChave(), campo.resolver(colaborador));
        return resultado;
    }

    private static String valor(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor;
    }

    private static String data(LocalDate data) {
        return data == null ? "Não informado" : data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String moeda(BigDecimal valor) {
        return valor == null ? "Não informado"
            : NumberFormat.getCurrencyInstance(Locale.of("pt", "BR")).format(valor);
    }
}
