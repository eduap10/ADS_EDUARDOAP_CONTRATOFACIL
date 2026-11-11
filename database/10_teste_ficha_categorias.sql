-- Teste sem persistir dados, após 07 e 08.
BEGIN;
DO $$
DECLARE
  matricula_teste TEXT := 'CF' || txid_current()::TEXT;
  id_teste BIGINT;
  modelo_teste BIGINT;
BEGIN
  IF current_database() <> 'contratofacil' OR current_user <> 'contratofacil_app' THEN
    RAISE EXCEPTION 'Banco ou usuário incorretos.';
  END IF;
  INSERT INTO public.colaboradores
    (matricula,nome,cargo,data_nascimento,data_admissao,cep,cidade,uf,centro_custo_codigo,centro_custo_descricao,salario)
  VALUES (matricula_teste,'Pessoa de Teste','Analista','1990-02-10','2020-01-15','99000-000',
          'Passo Fundo','RS','100','Administrativo',3250.25)
  RETURNING id INTO id_teste;
  IF NOT EXISTS (SELECT 1 FROM public.colaboradores
    WHERE id=id_teste AND salario=3250.25 AND cidade='Passo Fundo') THEN
      RAISE EXCEPTION 'Falha na ficha ampliada.';
  END IF;
  INSERT INTO public.modelos_contrato(titulo,conteudo,categoria)
  VALUES ('Modelo temporário','{{colaborador.nome}}','CONTRATO_TRABALHO')
  RETURNING id INTO modelo_teste;
  IF NOT EXISTS (SELECT 1 FROM public.modelos_contrato
    WHERE id=modelo_teste AND categoria='CONTRATO_TRABALHO') THEN
      RAISE EXCEPTION 'Falha na categoria de modelo.';
  END IF;
  RAISE NOTICE 'Ficha ampliada e categoria: OK. Nenhum dado de teste será persistido.';
END $$;
ROLLBACK;
