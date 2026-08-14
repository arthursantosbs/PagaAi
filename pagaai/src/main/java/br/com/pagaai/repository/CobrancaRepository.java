package br.com.pagaai.repository;

import br.com.pagaai.domain.Cobranca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Consultas de divida.
 *
 * <p>Note que nao existe consulta de "divida quitada" nem de "divida atrasada":
 * <b>isso nao esta no banco</b>. Quitada e atrasada sao resultado de calculo
 * (ver {@code CalculadoraDeDivida}), nao coluna. O banco so sabe se a cobranca
 * esta {@code ativa} — uma chave manual, para o dono pausar uma cobranca sem
 * apagar o historico.
 */
public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    @Query("select c from Cobranca c join fetch c.cliente where c.ativa = true")
    List<Cobranca> findAtivasComCliente();

    /** Inclui as pausadas. A tela de dividas precisa mostrar tudo que existe. */
    @Query("select c from Cobranca c join fetch c.cliente")
    List<Cobranca> findTodasComCliente();

    /**
     * O cliente vem junto de proposito: o calculo da divida le o nome e o contato,
     * e sem o fetch isso estoura LazyInitializationException fora da transacao
     * (open-in-view esta desligado).
     */
    @Query("select c from Cobranca c join fetch c.cliente where c.id = :id")
    Optional<Cobranca> findByIdComCliente(Long id);

    List<Cobranca> findByClienteIdOrderByCriadoEmDesc(Long clienteId);
}
