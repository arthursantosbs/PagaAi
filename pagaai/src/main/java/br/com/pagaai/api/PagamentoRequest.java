package br.com.pagaai.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Dinheiro recebido. O valor e livre: pode ser parcial ou adiantado. */
public record PagamentoRequest(
        @NotNull(message = "Informe o valor recebido")
        @DecimalMin(value = "0.01", message = "O valor precisa ser maior que zero")
        BigDecimal valor,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dataPagamento,

        String observacao) {
}
