# ContratoFácil

Aplicação acadêmica para cadastro de colaboradores, manutenção de modelos e elaboração de contratos com campos dinâmicos. Usa **Java 21, JSF/Jakarta Faces, PrimeFaces, Maven, Tomcat 10.1, JDBC e PostgreSQL**. O projeto é um WAR tradicional, sem frontend adicional.

## Funcionalidades implementadas no código

- Login administrativo com sessão, logout e filtro de acesso.
- CRUD de colaboradores com ficha pessoal, endereço e dados profissionais.
- Cadastro independente de tipos de contrato, com os exemplos acadêmicos iniciais.
- Editor de modelos de contrato em página inteira, com pré-visualização durante a digitação.
- Geração de contratos por seleção pesquisável de colaborador e tipo/modelo; o texto usado é preservado no contrato emitido.
- Editor visual **PrimeFaces p:textEditor**, formatação de texto e inserção de tags.
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
3. **Modelos:** clique em "Criar modelo". Uma página completa apresenta a edição formatada com tags e uma prévia isolada que acompanha a digitação. Salve o modelo vinculado ao tipo desejado.
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

As tags permanecem no texto salvo e são substituídas **somente na visualização/exportação**. Tag desconhecida aparece como campo não informado. Para manter os marcadores íntegros, evite formatar apenas parte de uma tag no editor.

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
