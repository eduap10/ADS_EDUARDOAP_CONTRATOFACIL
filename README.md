# ContratoFácil

Aplicação acadêmica para cadastro de colaboradores, manutenção de modelos e elaboração de contratos com campos dinâmicos. Usa **Java 21, JSF/Jakarta Faces, PrimeFaces, Maven, Tomcat 10.1, JDBC e PostgreSQL**. O projeto é um WAR tradicional, sem frontend adicional.

## Guia para estudar a implementação

O arquivo [Guia de estudo e arquitetura](docs/GUIA_DE_ESTUDO.md) explica camadas, fluxo de emissão, segurança, editor A4, testes e perguntas para apresentação.

## Funcionalidades implementadas no código

- Login administrativo com sessão, logout e filtro de acesso.
- CRUD de colaboradores com ficha pessoal, endereço e dados profissionais.
- Cadastro independente de tipos de contrato, com os exemplos acadêmicos iniciais.
- Editor de modelos de contrato em página inteira, com pré-visualização durante a digitação.
- Geração de contratos por seleção pesquisável de colaborador e tipo/modelo; o texto usado é preservado no contrato emitido.
- Editor visual A4 com **HTML/JavaScript leve integrado ao JSF/PrimeFaces**, caixas de texto, linhas, tabelas, assinaturas e tags.
- Visualização com substituição dos dados cadastrados.
- Exportação em **PDF** e **DOCX** (conversão parcial das formatações para Word).
- Logo global opcional (PNG/JPEG até 1 MB), posicionado no texto pela tag `{{empresa.logo}}`.
- Linha de assinatura inserida pela tag `{{documento.assinatura}}`. **Não equivale a assinatura digital ou eletrônica.**

Os CRUDs anteriores foram utilizados na instalação local. As alterações mais recentes, relacionadas a editor, migrações e exportações, **ainda exigem compilação, testes e homologação no notebook**. Não utilizar com dados pessoais reais ou como aplicação pública até essa validação.

## Primeira instalação

Pré-requisitos: Java JDK 21, Maven, PostgreSQL e Tomcat 10.1. As configurações de banco são definidas pelas variáveis de ambiente `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER` e `DB_PASSWORD`. **Não armazene senhas no repositório.** O arquivo `.env.example` serve somente de referência e não é carregado automaticamente.

Banco: `contratofacil`, usuário de aplicação: `contratofacil_app`. Execute scripts como esse usuário, na sequência abaixo. Scripts de migração usam `IF NOT EXISTS` e preservam registros existentes.

| Ordem | Script | Finalidade |
|---|---|---|
| 1 | `database/01_schema.sql` | Tabela colaboradores |
| 2 | `database/04_modelos_contratos.sql` | Modelos e contratos |
| 3 | `database/06_usuarios.sql` | Usuários do login |
| 4 | `database/07_ficha_colaborador.sql` | Novos campos da ficha |
| 5 | `database/08_categorias_documentos.sql` | Categorias dos documentos |
| 6 | `database/09_logo_empresa.sql` | Logo global opcional |
| 7 | `database/11_tipos_contrato.sql` | Tipos de contratos e associação preservando registros existentes |

Caso as tabelas iniciais já existam, execute somente as migrações ainda pendentes, respeitando a ordem. **Não exclua ou recrie o banco**. Faça um backup antes de migrar caso já tenha informações importantes.

Exemplo no PowerShell, aberto na pasta do projeto (o PostgreSQL 18 pode estar em outro caminho):

```powershell
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
& $psql -X -W -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U contratofacil_app -d contratofacil -f .\database\07_ficha_colaborador.sql
& $psql -X -W -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U contratofacil_app -d contratofacil -f .\database\08_categorias_documentos.sql
& $psql -X -W -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U contratofacil_app -d contratofacil -f .\database\09_logo_empresa.sql
& $psql -X -W -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U contratofacil_app -d contratofacil -f .\database\11_tipos_contrato.sql
```

O parâmetro `-W` solicita a senha sem escrevê-la no comando. Na primeira instalação, execute também os scripts anteriores.

## Compilar e executar

```powershell
mvn -U clean verify
powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tomcat.ps1
```

O comando `verify` executa os testes unitários configurados e gera `target/contratofacil.war`. O Tomcat utiliza uma pasta local em `.runtime/` (ignorada pelo Git), sem alterar outros servidores. Informe a senha do banco no prompt. Na instalação do notebook, a porta do Tomcat pode ser **8081**, porque a 8080 já estava ocupada por outro processo. Consulte o terminal do script para confirmar o endereço.

A primeira vez que ativar o login, crie o administrador no próprio computador em:

- `http://localhost:8081/contratofacil/primeiro-acesso.xhtml`
- Depois acesse `http://localhost:8081/contratofacil/login.xhtml`.

A página de primeiro acesso só aceita conexões locais e deixa de permitir novos cadastros após a criação do primeiro administrador. Não há senha inicial padrão. O acesso sem login aos cadastros e downloads deve ser bloqueado.

## Fluxo de elaboração de documentos

1. **Colaboradores:** mantenha a ficha cadastral de quem receberá os documentos.
2. **Tipos:** crie as finalidades, como contrato de trabalho ou descrição de cargo. Cinco opções iniciais e "Outros" são sugeridas pela migração, mas você pode criar outras.
3. **Modelos:** clique em "Criar modelo". A tela contém um **catálogo pesquisável de campos** com filtros por identificação, dados pessoais, endereço, dados contratuais e documento. Clique no campo para inseri-lo no cursor, sem digitar manualmente o código. Ao passar o mouse, o sistema mostra o significado e um exemplo. A prévia alterna entre **Exemplo preenchido** (dados exclusivamente fictícios) e **Ver tags**. O botão de ajuda mostra um passo a passo. Salve o modelo vinculado ao tipo desejado.
4. **Contratos:** clique em "Gerar contrato". Pesquise pelo nome/matrícula do colaborador, selecione tipo e modelo. A prévia utiliza os dados do colaborador.
5. Clique em **Gerar e visualizar** para criar o registro e abrir a versão final. A exportação PDF/DOCX fica na visualização.
6. Editar um contrato emitido não reescreve automaticamente seu texto se o modelo original tiver sido alterado. Para atualizar a base do texto, selecione outro modelo.

Modelos e contratos antigos foram preservados. A migração associa seus valores de categoria aos novos tipos. Não execute novamente o script inicial de criação do banco.

Algumas tags disponíveis:

| Tag | Conteúdo |
|---|---|
| `{{colaborador.nome}}` | Nome |
| `{{colaborador.cpf}}` | CPF |
| `{{colaborador.matricula}}` | Matrícula |
| `{{colaborador.cargo}}` | Cargo |
| `{{colaborador.dataAdmissao}}` | Admissão |
| `{{colaborador.salario}}` | Salário |
| `{{colaborador.centroCustoDescricao}}` | Centro de custo |
| `{{colaborador.logradouro}}` | Endereço |
| `{{colaborador.cidade}}` | Cidade |
| `{{documento.dataAtual}}` | Data atual |
| `{{documento.assinatura}}` | Linha de assinatura |
| `{{empresa.logo}}` | Logo previamente cadastrado |

As tags permanecem no texto salvo. O editor oferece **prévia demonstrativa em tempo real**, sem consultar colaboradores verdadeiros. Na aba Contratos, a prévia, a visualização e as exportações consultam a ficha real selecionada e fazem o preenchimento no servidor.

O catálogo é mantido no Java em `CatalogoCampos.java`: ele define a chave, o rótulo, a descrição, o exemplo fictício e a leitura do campo. Os campos da ficha incluem sexo, raça/cor, estado civil, nacionalidade, escolaridade, gênero, endereço completo, centro de custo e salário, além dos dados tradicionais. Use dados sensíveis somente quando necessários ao documento. Tag desconhecida aparece como campo não informado. Para manter os marcadores íntegros, evite formatar apenas parte de uma tag no editor.

Para enviar o logo, abra **Configurar logo** no editor. O arquivo fica salvo na tabela de configuração do próprio projeto. O editor não permite upload arbitrário de HTML/imagens: logo é inserido por tag.

## Testes e cuidados

Além dos testes Java com `mvn test`, existem os scripts SQL `database/05_teste_cruds.sql`, `database/10_teste_ficha_categorias.sql` e `database/12_teste_fluxo_tipos.sql`. Os dois utilizam `ROLLBACK`, de modo que os dados fictícios de teste não ficam persistidos.

A validação automática do GitHub compila o WAR e testa a formação das páginas XHTML e as tags. Ela **não substitui os testes de login, persistência e navegação JSF no Tomcat do notebook**.

Validações manuais obrigatórias antes de considerar esta versão pronta:
- Incluir, pesquisar, editar e excluir colaboradores, modelos e contratos.
- Criar um modelo formatado, salvar e reabrir.
- Inserir tags e conferir os dados após salvar.
- Inserir logo, alterar alinhamento e gerar PDF e DOCX.
- Abrir os arquivos exportados no leitor PDF e no Word.
- Confirmar login, senha incorreta, logout e proteção das URLs.
- Verificar layout em tela pequena.

A geração DOCX preserva **formatação textual básica** (negrito, itálico, sublinhado, alinhamento, parágrafos e listas simples). Não replica todos os recursos visuais do Quill. O PDF utiliza um renderizador XHTML/CSS, com suporte variável a estilos de fonte. Assinatura é apenas área para assinatura manual, sem validade de assinatura eletrônica.

**Template Manhattan:** os arquivos do template da disciplina ainda não foram disponibilizados; o layout atual é CSS/PrimeFaces próprio e não deve ser apresentado como o Manhattan oficial.

**Privacidade:** o cadastro contempla campos pessoais e sensíveis. Para a demonstração acadêmica, use exclusivamente registros fictícios; o projeto não tem uma política completa de retenção, consentimento e auditoria de dados.


## Editor visual A4

A edição dos modelos usa um **canvas A4 de composição livre**, com objetos posicionados por coordenadas X/Y. Em **Modelos → Criar modelo**, adicione **Texto**, **Linha**, **Tabela** ou **Assinatura**. Clique em um objeto e utilize o ícone ⠿ para arrastar, ↘ para redimensionar. A barra contextual oferece tamanho, cor, orientação e espessura da linha; cabeçalho, estilos (grade, listrado e simples) e inserção/remoção de linhas ou colunas na tabela; e papel do assinante. O nome aparece **abaixo** da linha de assinatura. Caixas de texto e células são editáveis diretamente, inclusive com tags.

A prévia é demonstrativa; durante a geração do contrato os campos são preenchidos no servidor a partir do colaborador selecionado. O projeto preserva JSF + PrimeFaces + PostgreSQL e não adiciona dependências ao frontend.

### Edição direta, teclado e imagens

A barra de formatação permanece **fixa acima da página A4**, mesmo sem objetos selecionados. Um clique seleciona o componente e permite **arrastá-lo por sua superfície**. As alças aparecem nas quatro bordas e nos quatro cantos para ajustar posição/tamanho. **Dois cliques** entram no modo de edição de textos e células; **Esc** sai da edição para voltar a mover. Para texto novo, a folha já contém uma caixa inicial; dar dois cliques no espaço vazio também cria uma caixa naquele ponto.

Quando um objeto está selecionado e fora do modo de digitação, **Delete/Backspace** remove o objeto; **setas** movem 1 unidade (Shift + seta move 10), **Ctrl+D** duplica, **Ctrl+C/V** copia e cola componentes dentro da página, e **Ctrl+Z/Ctrl+Y** desfaz/refaz operações de layout. Enquanto digita, as teclas operam normalmente sobre os caracteres do texto.

A ferramenta **＋ Imagem**, junto das opções de inserir texto, linha, tabela e assinatura, permite colocar qualquer imagem PNG/JPEG de até 8 MB de origem, compactada pelo navegador e armazenada no conteúdo do modelo (limite aproximado de **270 KB por imagem após compressão**, e 2 MB por documento). O backend verifica o conteúdo real do arquivo e não aceita SVG. As imagens aparecem na prévia e no PDF/DOCX; o DOCX é editável, mas continua reordenando os objetos pelo fluxo vertical.

### Formatação de texto no Canvas

Ao selecionar uma caixa de **Texto**, aparece uma faixa compacta semelhante à de um editor de documentos: 23 famílias de fonte (Aptos, Calibri, Cambria, Arial, Times New Roman, Georgia, Garamond e outras), tamanho numérico **6 a 96 pt**, aumento/redução por pontos, negrito, itálico, sublinhado, tachado, cor da fonte, marca-texto, limpar formatação, alinhamento, estilo de parágrafo, listas e recuo.

**Selecione uma palavra ou trecho** dentro da caixa de texto e use a formatação. Sem seleção de texto, os estilos são aplicados aos parágrafos da caixa. O HTML formatado é sanitizado no Java, salvo no modelo e usado nas exportações PDF/DOCX. No Word, a fonte e a altura dos trechos formatados são preservadas. A fidelidade visual no PDF e no DOCX depende de as fontes estarem disponíveis na máquina que renderiza o documento; **não há fontes licenciadas incorporadas**. As células de tabela aceitam texto e tags, mas a formatação rica individual de cada célula ainda não é persistida.

A faixa não depende de bibliotecas externas e preserva a arquitetura Java 21 + JSF + PrimeFaces + PostgreSQL.

### Compatibilidade e persistência

O novo formato `CFCANVAS1` é validado e salvo **na mesma coluna** `conteudo` usada pelos modelos antigos. A conversão de um modelo antigo para canvas só é persistida **ao salvar** no editor novo. Os marcadores legados continuam renderizados no servidor para contratos já emitidos, sem migração de banco. Faça backup antes de converter modelos valiosos.

**Limites conscientes:** uma página A4 por modelo, até 100 componentes; a exportação **PDF** respeita coordenadas livres e a exportação **DOCX** preserva texto/tabelas editáveis, mas reordena objetos pelo eixo vertical, sem reproduzir fielmente sobreposições. Não é uma ferramenta de assinatura eletrônica. Homologue no notebook antes de usar dados pessoais reais.

### Atualização local

Nenhuma migração SQL é necessária para a versão canvas. Execute `git status`, confirme que não há alterações locais, faça `git pull --ff-only origin main`, compile com `mvn -U clean verify` e reinicie o Tomcat via `scripts/tomcat.ps1`. Faça um teste: crie uma tabela, arraste, salve e reabra; gere um contrato fictício e confira PDF/DOCX.

## Ferramentas de composição de documentos (versão atual)

### Tags personalizadas

Após aplicar a migração `database/13_tags_personalizadas.sql`, use a nova aba **Tags** para cadastrar um identificador em letras minúsculas/números e sublinhado, nome amigável, descrição e origem:

- Texto fixo (por exemplo, nome da empresa)
- Campo vinculado à ficha de colaborador
- Data, hora, data e hora ou ano do sistema, no fuso de Brasília.

Uma tag criada com código `nome_empresa` aparece no editor como `{{personalizada.nome_empresa}}`. O catálogo permite pesquisar e inserir a tag no cursor. Campos dinâmicos são resolvidos no servidor na prévia do contrato e na exportação. Não é permitido executar SQL ou JavaScript por uma tag.

### Linhas e tabelas

Os objetos são criados visualmente no canvas, sem digitar marcadores `{{visual.*}}`. As tabelas permitem cabeçalho, edição direta das células e estilos simples. Linhas permitem cor, espessura e orientação. O documento final usa os dados reais substituídos na emissão.

### Assinaturas

Selecione o objeto **Assinatura** no canvas e escolha o papel: **colaborador**, **gestor**, **testemunha 1**, **testemunha 2** ou **outro participante**. Ao gerar um contrato, os nomes desses participantes podem ser digitados ou selecionados pela busca de colaboradores.

Os nomes são armazenados no registro emitido, após a migração `database/14_assinaturas_contrato.sql`. O campo de assinatura é **um espaço para assinatura manual**, não uma assinatura eletrônica ou certificada. O sistema não possui processo de assinatura digital.

### Atualização de uma base existente

Após backup do PostgreSQL, execute uma vez, na ordem:

```powershell
$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
& $psql -X -W -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U contratofacil_app -d contratofacil -f .\database\13_tags_personalizadas.sql
& $psql -X -W -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 5432 -U contratofacil_app -d contratofacil -f .\database\14_assinaturas_contrato.sql
```

Não apague as tabelas antigas. Depois compile `mvn -U clean verify` e reinicie apenas o Tomcat local do ContratoFácil. A CI valida Java, XHTML, JavaScript e a persistência usando PostgreSQL temporário. Teste o funcionamento das telas no notebook antes de considerar a versão pronta.
