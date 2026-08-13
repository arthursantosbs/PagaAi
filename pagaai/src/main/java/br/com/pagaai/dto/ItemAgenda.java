package br.com.pagaai.dto;

/** Uma parcela na agenda, junto com a divida de onde ela veio. */
public record ItemAgenda(SituacaoCobranca cobranca, ParcelaView parcela) {
}
