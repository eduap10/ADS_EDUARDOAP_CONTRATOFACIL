-- Dados totalmente ficticios para testes locais (opcional).
BEGIN;
DO $$
BEGIN
    IF current_database() <> 'contratofacil' OR current_user <> 'contratofacil_app' THEN
        RAISE EXCEPTION 'Execute somente em contratofacil como contratofacil_app.';
    END IF;
END $$;
INSERT INTO colaboradores (matricula, nome, cargo, cpf, email)
VALUES
    ('TESTE001', 'Colaborador Exemplo', 'Assistente Administrativo', NULL, 'exemplo@invalid.local'),
    ('TESTE002', 'Pessoa de Teste', 'Analista', NULL, 'teste@invalid.local')
ON CONFLICT (matricula) DO NOTHING;
COMMIT;
