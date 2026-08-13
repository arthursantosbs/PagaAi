package br.com.pagaai.api;

import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.domain.Periodicidade;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

public record CobrancaResponse(
        Long id,
        Long clienteId,
        String clienteNome,
        String descricao,
        boolean valorFechado,
        BigDecimal valorTotal,
        BigDecimal valorParcela,
        Periodicidade periodicidade,
        Integer diaDoMes,
        DayOfWeek diaDaSemana,
        LocalDate dataInicio,
        LocalDate dataFim,
        boolean ativa) {

    public static CobrancaResponse de(Cobranca c) {
        return new CobrancaResponse(c.getId(), c.getCliente().getId(), c.getCliente().getNome(),
                c.getDescricao(), c.isValorFechado(), c.getValorTotal(), c.getValorParcela(),
                c.getPeriodicidade(), c.getDiaDoMes(), c.getDiaDaSemana(),
                c.getDataInicio(), c.getDataFim(), c.isAtiva());
    }
}
