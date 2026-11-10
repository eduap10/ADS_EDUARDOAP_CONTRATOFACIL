-- Amplia o cadastro sem perder dados anteriores. Executar após 01_schema.sql.
BEGIN;
DO $$
BEGIN
 IF current_database() <> 'contratofacil' OR current_user <> 'contratofacil_app' THEN
  RAISE EXCEPTION 'Execute apenas em contratofacil como contratofacil_app.';
 END IF;
END $$;
ALTER TABLE public.colaboradores
 ADD COLUMN IF NOT EXISTS data_nascimento DATE,
 ADD COLUMN IF NOT EXISTS sexo VARCHAR(40),
 ADD COLUMN IF NOT EXISTS raca_cor VARCHAR(40),
 ADD COLUMN IF NOT EXISTS estado_civil VARCHAR(40),
 ADD COLUMN IF NOT EXISTS nacionalidade VARCHAR(60),
 ADD COLUMN IF NOT EXISTS escolaridade VARCHAR(60),
 ADD COLUMN IF NOT EXISTS genero VARCHAR(60),
 ADD COLUMN IF NOT EXISTS cep VARCHAR(9),
 ADD COLUMN IF NOT EXISTS logradouro VARCHAR(150),
 ADD COLUMN IF NOT EXISTS numero VARCHAR(20),
 ADD COLUMN IF NOT EXISTS complemento VARCHAR(100),
 ADD COLUMN IF NOT EXISTS bairro VARCHAR(100),
 ADD COLUMN IF NOT EXISTS cidade VARCHAR(100),
 ADD COLUMN IF NOT EXISTS uf VARCHAR(2),
 ADD COLUMN IF NOT EXISTS pais VARCHAR(60),
 ADD COLUMN IF NOT EXISTS data_admissao DATE,
 ADD COLUMN IF NOT EXISTS centro_custo_codigo VARCHAR(30),
 ADD COLUMN IF NOT EXISTS centro_custo_descricao VARCHAR(120),
 ADD COLUMN IF NOT EXISTS salario NUMERIC(14,2);
COMMIT;
