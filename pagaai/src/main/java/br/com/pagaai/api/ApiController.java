package br.com.pagaai.api;

import br.com.pagaai.dto.*;
import br.com.pagaai.service.CarteiraService;
import br.com.pagaai.service.ClienteService;
import br.com.pagaai.service.CobrancaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API REST consumida pelo futuro app mobile.
 * Autenticacao HTTP Basic (ver SecurityConfig#apiFilterChain).
 */
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    private final ClienteService clienteService;
    private final CobrancaService cobrancaService;
    private final CarteiraService carteira;

    public ApiController(ClienteService clienteService, CobrancaService cobrancaService,
                         CarteiraService carteira) {
        this.clienteService = clienteService;
        this.cobrancaService = cobrancaService;
        this.carteira = carteira;
    }

    @GetMapping("/me")
    public Map<String, Object> eu(@AuthenticationPrincipal UserDetails usuario) {
        return Map.of(
                "login", usuario.getUsername(),
                "papeis", usuario.getAuthorities().stream().map(Object::toString).toList());
    }

    @GetMapping("/resumo")
    public ResumoDashboard resumo() {
        return carteira.resumo();
    }

    /** Todos os devedores, atrasados primeiro. */
    @GetMapping("/devedores")
    public List<PendenciaCliente> devedores() {
        return carteira.devedores();
    }

    /** So quem passou da data. */
    @GetMapping("/devedores/atrasados")
    public List<PendenciaCliente> atrasados() {
        return carteira.emAtraso();
    }

    /** Dividas com saldo, com o detalhe das parcelas. */
    @GetMapping("/dividas")
    public List<SituacaoCobranca> dividas() {
        return carteira.emAberto();
    }

    @GetMapping("/agenda")
    public List<ItemAgenda> agenda(@RequestParam(defaultValue = "7") int dias) {
        return carteira.agenda(dias);
    }

    @GetMapping("/clientes")
    public List<ClienteResponse> clientes(@RequestParam(required = false) String q) {
        return clienteService.listar(q).stream().map(ClienteResponse::de).toList();
    }

    @GetMapping("/clientes/{id}")
    public ClienteResponse cliente(@PathVariable Long id) {
        return ClienteResponse.de(clienteService.buscarPorId(id));
    }

    /** Situacao de todas as dividas do cliente, quitadas inclusive. */
    @GetMapping("/clientes/{id}/dividas")
    public List<SituacaoCobranca> dividasDoCliente(@PathVariable Long id) {
        return carteira.dividasDoCliente(clienteService.buscarPorId(id));
    }

    @PostMapping("/clientes")
    public ResponseEntity<ClienteResponse> criarCliente(@Valid @RequestBody ClienteForm form) {
        form.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClienteResponse.de(clienteService.salvar(form)));
    }

    @PutMapping("/clientes/{id}")
    public ClienteResponse atualizarCliente(@PathVariable Long id, @Valid @RequestBody ClienteForm form) {
        form.setId(id);
        return ClienteResponse.de(clienteService.salvar(form));
    }

    @DeleteMapping("/clientes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCliente(@PathVariable Long id) {
        clienteService.excluir(id);
    }

    @GetMapping("/cobrancas")
    public List<CobrancaResponse> cobrancas() {
        return cobrancaService.ativas().stream().map(CobrancaResponse::de).toList();
    }

    @GetMapping("/cobrancas/{id}")
    public SituacaoCobranca cobranca(@PathVariable Long id) {
        return carteira.situacaoDa(cobrancaService.buscarComCliente(id));
    }

    @PostMapping("/cobrancas")
    public ResponseEntity<CobrancaResponse> criarCobranca(@Valid @RequestBody CobrancaForm form) {
        form.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CobrancaResponse.de(cobrancaService.salvar(form)));
    }

    @PutMapping("/cobrancas/{id}")
    public CobrancaResponse atualizarCobranca(@PathVariable Long id, @Valid @RequestBody CobrancaForm form) {
        form.setId(id);
        return CobrancaResponse.de(cobrancaService.salvar(form));
    }

    @DeleteMapping("/cobrancas/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirCobranca(@PathVariable Long id) {
        cobrancaService.excluir(id);
    }

    /** Registra um recebimento. Devolve a situacao atualizada da divida. */
    @PostMapping("/cobrancas/{id}/pagamentos")
    @ResponseStatus(HttpStatus.CREATED)
    public SituacaoCobranca pagar(@PathVariable Long id,
                                  @Valid @RequestBody PagamentoRequest requisicao,
                                  @AuthenticationPrincipal UserDetails usuario) {
        cobrancaService.registrarPagamento(id, requisicao.valor(), requisicao.dataPagamento(),
                requisicao.observacao(), usuario == null ? null : usuario.getUsername());
        return carteira.situacaoDa(cobrancaService.buscarComCliente(id));
    }

    @DeleteMapping("/pagamentos/{pagamentoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void estornar(@PathVariable Long pagamentoId) {
        cobrancaService.estornarPagamento(pagamentoId);
    }
}
