package br.com.pagaai.domain;

/** Situacao de uma parcela depois de receber os pagamentos alocados. */
public enum SituacaoParcela {

    QUITADA("Quitada"),
    ATRASADA("Atrasada"),
    VENCE_HOJE("Vence hoje"),
    A_VENCER("A vencer");

    private final String descricao;

    SituacaoParcela(String descricao) {
        this.descricao = descricao;
    }

    /** Texto exibido na coluna "Situacao" das telas de parcelas. */
    public String getDescricao() {
        return descricao;
    }
}
