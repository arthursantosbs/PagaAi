package br.com.pagaai.domain;

/** Como a divida termina. */
public enum TipoCobranca {

    /** Fiado, venda parcelada: tem valor total e acaba quando ele e pago. */
    VALOR_FECHADO("Dívida com valor total (fiado, venda parcelada)"),

    /** Mensalidade, assinatura: cobra por tempo indeterminado. */
    RECORRENTE("Cobrança recorrente sem fim (mensalidade)");

    private final String descricao;

    TipoCobranca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
