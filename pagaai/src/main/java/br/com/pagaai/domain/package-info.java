/**
 * <h2>domain — o que existe no banco</h2>
 *
 * As entidades JPA (viram tabela) e os enums do negocio. Esta e a camada mais
 * profunda: ela nao importa nada de {@code service}, {@code web} ou {@code api}.
 *
 * <h3>As quatro tabelas</h3>
 *
 * <pre>
 *   Usuario     quem entra no sistema (voce e seu socio)
 *
 *   Cliente 1---N Cobranca 1---N Pagamento
 *   (quem deve)   (a divida)     (o dinheiro que entrou)
 * </pre>
 *
 * <ul>
 *   <li>{@link br.com.pagaai.domain.Cliente} — nome, contato, endereco.</li>
 *   <li>{@link br.com.pagaai.domain.Cobranca} — uma divida: valor total, valor da
 *       parcela e quando vence. <b>As parcelas nao sao tabela</b>, sao calculadas.</li>
 *   <li>{@link br.com.pagaai.domain.Pagamento} — valor + data. Nao aponta para
 *       parcela nenhuma, de proposito.</li>
 *   <li>{@link br.com.pagaai.domain.Usuario} — login e hash da senha (BCrypt).</li>
 * </ul>
 *
 * <h3>Enums</h3>
 *
 * <ul>
 *   <li>{@link br.com.pagaai.domain.TipoCobranca} — divida com valor fechado (fiado)
 *       x cobranca recorrente (mensalidade).</li>
 *   <li>{@link br.com.pagaai.domain.Periodicidade} — mensal ou semanal.</li>
 *   <li>{@link br.com.pagaai.domain.SituacaoParcela} — quitada, atrasada, vence hoje, a vencer.</li>
 *   <li>{@link br.com.pagaai.domain.Papel} — ADMIN ou SOCIO.</li>
 *   <li>{@link br.com.pagaai.domain.DiasSemana} — nomes dos dias em portugues (nao e enum,
 *       e uma tabela de traducao para {@code java.time.DayOfWeek}).</li>
 * </ul>
 *
 * <h3>ONDE MEXER AQUI IMPACTA</h3>
 *
 * Esta e a pasta mais perigosa do projeto, porque <b>toda alteracao mexe no banco</b>.
 *
 * <ol>
 *   <li><b>Adicionar campo</b> — o Hibernate cria a coluna sozinho no proximo start
 *       ({@code ddl-auto: update}). Precisa tambem: getter/setter, o campo no
 *       {@code dto/*Form}, o input no template, e a coluna na tela que mostra.</li>
 *   <li><b>Renomear ou remover campo</b> — <span style="color:red">CUIDADO</span>.
 *       O {@code ddl-auto: update} NAO apaga a coluna velha. Se ela for
 *       {@code nullable = false}, todo INSERT novo passa a falhar e a aplicacao
 *       quebra em producao. Ja aconteceu neste projeto. Ou voce apaga o banco
 *       (perde tudo) ou escreve o ALTER TABLE na mao. Isso e exatamente o que o
 *       Flyway resolve — item 1 do ROADMAP.md.</li>
 *   <li><b>Mexer em {@code valorTotal} / {@code valorParcela} da Cobranca</b> —
 *       muda o calculo inteiro. Rode os testes.</li>
 *   <li><b>Mexer nas colecoes {@code @OneToMany}</b> — o {@code cascade = ALL} +
 *       {@code orphanRemoval} e o que faz "excluir cliente" apagar as dividas e os
 *       pagamentos dele junto. Tirar isso quebra a exclusao com erro de chave
 *       estrangeira.</li>
 * </ol>
 *
 * <h3>Por que tanto getter e setter</h3>
 *
 * Nao e enfeite: o Hibernate e o Thymeleaf leem os objetos por esses metodos.
 * O projeto nao usa Lombok de proposito — mais linhas, porem nada de magica e
 * nenhum plugin de IDE necessario para o codigo compilar.
 */
package br.com.pagaai.domain;
