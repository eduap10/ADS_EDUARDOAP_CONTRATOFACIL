-- Dados totalmente ficticios para testes locais (opcional).
INSERT INTO colaboradores (matricula, nome, cargo, cpf, email)
VALUES
    ('TESTE001', 'Colaborador Exemplo', 'Assistente Administrativo', NULL, 'exemplo@invalid.local'),
    ('TESTE002', 'Pessoa de Teste', 'Analista', NULL, 'teste@invalid.local')
ON CONFLICT (matricula) DO NOTHING;
