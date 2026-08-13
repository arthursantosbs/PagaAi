package br.com.pagaai.service;

import br.com.pagaai.domain.Cliente;
import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.domain.Periodicidade;
import br.com.pagaai.domain.SituacaoParcela;
import br.com.pagaai.dto.ParcelaView;
import br.com.pagaai.dto.SituacaoCobranca;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraDeDividaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 13);

    private final CalculadoraDeDivida calculadora = new CalculadoraDeDivida(new CalendarioCobranca());

    private final Cliente cliente = cliente();

    private Cliente cliente() {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setNome("Maria");
        return c;
    }

    private Cobranca fiado(String total, String parcela, int diaDoMes, LocalDate inicio) {
        Cobranca c = new Cobranca();
        c.setId(1L);
        c.setCliente(cliente);
        c.setDescricao("Fiado");
        c.setValorTotal(new BigDecimal(total));
        c.setValorParcela(new BigDecimal(parcela));
        c.setPeriodicidade(Periodicidade.MENSAL);
        c.setDiaDoMes(diaDoMes);
        c.setDataInicio(inicio);
        return c;
    }

    private SituacaoCobranca calcular(Cobranca cobranca, String totalPago) {
        return calculadora.calcular(cobranca, cliente, new BigDecimal(totalPago), HOJE);
    }

    // ------------------------------------------------------------------
    // O caso do enunciado: devia 100 no dia 20, pagou 80.
    // ------------------------------------------------------------------

    @Test
    void pagamentoParcialDeixaORestoEmAtraso() {
        // Divida de 100 com vencimento unico em 20/07, ja passou.
        Cobranca c = fiado("100.00", "100.00", 20, LocalDate.of(2026, 7, 20));

        SituacaoCobranca s = calcular(c, "80.00");

        assertThat(s.saldoDevedor()).isEqualByComparingTo("20.00");
        assertThat(s.valorEmAtraso()).isEqualByComparingTo("20.00");
        assertThat(s.quitada()).isFalse();
        assertThat(s.diasAtraso()).isEqualTo(24);

        ParcelaView parcela = s.parcelas().get(0);
        assertThat(parcela.pago()).isEqualByComparingTo("80.00");
        assertThat(parcela.saldo()).isEqualByComparingTo("20.00");
        assertThat(parcela.situacao()).isEqualTo(SituacaoParcela.ATRASADA);
        assertThat(parcela.isParcial()).isTrue();
    }

    @Test
    void completarOValorQuitaADivida() {
        Cobranca c = fiado("100.00", "100.00", 20, LocalDate.of(2026, 7, 20));

        SituacaoCobranca s = calcular(c, "100.00");

        assertThat(s.quitada()).isTrue();
        assertThat(s.saldoDevedor()).isEqualByComparingTo("0.00");
        assertThat(s.valorEmAtraso()).isEqualByComparingTo("0.00");
        assertThat(s.diasAtraso()).isZero();
        assertThat(s.isDevedor()).isFalse();
    }

    // ------------------------------------------------------------------
    // Divida parcelada: acaba quando o total e alcancado.
    // ------------------------------------------------------------------

    @Test
    void dividaFechadaGeraParcelasAteCobrirOTotalComRestoNaUltima() {
        // 500 em parcelas de 150 -> 150 + 150 + 150 + 50
        Cobranca c = fiado("500.00", "150.00", 5, LocalDate.of(2026, 5, 5));

        SituacaoCobranca s = calcular(c, "0");

        assertThat(s.parcelas()).extracting(ParcelaView::valor)
                .containsExactly(new BigDecimal("150.00"), new BigDecimal("150.00"),
                        new BigDecimal("150.00"), new BigDecimal("50.00"));
        assertThat(s.parcelas()).extracting(ParcelaView::vencimento)
                .containsExactly(LocalDate.of(2026, 5, 5), LocalDate.of(2026, 6, 5),
                        LocalDate.of(2026, 7, 5), LocalDate.of(2026, 8, 5));
        assertThat(s.saldoDevedor()).isEqualByComparingTo("500.00");
    }

    @Test
    void pagamentoEntraDaParcelaMaisAntigaParaAMaisNova() {
        Cobranca c = fiado("500.00", "150.00", 5, LocalDate.of(2026, 5, 5));

        // 380 = quita as duas primeiras (300) e deixa 80 na terceira.
        SituacaoCobranca s = calcular(c, "380.00");

        assertThat(s.parcelas()).extracting(ParcelaView::situacao)
                .containsExactly(SituacaoParcela.QUITADA, SituacaoParcela.QUITADA,
                        SituacaoParcela.ATRASADA, SituacaoParcela.ATRASADA);
        assertThat(s.parcelas().get(2).pago()).isEqualByComparingTo("80.00");
        assertThat(s.parcelas().get(2).saldo()).isEqualByComparingTo("70.00");
        assertThat(s.parcelasQuitadas()).isEqualTo(2);

        assertThat(s.saldoDevedor()).isEqualByComparingTo("120.00");
        assertThat(s.valorEmAtraso()).isEqualByComparingTo("120.00");
        // Atraso conta da parcela mais antiga em aberto: 05/07.
        assertThat(s.vencimentoMaisAntigoEmAberto()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(s.diasAtraso()).isEqualTo(39);
    }

    @Test
    void quitarTodasAsParcelasEncerraACobranca() {
        Cobranca c = fiado("500.00", "150.00", 5, LocalDate.of(2026, 5, 5));

        SituacaoCobranca s = calcular(c, "500.00");

        assertThat(s.quitada()).isTrue();
        assertThat(s.parcelas()).allMatch(p -> p.situacao() == SituacaoParcela.QUITADA);
        assertThat(s.getPercentualPago()).isEqualByComparingTo("100");
    }

    // ------------------------------------------------------------------
    // Separacao entre "deve" e "esta atrasado".
    // ------------------------------------------------------------------

    @Test
    void oQueAindaNaoVenceuNaoContaComoAtraso() {
        // Comeca hoje: nada vencido ainda, mas a divida inteira e devida.
        Cobranca c = fiado("300.00", "100.00", 13, HOJE);

        SituacaoCobranca s = calcular(c, "0");

        assertThat(s.saldoDevedor()).isEqualByComparingTo("300.00");
        assertThat(s.valorEmAtraso()).isEqualByComparingTo("0.00");
        assertThat(s.valorVencendoHoje()).isEqualByComparingTo("100.00");
        assertThat(s.valorAVencer()).isEqualByComparingTo("200.00");
        assertThat(s.isDevedor()).isTrue();
        assertThat(s.isEmAtraso()).isFalse();
        assertThat(s.diasAtraso()).isZero();
    }

    @Test
    void pagarAdiantadoNaoGeraAtrasoNegativo() {
        Cobranca c = fiado("300.00", "100.00", 5, LocalDate.of(2026, 8, 5));

        // Venceu 100 em 05/08 e ele pagou 250.
        SituacaoCobranca s = calcular(c, "250.00");

        assertThat(s.valorEmAtraso()).isEqualByComparingTo("0.00");
        assertThat(s.saldoDevedor()).isEqualByComparingTo("50.00");
        assertThat(s.valorAVencer()).isEqualByComparingTo("50.00");
        assertThat(s.parcelas().get(1).situacao()).isEqualTo(SituacaoParcela.QUITADA);
    }

    @Test
    void pagarAMaisNaoDeixaSaldoNegativo() {
        Cobranca c = fiado("100.00", "100.00", 20, LocalDate.of(2026, 7, 20));

        SituacaoCobranca s = calcular(c, "130.00");

        assertThat(s.saldoDevedor()).isEqualByComparingTo("0.00");
        assertThat(s.quitada()).isTrue();
        assertThat(s.getValorPagoAMais()).isEqualByComparingTo("30.00");
    }

    @Test
    void valorParaFicarEmDiaSomaAtrasoComOQueVenceHoje() {
        // Parcelas de 100 em 13/07 e 13/08 (hoje). Nada pago.
        Cobranca c = fiado("200.00", "100.00", 13, LocalDate.of(2026, 7, 13));

        SituacaoCobranca s = calcular(c, "0");

        assertThat(s.valorEmAtraso()).isEqualByComparingTo("100.00");
        assertThat(s.valorVencendoHoje()).isEqualByComparingTo("100.00");
        assertThat(s.getValorParaFicarEmDia()).isEqualByComparingTo("200.00");
        assertThat(s.getValorSugerido()).isEqualByComparingTo("200.00");
    }

    // ------------------------------------------------------------------
    // Cobranca recorrente (mensalidade): nao tem total, nao quita sozinha.
    // ------------------------------------------------------------------

    @Test
    void recorrenteSoDeveOqueJaVenceu() {
        Cobranca c = new Cobranca();
        c.setId(2L);
        c.setCliente(cliente);
        c.setDescricao("Mensalidade");
        c.setValorTotal(null);
        c.setValorParcela(new BigDecimal("150.00"));
        c.setPeriodicidade(Periodicidade.MENSAL);
        c.setDiaDoMes(5);
        c.setDataInicio(LocalDate.of(2026, 5, 5));

        // Venceram 05/05, 05/06, 05/07 e 05/08 = 600. Pagou 300.
        SituacaoCobranca s = calcular(c, "300.00");

        assertThat(s.valorFechado()).isFalse();
        assertThat(s.quitada()).isFalse();
        assertThat(s.saldoDevedor()).isEqualByComparingTo("300.00");
        assertThat(s.valorEmAtraso()).isEqualByComparingTo("300.00");
        assertThat(s.valorAVencer()).isEqualByComparingTo("0.00");
        assertThat(s.getPercentualPago()).isEqualByComparingTo("0");
    }

    @Test
    void semanalTambemAcumulaSaldo() {
        Cobranca c = new Cobranca();
        c.setId(3L);
        c.setCliente(cliente);
        c.setDescricao("Fiado semanal");
        c.setValorTotal(new BigDecimal("120.00"));
        c.setValorParcela(new BigDecimal("40.00"));
        c.setPeriodicidade(Periodicidade.SEMANAL);
        c.setDiaDaSemana(DayOfWeek.FRIDAY);
        c.setDataInicio(LocalDate.of(2026, 7, 24));

        // Sextas: 24/07, 31/07, 07/08. Todas vencidas. Pagou 50.
        SituacaoCobranca s = calcular(c, "50.00");

        assertThat(s.parcelas()).hasSize(3);
        assertThat(s.parcelas()).extracting(ParcelaView::vencimento)
                .containsExactly(LocalDate.of(2026, 7, 24), LocalDate.of(2026, 7, 31),
                        LocalDate.of(2026, 8, 7));
        assertThat(s.saldoDevedor()).isEqualByComparingTo("70.00");
        assertThat(s.valorEmAtraso()).isEqualByComparingTo("70.00");
        assertThat(s.parcelas().get(0).situacao()).isEqualTo(SituacaoParcela.QUITADA);
        assertThat(s.parcelas().get(1).pago()).isEqualByComparingTo("10.00");
        // Atraso conta da segunda parcela, que e a mais antiga ainda aberta.
        assertThat(s.diasAtraso()).isEqualTo(13);
    }

    @Test
    void pagamentoUnicoQuandoNaoInformaParcela() {
        Cobranca c = fiado("250.00", "250.00", 30, LocalDate.of(2026, 8, 30));

        SituacaoCobranca s = calcular(c, "0");

        assertThat(s.parcelas()).hasSize(1);
        assertThat(s.parcelas().get(0).valor()).isEqualByComparingTo("250.00");
        assertThat(s.parcelas().get(0).situacao()).isEqualTo(SituacaoParcela.A_VENCER);
        assertThat(s.isEmAtraso()).isFalse();
    }

    @Test
    void parcelaQuebradaNoCentavoNaoPerdeDinheiro() {
        // 100 em parcelas de 30 -> 30 + 30 + 30 + 10
        Cobranca c = fiado("100.00", "30.00", 5, LocalDate.of(2026, 5, 5));

        SituacaoCobranca s = calcular(c, "0");

        BigDecimal somaDasParcelas = s.parcelas().stream()
                .map(ParcelaView::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(somaDasParcelas).isEqualByComparingTo("100.00");
    }
}

