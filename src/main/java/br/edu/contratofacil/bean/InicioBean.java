package br.edu.contratofacil.bean;

import br.edu.contratofacil.db.Conexao;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/** Visão geral de leitura: indicadores reais, sem duplicar dados em outras tabelas. */
@Named
@RequestScoped
public class InicioBean {
    private static final Logger LOG=Logger.getLogger(InicioBean.class.getName());
    private long colaboradores, modelos, contratos, rascunhos;
    private boolean disponivel;
    private final List<Resumo> recentes=new ArrayList<>();

    @PostConstruct
    public void carregar() {
        recentes.clear();
        disponivel=false;
        String totais="SELECT (SELECT count(*) FROM public.colaboradores),"
            +"(SELECT count(*) FROM public.modelos_contrato),"
            +"(SELECT count(*) FROM public.contratos),"
            +"(SELECT count(*) FROM public.contratos WHERE situacao='RASCUNHO')";
        String ultimos="SELECT c.id,c.titulo,p.nome,c.situacao FROM public.contratos c "
            +"JOIN public.colaboradores p ON p.id=c.colaborador_id ORDER BY c.id DESC LIMIT 5";
        try(var conexao=Conexao.abrir()){
            try(var ps=conexao.prepareStatement(totais);var rs=ps.executeQuery()){
                if(rs.next()){colaboradores=rs.getLong(1);modelos=rs.getLong(2);contratos=rs.getLong(3);rascunhos=rs.getLong(4);}
            }
            try(var ps=conexao.prepareStatement(ultimos);var rs=ps.executeQuery()){
                while(rs.next())recentes.add(new Resumo(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4)));
            }
            disponivel=true;
        }catch(SQLException e){
            LOG.warning("Indicadores indisponíveis. SQLState="+e.getSQLState());
        }
    }
    public long getColaboradores(){return colaboradores;}
    public long getModelos(){return modelos;}
    public long getContratos(){return contratos;}
    public long getRascunhos(){return rascunhos;}
    public boolean isDisponivel(){return disponivel;}
    public List<Resumo> getRecentes(){return List.copyOf(recentes);}

    // Getters JavaBeans permitem que a expressão EL do JSF acesse cada coluna.
    public static final class Resumo {
        private final long id;
        private final String titulo,colaborador,situacao;
        public Resumo(long id,String titulo,String colaborador,String situacao){
            this.id=id;this.titulo=titulo;this.colaborador=colaborador;this.situacao=situacao;
        }
        public long getId(){return id;}
        public String getTitulo(){return titulo;}
        public String getColaborador(){return colaborador;}
        public String getSituacao(){return situacao;}
    }
}
