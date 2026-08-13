package br.com.pagaai.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Uma divida do cliente. O que importa e o {@code valorTotal}: a cobranca existe
 * ate a soma dos pagamentos alcancar esse valor, e ai ela esta quitada.
 *
 * <p>O {@code valorParcela} e so o ritmo combinado de pagamento ("R$ 100 por
 * semana"). As parcelas nao viram registro no banco — sao calculadas por
 * {@code CalendarioCobranca} a partir de dataInicio + periodicidade, e param
 * assim que a soma delas cobre o valor total.
 *
 * <p>{@code valorTotal} nulo = divida sem fim definido (mensalidade, assinatura).
 * Nesse caso o devido cresce a cada vencimento e a cobranca nunca quita sozinha.
 */
@Entity
@Table(name = "cobranca")
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false, length = 150)
    private String descricao;

    /** Quanto o cliente deve no total. Nulo = cobranca recorrente sem fim. */
    @Column(name = "valor_total", precision = 12, scale = 2)
    private BigDecimal valorTotal;

    /** Quanto ele se comprometeu a pagar a cada vencimento. */
    @Column(name = "valor_parcela", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorParcela;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Periodicidade periodicidade;

    /** Dia do mes (1-31) quando MENSAL. Dias maiores que o mes caem no ultimo dia. */
    @Column(name = "dia_do_mes")
    private Integer diaDoMes;

    /** Dia da semana quando SEMANAL. */
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_da_semana", length = 12)
    private DayOfWeek diaDaSemana;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio = LocalDate.now();

    /** Opcional: para de gerar vencimentos nesta data. */
    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "cobranca", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pagamento> pagamentos = new ArrayList<>();

    /** Divida fechada (compra fiada) x recorrente (mensalidade). */
    @Transient
    public boolean isValorFechado() {
        return valorTotal != null;
    }

    /** Paga tudo de uma vez so. */
    @Transient
    public boolean isPagamentoUnico() {
        return valorTotal != null && valorParcela != null && valorParcela.compareTo(valorTotal) >= 0;
    }

    @Transient
    public String getResumoRecorrencia() {
        if (isPagamentoUnico()) {
            return "Pagamento unico";
        }
        if (periodicidade == Periodicidade.MENSAL) {
            return "Todo dia " + (diaDoMes == null ? "?" : diaDoMes);
        }
        return "Toda " + DiasSemana.nome(diaDaSemana);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public void setValorTotal(BigDecimal valorTotal) {
        this.valorTotal = valorTotal;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public void setValorParcela(BigDecimal valorParcela) {
        this.valorParcela = valorParcela;
    }

    public Periodicidade getPeriodicidade() {
        return periodicidade;
    }

    public void setPeriodicidade(Periodicidade periodicidade) {
        this.periodicidade = periodicidade;
    }

    public Integer getDiaDoMes() {
        return diaDoMes;
    }

    public void setDiaDoMes(Integer diaDoMes) {
        this.diaDoMes = diaDoMes;
    }

    public DayOfWeek getDiaDaSemana() {
        return diaDaSemana;
    }

    public void setDiaDaSemana(DayOfWeek diaDaSemana) {
        this.diaDaSemana = diaDaSemana;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDate dataFim) {
        this.dataFim = dataFim;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public List<Pagamento> getPagamentos() {
        return pagamentos;
    }

    public void setPagamentos(List<Pagamento> pagamentos) {
        this.pagamentos = pagamentos;
    }
}
