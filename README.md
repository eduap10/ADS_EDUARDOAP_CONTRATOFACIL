# ContratoFácil

Sistema web acadêmico para cadastro de colaboradores, criação de modelos de documentos e elaboração de contratos com preenchimento de dados.

## Objetivo

Desenvolver uma aplicação **simples, intuitiva e funcional**, seguindo a arquitetura exigida na disciplina: **Java + JSF + PrimeFaces (Template Manhattan) + PostgreSQL**.

O editor permitirá elaborar contratos a partir de modelos de texto, associá-los a um colaborador e armazenar o documento no banco. O foco é atender aos requisitos da atividade sem criar funcionalidades desnecessárias.

## Requisitos do trabalho

- [ ] Autenticação e filtro de acesso administrativo.
- [ ] Uso de pelo menos um `enum` Java (situação do contrato).
- [x] CRUD de colaboradores: código inicial para criar, editar, excluir, listar e buscar (pendente teste integrado).
- [ ] CRUD de modelos de contrato: criar, editar, excluir, listar e buscar.
- [ ] CRUD de contratos: criar, editar, excluir, listar e buscar.
- [ ] Funcionalidade principal: elaborar e salvar contratos com editor de texto e preenchimento de dados do colaborador.
- [ ] Relacionamento **um-para-muitos**: um colaborador pode ter vários contratos.
- [x] Persistência: conexão JDBC e script PostgreSQL preparados (pendente teste integrado).

## Tecnologias previstas

| Camada | Tecnologia |
| --- | --- |
| Linguagem | Java 21 |
| Interface | JSF e PrimeFaces |
| Visual | Template Manhattan disponibilizado pela disciplina |
| Construção | Maven |
| Banco de dados | PostgreSQL |
| Controle de versão | Git e GitHub |
| IDE | Eclipse ou VS Code com extensões Java |

 > **Compatibilidade pendente:** a base inicial usa Jakarta Faces 4 / PrimeFaces 16 / Tomcat 10.1; a versão do Manhattan da disciplina ainda precisa ser confirmada para integração.

## Estrutura planejada

- `usuarios`: autenticação e permissões.
- `colaboradores`: informações utilizadas nos contratos.
- `modelos_contrato`: textos-base reutilizáveis.
- `contratos`: documentos vinculados a colaboradores e, quando aplicável, a modelos.

A estrutura será criada por scripts SQL versionados para facilitar a configuração e a manutenção do banco de dados.

## Organização do projeto

O código-fonte e os scripts de banco de dados serão mantidos no mesmo repositório. O Maven será utilizado para gerenciar as dependências e facilitar a execução do projeto.

Regras do projeto:

- Não incluir senhas, arquivos `.env` locais, dados pessoais reais ou credenciais no GitHub.
- Não versionar bibliotecas, artefatos compilados ou configurações particulares da IDE.
- Registrar instruções para configurar o projeto a partir de uma cópia limpa.
- Versionar scripts SQL e utilizar apenas dados fictícios nos testes.

## Etapas de desenvolvimento

1. [x] Definição do objetivo e levantamento dos requisitos.
2. [x] Verificação inicial das ferramentas Java, Maven, Git e PostgreSQL.
3. [ ] Confirmação da versão do template Manhattan e do servidor Java.
4. [x] Criação inicial do projeto Maven e das páginas JSF (pendente compilação e testes).
5. [ ] Modelagem completa do banco; script inicial de colaboradores já preparado.
6. [ ] Implementação do login, `enum` e dos três CRUDs.
7. [ ] Integração do editor de contratos com os dados dos colaboradores.
8. [ ] Testes, documentação final e demonstração.

## Estado atual

**19/10/2026:** estrutura Maven, páginas JSF e CRUD inicial de colaboradores adicionados. Ainda não foram realizados testes integrados em Tomcat/PostgreSQL.

Este README será atualizado conforme as funcionalidades forem desenvolvidas e testadas.


## Primeira execução

1. Criar um banco PostgreSQL chamado `contratofacil` e um usuário de aplicação `contratofacil_app`, com privilégios no banco.
2. Executar `database/01_schema.sql` conectado ao banco como usuário da aplicação. O arquivo `database/02_dados_exemplo.sql` contém apenas exemplos fictícios opcionais.
3. Configurar a variável de ambiente `DB_PASSWORD` com a senha local do usuário. As outras configurações aparecem em `.env.example`, que é apenas referência: **o código não carrega esse arquivo automaticamente**.
4. Compilar com `mvn clean package` (Java 21 e Maven). Implantar o WAR `target/contratofacil.war` em Tomcat 10.1.
5. Abrir `http://localhost:8080/contratofacil/` e testar o cadastro de colaboradores.

**Segurança:** esta versão ainda não implementa login nem controle de acesso. Utilizar somente em ambiente de desenvolvimento isolado e com dados fictícios; não publicar na internet.
