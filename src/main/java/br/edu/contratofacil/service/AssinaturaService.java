package br.edu.contratofacil.service;

import br.edu.contratofacil.model.Colaborador;
import br.edu.contratofacil.model.Contrato;
import java.util.LinkedHashMap;
import java.util.Map;

/** Espaço para assinatura manual, sem qualquer pretensão de assinatura digital. */
public final class AssinaturaService {
    private AssinaturaService() { }

    public static Map<String, String> combinar(Map<String, String> personalizadas,
            Contrato contrato, Colaborador colaborador) {
        Map<String, String> valores = new LinkedHashMap<>();
        if (personalizadas != null) valores.putAll(personalizadas);
        valores.put("assinatura.colaborador",
                linha("Colaborador", colaborador == null ? null : colaborador.getNome()));
        valores.put("assinatura.gestor", linha("Gestor", contrato.getGestorNome()));
        valores.put("assinatura.testemunha1", linha("Testemunha 1", contrato.getTestemunha1Nome()));
        valores.put("assinatura.testemunha2", linha("Testemunha 2", contrato.getTestemunha2Nome()));
        valores.put("assinatura.outro", linha("Outro participante", contrato.getOutroAssinanteNome()));
        return valores;
    }

    private static String linha(String papel, String nome) {
        return "________________________________________ " + papel + ": "
                + (nome == null || nome.isBlank() ? "Não informado" : nome.strip());
    }
}
