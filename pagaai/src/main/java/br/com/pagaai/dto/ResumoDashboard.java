package br.com.pagaai.dto;

import java.math.BigDecimal;

/** Numeros do topo do painel. */
public record ResumoDashboard(
        BigDecimal totalAReceber,
        BigDecimal totalEmAtraso,
        BigDecimal totalAVencer,
        int clientesDevedores,
        int clientesEmAtraso,
        int dividasAbertas,
        int dividasQuitadas,
        BigDecimal recebidoNoMes,
        long totalClientes,
        long maiorAtrasoEmDias) {
}
