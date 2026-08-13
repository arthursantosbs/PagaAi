package br.com.pagaai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Um vencimento previsto e quanto ele cobra. Calculado, nunca gravado. */
public record Parcela(LocalDate vencimento, BigDecimal valor) {
}
