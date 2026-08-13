/**
 * <h2>repository — acesso ao banco</h2>
 *
 * Interfaces do Spring Data JPA. Voce declara o metodo; a implementacao e gerada
 * em tempo de execucao. Nao existe classe {@code *RepositoryImpl} para procurar.
 *
 * <h3>As duas formas de declarar uma consulta</h3>
 *
 * <ol>
 *   <li><b>Pelo nome do metodo</b> — {@code findByClienteIdOrderByCriadoEmDesc} vira
 *       {@code WHERE cliente_id = ? ORDER BY criado_em DESC} sozinho. Pratico, mas o nome
 *       fica comprido e um erro de digitacao so aparece quando a aplicacao sobe.</li>
 *   <li><b>Com {@code @Query}</b> — JPQL escrito na mao. Usamos quando a consulta tem
 *       {@code join fetch}, agregacao ou projecao.</li>
 * </ol>
 *
 * <h3>Duas coisas nao obvias que sustentam o desempenho</h3>
 *
 * <ul>
 *   <li><b>{@code join fetch}</b> — em {@code findAtivasComCliente} e
 *       {@code findByIdComCliente}. Traz a cobranca e o cliente numa consulta so.
 *       Sem isso: {@code LazyInitializationException} ou uma consulta por linha
 *       (o problema N+1).</li>
 *   <li><b>{@code PagamentoRepository#totaisPagos}</b> — devolve a SOMA por cobranca,
 *       agregada pelo banco, dentro de um {@code TotalPorCobranca}. O calculo da divida
 *       so precisa do total, nunca da lista de pagamentos. Por isso o painel custa o
 *       mesmo com 10 ou com 100.000 pagamentos gravados.</li>
 * </ul>
 *
 * <h3>ONDE MEXER AQUI IMPACTA</h3>
 *
 * <ul>
 *   <li>Trocar {@code join fetch} por consulta simples reintroduz o
 *       {@code LazyInitializationException} — que so aparece em tempo de execucao,
 *       nao na compilacao.</li>
 *   <li>Trocar a agregacao {@code totaisPagos} por "carregar todos os pagamentos"
 *       funciona hoje e fica lento em um ano.</li>
 *   <li>Renomear um metodo derivado do nome exige que o novo nome continue valido —
 *       se nao for, a aplicacao <b>nao sobe</b>, com erro na criacao do bean.</li>
 *   <li>Uma consulta com {@code @Query} referencia nome de ENTIDADE e de CAMPO Java,
 *       nao nome de tabela e coluna. Renomear campo na {@code domain} quebra a query.</li>
 * </ul>
 */
package br.com.pagaai.repository;
