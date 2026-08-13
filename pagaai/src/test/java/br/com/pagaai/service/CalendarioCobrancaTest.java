package br.com.pagaai.service;

import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.domain.Periodicidade;
import br.com.pagaai.dto.Parcela;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa a GERACAO das parcelas (datas e valores).
 * Quem testa a alocacao do dinheiro sobre elas e {@code CalculadoraDeDividaTest}.
 *
 * <p>Todos os casos passam por {@code parcelas(...)}, que e o unico metodo publico
 * da classe e o caminho que a aplicacao realmente usa.
 */
class CalendarioCobrancaTest {

    private static final LocalDate HORIZONTE_LONGO = LocalDate.of(2027, 12, 31);

    private final CalendarioCobranca calendario = new CalendarioCobranca();

    /** Cobranca recorrente: sem valor total, cobra o mesmo valor para sempre. */
    private Cobranca recorrenteMensal(String valorParcela, int diaDoMes, LocalDate inicio) {
        Cobranca c = new Cobranca();
        c.setValorTotal(null);
        c.setValorParcela(new BigDecimal(valorParcela));
        c.setPeriodicidade(Periodicidade.MENSAL);
        c.setDiaDoMes(diaDoMes);
        c.setDataInicio(inicio);
        return c;
    }

    private Cobranca recorrenteSemanal(String valorParcela, DayOfWeek dia, LocalDate inicio) {
        Cobranca c = new Cobranca();
        c.setValorTotal(null);
        c.setValorParcela(new BigDecimal(valorParcela));
        c.setPeriodicidade(Periodicidade.SEMANAL);
        c.setDiaDaSemana(dia);
        c.setDataInicio(inicio);
        return c;
    }

    /** Divida de valor fechado: acaba quando a soma das parcelas cobre o total. */
    private Cobranca fechadaMensal(String total, String parcela, int diaDoMes, LocalDate inicio) {
        Cobranca c = recorrenteMensal(parcela, diaDoMes, inicio);
        c.setValorTotal(new BigDecimal(total));
        return c;
    }

    private List<LocalDate> datas(List<Parcela> parcelas) {
        return parcelas.stream().map(Parcela::vencimento).toList();
    }

    // ------------------------------------------------------------------
    // Datas: mensal
    // ------------------------------------------------------------------

    @Test
    void mensalGeraUmaParcelaPorMes() {
        List<Parcela> parcelas = calendario.parcelas(
                recorrenteMensal("150.00", 5, LocalDate.of(2026, 5, 5)), LocalDate.of(2026, 8, 13));

        assertThat(datas(parcelas)).containsExactly(
                LocalDate.of(2026, 5, 5),
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 7, 5),
                LocalDate.of(2026, 8, 5));
    }

    @Test
    void mensalDia31CaiNoUltimoDiaDosMesesCurtos() {
        List<Parcela> parcelas = calendario.parcelas(
                recorrenteMensal("100.00", 31, LocalDate.of(2026, 1, 31)), LocalDate.of(2026, 4, 30));

        assertThat(datas(parcelas)).containsExactly(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 28),
                LocalDate.of(2026, 3, 31),
                LocalDate.of(2026, 4, 30));
    }

    @Test
    void mensalIgnoraVencimentoAnteriorAoInicio() {
        // Comeca dia 20/05 com vencimento no dia 5: maio nao conta.
        List<Parcela> parcelas = calendario.parcelas(
                recorrenteMensal("100.00", 5, LocalDate.of(2026, 5, 20)), LocalDate.of(2026, 7, 10));

        assertThat(datas(parcelas)).containsExactly(
                LocalDate.of(2026, 6, 5),
                LocalDate.of(2026, 7, 5));
    }

    // ------------------------------------------------------------------
    // Datas: semanal
    // ------------------------------------------------------------------

    @Test
    void semanalCaiSempreNoMesmoDiaDaSemana() {
        // 15/07/2026 e uma quarta; a primeira sexta e 17/07.
        List<Parcela> parcelas = calendario.parcelas(
                recorrenteSemanal("40.00", DayOfWeek.FRIDAY, LocalDate.of(2026, 7, 15)),
                LocalDate.of(2026, 8, 13));

        assertThat(datas(parcelas)).containsExactly(
                LocalDate.of(2026, 7, 17),
                LocalDate.of(2026, 7, 24),
                LocalDate.of(2026, 7, 31),
                LocalDate.of(2026, 8, 7));
        assertThat(datas(parcelas)).allMatch(d -> d.getDayOfWeek() == DayOfWeek.FRIDAY);
    }

    // ------------------------------------------------------------------
    // Limites: horizonte e dataFim
    // ------------------------------------------------------------------

    @Test
    void recorrenteParaNoHorizontePedido() {
        Cobranca c = recorrenteMensal("150.00", 5, LocalDate.of(2026, 5, 5));

        assertThat(calendario.parcelas(c, LocalDate.of(2026, 6, 30))).hasSize(2);
        assertThat(calendario.parcelas(c, LocalDate.of(2026, 12, 31))).hasSize(8);
    }

    @Test
    void recorrenteRespeitaDataFim() {
        Cobranca c = recorrenteSemanal("40.00", DayOfWeek.FRIDAY, LocalDate.of(2026, 7, 17));
        c.setDataFim(LocalDate.of(2026, 7, 31));

        assertThat(datas(calendario.parcelas(c, HORIZONTE_LONGO))).containsExactly(
                LocalDate.of(2026, 7, 17),
                LocalDate.of(2026, 7, 24),
                LocalDate.of(2026, 7, 31));
    }

    @Test
    void dividaFechadaIgnoraDataFimPorqueQuemEncerraEOValorPago() {
        // Decisao de projeto: numa divida fechada a data nao pode fazer dinheiro sumir.
        // Mesmo com dataFim em maio, as 4 parcelas que cobrem os 500 continuam existindo.
        Cobranca c = fechadaMensal("500.00", "150.00", 5, LocalDate.of(2026, 5, 5));
        c.setDataFim(LocalDate.of(2026, 5, 31));

        List<Parcela> parcelas = calendario.parcelas(c, LocalDate.of(2026, 6, 1));

        assertThat(parcelas).hasSize(4);
        assertThat(datas(parcelas)).endsWith(LocalDate.of(2026, 8, 5));
    }

    // ------------------------------------------------------------------
    // Valores: a soma tem que fechar com o total, sem sobra nem falta
    // ------------------------------------------------------------------

    @Test
    void dividaFechadaParaQuandoCobreOTotalEQuebraAUltimaParcela() {
        List<Parcela> parcelas = calendario.parcelas(
                fechadaMensal("500.00", "150.00", 5, LocalDate.of(2026, 5, 5)), LocalDate.of(2026, 6, 1));

        assertThat(parcelas).extracting(Parcela::valor).containsExactly(
                new BigDecimal("150.00"), new BigDecimal("150.00"),
                new BigDecimal("150.00"), new BigDecimal("50.00"));
    }

    @Test
    void somaDasParcelasSempreFechaComOTotal() {
        // Divisao que nao e exata: 100 em parcelas de 30.
        List<Parcela> parcelas = calendario.parcelas(
                fechadaMensal("100.00", "30.00", 5, LocalDate.of(2026, 5, 5)), LocalDate.of(2026, 6, 1));

        BigDecimal soma = parcelas.stream().map(Parcela::valor).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("100.00");
        assertThat(parcelas).hasSize(4);
    }

    @Test
    void parcelaMaiorOuIgualAoTotalViraPagamentoUnico() {
        List<Parcela> parcelas = calendario.parcelas(
                fechadaMensal("250.00", "250.00", 30, LocalDate.of(2026, 8, 30)), HORIZONTE_LONGO);

        assertThat(parcelas).hasSize(1);
        assertThat(parcelas.get(0).valor()).isEqualByComparingTo("250.00");
    }

    // ------------------------------------------------------------------
    // Bordas defensivas
    // ------------------------------------------------------------------

    @Test
    void semValorDeParcelaNaoGeraNada() {
        // Protege contra laco infinito: parcela zero nunca cobriria o total.
        Cobranca c = fechadaMensal("500.00", "150.00", 5, LocalDate.of(2026, 5, 5));
        c.setValorParcela(BigDecimal.ZERO);

        assertThat(calendario.parcelas(c, HORIZONTE_LONGO)).isEmpty();
    }

    @Test
    void horizonteAnteriorAoInicioNaoGeraNada() {
        assertThat(calendario.parcelas(
                recorrenteMensal("150.00", 5, LocalDate.of(2026, 5, 5)), LocalDate.of(2026, 1, 1)))
                .isEmpty();
    }
}
