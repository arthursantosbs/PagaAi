/**
 * <h2>dto — os objetos que atravessam as camadas</h2>
 *
 * DTO = Data Transfer Object. Nada aqui vira tabela. Existem dois grupos com
 * finalidades opostas:
 *
 * <h3>1. Entrada — o que vem do formulario (classes com setter)</h3>
 *
 * <ul>
 *   <li>{@link br.com.pagaai.dto.ClienteForm} — cadastro do cliente.</li>
 *   <li>{@link br.com.pagaai.dto.CobrancaForm} — cadastro da divida. Tem
 *       {@code normalizar()}, que apaga o campo que nao pertence a escolha feita
 *       (divida fechada nao usa dataFim; mensal nao usa dia da semana).</li>
 * </ul>
 *
 * Sao classes, e nao records, porque o Thymeleaf precisa de setter para preencher
 * os campos ({@code th:field}). Existem para o formulario nao escrever direto na
 * entidade: assim ninguem consegue alterar {@code id} ou {@code criadoEm} mandando
 * um campo escondido no POST.
 *
 * <h3>2. Saida — o resultado do calculo (records, imutaveis)</h3>
 *
 * <ul>
 *   <li>{@link br.com.pagaai.dto.Parcela} — data + valor. A parcela "seca", antes de
 *       saber de pagamento.</li>
 *   <li>{@link br.com.pagaai.dto.ParcelaView} — a parcela depois de receber o dinheiro
 *       alocado: quanto entrou, quanto falta, situacao, dias de atraso.</li>
 *   <li>{@link br.com.pagaai.dto.SituacaoCobranca} — o retrato de uma divida hoje. E o
 *       objeto mais importante do sistema; quase toda tela le dele.</li>
 *   <li>{@link br.com.pagaai.dto.PendenciaCliente} — as dividas de um cliente somadas.</li>
 *   <li>{@link br.com.pagaai.dto.ResumoDashboard} — os numeros do topo do painel.</li>
 *   <li>{@link br.com.pagaai.dto.ItemAgenda} — uma parcela + a divida dela, para a agenda.</li>
 *   <li>{@link br.com.pagaai.dto.TotalPorCobranca} — projecao da soma vinda agregada do banco.</li>
 *   <li>{@link br.com.pagaai.dto.Severidade} — traduz dias de atraso em faixa de cor.</li>
 * </ul>
 *
 * <h3>Detalhe que confunde: accessor x getter</h3>
 *
 * Um {@code record} gera acessores SEM o prefixo "get". No template, os dois convivem:
 *
 * <pre>
 *   ${s.saldoDevedor()}   -&gt; componente do record (metodo saldoDevedor())
 *   ${s.severidade}       -&gt; metodo extra getSeveridade()
 *   ${s.emAtraso}         -&gt; metodo extra isEmAtraso()
 * </pre>
 *
 * Os metodos extras ({@code getSeveridade}, {@code getPercentualPago},
 * {@code isEmAtraso}...) nao sao enfeite: sao calculo de apresentacao que ficaria
 * ilegivel dentro do HTML. E eles tambem <b>viram campo JSON na API</b>, porque o
 * Jackson enxerga getter de record.
 *
 * <h3>ONDE MEXER AQUI IMPACTA</h3>
 *
 * <ul>
 *   <li><b>Adicionar componente em um record</b> — quebra a compilacao em todo lugar
 *       que constroi o objeto (o compilador aponta; e o tipo bom de quebra).</li>
 *   <li><b>Renomear componente</b> — o compilador NAO ve o uso dentro do
 *       {@code .html}. A tela quebra so quando alguem abre. Depois de renomear,
 *       procure o nome antigo em {@code src/main/resources/templates}.</li>
 *   <li><b>Qualquer mudanca aqui muda o JSON da API</b> e pode quebrar o app mobile.</li>
 *   <li><b>Adicionar campo em um Form</b> — precisa do input no template e do
 *       tratamento no service.</li>
 * </ul>
 */
package br.com.pagaai.dto;
