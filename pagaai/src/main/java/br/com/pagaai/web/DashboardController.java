package br.com.pagaai.web;

import br.com.pagaai.dto.PendenciaCliente;
import br.com.pagaai.service.CarteiraService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;

/**
 * O painel ({@code /}), primeira tela depois do login.
 *
 * <p>A decisao de produto que este controller implementa: <b>todo cliente com
 * saldo e devedor</b>, mas quem passou da data e tratado a parte. Por isso a lista
 * de {@code CarteiraService#devedores()} e quebrada em dois atributos —
 * {@code atrasados} e {@code emDia} — que viram duas tabelas com peso visual
 * diferente.
 *
 * <p><b>Se voce mexer aqui:</b> os nomes {@code resumo}, {@code atrasados},
 * {@code emDia} e {@code agenda} sao lidos por {@code templates/dashboard.html}.
 * Renomear um deles deixa a tela vazia sem estourar erro.
 */
@Controller
public class DashboardController {

    private final CarteiraService carteira;

    public DashboardController(CarteiraService carteira) {
        this.carteira = carteira;
    }

    @GetMapping("/")
    public String painel(Model model) {
        List<PendenciaCliente> devedores = carteira.devedores();

        model.addAttribute("resumo", carteira.resumo());
        // Todos sao devedores; os atrasados ganham tabela propria em cima.
        model.addAttribute("atrasados", devedores.stream().filter(PendenciaCliente::isEmAtraso).toList());
        model.addAttribute("emDia", devedores.stream().filter(d -> !d.isEmAtraso()).toList());
        model.addAttribute("agenda", carteira.agenda(7));
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("pagina", "painel");
        return "dashboard";
    }
}
