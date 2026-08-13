package br.com.pagaai.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Dinheiro que entrou. Um pagamento nao pertence a uma parcela especifica:
 * ele abate o saldo da divida, e a alocacao entre as parcelas e calculada
 * da mais antiga para a mais nova (ver {@code CalculadoraDeDivida}).
 *
 * <p>E isso que permite o caso real: devia 100 no dia 20, pagou 80 —
 * ficam 20 em atraso, sem precisar inventar uma segunda parcela.
 */
@Entity
@Table(name = "pagamento", indexes = @Index(name = "idx_pagamento_cobranca", columnList = "cobranca_id"))
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cobranca_id", nullable = false)
    private Cobranca cobranca;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento = LocalDate.now();

    @Column(name = "valor_pago", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorPago;

    @Column(length = 255)
    private String observacao;

    @Column(name = "registrado_por", length = 60)
    private String registradoPor;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cobranca getCobranca() {
        return cobranca;
    }

    public void setCobranca(Cobranca cobranca) {
        this.cobranca = cobranca;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public BigDecimal getValorPago() {
        return valorPago;
    }

    public void setValorPago(BigDecimal valorPago) {
        this.valorPago = valorPago;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public void setRegistradoPor(String registradoPor) {
        this.registradoPor = registradoPor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
