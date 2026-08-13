package br.com.pagaai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tudo que um cliente deve, somando as dividas dele.
 * Todo mundo com saldo e devedor; quem tem {@code totalEmAtraso} maior que zero
 * e o que precisa de cobranca hoje.
 */
public record PendenciaCliente(
        Long clienteId,
        String clienteNome,
        String telefone,
        String instagram,
        BigDecimal totalDevido,
        BigDecimal totalEmAtraso,
        BigDecimal totalAVencer,
        BigDecimal totalJaPago,
        long diasAtraso,
        int dividasAbertas,
        LocalDate proximoVencimento,
        BigDecimal proximoValor,
        List<SituacaoCobranca> cobrancas) {

    public boolean isEmAtraso() {
        return totalEmAtraso.signum() > 0;
    }

    public String getSeveridade() {
        return Severidade.porDiasDeAtraso(diasAtraso);
    }
}
