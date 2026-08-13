/**
 * <h2>config — seguranca e partida da aplicacao</h2>
 *
 * Poucos arquivos, muito impacto: e aqui que se decide quem entra e o que a
 * aplicacao exige para subir.
 *
 * <ul>
 *   <li>{@link br.com.pagaai.config.SecurityConfig} — as duas cadeias de seguranca
 *       (API e site), o BCrypt e as regras de acesso.</li>
 *   <li>{@link br.com.pagaai.config.PagaAiProperties} — le as chaves {@code pagaai.*}
 *       do application.yml para um objeto Java.</li>
 *   <li>{@link br.com.pagaai.config.DataSeeder} — cria o usuario inicial no primeiro
 *       start e avisa no log enquanto a senha padrao continuar valendo.</li>
 *   <li>{@link br.com.pagaai.config.VerificacaoDeSeguranca} — derruba a aplicacao na
 *       partida se a configuracao de acesso estiver insegura.</li>
 * </ul>
 *
 * <h3>Por que a aplicacao se recusa a subir em alguns casos</h3>
 *
 * Falhar cedo e barulhento e melhor do que rodar inseguro em silencio. Sao dois casos:
 *
 * <ol>
 *   <li><b>{@code ADMIN_SENHA} nao definida em producao.</b> Descobrimos testando que o
 *       placeholder {@code ${ADMIN_SENHA}} nao resolvido NAO estoura sozinho: ele fica
 *       gravado como texto literal e vira a senha do administrador — uma senha que
 *       qualquer um que leia o repositorio adivinha.</li>
 *   <li><b>Senha padrao {@code 12345678} no perfil {@code prod}.</b> Ela existe so para
 *       desenvolvimento.</li>
 * </ol>
 *
 * <h3>As duas cadeias de seguranca (a ordem importa)</h3>
 *
 * <pre>
 *   @Order(1)  /api/**    Basic, stateless, sem CSRF, responde 401
 *   @Order(2)  todo resto formulario de login, sessao, com CSRF, redireciona
 * </pre>
 *
 * A da API vem primeiro porque a segunda casa com tudo. Inverter a ordem faz a API
 * passar a redirecionar para a tela de login.
 *
 * <h3>ONDE MEXER AQUI IMPACTA</h3>
 *
 * <ul>
 *   <li><b>Rota nova nao listada</b> — passa a exigir login. Geralmente e o que
 *       se quer; lembre-se das excecoes ({@code /login}, {@code /css/**},
 *       {@code /actuator/health}).</li>
 *   <li><b>Mexer no {@code PasswordEncoder}</b> — invalida TODAS as senhas gravadas.
 *       Ninguem mais entra, inclusive voce.</li>
 *   <li><b>Desligar CSRF no site</b> — abre a porta para um site de terceiros
 *       registrar pagamento no seu sistema usando a sessao aberta do seu navegador.</li>
 *   <li><b>{@code COOKIE_SECURE=true} sem HTTPS</b> — o navegador descarta o cookie de
 *       sessao e o login entra em loop, sem mensagem de erro. Ver DEPLOY.md.</li>
 * </ul>
 */
package br.com.pagaai.config;
