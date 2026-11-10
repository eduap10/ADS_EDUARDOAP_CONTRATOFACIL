-- Logo opcional para os documentos; mantém o banco e os registros existentes.
BEGIN;
DO $$
BEGIN
 IF current_database() <> 'contratofacil' OR current_user <> 'contratofacil_app' THEN
  RAISE EXCEPTION 'Execute em contratofacil como contratofacil_app.';
 END IF;
END $$;
CREATE TABLE IF NOT EXISTS public.configuracao_documento (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    logo BYTEA,
    mime VARCHAR(20)
);
COMMIT;
