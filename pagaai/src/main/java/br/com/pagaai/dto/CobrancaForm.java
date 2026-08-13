package br.com.pagaai.dto;

import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.domain.Periodicidade;
import br.com.pagaai.domain.TipoCobranca;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public class CobrancaForm {

    private Long id;

    @NotNull(message = "Selecione o cliente")
    private Long clienteId;

    @NotBlank(message = "Descreva a dívida")
    @Size(max = 150)
    private String descricao;

    @NotNull(message = "Escolha o tipo da cobrança")
    private TipoCobranca tipo = TipoCobranca.VALOR_FECHADO;

    /** Obrigatorio quando o tipo e VALOR_FECHADO. */
    @DecimalMin(value = "0.01", message = "O valor total precisa ser maior que zero")
    private BigDecimal valorTotal;

    /** Quanto ele paga por vez. Em branco numa divida fechada = paga tudo de uma vez. */
    @DecimalMin(value = "0.01", message = "O valor da parcela precisa ser maior que zero")
    private BigDecimal valorParcela;

    @NotNull(message = "Escolha mensal ou semanal")
    private Periodicidade periodicidade = Periodicidade.MENSAL;

    @Min(value = 1, message = "Dia do mês entre 1 e 31")
    @Max(value = 31, message = "Dia do mês entre 1 e 31")
    private Integer diaDoMes;

    private DayOfWeek diaDaSemana;

    @NotNull(message = "Informe a data do primeiro vencimento")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataInicio = LocalDate.now();

    /** So faz sentido em cobranca recorrente. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataFim;

    private boolean ativa = true;

    public static CobrancaForm de(Cobranca cobranca) {
        CobrancaForm form = new CobrancaForm();
        form.id = cobranca.getId();
        form.clienteId = cobranca.getCliente().getId();
        form.descricao = cobranca.getDescricao();
        form.tipo = cobranca.isValorFechado() ? TipoCobranca.VALOR_FECHADO : TipoCobranca.RECORRENTE;
        form.valorTotal = cobranca.getValorTotal();
        form.valorParcela = cobranca.getValorParcela();
        form.periodicidade = cobranca.getPeriodicidade();
        form.diaDoMes = cobranca.getDiaDoMes();
        form.diaDaSemana = cobranca.getDiaDaSemana();
        form.dataInicio = cobranca.getDataInicio();
        form.dataFim = cobranca.getDataFim();
        form.ativa = cobranca.isAtiva();
        return form;
    }

    /** Zera os campos que nao pertencem as escolhas feitas e preenche os obvios. */
    public void normalizar() {
        if (tipo == TipoCobranca.RECORRENTE) {
            valorTotal = null;
        } else {
            // Numa divida fechada quem encerra e o valor pago, nao a data.
            dataFim = null;
            if (valorParcela == null && valorTotal != null) {
                valorParcela = valorTotal;
            }
        }

        if (periodicidade == Periodicidade.MENSAL) {
            diaDaSemana = null;
            if (diaDoMes == null && dataInicio != null) {
                diaDoMes = dataInicio.getDayOfMonth();
            }
        } else {
            diaDoMes = null;
            if (diaDaSemana == null && dataInicio != null) {
                diaDaSemana = dataInicio.getDayOfWeek();
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public TipoCobranca getTipo() {
        return tipo;
    }

    public void setTipo(TipoCobranca tipo) {
        this.tipo = tipo;
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
}
