package br.com.pagaai.dto;

import br.com.pagaai.domain.SituacaoParcela;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Parcela depois de receber os pagamentos alocados.
 *
 * @param pago  quanto dos pagamentos foi alocado nesta parcela
 * @param saldo quanto ainda falta nela ({@code valor - pago})
 */
public record ParcelaView(
        int numero,
        LocalDate vencimento,
        BigDecimal valor,
        BigDecimal pago,
        BigDecimal saldo,
        SituacaoParcela situacao,
        long diasAtraso) {

    /** Recebeu parte do valor, mas nao tudo. */
    public boolean isParcial() {
        return pago.signum() > 0 && saldo.signum() > 0;
    }

    public String getSeveridade() {
        return switch (situacao) {
            case QUITADA -> "pago";
            case A_VENCER -> "aberto";
            case VENCE_HOJE -> "atraso-leve";
            case ATRASADA -> Severidade.porDiasDeAtraso(diasAtraso);
        };
    }
}
