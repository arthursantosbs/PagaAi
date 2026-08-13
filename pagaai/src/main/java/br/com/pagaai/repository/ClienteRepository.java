package br.com.pagaai.repository;

import br.com.pagaai.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Consultas de cliente.
 *
 * <p>{@code JpaRepository} ja entrega de graca {@code save}, {@code findById},
 * {@code findAll}, {@code delete} e {@code count}. Aqui so acrescentamos o que e
 * especifico.
 *
 * <p>A busca usa {@code lower(...) like %termo%} nos tres campos que o dono lembra
 * de cabeca — nome, Instagram e telefone. E {@code coalesce} porque Instagram e
 * telefone podem ser nulos, e comparacao com NULL em SQL nunca da verdadeiro,
 * o que faria o cliente sumir do resultado.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    List<Cliente> findAllByOrderByNomeAsc();

    @Query("""
            select c from Cliente c
            where lower(c.nome) like lower(concat('%', :termo, '%'))
               or lower(coalesce(c.instagram, '')) like lower(concat('%', :termo, '%'))
               or coalesce(c.telefone, '') like concat('%', :termo, '%')
            order by c.nome asc
            """)
    List<Cliente> buscar(String termo);
}
