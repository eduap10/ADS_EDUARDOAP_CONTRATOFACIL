package br.edu.contratofacil.service;

import br.edu.contratofacil.model.Colaborador;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jsoup.nodes.Entities;

/**
 * Documento de composição livre (A4, coordenadas relativas).
 * Protocolo textual versionado, sem JSON externo e sem HTML arbitrário.
 * Os textos ricos são sanitizados por DocumentoService antes de persistir.
 */
/**
 * Interpreta o formato CFCANVAS1 da página A4: posição, dimensão e conteúdo
 *  * de cada objeto. Valida limites e tipos antes de produzir HTML seguro.
 *  * Separar esta validação do JavaScript impede confiar apenas no navegador.
 */
public final class CanvasDocumentoService {
    public static final String CABECALHO = "CFCANVAS1\n";
    private static final int LARGURA = 642, ALTURA = 907, MAX_OBJETOS = 100;
    private static final int MAX_IMAGEM_BYTES = 270_000, MAX_DOCUMENTO = 2_000_000;
    private static final Set<String> LINHAS = Set.of("horizontal","vertical","diagonal");
    private static final Set<String> ESTILOS = Set.of("grade","listrado","simples");
    private static final Set<String> PAPEIS = Set.of("colaborador","gestor","testemunha1","testemunha2","outro");
    private CanvasDocumentoService() {}

    public static boolean ehCanvas(String texto) {
        return texto != null && texto.startsWith(CABECALHO);
    }

    private record Objeto(String tipo,int x,int y,int w,int h,String[] campos) { }

    private static int numero(String s,int min,int max) {
        try {
            int n = Integer.parseInt(s);
            if(n < min || n > max) throw new IllegalArgumentException("Coordenada fora da página.");
            return n;
        } catch(NumberFormatException e) {
            throw new IllegalArgumentException("Coordenada inválida.",e);
        }
    }

    private static String decodificar(String dado,int max) {
        if (dado.length() > max * 2) throw new IllegalArgumentException("Conteúdo longo demais.");
        try {
            String valor = new String(Base64.getDecoder().decode(dado),StandardCharsets.UTF_8);
            if (valor.length() > max) throw new IllegalArgumentException("Conteúdo longo demais.");
            return valor;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Conteúdo do componente inválido.",e);
        }
    }

    private static byte[] imagemValida(String tipo,String dados) {
        if(!Set.of("image/png","image/jpeg").contains(tipo) ||
           dados == null || dados.length() > 365_000 || dados.isBlank())
            throw new IllegalArgumentException("Somente imagens PNG/JPEG compactadas são permitidas.");
        byte[] bytes;
        try { bytes=Base64.getDecoder().decode(dados); }
        catch(IllegalArgumentException e) { throw new IllegalArgumentException("Dados de imagem inválidos.",e); }
        if(bytes.length<12 || bytes.length>MAX_IMAGEM_BYTES)
            throw new IllegalArgumentException("Imagem deve ter no máximo 270 KB.");
        boolean png=bytes.length>=8 &&
            (bytes[0]&255)==137&&(bytes[1]&255)==80&&(bytes[2]&255)==78&&(bytes[3]&255)==71 &&
            (bytes[4]&255)==13&&(bytes[5]&255)==10&&(bytes[6]&255)==26&&(bytes[7]&255)==10;
        boolean jpeg=bytes.length>=4&&(bytes[0]&255)==255&&(bytes[1]&255)==216 &&
            (bytes[2]&255)==255&&(bytes[bytes.length-2]&255)==255&&(bytes[bytes.length-1]&255)==217;
        if(!("image/png".equals(tipo) && png || "image/jpeg".equals(tipo) && jpeg))
            throw new IllegalArgumentException("Assinatura de imagem PNG/JPEG inválida.");
        return bytes;
    }

    private static String codificar(String valor) {
        return Base64.getEncoder().encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }

    private static List<Objeto> ler(String bruto) {
        if (!ehCanvas(bruto) || bruto.length() > MAX_DOCUMENTO)
            throw new IllegalArgumentException("Formato de documento inválido.");
        String[] linhas = bruto.substring(CABECALHO.length()).split("\n",-1);
        List<Objeto> resultado = new ArrayList<>();
        for(String linha:linhas) {
            if(linha.isBlank()) continue;
            if(resultado.size() >= MAX_OBJETOS) throw new IllegalArgumentException("Limite de 100 elementos.");
            String[] f = linha.split("\\|",-1);
            if(f.length < 6) throw new IllegalArgumentException("Componente incompleto.");
            int x=numero(f[1],0,LARGURA),y=numero(f[2],0,ALTURA);
            int w=numero(f[3],20,LARGURA),h=numero(f[4],10,ALTURA);
            if(x+w > LARGURA || y+h > ALTURA)
                throw new IllegalArgumentException("Elemento fora dos limites da página.");
            switch(f[0]) {
                case "T" -> {
                    if(f.length!=6) throw new IllegalArgumentException("Texto inválido.");
                    decodificar(f[5],20_000);
                }
                case "L" -> {
                    if(f.length!=8 || !LINHAS.contains(f[5]) || !f[7].matches("[0-9a-fA-F]{6}"))
                        throw new IllegalArgumentException("Linha inválida.");
                    numero(f[6],1,12);
                }
                case "S" -> {
                    if(f.length!=6 || !PAPEIS.contains(f[5]))
                        throw new IllegalArgumentException("Assinatura inválida.");
                }
                case "G" -> {
                    if(f.length!=10 || !ESTILOS.contains(f[5]) || !Set.of("0","1").contains(f[6]))
                        throw new IllegalArgumentException("Tabela inválida.");
                    int nlin=numero(f[7],1,12), ncol=numero(f[8],1,8);
                    String celulas=decodificar(f[9],25_000);
                    if(celulas.split("\u001f",-1).length!=nlin*ncol)
                        throw new IllegalArgumentException("Quantidade de células inválida.");
                }
                case "I" -> {
                    if(f.length!=7)throw new IllegalArgumentException("Componente de imagem inválido.");
                    imagemValida(f[5],f[6]);
                }
                default -> throw new IllegalArgumentException("Tipo de componente desconhecido.");
            }
            resultado.add(new Objeto(f[0],x,y,w,h,f));
        }
        if(resultado.isEmpty()) throw new IllegalArgumentException("Documento vazio.");
        return resultado;
    }

    public static String normalizar(String entrada) {
        List<Objeto> objetos=ler(entrada);
        StringBuilder seguro=new StringBuilder(CABECALHO);
        for(Objeto o:objetos) {
            String[] f=o.campos().clone();
            if(o.tipo().equals("T")) f[5]=codificar(DocumentoService.sanitizar(decodificar(f[5],20_000)));
            if(o.tipo().equals("I")) f[6]=Base64.getEncoder().encodeToString(imagemValida(f[5],f[6]));
            if(o.tipo().equals("G")) {
                String[] celulas=decodificar(f[9],25_000).split("\u001f",-1);
                for(int i=0;i<celulas.length;i++) {
                    celulas[i]=celulas[i].replace("\u001e","").replace("\n"," ").strip();
                    if(celulas[i].length()>500) throw new IllegalArgumentException("Texto da célula muito longo.");
                }
                f[9]=codificar(String.join("\u001f",celulas));
            }
            seguro.append(String.join("|",f)).append('\n');
        }
        return seguro.toString();
    }

    public static String preencher(String dados,Colaborador pessoa,Map<String,String> extras) {
        List<Objeto> objetos=ler(dados);
        StringBuilder html=new StringBuilder(1000+dados.length());
        html.append("<div class=\"cf-canvas-documento\" style=\"position:relative;width:170mm;height:240mm;\">");
        for(Objeto o:objetos) {
            String[] f=o.campos();
            String estilo="position:absolute;left:"+mm(o.x(),LARGURA,170)
                +"mm;top:"+mm(o.y(),ALTURA,240)+"mm;width:"+mm(o.w(),LARGURA,170)
                +"mm;height:"+mm(o.h(),ALTURA,240)+"mm;overflow:hidden;box-sizing:border-box;";
            html.append("<div class=\"cf-canvas-item cf-canvas-"+o.tipo()+"\" style=\"")
                .append(estilo).append("\">");
            switch(o.tipo()) {
                case "T" -> {
                    String texto=decodificar(f[5],20_000);
                    html.append("<div class=\"cf-canvas-texto\">")
                        .append(DocumentoService.preencher(texto,pessoa,extras)).append("</div>");
                }
                case "L" -> {
                    String cor=f[7], grossura=f[6],direcao=f[5];
                    if("vertical".equals(direcao))
                        html.append("<div style=\"height:100%;border-left:").append(grossura)
                            .append("px solid #").append(cor).append(";\"></div>");
                    else
                        html.append("<div class=\"cf-linha-").append(direcao)
                            .append("\" style=\"height:50%;border-bottom:").append(grossura)
                            .append("px solid #").append(cor).append(";\"></div>");
                }
                case "G" -> {
                    int linhas=Integer.parseInt(f[7]),colunas=Integer.parseInt(f[8]);
                    String[] celulas=decodificar(f[9],25_000).split("\u001f",-1);
                    html.append("<table class=\"cf-tabela cf-tabela-").append(f[5])
                        .append("\" style=\"width:100%;border-collapse:collapse;table-layout:fixed;\">");
                    for(int i=0;i<linhas;i++) {
                        html.append("<tr>");
                        for(int j=0;j<colunas;j++) {
                            boolean cabecalho=i==0 && "1".equals(f[6]);
                            String nome=cabecalho?"th":"td";
                            String valor=celulas[i*colunas+j];
                            String preenchido=org.jsoup.Jsoup.parseBodyFragment(
                                DocumentoService.preencher("<p>"+Entities.escape(valor)+"</p>",pessoa,extras)).text();
                            html.append("<").append(nome)
                                .append(" style=\"padding:4px;vertical-align:top;");
                            if(!"simples".equals(f[5])) html.append("border:1px solid #64748b;");
                            if(cabecalho) html.append("background:#e2e8f0;font-weight:bold;");
                            else if("listrado".equals(f[5]) && i%2!=0) html.append("background:#f1f5f9;");
                            html.append("\">").append(Entities.escape(preenchido)).append("</").append(nome).append(">");
                        }
                        html.append("</tr>");
                    }
                    html.append("</table>");
                }
                case "I" -> {
                    html.append("<img class=\"cf-canvas-imagem\" alt=\"Imagem do documento\" src=\"data:")
                        .append(f[5]).append(";base64,").append(f[6])
                        .append("\" style=\"width:100%;height:100%;object-fit:contain;\"/>");
                }
                case "S" -> {
                    String papel=f[5];
                    String label=switch(papel) {
                        case "colaborador" -> "Colaborador";
                        case "gestor" -> "Gestor";
                        case "testemunha1" -> "Testemunha 1";
                        case "testemunha2" -> "Testemunha 2";
                        default -> "Outro participante";
                    };
                    String assinante=extras==null?"":extras.getOrDefault("assinatura."+papel,"");
                    String nome=assinante.contains(":") ? assinante.substring(assinante.indexOf(':')+1).strip() : "Não informado";
                    html.append("<div class=\"cf-canvas-assinatura\" style=\"text-align:center;padding-top:12px;\">")
                        .append("<div style=\"border-bottom:1px solid #334155;height:17px;\"></div>")
                        .append("<p style=\"font-size:11px;margin:4px 0;\">")
                        .append(Entities.escape(label+": "+nome)).append("</p></div>");
                }
                default -> { }
            }
            html.append("</div>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static String mm(int pixels,int base,int mm) {
        return String.format(Locale.ROOT,"%.2f",((double)pixels*mm)/base);
    }
}
