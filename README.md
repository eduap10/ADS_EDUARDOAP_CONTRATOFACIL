# ContratoFácil

Sistema acadêmico individual para cadastrar colaboradores, criar modelos de documentos e elaborar contratos. A arquitetura permanece **Java 21 + JSF + PrimeFaces + PostgreSQL**, em um projeto Maven WAR tradicional.

## Requisitos do trabalho

- [ ] Autenticação e filtro de acesso administrativo.
- [ ] Uso de um enum para situação do contrato.
- [x] Código do CRUD de colaboradores: criar, editar, excluir, listar e pesquisar (teste integrado pendente).
- [ ] CRUD de modelos de contrato.
- [ ] CRUD de contratos e relacionamento um-para-muitos com colaboradores.
- [ ] Editor de contratos a partir de modelos e dados do colaborador.
- [x] Conexão JDBC e schema inicial PostgreSQL (validação autenticada pendente).

**Manhattan ainda não foi fornecido.** O visual atual é provisório; não atende a esse requisito. Sua compatibilidade será verificada quando o template da disciplina estiver disponível.

## Estrutura

```text
src/main/java/br/edu/contratofacil/   # model, dao, bean e conexao JDBC
src/main/webapp/                    # paginas JSF, recursos e WEB-INF
database/01_schema.sql              # tabela de colaboradores
database/02_dados_exemplo.sql       # dados ficticios opcionais
scripts/build.ps1                   # atalho opcional para Maven no Windows
scripts/tomcat.ps1                  # executar Tomcat local no terminal
.vscode/tasks.json                 # compilar, executar e parar
pom.xml
README.md
.gitignore
.env.example                       # referencia sem senha
```

`target/` e `.runtime/` são locais e ignorados pelo Git. Não versionar WARs, logs, dumps, certificados privados, senhas, configurações pessoais do VS Code ou arquivos `.env`.

## Executar no Windows / VS Code

Pré-requisitos: JDK 21 e Maven no PATH, PostgreSQL em execução e Tomcat 10.1. O `pom.xml` inclui Mojarra 4.0.17, PrimeFaces 16.0.0, Weld 5.1.6.Final e JDBC PostgreSQL 42.7.8. Faces e CDI vão no WAR porque o Tomcat não os fornece. Nenhuma dependência foi adicionada nesta preparação.

1. **Banco:** com autorização e credenciais administrativas, crie pelo pgAdmin, somente se ausentes, o usuário `contratofacil_app` (login habilitado, sem privilégios administrativos) e o banco `contratofacil`, pertencente a ele. Defina a senha no diálogo local. Preserve usuários/bancos existentes, incluindo `dbprimefaces` e `postgres`.
2. **Schema:** conectado como `contratofacil_app` ao banco `contratofacil`, execute `database/01_schema.sql`. Pode usar o Query Tool do pgAdmin ou, com `psql` no PATH, o terminal na raiz do projeto:

   ```powershell
   psql -X -W -h localhost -p 5432 -U contratofacil_app -d contratofacil -v ON_ERROR_STOP=1 -f database/01_schema.sql
   ```

   A senha é solicitada sem eco. O SQL recusa outro banco/usuário. `02_dados_exemplo.sql` é opcional e contém apenas dados fictícios.
3. **Tomcat:** baixe o ZIP Core 10.1 da [Apache](https://tomcat.apache.org/download-10.cgi), confira seu SHA-512 com `Get-FileHash arquivo.zip -Algorithm SHA512` e extraia em `.runtime/`, mantendo o nome `apache-tomcat-10.1.x`. A distribuição 10.1.61 já foi baixada e verificada neste workspace; não é necessário reinstalá-la.
4. **VS Code:** use **Terminal → Executar Tarefa → ContratoFacil: compilar** (`Ctrl+Shift+B`), depois **ContratoFacil: executar Tomcat**. A execução pede a senha de `contratofacil_app` no terminal se `DB_PASSWORD` não estiver no ambiente.
5. Abra [http://localhost:8080/contratofacil/](http://localhost:8080/contratofacil/). Os logs aparecem no mesmo terminal. Para encerrar, use a tarefa **ContratoFacil: parar Tomcat**. Após modificar código: **parar → compilar → executar**.

Alternativa no terminal, sem depender das tarefas do VS Code:

```powershell
mvn clean package
powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tomcat.ps1
```

Para parar pelo terminal, abra outro terminal e execute `powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tomcat.ps1 -Stop`. Somente o processo Java com a base deste projeto é encerrado; outros servidores e o Java do VS Code são preservados.

O script encontra o Tomcat dentro de `.runtime/`, ou aceita `-TomcatHome` / `CATALINA_HOME`. Cria uma base exclusiva em `.runtime/tomcat-base`, copia o WAR e atende somente em `127.0.0.1:8080`, sem serviço Windows nem alterações na instalação do servidor. Prefere `JAVA_HOME` e, quando ausente, identifica o JDK do PATH. Não há caminhos pessoais nos arquivos versionáveis.

`DB_HOST`, `DB_PORT`, `DB_NAME` e `DB_USER` estão descritos em `.env.example`: ele **não é carregado automaticamente**. A senha fica apenas no ambiente do processo, nunca em argumentos ou arquivos. O script local aceita somente o banco/usuário do projeto. Reinicie o servidor após configurar a senha. `-SkipPasswordPrompt` permite diagnóstico sem banco e **não habilita o CRUD**.

Os dois scripts são conveniências do Windows: a aplicação continua sendo um WAR Maven comum, implantável em Tomcat 10.1 com as variáveis de banco configuradas. `RemoteSigned` vale apenas para o processo da tarefa; nenhuma política global do Windows é alterada.

Se a tabela aparece no pgAdmin mas listar/pesquisar/salvar falham na aplicação, confirme a conexão do pgAdmin com `SELECT current_database(), current_user;`. A aplicação usa `contratofacil`/`contratofacil_app`. Um Tomcat iniciado em diagnóstico sem senha não acessa o banco: pare e execute novamente pela tarefa normal, informando a senha no terminal. Os logs registram somente o código SQLState dos erros, sem senha ou conteúdo dos registros.

## Se ocorrer PKIX no Maven

Use primeiro `mvn clean package`, que passou nesta máquina. O PKIX relatado não foi reproduzido; foi observada inspeção HTTPS pelo Kaspersky, com autoridade já confiável no Windows.

Se necessário, a alternativa local mantém a validação TLS usando as autoridades confiáveis do Windows:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/build.ps1 -WindowsTrust
```

Essa opção usa `Windows-ROOT` apenas no processo Maven e restaura `MAVEN_OPTS` ao terminar. Não importa certificados, não altera `cacerts`, proxy ou configurações globais. Sem a opção, a compilação usa os padrões do Java e do Maven. [Referência do JDK](https://docs.oracle.com/en/java/javase/21/security/oracle-providers.html).

## Validação

Com o banco configurado, teste pela tela usando uma matrícula fictícia exclusiva: incluir, conferir a listagem, pesquisar por matrícula/nome, editar e excluir somente o registro criado no teste. Confira também os campos obrigatórios e a limpeza do diálogo ao abrir Novo depois de um erro.

| Verificação em 21/10/2026 | Resultado |
| --- | --- |
| Maven padrão e geração do WAR Java 21 | Aprovados; não há testes JUnit no projeto |
| Execução com os scripts simplificados | Inicialização e logs no terminal aprovados |
| Parada pelo mesmo script | Aprovada; porta 8080 liberada e Java do VS Code preservado |
| Página inicial, colaboradores, CSS e JavaScript | HTTP 200 |
| Nome obrigatório e limpeza de Novo após erro | Aprovados; correções preservadas |
| Falha do banco visível na primeira abertura | Aprovada |
| PostgreSQL | Serviço disponível; faltam credenciais para autenticar |
| Schema e CRUD persistido | **Bloqueados**, não aprovados |
| Git | Temporários e credenciais ignorados; sem commit/push enquanto o CRUD estiver bloqueado |

O teste HTTP auxiliar desta sessão foi mantido apenas em `.runtime/`, fora do Git; ele não é necessário para executar a aplicação. O banco e os usuários existentes não foram alterados. Os avisos do Weld sobre JPA/Bean Validation opcionais não impediram a renderização; não foram adicionadas bibliotecas apenas para silenciá-los.

**Uso local:** esta versão ainda não implementa autenticação. Utilize apenas dados fictícios e não publique a aplicação na internet.
