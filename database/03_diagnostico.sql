-- Execute estas consultas no PostgreSQL para diagnosticar a instalacao.
-- IMPORTANTE: conecte-se ao banco contratofacil com o usuario utilizado pela aplicacao.
SELECT current_database() AS banco, current_user AS usuario, current_schema() AS esquema;
SELECT to_regclass('public.colaboradores') AS tabela_colaboradores;
SELECT has_table_privilege(current_user, 'public.colaboradores', 'SELECT') AS pode_consultar,
       has_table_privilege(current_user, 'public.colaboradores', 'INSERT') AS pode_inserir,
       has_table_privilege(current_user, 'public.colaboradores', 'UPDATE') AS pode_atualizar,
       has_table_privilege(current_user, 'public.colaboradores', 'DELETE') AS pode_excluir;
SELECT id, matricula, nome, cargo FROM public.colaboradores ORDER BY nome LIMIT 10;
-- Para INSERIR em uma coluna identity, tambem sao necessarios
-- privilegios de uso da sequence (quando aplicavel).
