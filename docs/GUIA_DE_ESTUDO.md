# Guia de estudo do ContratoFácil

Este roteiro serve para entender a implementação e explicar decisões técnicas na apresentação de ADS. Leia o arquivo ao lado do código e reproduza os exercícios; não basta decorar os termos.

## 1. O que o sistema resolve?

O ContratoFácil cadastra fichas de colaboradores, permite definir **tipos de contrato**, montar **modelos de documentos** reutilizáveis e **emitir contratos** preenchidos com os dados de uma pessoa. O resultado é visualizado e exportado para PDF ou DOCX.

- **Tipo**: finalidade, como "Descrição de cargo" ou "Termo de segurança da informação".
- **Modelo**: texto-base e componentes visuais, ainda com tags como \`{{colaborador.nome}}\`.
- **Contrato emitido**: resultado de associar um modelo a um colaborador e preencher as tags. Pode conter nomes de participantes para assinatura manual.

## 2. Ferramentas usadas e por quê

| Tecnologia | Responsabilidade no projeto |
| --- | --- |
| Java 21 | Lógica da aplicação no servidor |
| Jakarta Faces (JSF) | Vincular componentes XHTML aos beans Java |
| PrimeFaces | Formulários, tabelas e componentes da interface |
| JavaScript + CSS | Manipulação direta do canvas A4 e experiência de edição |
| JDBC PostgreSQL | Acesso ao banco por comandos SQL parametrizados |
| Tomcat 10.1 | Servidor que recebe requisições HTTP e executa o WAR |
| Maven | Compilar, testar e empacotar \`contratofacil.war\` |
| Jsoup e OWASP HTML Sanitizer | Ler HTML e remover conteúdo perigoso antes de persistir/renderizar |
| Apache POI | Gerar DOCX com estrutura editável |
| OpenHTMLtoPDF | Gerar PDF do documento preenchido |
| JUnit e testes no Chrome | Evitar regressões do Java e da interação de usuário |

## 3. Mapa de pastas

\`\`\`text
src/main/java/br/edu/contratofacil/
├── bean/       → Recebe eventos das páginas JSF
├── model/      → Entidades Java: Colaborador, TipoContrato, ModeloContrato, Contrato
├── dao/        → Consultas e operações JDBC no PostgreSQL
├── db/         → Configuração da conexão
├── service/    → Tags, sanitização, canvas e exportações
├── filter/     → Validação de acesso antes das telas protegidas
└── servlet/    → Endpoint de exportação de contratos
src/main/webapp/
├── *.xhtml     → Páginas JSF e seus componentes PrimeFaces
└── resources/
    ├── js/canvas-modelo.js → Interações e persistência do canvas no navegador
    └── css/estilo.css      → Aparência, responsividade e seleção de objetos
database/       → SQL de instalação, migração e testes
scripts/        → Compilação, Tomcat e testes de interação
src/test/       → Testes Java
\`\`\`

## 4. Entenda uma operação de ponta a ponta

Exemplo: **Salvar um modelo de contrato**.

1. O usuário abre \`editar-modelo.xhtml\`. A página carrega o \`ModeloContratoBean\`, que usa \`ModeloContratoDAO\` para buscar os dados.
2. \`canvas-modelo.js\` transforma o conteúdo salvo em objetos na folha A4: texto, linha, tabela, assinatura e imagem.
3. A pessoa edita o documento. O JavaScript guarda **posição X/Y**, tamanho e conteúdo do objeto. A barra de parágrafo trabalha no campo \`contenteditable\`; ao clicar nela, deve preservar a seleção anterior.
4. O modelo é serializado no formato \`CFCANVAS1\` para envio ao formulário JSF. Esse formato **não** substitui a validação no servidor.
5. \`DocumentoService\` e \`CanvasDocumentoService\` validam HTML, tamanho dos objetos e dados dos componentes.
6. \`ModeloContratoDAO\` grava o modelo no PostgreSQL por JDBC.

Exemplo: **Emitir um contrato**.

1. Em \`editar-contrato.xhtml\`, o \`ContratoBean\` identifica colaborador, tipo e modelo.
2. \`DocumentoService\` consulta o catálogo de campos e substitui as tags, por exemplo \`{{colaborador.nome}}\`.
3. A emissão é persistida com referência à pessoa e ao modelo utilizado.
4. \`ExportarContratoServlet\` recebe o pedido de download e aciona \`ExportacaoDocumento\`, que gera PDF ou DOCX.

## 5. Por que separar Bean, DAO, Model e Service?

- **Bean**: responde ao clique "Salvar" e mostra erros na interface. Não deveria conhecer detalhes do SQL.
- **Model**: representa os dados em objetos, por exemplo a matrícula e o nome de um colaborador.
- **DAO**: executa o SQL com \`PreparedStatement\`, lê o \`ResultSet\` e converte o resultado em objetos.
- **Service**: concentra regras reutilizáveis, como sanitizar HTML, resolver tags e exportar um documento.
- **Filter**: verifica se o usuário tem uma sessão válida antes de liberar páginas protegidas.

A separação facilita testes e correções: alterar a consulta não exige reescrever a tela.

## 6. Editando o canvas: dois níveis diferentes

**Seleção de objeto:** o usuário escolhe uma caixa para arrastar, redimensionar ou excluir. Delete apaga o objeto **somente fora da digitação**.

**Seleção de texto:** dois cliques entram na edição; o cursor e o intervalo \`Range\` apontam para caracteres dentro do \`contenteditable\`. Negrito, itálico, cor, alinhamento, listas e recuo são operações sobre texto/parágrafo. A seleção pode ser perdida quando a pessoa clica na barra, portanto o JavaScript deve guardar/restaurar a posição.

O navegador edita uma representação temporária. O servidor valida o conteúdo antes de gravar. Ao reabrir, o canvas reconstrói os objetos do modelo. A prévia da emissão usa dados fictícios no editor e dados cadastrados quando o contrato é gerado.

> **Observação:** alinhamento justificado só se percebe visualmente quando o texto ocupa várias linhas. Em uma frase curta, ele pode parecer igual ao alinhamento à esquerda. Não confundir isso com o botão estar quebrado.

## 7. Segurança que você precisa saber explicar

- O login passa pelo \`LoginBean\` e \`UsuarioDAO\`, e o filtro de acesso protege as páginas internas.
- As senhas do banco são fornecidas por variável de ambiente: \`DB_PASSWORD\`, não devem entrar no Git.
- Comandos SQL usam parâmetros (\`PreparedStatement\`) em vez de concatenar campos recebidos do usuário.
- O HTML do editor passa por sanitização, porque o navegador e o usuário não são fontes confiáveis.
- Não é assinatura digital: os componentes de assinatura reservam espaços e identificam participantes.
- Banco de laboratório não é banco de produção. Faça backup antes de aplicar novas migrações.

## 8. Como executar e depurar

Na pasta do projeto, com Java 21 e Maven instalados:

\`\`\`powershell
git status
git pull --ff-only origin main
mvn -U clean verify
.\scripts\tomcat.ps1
\`\`\`

Verifique o endereço exibido pelo Tomcat; no notebook pode ser a porta 8081. Para parar, use \`.\scripts\tomcat.ps1 -Stop\`. A senha JDBC deve ser configurada no ambiente/processo conforme as instruções do README. Os scripts SQL são aplicados **separadamente**, quando necessários. Alterar Java não exige executar SQL de novo.

Para estudar com depuração no VS Code, coloque pontos de parada no método do Bean chamado pelo botão, acompanhe a chamada ao DAO e observe os valores que chegam ao service. No navegador, use as ferramentas de desenvolvedor para ver erros JavaScript e o HTML produzido pelo canvas.

## 9. Perguntas prováveis da apresentação

**Por que Java + JSF + PrimeFaces?** Para construir uma aplicação web Java com componentes prontos, mantendo backend e interface integrados conforme a arquitetura do projeto.

**Qual a diferença entre modelo e contrato?** Modelo é reutilizável e contém campos variáveis. Contrato é a emissão associada a uma ficha.

**Como funcionam as tags?** São identificadores autorizados por um catálogo. O sistema resolve seus valores na ficha/variáveis e substitui no texto antes de mostrar ou exportar.

**O que é JDBC?** A API de Java utilizada para conectar ao PostgreSQL, executar consultas e mapear resultados.

**Por que não concatenar SQL?** Para separar valores dos comandos e diminuir o risco de injeção de SQL.

**O que significa \`@ViewScoped\`?** O bean mantém o estado de interação enquanto a mesma view JSF estiver ativa.

**Como o editor guarda posição de linha/tabela?** O objeto contém coordenadas e dimensões que são serializadas no conteúdo do modelo; o servidor verifica limites e tipos.

**Por que PDF e DOCX não saem sempre idênticos?** PDF prioriza apresentação fixa. DOCX prioriza estrutura editável e pode reorganizar posicionamentos livres.

**Como você verifica se funcionou?** Compilação Maven, testes Java, integração PostgreSQL, testes de interação no Chrome e homologação manual no Tomcat.

## 10. Exercícios para se preparar

1. Localize no Bean o método que salva um colaborador. Siga a chamada até o \`PreparedStatement\` no DAO.
2. Crie um modelo fictício com \`{{colaborador.nome}}\` e descubra no serviço onde a tag é substituída.
3. Selecione um parágrafo no canvas, altere alinhamento/lista/recuo, salve e reabra. Identifique no JS onde a seleção \`Range\` é restaurada.
4. Gere um PDF e um DOCX fictícios. Compare o que cada biblioteca preserva.
5. Explique, sem consultar anotações, por que \`TipoContrato\`, \`ModeloContrato\` e \`Contrato\` são entidades separadas.

Ao terminar esses cinco exercícios, tente fazer uma demonstração de ponta a ponta sem ajuda. A meta é compreender as decisões, não decorar nomes de métodos.
