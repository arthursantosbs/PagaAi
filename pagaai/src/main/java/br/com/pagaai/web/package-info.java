/**
 * <h2>web — as telas HTML</h2>
 *
 * Controllers do site. Cada metodo recebe uma URL, chama um service, coloca o
 * resultado no {@code Model} e devolve o NOME de um template.
 *
 * <pre>
 *   return "clientes/detalhe";
 *          ^ vira src/main/resources/templates/clientes/detalhe.html
 * </pre>
 *
 * <h3>As telas</h3>
 *
 * <ul>
 *   <li>{@link br.com.pagaai.web.DashboardController} — {@code /} o painel. Separa
 *       atrasados dos devedores em dia.</li>
 *   <li>{@link br.com.pagaai.web.ClienteController} — {@code /clientes} lista, cadastro
 *       e ficha do cliente.</li>
 *   <li>{@link br.com.pagaai.web.CobrancaController} — {@code /cobrancas} dividas,
 *       cadastro, detalhe e registro de pagamento. E o maior.</li>
 *   <li>{@link br.com.pagaai.web.UsuarioController} — {@code /usuarios} so ADMIN.</li>
 *   <li>{@link br.com.pagaai.web.LoginController} — so mostra a tela; quem valida a senha
 *       e o Spring Security.</li>
 * </ul>
 *
 * <h3>Padroes usados aqui</h3>
 *
 * <ul>
 *   <li><b>POST-Redirect-GET</b> — todo POST termina em {@code "redirect:..."}. Sem isso,
 *       apertar F5 depois de salvar registraria o pagamento de novo.</li>
 *   <li><b>{@code RedirectAttributes}</b> — a mensagem verde/vermelha sobrevive ao
 *       redirect. Chaves: {@code sucesso} e {@code erro}, lidas pelo fragmento
 *       {@code layout :: avisos}.</li>
 *   <li><b>{@code @ModelAttribute}</b> — metodos anotados assim rodam antes de TODA acao
 *       do controller e alimentam os {@code <select>} (lista de dias da semana, tipos...).</li>
 *   <li><b>{@code voltarPara}</b> — campo escondido que diz para onde voltar depois da acao,
 *       porque "Registrar recebimento" existe em tres telas. O metodo {@code destino()}
 *       so aceita caminho que comeca com "/" — sem isso viraria
 *       <i>open redirect</i>, um jeito de mandar o usuario para site de golpe.</li>
 *   <li><b>CSRF</b> — o Thymeleaf insere o token escondido automaticamente em
 *       {@code <form th:action>}. Formulario escrito com {@code action=} comum e
 *       rejeitado com 403.</li>
 * </ul>
 *
 * <h3>ONDE MEXER AQUI IMPACTA</h3>
 *
 * <ul>
 *   <li><b>Trocar o nome de um atributo do Model</b> — a tela para de achar o dado.
 *       O nome no {@code model.addAttribute("dividas", ...)} tem que bater com o
 *       {@code th:each="s : ${dividas}"} do HTML.</li>
 *   <li><b>Mudar uma URL</b> — quebra os links de TODOS os templates e os favoritos
 *       do usuario. Procure a URL antiga em {@code templates/} antes.</li>
 *   <li><b>Criar rota nova</b> — confira o {@code SecurityConfig}: tudo que nao esta
 *       liberado exige login (que e o desejado; so nao se esqueca das rotas publicas).</li>
 * </ul>
 */
package br.com.pagaai.web;
