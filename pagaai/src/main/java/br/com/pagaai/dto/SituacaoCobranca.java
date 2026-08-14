package br.com.pagaai.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Retrato de uma divida hoje: quanto era, quanto entrou, quanto falta e
 * quanto disso ja deveria ter sido pago.
 *
 * <p>A conta que interessa: {@code saldoDevedor = valorTotal - totalPago} e
 * {@code valorEmAtraso = parcelas ja vencidas - totalPago} (nunca negativo).
 * O caso "devia 100 no dia 20, pagou 80" aparece como saldo 20 e atraso 20.
 *
 * @param valorAVencer   parte do saldo que ainda nao venceu
 * @param quitada        so para divida de valor fechado: pagou tudo, acabou
 */
public record SituacaoCobranca(
        Long cobrancaId,
        Long clienteId,
        String clienteNome,
        String clienteTelefone,
        String clienteInstagram,
        String descricao,
        boolean valorFechado,
        boolean ativa,
        BigDecimal valorTotal,
        BigDecimal totalPago,
        BigDecimal saldoDevedor,
        BigDecimal valorEmAtraso,
        BigDecimal valorVencendoHoje,
        BigDecimal valorAVencer,
        boolean quitada,
        long diasAtraso,
        LocalDate vencimentoMaisAntigoEmAberto,
        LocalDate proximoVencimento,
        BigDecimal proximoValor,
        int parcelasTotais,
        int parcelasQuitadas,
        List<ParcelaView> parcelas) {

    public boolean isEmAtraso() {
        return valorEmAtraso.signum() > 0;
    }

    public boolean isDevedor() {
        return saldoDevedor.signum() > 0;
    }

    /**
     * Cobranca que existe mas hoje nao tem nada a receber, sem estar quitada.
     * O caso tipico e a cobranca recorrente cadastrada antes do primeiro
     * vencimento: o saldo de hoje e zero, mas ela nao acabou.
     *
     * <p>Existe porque as telas separavam apenas "devedora" e "quitada", e uma
     * cobranca nesta situacao sumia de todas elas — parecia que o cadastro nao
     * tinha sido salvo. Sempre inclua este grupo ao listar dividas.
     */
    public boolean isSemSaldoHoje() {
        return !quitada && saldoDevedor.signum() == 0;
    }

    /** Quanto o cliente precisa pagar agora para ficar em dia (atraso + o que vence hoje). */
    public BigDecimal getValorParaFicarEmDia() {
        return valorEmAtraso.add(valorVencendoHoje);
    }

    /**
     * Valor que o formulario de pagamento ja vem preenchido: o que fecha o atraso,
     * ou a proxima parcela quando esta tudo em dia.
     */
    public BigDecimal getValorSugerido() {
        BigDecimal paraFicarEmDia = getValorParaFicarEmDia();
        if (paraFicarEmDia.signum() > 0) {
            return paraFicarEmDia;
        }
        if (proximoValor != null && proximoValor.signum() > 0) {
            return proximoValor;
        }
        return saldoDevedor;
    }

    /** 0 a 100, para a barra de progresso. Divida recorrente nao tem fim, entao nao tem barra. */
    public BigDecimal getPercentualPago() {
        if (!valorFechado || valorTotal == null || valorTotal.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return totalPago.multiply(BigDecimal.valueOf(100))
                .divide(valorTotal, 0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(100));
    }

    /** Pagou mais do que devia — sobra registrada para nao sumir do caixa. */
    public BigDecimal getValorPagoAMais() {
        if (!valorFechado || valorTotal == null) {
            return BigDecimal.ZERO;
        }
        return totalPago.subtract(valorTotal).max(BigDecimal.ZERO);
    }

    public String getSeveridade() {
        if (quitada) {
            return "pago";
        }
        return Severidade.porDiasDeAtraso(diasAtraso);
    }
}
