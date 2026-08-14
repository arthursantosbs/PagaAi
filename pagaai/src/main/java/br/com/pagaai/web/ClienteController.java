package br.com.pagaai.web;

import br.com.pagaai.domain.Cliente;
import br.com.pagaai.dto.ClienteForm;
import br.com.pagaai.dto.SituacaoCobranca;
import br.com.pagaai.service.CarteiraService;
import br.com.pagaai.service.ClienteService;
import br.com.pagaai.service.CobrancaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Telas de cliente: lista com busca, cadastro/edicao e a ficha completa.
 *
 * <p>A ficha ({@code GET /clientes/{id}}) e a tela mais densa do sistema. Ela junta
 * o cadastro, as dividas em aberto (cada uma com barra de progresso e campo de
 * recebimento), as dividas quitadas e o historico de pagamentos.
 *
 * <p><b>Se voce mexer aqui:</b> os totais ({@code totalDevido},
 * {@code totalEmAtraso}, {@code totalPago}) sao somados a partir do que o
 * {@code CarteiraService} ja calculou — nunca refaca essa conta no controller nem
 * no template, senao existem duas versoes da verdade e elas divergem.
 */
@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;
    private final CobrancaService cobrancaService;
    private final CarteiraService carteira;

    public ClienteController(ClienteService clienteService, CobrancaService cobrancaService,
                             CarteiraService carteira) {
        this.clienteService = clienteService;
        this.cobrancaService = cobrancaService;
        this.carteira = carteira;
    }

    @ModelAttribute("pagina")
    public String pagina() {
        return "clientes";
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("clientes", clienteService.listar(q));
        model.addAttribute("q", q);
        return "clientes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("form", new ClienteForm());
        return "clientes/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", ClienteForm.de(clienteService.buscarPorId(id)));
        return "clientes/formulario";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("form") ClienteForm form, BindingResult erros,
                         RedirectAttributes flash) {
        if (erros.hasErrors()) {
            return "clientes/formulario";
        }
        Cliente cliente = clienteService.salvar(form);
        flash.addFlashAttribute("sucesso", "Cliente salvo.");
        return "redirect:/clientes/" + cliente.getId();
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Cliente cliente = clienteService.buscarPorId(id);
        List<SituacaoCobranca> dividas = carteira.dividasDoCliente(cliente);

        model.addAttribute("cliente", cliente);
        model.addAttribute("dividas", dividas);
        model.addAttribute("abertas", dividas.stream().filter(SituacaoCobranca::isDevedor).toList());
        model.addAttribute("quitadas", dividas.stream().filter(SituacaoCobranca::quitada).toList());
        // Terceiro grupo, obrigatorio: o que nao e devedor nem quitado — recorrente
        // antes do primeiro vencimento, ou cobranca pausada. Sem ele, a divida some
        // da tela e o usuario acha que o cadastro nao salvou.
        model.addAttribute("outras", dividas.stream()
                .filter(s -> !s.isDevedor() && !s.quitada())
                .toList());
        model.addAttribute("totalDevido", soma(dividas, SituacaoCobranca::saldoDevedor));
        model.addAttribute("totalEmAtraso", soma(dividas, SituacaoCobranca::valorEmAtraso));
        model.addAttribute("totalPago", soma(dividas, SituacaoCobranca::totalPago));
        model.addAttribute("pagamentos", cobrancaService.historicoDoCliente(id));
        model.addAttribute("hoje", LocalDate.now());
        return "clientes/detalhe";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, RedirectAttributes flash) {
        clienteService.excluir(id);
        flash.addFlashAttribute("sucesso", "Cliente excluído.");
        return "redirect:/clientes";
    }

    private BigDecimal soma(List<SituacaoCobranca> itens,
                            java.util.function.Function<SituacaoCobranca, BigDecimal> campo) {
        return itens.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
