/**
 * <h2>service — a regra de negocio</h2>
 *
 * O cerebro. Se uma conta esta errada, o erro esta aqui — nunca no template.
 *
 * <h3>As duas classes que voce precisa entender</h3>
 *
 * O calculo da divida foi quebrado em duas etapas de proposito, porque cada uma
 * responde uma pergunta diferente e pode ser testada sozinha:
 *
 * <pre>
 *   1. CalendarioCobranca   "quais parcelas existem?"     -> [(05/05, 150), (05/06, 150), ...]
 *   2. CalculadoraDeDivida  "onde o dinheiro entrou?"     -> parcela 1 quitada, parcela 2 com 80 de 150
 * </pre>
 *
 * <ul>
 *   <li>{@link br.com.pagaai.service.CalendarioCobranca} — gera as parcelas a partir de
 *       dataInicio + periodicidade + valorParcela, e para quando a soma cobre o valorTotal.
 *       Nao acessa banco.</li>
 *   <li>{@link br.com.pagaai.service.CalculadoraDeDivida} — recebe as parcelas e o total pago,
 *       aloca da parcela mais antiga para a mais nova, e devolve saldo, atraso e dias.
 *       Nao acessa banco, nao tem estado: e so aritmetica, e por isso e facil de testar.</li>
 * </ul>
 *
 * <h3>Os servicos que falam com o banco</h3>
 *
 * <ul>
 *   <li>{@link br.com.pagaai.service.CarteiraService} — junta as duas classes acima com os
 *       dados do banco e monta o retrato da carteira. <b>Toda tela de leitura passa por
 *       aqui.</b> Faz isso em 2 consultas fixas, nao importa o tamanho do historico.</li>
 *   <li>{@link br.com.pagaai.service.ClienteService} — CRUD de cliente.</li>
 *   <li>{@link br.com.pagaai.service.CobrancaService} — CRUD de divida e registro/estorno de
 *       pagamento. E o unico lugar que ESCREVE pagamento.</li>
 *   <li>{@link br.com.pagaai.service.UsuarioService} — login (implementa o
 *       {@code UserDetailsService} do Spring Security) e troca de senha.</li>
 * </ul>
 *
 * <h3>Regras da casa</h3>
 *
 * <ul>
 *   <li><b>{@code @Transactional(readOnly = true)} na classe, {@code @Transactional} no metodo
 *       que escreve.</b> Ler sem transacao de escrita evita lock desnecessario.</li>
 *   <li><b>Cuidado com lazy loading.</b> {@code open-in-view} esta desligado. Se um metodo
 *       devolve uma {@code Cobranca} e alguem le {@code getCliente().getNome()} depois,
 *       estoura {@code LazyInitializationException}. Use
 *       {@link br.com.pagaai.service.CobrancaService#buscarComCliente(java.lang.Long)}.</li>
 *   <li><b>Nunca consulte dentro de laco.</b> O padrao do projeto e: uma consulta traz a lista,
 *       outra traz os totais agregados, e o cruzamento e feito em memoria. Ver
 *       {@code CarteiraService#situacoes()}.</li>
 *   <li><b>Erro de usuario vira {@code ResponseStatusException}</b> com mensagem em portugues.
 *       O controller a exibe como aviso vermelho na tela.</li>
 * </ul>
 *
 * <h3>ONDE MEXER AQUI IMPACTA</h3>
 *
 * <ul>
 *   <li><b>CalculadoraDeDivida ou CalendarioCobranca</b> — muda TODO numero do sistema:
 *       painel, lista de dividas, tela do cliente, tela da divida e a API. Sempre rode
 *       {@code mvn test} (25 casos) antes de dar por encerrado.</li>
 *   <li><b>CarteiraService</b> — muda o painel e as listagens. Se voce adicionar consulta
 *       aqui, confira que ela nao entrou dentro de um laco.</li>
 *   <li><b>CobrancaService#registrarPagamento</b> — e o coracao financeiro. Toda validacao
 *       de valor e data mora ali.</li>
 * </ul>
 */
package br.com.pagaai.service;
