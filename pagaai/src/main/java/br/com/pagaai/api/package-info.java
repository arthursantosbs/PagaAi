/**
 * <h2>api — a API REST para o app mobile</h2>
 *
 * Tudo sob {@code /api/v1}. Devolve JSON, nunca HTML.
 *
 * <h3>Por que existe uma cadeia de seguranca separada</h3>
 *
 * O site e a API precisam falhar de formas diferentes. Quando o site recebe uma
 * requisicao sem login, o certo e redirecionar para a tela de login. Quando a API
 * recebe, o certo e responder {@code 401}.
 *
 * Isso ja deu problema real neste projeto: com senha errada a API devolvia
 * {@code 302} para {@code /login}, e um app mobile seguiria o redirecionamento,
 * receberia HTML com status {@code 200} e concluiria que o login deu certo.
 * A correcao esta em {@code config/SecurityConfig#apiFilterChain}, que define o
 * {@code authenticationEntryPoint} em DOIS lugares: no {@code exceptionHandling}
 * (requisicao sem credencial) e no {@code httpBasic} (credencial errada).
 *
 * <h3>O que tem aqui</h3>
 *
 * <ul>
 *   <li>{@link br.com.pagaai.api.ApiController} — todos os endpoints.</li>
 *   <li>{@link br.com.pagaai.api.ClienteResponse} e {@link br.com.pagaai.api.CobrancaResponse} —
 *       convertem a entidade JPA em JSON. Existem para a entidade nao ser serializada
 *       crua: isso vazaria estrutura interna e ainda estouraria erro de lazy loading
 *       no meio da resposta.</li>
 *   <li>{@link br.com.pagaai.api.PagamentoRequest} — corpo do POST de recebimento.</li>
 * </ul>
 *
 * Os DTOs de leitura ({@code SituacaoCobranca}, {@code PendenciaCliente}...) sao
 * devolvidos direto do pacote {@code dto}, sem tradutor no meio.
 *
 * <h3>Autenticacao</h3>
 *
 * HTTP Basic, stateless. Serve para comecar; JWT esta no bloco 4 do ROADMAP.md.
 * Basic manda a senha em base64 a cada chamada, entao <b>so use com HTTPS</b>.
 *
 * <pre>
 *   curl -u HERA123:senha http://localhost:8080/api/v1/devedores
 * </pre>
 *
 * <h3>ONDE MEXER AQUI IMPACTA</h3>
 *
 * <ul>
 *   <li><b>Todo campo que voce adiciona ou renomeia em um DTO muda o JSON</b> e pode
 *       quebrar o app mobile, que voce nao recompila junto. Adicionar campo e seguro;
 *       renomear e remover nao sao.</li>
 *   <li><b>O prefixo {@code /v1} existe para isso</b>: quando precisar de uma mudanca
 *       que quebra, crie {@code /api/v2} e mantenha o v1 no ar ate o app atualizar.</li>
 *   <li><b>Metodo publico novo em um record de DTO vira campo JSON automaticamente</b>
 *       (o Jackson enxerga getters). Nao e bug, mas surpreende.</li>
 * </ul>
 */
package br.com.pagaai.api;
