package br.com.pagaai.dto;

/** Faixa de atraso usada para colorir linhas e etiquetas na tela. */
public final class Severidade {

    private Severidade() {
    }

    public static String porDiasDeAtraso(long dias) {
        if (dias <= 0) {
            return "aberto";
        }
        if (dias <= 7) {
            return "atraso-leve";
        }
        if (dias <= 30) {
            return "atraso-medio";
        }
        return "atraso-grave";
    }
}
