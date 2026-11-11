-- Teste transacional dos novos tipos e da emissão, sem manter dados.
-- Executar depois de 11_tipos_contrato.sql como contratofacil_app em contratofacil.
BEGIN;
DO $$
DECLARE
  id_tipo BIGINT;
  id_modelo BIGINT;
  id_colaborador BIGINT;
  id_documento BIGINT;
  identificador TEXT := 'CF' || txid_current()::TEXT;
BEGIN
  IF current_database() <> 'contratofacil' OR current_user <> 'contratofacil_app' THEN
    RAISE EXCEPTION 'Conecte em contratofacil como contratofacil_app.';
  END IF;
  INSERT INTO public.tipos_contrato(nome, descricao)
    VALUES ('Tipo teste ' || identificador, 'Apenas teste')
    RETURNING id INTO id_tipo;
  INSERT INTO public.modelos_contrato(titulo, conteudo, categoria, tipo_id)
    VALUES ('Modelo teste', '<p>Olá {{colaborador.nome}}</p>', 'OUTROS', id_tipo)
    RETURNING id INTO id_modelo;
  INSERT INTO public.colaboradores(matricula, nome, cargo)
    VALUES (identificador, 'Pessoa de Demonstração', 'Analista')
    RETURNING id INTO id_colaborador;
  INSERT INTO public.contratos(colaborador_id, modelo_id, titulo, conteudo,
                              situacao, categoria, tipo_id)
    VALUES (id_colaborador, id_modelo, 'Contrato teste',
            '<p>Olá {{colaborador.nome}}</p>', 'RASCUNHO', 'OUTROS', id_tipo)
    RETURNING id INTO id_documento;
  IF NOT EXISTS(
    SELECT 1 FROM public.contratos c
    JOIN public.modelos_contrato m ON m.id = c.modelo_id
    JOIN public.tipos_contrato t ON t.id = c.tipo_id
    WHERE c.id = id_documento AND m.tipo_id = id_tipo
      AND t.nome = 'Tipo teste ' || identificador
  ) THEN
    RAISE EXCEPTION 'Relacionamento tipo/modelo/contrato não encontrado.';
  END IF;
  RAISE NOTICE 'Fluxo de tipos, modelo e contrato: OK. ROLLBACK a seguir.';
END $$;
ROLLBACK;
