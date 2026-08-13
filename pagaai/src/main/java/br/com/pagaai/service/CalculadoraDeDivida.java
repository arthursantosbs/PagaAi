package br.com.pagaai.service;

import br.com.pagaai.domain.Cliente;
import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.domain.SituacaoParcela;
import br.com.pagaai.dto.Parcela;
import br.com.pagaai.dto.ParcelaView;
import br.com.pagaai.dto.SituacaoCobranca;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * O coracao do sistema: dado o que foi combinado e quanto o cliente ja pagou,
 * responde quanto ele ainda deve, quanto disso esta atrasado e ha quantos dias.
 *
 * <p>Os pagamentos nao sao amarrados a parcelas. Eles entram num caixa unico da
 * divida e sao alocados da parcela mais antiga para a mais nova. Isso resolve
 * sozinho o caso do enunciado: parcela de 100 vencida no dia 20, pagamento de 80
 * -> a parcela fica com 20 em aberto e conta atraso desde o dia 20.
 *
 * <p>Classe sem estado e sem acesso a banco — por isso e barata de chamar em lote
 * e facil de testar.
 */
@Component
public class CalculadoraDeDivida {

    /** Ate onde projetar parcelas de cobranca recorrente (que nao tem fim). */
    private static final int HORIZONTE_DIAS_RECORRENTE = 120;

    private final CalendarioCobranca calendario;

    public CalculadoraDeDivida(CalendarioCobranca calendario) {
        this.calendario = calendario;
    }

    public SituacaoCobranca calcular(Cobranca cobranca, Cliente cliente, BigDecimal totalPago, LocalDate hoje) {
        BigDecimal pago = normalizar(totalPago);
        List<Parcela> parcelas = calendario.parcelas(cobranca, hoje.plusDays(HORIZONTE_DIAS_RECORRENTE));

        List<ParcelaView> detalhe = alocar(parcelas, pago, hoje);

        BigDecimal vencidoAteOntem = somar(parcelas, p -> p.vencimento().isBefore(hoje));
        BigDecimal vencidoAteHoje = somar(parcelas, p -> !p.vencimento().isAfter(hoje));

        // Nunca negativo: pagar adiantado nao vira "atraso negativo".
        BigDecimal emAtraso = vencidoAteOntem.subtract(pago).max(BigDecimal.ZERO);
        BigDecimal devidoAteHoje = vencidoAteHoje.subtract(pago).max(BigDecimal.ZERO);
        BigDecimal vencendoHoje = devidoAteHoje.subtract(emAtraso).max(BigDecimal.ZERO);

        BigDecimal saldo = cobranca.isValorFechado()
                ? cobranca.getValorTotal().subtract(pago).max(BigDecimal.ZERO)
                // Recorrente nao tem total: o que se pode afirmar e o vencido em aberto.
                : devidoAteHoje;
        BigDecimal aVencer = saldo.subtract(devidoAteHoje).max(BigDecimal.ZERO);

        boolean quitada = cobranca.isValorFechado() && saldo.signum() == 0;

        ParcelaView maisAntigaEmAberto = detalhe.stream()
                .filter(p -> p.situacao() == SituacaoParcela.ATRASADA)
                .findFirst()
                .orElse(null);
        ParcelaView proxima = detalhe.stream()
                .filter(p -> p.saldo().signum() > 0 && !p.vencimento().isBefore(hoje))
                .findFirst()
                .orElse(null);

        long diasAtraso = maisAntigaEmAberto == null ? 0 : maisAntigaEmAberto.diasAtraso();

        return new SituacaoCobranca(
                cobranca.getId(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getTelefone(),
                cliente.getInstagram(),
                cobranca.getDescricao(),
                cobranca.isValorFechado(),
                cobranca.getValorTotal(),
                pago,
                escala(saldo),
                escala(emAtraso),
                escala(vencendoHoje),
                escala(aVencer),
                quitada,
                diasAtraso,
                maisAntigaEmAberto == null ? null : maisAntigaEmAberto.vencimento(),
                proxima == null ? null : proxima.vencimento(),
                proxima == null ? null : proxima.saldo(),
                parcelas.size(),
                (int) detalhe.stream().filter(p -> p.situacao() == SituacaoParcela.QUITADA).count(),
                detalhe);
    }

    /**
     * Distribui o total pago entre as parcelas, da mais antiga para a mais nova.
     * Uma parcela so comeca a receber depois que a anterior fechou.
     */
    private List<ParcelaView> alocar(List<Parcela> parcelas, BigDecimal totalPago, LocalDate hoje) {
        List<ParcelaView> saida = new ArrayList<>(parcelas.size());
        BigDecimal disponivel = totalPago;
        int numero = 1;

        for (Parcela parcela : parcelas) {
            BigDecimal alocado = disponivel.min(parcela.valor()).max(BigDecimal.ZERO);
            disponivel = disponivel.subtract(alocado);
            BigDecimal saldo = parcela.valor().subtract(alocado);

            SituacaoParcela situacao;
            long diasAtraso = 0;
            if (saldo.signum() <= 0) {
                situacao = SituacaoParcela.QUITADA;
            } else if (parcela.vencimento().isBefore(hoje)) {
                situacao = SituacaoParcela.ATRASADA;
                diasAtraso = ChronoUnit.DAYS.between(parcela.vencimento(), hoje);
            } else if (parcela.vencimento().isEqual(hoje)) {
                situacao = SituacaoParcela.VENCE_HOJE;
            } else {
                situacao = SituacaoParcela.A_VENCER;
            }

            saida.add(new ParcelaView(numero++, parcela.vencimento(), escala(parcela.valor()),
                    escala(alocado), escala(saldo), situacao, diasAtraso));
        }
        return saida;
    }

    private BigDecimal somar(List<Parcela> parcelas, java.util.function.Predicate<Parcela> filtro) {
        return parcelas.stream()
                .filter(filtro)
                .map(Parcela::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : escala(valor);
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
