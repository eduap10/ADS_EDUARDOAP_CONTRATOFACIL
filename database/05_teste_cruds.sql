-- Teste SQL dos cadastros de colaboradores e modelos.
-- Os dados fictícios são removidos automaticamente pelo ROLLBACK final.
-- Execute no banco contratofacil como contratofacil_app, após 01_schema.sql e 04_modelos_contratos.sql.
BEGIN;

DO $teste$
DECLARE
    id_colaborador BIGINT;
    id_modelo BIGINT;
    matricula_teste TEXT := 'TESTE' || txid_current()::TEXT;
BEGIN
    IF current_database() <> 'contratofacil' OR current_user <> 'contratofacil_app' THEN
        RAISE EXCEPTION 'Execute o teste somente em contratofacil como contratofacil_app.';
    END IF;

    INSERT INTO public.colaboradores (matricula, nome, cargo)
    VALUES (matricula_teste, 'Colaborador de Teste', 'Teste')
    RETURNING id INTO id_colaborador;

    IF NOT EXISTS (
        SELECT 1 FROM public.colaboradores WHERE id = id_colaborador AND nome = 'Colaborador de Teste'
    ) THEN
        RAISE EXCEPTION 'Falha ao listar colaborador inserido.';
    END IF;

    UPDATE public.colaboradores SET nome = 'Colaborador Atualizado' WHERE id = id_colaborador;
    IF NOT EXISTS (
        SELECT 1 FROM public.colaboradores WHERE id = id_colaborador AND nome = 'Colaborador Atualizado'
    ) THEN
        RAISE EXCEPTION 'Falha ao atualizar colaborador.';
    END IF;

    INSERT INTO public.modelos_contrato (titulo, conteudo)
    VALUES ('Modelo de Teste', 'Texto fictício para testar o cadastro.')
    RETURNING id INTO id_modelo;

    IF NOT EXISTS (
        SELECT 1 FROM public.modelos_contrato WHERE id = id_modelo AND titulo = 'Modelo de Teste'
    ) THEN
        RAISE EXCEPTION 'Falha ao listar modelo inserido.';
    END IF;

    UPDATE public.modelos_contrato SET titulo = 'Modelo Atualizado' WHERE id = id_modelo;
    IF NOT EXISTS (
        SELECT 1 FROM public.modelos_contrato WHERE id = id_modelo AND titulo = 'Modelo Atualizado'
    ) THEN
        RAISE EXCEPTION 'Falha ao atualizar modelo.';
    END IF;

    DELETE FROM public.modelos_contrato WHERE id = id_modelo;
    IF EXISTS (SELECT 1 FROM public.modelos_contrato WHERE id = id_modelo) THEN
        RAISE EXCEPTION 'Falha ao excluir modelo.';
    END IF;

    DELETE FROM public.colaboradores WHERE id = id_colaborador;
    IF EXISTS (SELECT 1 FROM public.colaboradores WHERE id = id_colaborador) THEN
        RAISE EXCEPTION 'Falha ao excluir colaborador.';
    END IF;

    RAISE NOTICE 'Consultas, inserções, atualizações e exclusões de colaboradores e modelos: OK.';
END
$teste$;

ROLLBACK;
