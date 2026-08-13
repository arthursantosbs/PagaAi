/**
 * <h2>Paga ai — raiz da aplicacao</h2>
 *
 * Sistema de controle de fiado: quem comprou, quanto deve, quanto ja pagou e ha
 * quantos dias esta atrasado.
 *
 * <h3>A ideia central (entenda isto antes de mexer em qualquer coisa)</h3>
 *
 * O sistema NAO controla "parcela paga / parcela nao paga". Ele controla
 * <b>saldo</b>. Uma divida sabe o valor total; um pagamento e so um valor com
 * uma data. O resto e conta:
 *
 * <pre>
 *   saldo devedor    = valor total          - tudo que pagou
 *   vencido em aberto = parcelas ja vencidas - tudo que pagou   (nunca negativo)
 *   quitada           = pagou &gt;= valor total
 * </pre>
 *
 * E por isso que "devia 100 no dia 20 e pagou 80" funciona sem nenhum caso
 * especial no codigo: sobra 20 de saldo e 20 de atraso, contando desde o dia 20.
 *
 * <h3>Mapa dos pacotes</h3>
 *
 * <table border="1">
 *   <caption>O que vive em cada pasta</caption>
 *   <tr><th>Pacote</th><th>Responsabilidade</th></tr>
 *   <tr><td>{@link br.com.pagaai.domain}</td>
 *       <td>As entidades gravadas no banco e os enums do negocio.</td></tr>
 *   <tr><td>{@link br.com.pagaai.repository}</td>
 *       <td>Acesso ao banco (Spring Data JPA). Uma interface por entidade.</td></tr>
 *   <tr><td>{@link br.com.pagaai.service}</td>
 *       <td>Toda a regra de negocio. O calculo da divida mora aqui.</td></tr>
 *   <tr><td>{@link br.com.pagaai.dto}</td>
 *       <td>Objetos de leitura (resultado de calculo) e de escrita (formularios).</td></tr>
 *   <tr><td>{@link br.com.pagaai.web}</td>
 *       <td>Controllers das telas HTML (Thymeleaf).</td></tr>
 *   <tr><td>{@link br.com.pagaai.api}</td>
 *       <td>API REST /api/v1, para o app mobile futuro.</td></tr>
 *   <tr><td>{@link br.com.pagaai.config}</td>
 *       <td>Seguranca, propriedades e usuario inicial.</td></tr>
 * </table>
 *
 * <h3>Sentido do fluxo</h3>
 *
 * <pre>
 *   web/ ou api/  ->  service/  ->  repository/  ->  banco
 *        (tela)       (regra)       (consulta)
 * </pre>
 *
 * A seta nunca volta: {@code service} nao conhece {@code web}, e {@code domain}
 * nao conhece ninguem. Se voce se pegar importando um controller dentro de um
 * service, parou no lugar errado.
 *
 * <h3>Onde procurar quando...</h3>
 *
 * <ul>
 *   <li><b>a conta saiu errada</b> — {@code service/CalculadoraDeDivida.java};</li>
 *   <li><b>a data do vencimento saiu errada</b> — {@code service/CalendarioCobranca.java};</li>
 *   <li><b>o painel esta lento ou com numero errado</b> — {@code service/CarteiraService.java};</li>
 *   <li><b>a tela esta feia ou faltando dado</b> — {@code src/main/resources/templates/};</li>
 *   <li><b>nao consigo entrar / permissao</b> — {@code config/SecurityConfig.java};</li>
 *   <li><b>preciso de um campo novo no cliente</b> — comece por {@code domain/Cliente.java}
 *       e siga o roteiro em ARQUITETURA.md.</li>
 * </ul>
 *
 * @see <a href="../../../../ARQUITETURA.md">ARQUITETURA.md — mapa completo e matriz de impacto</a>
 */
package br.com.pagaai;
