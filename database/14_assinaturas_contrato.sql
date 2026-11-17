-- Como ler este script: Campos para os participantes das assinaturas manuais dos documentos; não é assinatura digital.
-- SQL = definição/consulta de dados. Não é executado automaticamente só por atualizar o Git.
-- Participantes selecionados ao emitir um contrato. Não configura assinatura eletrônica.
BEGIN;
DO $$
BEGIN
 IF current_database() <> 'contratofacil' OR current_user <> 'contratofacil_app' THEN
  RAISE EXCEPTION 'Execute como contratofacil_app no banco contratofacil.';
 END IF;
END $$;
ALTER TABLE public.contratos ADD COLUMN IF NOT EXISTS gestor_nome VARCHAR(160);
ALTER TABLE public.contratos ADD COLUMN IF NOT EXISTS testemunha1_nome VARCHAR(160);
ALTER TABLE public.contratos ADD COLUMN IF NOT EXISTS testemunha2_nome VARCHAR(160);
ALTER TABLE public.contratos ADD COLUMN IF NOT EXISTS outro_assinante_nome VARCHAR(160);
COMMIT;
