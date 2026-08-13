package br.com.pagaai.repository;

import br.com.pagaai.domain.Pagamento;
import br.com.pagaai.dto.TotalPorCobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    List<Pagamento> findByCobrancaIdOrderByDataPagamentoDescIdDesc(Long cobrancaId);

    /**
     * Soma paga por cobranca, agregada no banco.
     * O painel inteiro precisa so disso — nao carrega pagamento por pagamento,
     * entao o custo nao cresce com o historico.
     */
    @Query("""
            select new br.com.pagaai.dto.TotalPorCobranca(p.cobranca.id, sum(p.valorPago))
            from Pagamento p
            group by p.cobranca.id
            """)
    List<TotalPorCobranca> totaisPagos();

    @Query("select coalesce(sum(p.valorPago), 0) from Pagamento p where p.cobranca.id = :cobrancaId")
    BigDecimal totalPagoDaCobranca(Long cobrancaId);

    @Query("""
            select new br.com.pagaai.dto.TotalPorCobranca(p.cobranca.id, sum(p.valorPago))
            from Pagamento p
            where p.cobranca.cliente.id = :clienteId
            group by p.cobranca.id
            """)
    List<TotalPorCobranca> totaisPagosDoCliente(Long clienteId);

    @Query("""
            select p from Pagamento p
            join fetch p.cobranca c
            join fetch c.cliente
            where c.cliente.id = :clienteId
            order by p.dataPagamento desc, p.id desc
            """)
    List<Pagamento> historicoDoCliente(Long clienteId);

    @Query("select coalesce(sum(p.valorPago), 0) from Pagamento p where p.dataPagamento between :de and :ate")
    BigDecimal totalRecebidoEntre(LocalDate de, LocalDate ate);
}
