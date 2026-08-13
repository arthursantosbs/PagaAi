package br.com.pagaai.domain;

/** Frequencia de vencimento de uma cobranca. */
public enum Periodicidade {
    MENSAL("Mensal"),
    SEMANAL("Semanal");

    private final String descricao;

    Periodicidade(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
