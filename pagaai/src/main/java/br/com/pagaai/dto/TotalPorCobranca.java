package br.com.pagaai.dto;

import java.math.BigDecimal;

/** Soma dos pagamentos de uma cobranca, vinda agregada do banco. */
public record TotalPorCobranca(Long cobrancaId, BigDecimal total) {
}
