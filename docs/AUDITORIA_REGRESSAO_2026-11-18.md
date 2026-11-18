# Auditoria de regressão do ContratoFácil — 18/11/2026

## Objetivo e escopo

Revisar os fluxos críticos sem reescrever a aplicação nem alterar o esquema PostgreSQL: editor A4, prévia do contrato, sanitização, importação de imagens, processamento de tags, exportações e persistência.

Ponto de partida: commit `b5c6d282` da branch `main`.
Correções isoladas inicialmente na branch `fix/varredura-contratofacil-20261118`, PR #1.

## Defeitos reproduzidos e corrigidos

1. **Prévia A4 sem colaborador:** `ContratoBean.atualizarPrevia()` usava `sanitizar()`, que devolve o protocolo `CFCANVAS1` em vez do HTML renderizado. Passou a usar `preencher(..., null)`, mantendo a folha visual com campos ainda não informados. Regressão: `PersistenciaIntegracaoTest.previaModeloCanvasSemColaboradorMostraFolhaNaoProtocolo`.
2. **PNG/JPEG corrompidos:** `CanvasDocumentoService.imagemValida()` validava somente os bytes de assinatura; uma imagem falsa era aceita até a etapa de exportação. A leitura agora passa pelo decodificador `ImageIO`, limita dimensões e rejeita formatos incompletos. Regressão: `CanvasDocumentoServiceTest.rejeitaImagemComCabecalhoPngMasSemImagemReal`.
3. **Tags de data dependentes do servidor:** `documento.dataAtual` e `documento.anoAtual` consultavam o fuso padrão da JVM, diferente das tags de hora. Todas agora usam `America/Sao_Paulo`. Regressão: `DocumentoServiceTest.tagsDeDataNaoDependemDoFusoHorarioDoServidor`.

Os testes dos dois primeiros casos **falharam antes das correções**, demonstrando o problema. O pipeline passou após os ajustes.

## Matriz de testes e simulações

| Área | Cobertura automática |
| --- | --- |
| Editor no Chrome real | Seleção de texto; ativar/desativar negrito, itálico, sublinhado e tachado; cores, destaque e remoção de formatação; alinhamento, listas e recuos; mover, redimensionar, atalhos e prévia |
| Canvas A4 | Serialização, reabertura, integridade dos objetos, limites, textos com Unicode, tags e HTML permitido |
| Simulações de fronteira | 160 cenários com semente fixa de coordenadas, dimensões, persistência e tentativas de posicionamento fora da folha |
| Documentos | Sanitização OWASP; preenchimento de tags divididas por formatação; tabela, assinatura, imagens; produção de PDF/DOCX e inspeção estrutural |
| PostgreSQL | Migrações num banco temporário, DAOs de colaboradores, modelos, contratos, tags e teste de prévia sem colaborador |
| Build | Checagem de sintaxe JavaScript e `mvn clean verify` |

Pipeline da última revisão com as simulações: https://github.com/eduap10/ADS_EDUARDOAP_CONTRATOFACIL/actions/runs/44423209822

## Riscos conhecidos e cobertura ainda pendente

- A suíte de autenticação HTTP `AutenticacaoIT` depende de um Tomcat local configurado com `CF_TEST_URL`; não há prova de execução desse conjunto no pipeline de CI padrão.
- O formato atual de células de tabelas do canvas persiste **texto simples**. Uma formatação rica aplicada dentro da célula pode parecer funcionar na prévia, mas não será mantida após salvar e reabrir. Exige evolução coordenada do protocolo `G`, sanitização e renderizadores; não foi modificada nesta rodada para não corromper documentos existentes.
- As tags são preenchidas novamente ao visualizar/exportar contratos. Se a regra de negócio exigir congelar valores e datas **da emissão**, será necessário definir um mecanismo de snapshot com migração e testes próprios.
- Ainda faltam inspeção visual manual em múltiplas resoluções, testes completos contra um Tomcat de homologação e carga concorrente real. Um pipeline verde não equivale a ausência total de bugs.

## Como conferir no notebook

```powershell
git status
git pull --ff-only origin main
mvn clean verify
.\scripts\tomcat.ps1 -Stop
.\scripts\tomcat.ps1
```

Antes de atualizar, preserve alterações locais apontadas por `git status`. No navegador, use `Ctrl+F5` para recarregar os recursos. Não há scripts SQL novos nesta auditoria.
