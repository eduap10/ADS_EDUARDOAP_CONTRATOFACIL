# ContratoFácil

Sistema acadêmico individual para cadastrar colaboradores, criar modelos de documentos e elaborar contratos. A arquitetura permanece **Java 21 + JSF + PrimeFaces + PostgreSQL**, em um projeto Maven WAR tradicional.

## Requisitos do trabalho

- [x] Autenticação e filtro administrativo validados com Tomcat/PostgreSQL.
- [x] Enum de situação do contrato implementado (pendente validação integrada).
- [x] CRUD de colaboradores: criar, editar, excluir, listar e pesquisar (SQL/JDBC validados; validação visual pendente).
- [x] CRUD de modelos de contrato: criar, editar, excluir, listar e pesquisar (SQL/JDBC validados; validação visual pendente).
- [x] CRUD inicial de contratos e relacionamento um-para-muitos no SQL (pendente validação integrada).
- [ ] Editor de contratos a partir de modelos e dados do colaborador.
- [x] Conexão JDBC e schema PostgreSQL (validação autenticada concluída).

**Manhattan ainda não foi fornecido.** O visual atual é provisório; não atende a esse requisito. Sua compatibilidade será verificada quando o template da disciplina estiver disponível.

## Estrutura

```text
src/main/java/br/edu/contratofacil/   # model, dao, bean e conexao JDBC
src/main/webapp/                    # paginas JSF, recursos e WEB-INF
database/01_schema.sql              # tabela de colaboradores
database/02_dados_exemplo.sql       # dados ficticios opcionais
database/04_modelos_contratos.sql    # tabelas de modelos e contratos
database/05_teste_cruds.sql         # teste transacional dos cadastros
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
2. **Schema:** conectado como `contratofacil_app` ao banco `contratofacil`, execute `database/01_schema.sql` e depois `database/04_modelos_contratos.sql` para habilitar os outros dois cadastros. Pode usar o Query Tool do pgAdmin ou, com `psql` no PATH, o terminal na raiz do projeto:

   ```powershell
   psql -X -W -h localhost -p 5432 -U contratofacil_app -d contratofacil -v ON_ERROR_STOP=1 -f database/01_schema.sql
   ```

   A senha é solicitada sem eco. O script `01_schema.sql` recusa outro banco/usuário. `02_dados_exemplo.sql` é opcional e contém apenas dados fictícios.
3. **Tomcat:** baixe o ZIP Core 10.1 da [Apache](https://tomcat.apache.org/download-10.cgi), confira seu SHA-512 com `Get-FileHash arquivo.zip -Algorithm SHA512` e extraia em `.runtime/`, mantendo o nome `apache-tomcat-10.1.x`. A distribuição 10.1.61 já foi baixada e verificada neste workspace; não é necessário reinstalá-la.
4. **VS Code:** use **Terminal → Executar Tarefa → ContratoFacil: compilar** (`Ctrl+Shift+B`), depois **ContratoFacil: executar Tomcat**. A execução pede a senha de `contratofacil_app` no terminal se `DB_PASSWORD` não estiver no ambiente.
5. Abra [http://localhost:8080/contratofacil/](http://localhost:8080/contratofacil/). Os logs aparecem no mesmo terminal. Para encerrar, use a tarefa **ContratoFacil: parar Tomcat**. Após modificar código: **parar → compilar → executar**.

Alternativa no terminal, sem depender das tarefas do VS Code:

```powershell
mvn clean package
powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tomcat.ps1
```

O script `build.ps1` usa o Maven do PATH ou, quando indisponível, a distribuição existente em `.runtime/apache-maven-*/bin`. Nesta configuração local, foi aproveitado o ZIP do Maven 3.10.0 já presente no computador, sem alterar o PATH global.

Para parar pelo terminal, abra outro terminal e execute `powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/tomcat.ps1 -Stop`. Somente o processo Java com a base deste projeto é encerrado; outros servidores e o Java do VS Code são preservados.

O script encontra o Tomcat dentro de `.runtime/`, ou aceita `-TomcatHome` / `CATALINA_HOME`. Cria uma base exclusiva em `.runtime/tomcat-base`, copia o WAR e atende somente em `127.0.0.1:8080`, sem serviço Windows nem alterações na instalação do servidor. Prefere `JAVA_HOME` e, quando ausente, identifica o JDK do PATH. Não há caminhos pessoais nos arquivos versionáveis.

**Configuração local de 04/11/2026:** a porta 8080 já pertence a outro processo Apache e foi preservada. O ContratoFácil usa [http://localhost:8081/contratofacil/](http://localhost:8081/contratofacil/). O script lê a porta do único `Connector` em `.runtime/tomcat-base/conf/server.xml`, mantendo o endereço `127.0.0.1`. Em uma base nova, o padrão continua sendo 8080.

`DB_HOST`, `DB_PORT`, `DB_NAME` e `DB_USER` estão descritos em `.env.example`: ele **não é carregado automaticamente**. A senha fica apenas no ambiente do processo, nunca em argumentos ou arquivos. O script local aceita somente o banco/usuário do projeto. Reinicie o servidor após configurar a senha. `-SkipPasswordPrompt` permite diagnóstico sem banco e **não habilita o CRUD**.

Os dois scripts são conveniências do Windows: a aplicação continua sendo um WAR Maven comum, implantável em Tomcat 10.1 com as variáveis de banco configuradas. `RemoteSigned` vale apenas para o processo da tarefa; nenhuma política global do Windows é alterada.

Se a tabela aparece no pgAdmin mas listar/pesquisar/salvar falham na aplicação, confirme a conexão do pgAdmin com `SELECT current_database(), current_user;`. A aplicação usa `contratofacil`/`contratofacil_app`. Um Tomcat iniciado em diagnóstico sem senha não acessa o banco: pare e execute novamente pela tarefa normal, informando a senha no terminal. Os logs registram somente o código SQLState dos erros, sem senha ou conteúdo dos registros. Se uma operação falhar, leia o aviso no cadastro e confira os logs do Tomcat.

## Se ocorrer PKIX no Maven

Use primeiro `mvn clean package`, que passou nesta máquina. O PKIX relatado não foi reproduzido; foi observada inspeção HTTPS pelo Kaspersky, com autoridade já confiável no Windows.

Se necessário, a alternativa local mantém a validação TLS usando as autoridades confiáveis do Windows:

```powershell
powershell -NoProfile -ExecutionPolicy RemoteSigned -File scripts/build.ps1 -WindowsTrust
```

Essa opção usa `Windows-ROOT` apenas no processo Maven e restaura `MAVEN_OPTS` ao terminar. Não importa certificados, não altera `cacerts`, proxy ou configurações globais. Sem a opção, a compilação usa os padrões do Java e do Maven. [Referência do JDK](https://docs.oracle.com/en/java/javase/21/security/oracle-providers.html).

## Testar os cadastros no PostgreSQL

Depois de executar os scripts 01 e 04, conecte-se com `contratofacil_app` ao banco `contratofacil` e execute:

```powershell
psql -X -W -v ON_ERROR_STOP=1 -h localhost -p 5432 -U contratofacil_app -d contratofacil -f database/05_teste_cruds.sql
```

O teste verifica inclusão, consulta, edição e exclusão de colaboradores e modelos com dados fictícios. Ele termina com `ROLLBACK`, sem manter os registros inseridos. **Esse teste deve ser executado na instalação local e não substitui os testes das telas JSF.**

## Correções de interface

- O AJAX do cadastro de colaboradores agora aponta para o componente de mensagens existente.
- Erros de campos obrigatórios aparecem dentro da janela de edição, que é limpa ao iniciar novo cadastro.
- Falhas comuns de PostgreSQL recebem mensagens orientativas sem mostrar credenciais.
- **A validação visual dessas correções permanece pendente; os testes SQL/JDBC passaram.**

## Validação

**04/11/2026:** ambiente local configurado e testes autenticados concluídos.

| Verificação | Resultado |
| --- | --- |
| PostgreSQL utilizado | Instância 18, servidor 18.6, localhost:5432 |
| Outra instância preservada | PostgreSQL 17, porta 5433; nenhum serviço/porta/configuração alterado |
| Recursos criados | Somente banco `contratofacil` e usuário `contratofacil_app`, proprietário do próprio banco |
| Privilégios | Sem superuser, createdb, createrole, replication ou bypassrls; acesso PUBLIC revogado somente no novo banco |
| Schema | `01_schema.sql` e `04_modelos_contratos.sql` executados nessa ordem |
| Teste SQL | `05_teste_cruds.sql` aprovado, encerrado com ROLLBACK |
| JDBC e DAOs | Cadastro, listagem/pesquisa, edição e exclusão dos dois CRUDs aprovados; persistência conferida por conexões independentes |
| Dados de teste | Apenas fictícios; registros criados pelos testes removidos ao concluir |
| Maven 3.10.0 / Java 21 | `clean package`: BUILD SUCCESS; não há testes JUnit no projeto |
| Tomcat | 10.1.61, distribuição oficial com SHA-512 conferido, base exclusiva em `.runtime/tomcat-base`, porta 8081 |
| HTTP | Página inicial, colaboradores e modelos: 200 |
| Interface no navegador | Validação visual pendente: automação interrompida por não conseguir confirmar a URL atual |
| Credenciais | Entrada no prompt protegido do Windows; senhas somente na memória/ambiente dos processos, sem arquivos de credenciais |
| Git | Distribuições, WAR e logs permanecem ignorados em `.runtime/` e `target/` |

Para reiniciar, execute a tarefa **ContratoFacil: executar Tomcat** e informe a senha de `contratofacil_app`. Os valores locais são `DB_HOST=localhost`, `DB_PORT=5432`, `DB_NAME=contratofacil` e `DB_USER=contratofacil_app`. O servidor atual foi iniciado com essas variáveis e `DB_PASSWORD` no ambiente; nenhuma variável global foi modificada. Os logs da execução atual estão em `.runtime/tomcat.stdout.log`, `.runtime/tomcat.stderr.log` e `.runtime/tomcat-base/logs/`.

Para concluir a validação visual, use uma matrícula fictícia exclusiva: incluir, conferir a listagem, pesquisar por matrícula/nome, editar e excluir somente o registro criado no teste. Repita para modelos de contrato e confira campos obrigatórios e limpeza do diálogo ao abrir Novo depois de um erro.

Os avisos informativos do Weld sobre JPA/Bean Validation opcionais não impediram a inicialização; não foram adicionadas bibliotecas apenas para silenciá-los. Os recursos existentes de outros projetos foram preservados.

**Uso acadêmico local:** utilize dados fictícios; esta configuração de Tomcat atende somente em loopback.

## Login administrativo (nova implementação)

A aplicação passou a exigir sessão autenticada. As páginas de colaboradores, modelos e contratos são protegidas por um filtro, com acesso exclusivo ao perfil `ADMIN`. As senhas são armazenadas com hash PBKDF2-HMAC-SHA256 e salt aleatório; nenhuma senha inicial é incluída no projeto.

Para ativar o login em um ambiente local já configurado:

1. Atualize os arquivos com `git pull origin main`.
2. Conecte-se ao banco `contratofacil` com o usuário `contratofacil_app` e execute **somente uma vez** `database/06_usuarios.sql`. Esse script não remove registros dos outros cadastros.
3. Recompile a aplicação com `mvn clean package` e reinicie o Tomcat com o script existente, informando a senha de banco como antes.
4. No próprio computador, acesse `http://localhost:8080/contratofacil/primeiro-acesso.xhtml` e cadastre seu primeiro usuário administrador. Esta página funciona somente em `localhost` e deixa de aceitar novos administradores depois da primeira criação.
5. Acesse `http://localhost:8080/contratofacil/login.xhtml`, faça login e verifique a navegação, o acesso direto por URL e o botão **Sair**.

O administrador inicial deverá ser criado manualmente em cada banco local. Não existe credencial padrão.

**Validado em 04/11/2026:** seis testes de integração passaram no Tomcat/PostgreSQL: primeiro administrador, senha correta/incorreta, usuário inativo, renovação de sessão, logout, bloqueio de usuário comum e de acesso direto, incluindo URLs com parâmetros de caminho e requisições AJAX sem sessão. O filtro exige ADMIN em todas as rotas autenticadas, incluindo futuras visualizações e downloads. Os usuários de teste têm senhas aleatórias e são removidos ao concluir; não existe senha padrão.

Os testes usam JUnit (somente no escopo de teste) e HTTP real. Com `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` no ambiente local e `CF_TEST_URL=http://localhost:8081/contratofacil/`, execute `mvn -Dtest=AutenticacaoIT test` após implantar o WAR. Não grave essas senhas em arquivos. O teste utiliza apenas o banco `contratofacil` e remove exclusivamente seus próprios usuários fictícios.

**Atenção:** o editor visual e o Template Manhattan continuam pendentes, e a interface atual ainda é provisória.
