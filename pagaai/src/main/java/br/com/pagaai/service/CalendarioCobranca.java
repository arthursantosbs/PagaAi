package br.com.pagaai.service;

import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.domain.Periodicidade;
import br.com.pagaai.dto.Parcela;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta o carne da divida: em que datas ela vence e quanto cada vencimento cobra.
 *
 * <p>Nada disso vai para o banco. As parcelas sao recalculadas a partir de
 * (dataInicio + periodicidade + valorParcela) toda vez que uma tela e montada.
 * A vantagem: mudar o dia do vencimento e so editar a cobranca, sem migrar linha
 * nenhuma. O custo: a geracao roda muitas vezes, entao ela precisa ser barata —
 * por isso nao ha acesso a banco aqui dentro.
 *
 * <p><b>Papel no sistema:</b> primeira metade do calculo. Produz as parcelas
 * "secas" (data + valor). Quem cruza isso com o dinheiro que entrou e a
 * {@link CalculadoraDeDivida}.
 *
 * <p><b>Se voce mexer aqui:</b> muda a data ou o valor de TODA parcela do sistema —
 * painel, telas de divida, agenda e API. Rode {@code CalendarioCobrancaTest} e
 * {@code CalculadoraDeDividaTest}: os dois dependem desta classe.
 */
@Component
public class CalendarioCobranca {

    /** Trava de seguranca contra uma dataInicio muito antiga ou parcela minuscula. */
    private static final int MAX_OCORRENCIAS = 600;

    /**
     * Parcelas da cobranca. Numa divida de valor fechado elas param assim que a
     * soma cobre o valor total — a ultima vem quebrada com o resto. Numa cobranca
     * recorrente (valorTotal nulo) vao ate o horizonte informado.
     */
    public List<Parcela> parcelas(Cobranca cobranca, LocalDate horizonte) {
        BigDecimal valorParcela = cobranca.getValorParcela();
        if (valorParcela == null || valorParcela.signum() <= 0) {
            return List.of();
        }

        BigDecimal total = cobranca.getValorTotal();
        List<LocalDate> datas = total != null
                // Divida fechada: o que encerra e o valor pago, nao o calendario.
                // Por isso aqui dataFim e horizonte nao limitam.
                ? datas(cobranca, null, quantidadeDeParcelas(total, valorParcela))
                : datas(cobranca, limite(cobranca, horizonte), MAX_OCORRENCIAS);

        List<Parcela> saida = new ArrayList<>(datas.size());
        BigDecimal acumulado = BigDecimal.ZERO;
        for (LocalDate data : datas) {
            BigDecimal valor = valorParcela;
            if (total != null) {
                BigDecimal restante = total.subtract(acumulado);
                if (restante.signum() <= 0) {
                    break;
                }
                valor = valor.min(restante);
            }
            saida.add(new Parcela(data, valor.setScale(2, RoundingMode.HALF_UP)));
            acumulado = acumulado.add(valor);
        }
        return saida;
    }

    /** Quantas parcelas cobrem o total (a ultima pode vir quebrada). */
    private int quantidadeDeParcelas(BigDecimal total, BigDecimal valorParcela) {
        int quantidade = total.divide(valorParcela, 0, RoundingMode.CEILING).intValue();
        return Math.min(Math.max(quantidade, 1), MAX_OCORRENCIAS);
    }

    private LocalDate limite(Cobranca cobranca, LocalDate horizonte) {
        if (cobranca.getDataFim() == null) {
            return horizonte;
        }
        if (horizonte == null) {
            return cobranca.getDataFim();
        }
        return cobranca.getDataFim().isBefore(horizonte) ? cobranca.getDataFim() : horizonte;
    }

    /** Datas de vencimento a partir de dataInicio, ate o limite ou ate o maximo de ocorrencias. */
    private List<LocalDate> datas(Cobranca cobranca, LocalDate limite, int maximo) {
        List<LocalDate> saida = new ArrayList<>();
        LocalDate inicio = cobranca.getDataInicio();
        if (limite != null && limite.isBefore(inicio)) {
            return saida;
        }

        if (cobranca.getPeriodicidade() == Periodicidade.MENSAL) {
            int dia = cobranca.getDiaDoMes() == null ? inicio.getDayOfMonth() : cobranca.getDiaDoMes();
            YearMonth mes = YearMonth.from(inicio);
            // O primeiro mes pode ser descartado quando o dia escolhido ja passou
            // na data de inicio; dai o teto de iteracoes ser maximo + 1.
            int iteracoes = 0;
            while (saida.size() < maximo && iteracoes++ <= maximo) {
                LocalDate data = mes.atDay(Math.min(dia, mes.lengthOfMonth()));
                if (limite != null && data.isAfter(limite)) {
                    break;
                }
                if (!data.isBefore(inicio)) {
                    saida.add(data);
                }
                mes = mes.plusMonths(1);
            }
        } else {
            LocalDate data = inicio;
            if (cobranca.getDiaDaSemana() != null) {
                while (data.getDayOfWeek() != cobranca.getDiaDaSemana()) {
                    data = data.plusDays(1);
                }
            }
            while (saida.size() < maximo) {
                if (limite != null && data.isAfter(limite)) {
                    break;
                }
                saida.add(data);
                data = data.plusWeeks(1);
            }
        }
        return saida;
    }

}
