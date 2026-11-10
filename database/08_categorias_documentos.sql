-- Classificação dos documentos existentes e novos, sem excluir dados.
BEGIN;
DO $$
BEGIN
 IF current_database() <> 'contratofacil' OR current_user <> 'contratofacil_app' THEN
  RAISE EXCEPTION 'Execute apenas em contratofacil como contratofacil_app.';
 END IF;
END $$;
ALTER TABLE public.modelos_contrato
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(40) NOT NULL DEFAULT 'OUTROS';
ALTER TABLE public.contratos
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(40) NOT NULL DEFAULT 'OUTROS';
COMMIT;
