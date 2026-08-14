package br.com.pagaai.web;

import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.domain.DiasSemana;
import br.com.pagaai.domain.Periodicidade;
import br.com.pagaai.domain.TipoCobranca;
import br.com.pagaai.dto.CobrancaForm;
import br.com.pagaai.dto.PendenciaCliente;
import br.com.pagaai.dto.SituacaoCobranca;
import br.com.pagaai.service.CarteiraService;
import br.com.pagaai.service.ClienteService;
import br.com.pagaai.service.CobrancaService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Telas de divida: lista, cadastro, detalhe e — o mais importante — o registro
 * de recebimento.
 *
 * <p>{@code POST /cobrancas/{id}/pagamentos} e a acao que move dinheiro no
 * sistema. Ela aceita <b>qualquer valor</b>, nao so o valor da parcela: e assim
 * que pagamento parcial funciona. O campo ja vem preenchido com
 * {@code SituacaoCobranca#getValorSugerido()}, que e quanto falta para o cliente
 * ficar em dia, mas o dono pode trocar.
 *
 * <p>Os metodos anotados com {@code @ModelAttribute} no topo rodam antes de toda
 * acao e alimentam os {@code <select>} do formulario.
 *
 * <p><b>Se voce mexer aqui:</b> o parametro {@code voltarPara} decide para onde o
 * usuario volta depois de registrar o pagamento, porque o mesmo formulario existe
 * em tres telas. O metodo {@code destino()} so aceita caminho interno de proposito;
 * afrouxar isso cria um <i>open redirect</i>.
 */
@Controller
@RequestMapping("/cobrancas")
public class CobrancaController {

    private final CobrancaService cobrancaService;
    private final ClienteService clienteService;
    private final CarteiraService carteira;

    public CobrancaController(CobrancaService cobrancaService, ClienteService clienteService,
                              CarteiraService carteira) {
        this.cobrancaService = cobrancaService;
        this.clienteService = clienteService;
        this.carteira = carteira;
    }

    @ModelAttribute("pagina")
    public String pagina() {
        return "cobrancas";
    }

    @ModelAttribute("periodicidades")
    public Periodicidade[] periodicidades() {
        return Periodicidade.values();
    }

    @ModelAttribute("tipos")
    public TipoCobranca[] tipos() {
        return TipoCobranca.values();
    }

    @ModelAttribute("diasSemana")
    public Map<DayOfWeek, String> diasSemana() {
        return DiasSemana.todos();
    }

    @GetMapping
    public String lista(Model model) {
        List<PendenciaCliente> devedores = carteira.devedores();
        model.addAttribute("atrasadas", carteira.emAberto().stream().filter(s -> s.isEmAtraso()).toList());
        model.addAttribute("abertas", carteira.emAberto());
        // Cobrancas vivas que hoje nao tem saldo (recorrente antes do primeiro
        // vencimento). Sem este bloco elas nao apareciam em tela nenhuma.
        model.addAttribute("aguardando", carteira.situacoes().stream()
                .filter(SituacaoCobranca::isSemSaldoHoje)
                .toList());
        // Pausadas ficam de fora de todas as consultas acima. Listadas aqui para
        // que nenhuma cobranca cadastrada seja invisivel nesta tela.
        model.addAttribute("pausadas", carteira.todas().stream()
                .filter(s -> !s.ativa())
                .toList());
        model.addAttribute("devedores", devedores);
        model.addAttribute("agenda", carteira.agenda(30));
        model.addAttribute("hoje", LocalDate.now());
        return "cobrancas/lista";
    }

    @GetMapping("/nova")
    public String nova(@RequestParam(required = false) Long clienteId, Model model) {
        CobrancaForm form = new CobrancaForm();
        form.setClienteId(clienteId);
        form.setDiaDoMes(LocalDate.now().getDayOfMonth());
        model.addAttribute("form", form);
        model.addAttribute("clientes", clienteService.listar(null));
        return "cobrancas/formulario";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", CobrancaForm.de(cobrancaService.buscarPorId(id)));
        model.addAttribute("clientes", clienteService.listar(null));
        return "cobrancas/formulario";
    }

    @GetMapping("/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        Cobranca cobranca = cobrancaService.buscarComCliente(id);
        model.addAttribute("situacao", carteira.situacaoDa(cobranca));
        model.addAttribute("cobranca", cobranca);
        model.addAttribute("pagamentos", cobrancaService.pagamentosDa(id));
        model.addAttribute("hoje", LocalDate.now());
        return "cobrancas/detalhe";
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("form") CobrancaForm form, BindingResult erros,
                         Model model, RedirectAttributes flash) {
        if (erros.hasErrors()) {
            model.addAttribute("clientes", clienteService.listar(null));
            return "cobrancas/formulario";
        }
        try {
            Cobranca cobranca = cobrancaService.salvar(form);
            flash.addFlashAttribute("sucesso", "Dívida salva.");
            return "redirect:/cobrancas/" + cobranca.getId();
        } catch (ResponseStatusException e) {
            model.addAttribute("clientes", clienteService.listar(null));
            model.addAttribute("erro", e.getReason());
            return "cobrancas/formulario";
        }
    }

    /** Registra dinheiro recebido: valor livre, abate o saldo. */
    @PostMapping("/{id}/pagamentos")
    public String pagar(@PathVariable Long id,
                        @RequestParam BigDecimal valor,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                        LocalDate dataPagamento,
                        @RequestParam(required = false) String observacao,
                        @RequestParam(required = false) String voltarPara,
                        @AuthenticationPrincipal UserDetails usuario,
                        RedirectAttributes flash) {
        try {
            cobrancaService.registrarPagamento(id, valor, dataPagamento, observacao,
                    usuario == null ? null : usuario.getUsername());
            flash.addFlashAttribute("sucesso", "Pagamento registrado.");
        } catch (ResponseStatusException e) {
            flash.addFlashAttribute("erro", e.getReason());
        }
        return "redirect:" + destino(voltarPara, "/cobrancas/" + id);
    }

    @PostMapping("/pagamentos/{pagamentoId}/estornar")
    public String estornar(@PathVariable Long pagamentoId,
                           @RequestParam(required = false) String voltarPara,
                           RedirectAttributes flash) {
        Long clienteId = cobrancaService.estornarPagamento(pagamentoId);
        flash.addFlashAttribute("sucesso", "Pagamento estornado.");
        return "redirect:" + destino(voltarPara, "/clientes/" + clienteId);
    }

    @PostMapping("/{id}/alternar")
    public String alternar(@PathVariable Long id, @RequestParam(required = false) String voltarPara,
                           RedirectAttributes flash) {
        cobrancaService.alternarAtiva(id);
        flash.addFlashAttribute("sucesso", "Dívida atualizada.");
        return "redirect:" + destino(voltarPara, "/cobrancas/" + id);
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, @RequestParam(required = false) String voltarPara,
                          RedirectAttributes flash) {
        cobrancaService.excluir(id);
        flash.addFlashAttribute("sucesso", "Dívida excluída.");
        return "redirect:" + destino(voltarPara, "/cobrancas");
    }

    /** So aceita caminho interno, para nao virar redirect aberto. */
    private String destino(String voltarPara, String padrao) {
        if (voltarPara == null || voltarPara.isBlank()
                || !voltarPara.startsWith("/") || voltarPara.startsWith("//")) {
            return padrao;
        }
        return voltarPara;
    }
}
