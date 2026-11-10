package br.edu.contratofacil.servlet;

import br.edu.contratofacil.dao.ColaboradorDAO;
import br.edu.contratofacil.dao.ContratoDAO;
import br.edu.contratofacil.model.Colaborador;
import br.edu.contratofacil.model.Contrato;
import br.edu.contratofacil.service.DocumentoService;
import br.edu.contratofacil.service.ExportacaoDocumento;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

@WebServlet("/exportar-contrato")
public class ExportarContratoServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long id;
        try {
            id = Long.parseLong(req.getParameter("id"));
            if (id < 1) throw new NumberFormatException();
        } catch (Exception e) {
            resp.sendError(400, "Identificador inválido.");
            return;
        }
        String formato = req.getParameter("formato");
        if (!"pdf".equals(formato) && !"docx".equals(formato)) {
            resp.sendError(400, "Formato não suportado.");
            return;
        }
        try {
            Contrato contrato = new ContratoDAO().buscar(id);
            if (contrato == null) {
                resp.sendError(404, "Contrato não encontrado.");
                return;
            }
            Colaborador colaborador = new ColaboradorDAO().buscar(contrato.getColaboradorId());
            if (colaborador == null) {
                resp.sendError(404, "Colaborador não encontrado.");
                return;
            }
            String conteudo = DocumentoService.preencher(contrato.getConteudo(), colaborador);
            resp.setContentType("pdf".equals(formato) ? "application/pdf"
                : "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            resp.setHeader("X-Content-Type-Options", "nosniff");
            resp.setHeader("Content-Disposition", "attachment; filename=\"contrato-" + id + "." + formato + "\"");
            if ("pdf".equals(formato)) {
                ExportacaoDocumento.pdf(contrato.getTitulo(), conteudo, resp.getOutputStream());
            } else {
                ExportacaoDocumento.docx(contrato.getTitulo(), conteudo, resp.getOutputStream());
            }
        } catch (SQLException e) {
            if (!resp.isCommitted()) resp.sendError(500, "Falha ao consultar o banco.");
        } catch (Exception e) {
            if (!resp.isCommitted()) resp.sendError(500, "Falha ao gerar o documento.");
        }
    }
}
