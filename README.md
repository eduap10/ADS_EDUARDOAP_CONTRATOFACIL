# ContratoFácil

Sistema web acadêmico para cadastro de colaboradores, criação de modelos de documentos e elaboração de contratos com preenchimento de dados.

## Objetivo

Desenvolver uma aplicação **simples, intuitiva e funcional**, seguindo a arquitetura exigida na disciplina: **Java + JSF + PrimeFaces (Template Manhattan) + PostgreSQL**.

O editor permitirá elaborar contratos a partir de modelos de texto, associá-los a um colaborador e armazenar o documento no banco. O foco é atender aos requisitos da atividade sem criar funcionalidades desnecessárias.

## Requisitos do trabalho

- [ ] Autenticação e filtro de acesso administrativo.
- [ ] Uso de pelo menos um `enum` Java (situação do contrato).
- [ ] CRUD de colaboradores: criar, editar, excluir, listar e buscar.
- [ ] CRUD de modelos de contrato: criar, editar, excluir, listar e buscar.
- [ ] CRUD de contratos: criar, editar, excluir, listar e buscar.
- [ ] Funcionalidade principal: elaborar e salvar contratos com editor de texto e preenchimento de dados do colaborador.
- [ ] Relacionamento **um-para-muitos**: um colaborador pode ter vários contratos.
- [ ] Persistência de dados em PostgreSQL.

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

> **Compatibilidade pendente:** confirmar a versão do Manhattan/PrimeFaces/JSF antes de escolher as dependências e o servidor de aplicação. Não adicionar uma versão arbitrária ao `pom.xml`.

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
4. [ ] Criação do projeto Maven e configuração da aplicação JSF.
5. [ ] Modelagem e criação do banco de dados da aplicação.
6. [ ] Implementação do login, `enum` e dos três CRUDs.
7. [ ] Integração do editor de contratos com os dados dos colaboradores.
8. [ ] Testes, documentação final e demonstração.

## Estado atual

**14/10/2026:** repositório iniciado, requisitos definidos e ferramentas básicas verificadas. A aplicação ainda **não está implementada**.

Este README será atualizado conforme as funcionalidades forem desenvolvidas e testadas.
