package br.com.pagaai.service;

import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.domain.Pagamento;
import br.com.pagaai.domain.Periodicidade;
import br.com.pagaai.domain.TipoCobranca;
import br.com.pagaai.dto.CobrancaForm;
import br.com.pagaai.repository.CobrancaRepository;
import br.com.pagaai.repository.PagamentoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CobrancaService {

    private final CobrancaRepository cobrancaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ClienteService clienteService;

    public CobrancaService(CobrancaRepository cobrancaRepository,
                           PagamentoRepository pagamentoRepository,
                           ClienteService clienteService) {
        this.cobrancaRepository = cobrancaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.clienteService = clienteService;
    }

    public Cobranca buscarPorId(Long id) {
        return cobrancaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobrança não encontrada"));
    }

    /** Usar sempre que o cliente da cobranca for lido fora da transacao. */
    public Cobranca buscarComCliente(Long id) {
        return cobrancaRepository.findByIdComCliente(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobrança não encontrada"));
    }

    public List<Cobranca> ativas() {
        return cobrancaRepository.findAtivasComCliente();
    }

    @Transactional
    public Cobranca salvar(CobrancaForm form) {
        form.normalizar();
        validar(form);

        Cobranca cobranca = form.getId() == null ? new Cobranca() : buscarPorId(form.getId());
        cobranca.setCliente(clienteService.buscarPorId(form.getClienteId()));
        cobranca.setDescricao(form.getDescricao().trim());
        cobranca.setValorTotal(form.getValorTotal());
        cobranca.setValorParcela(form.getValorParcela());
        cobranca.setPeriodicidade(form.getPeriodicidade());
        cobranca.setDiaDoMes(form.getDiaDoMes());
        cobranca.setDiaDaSemana(form.getDiaDaSemana());
        cobranca.setDataInicio(form.getDataInicio());
        cobranca.setDataFim(form.getDataFim());
        cobranca.setAtiva(form.isAtiva());
        return cobrancaRepository.save(cobranca);
    }

    private void validar(CobrancaForm form) {
        if (form.getTipo() == TipoCobranca.VALOR_FECHADO) {
            if (form.getValorTotal() == null || form.getValorTotal().signum() <= 0) {
                throw erro("Informe o valor total da dívida");
            }
        } else if (form.getValorParcela() == null) {
            throw erro("Informe o valor de cada cobrança");
        }

        if (form.getValorParcela() == null || form.getValorParcela().signum() <= 0) {
            throw erro("O valor de cada pagamento precisa ser maior que zero");
        }

        if (form.getPeriodicidade() == Periodicidade.MENSAL && form.getDiaDoMes() == null) {
            throw erro("Informe o dia do mês");
        }
        if (form.getPeriodicidade() == Periodicidade.SEMANAL && form.getDiaDaSemana() == null) {
            throw erro("Informe o dia da semana");
        }
        if (form.getDataFim() != null && form.getDataFim().isBefore(form.getDataInicio())) {
            throw erro("A data fim não pode ser antes do início");
        }

        // Bloqueia parcelamento absurdo (ex.: 10.000 em parcelas de 1 real = 10.000 vencimentos).
        if (form.getValorTotal() != null) {
            BigDecimal parcelas = form.getValorTotal().divide(form.getValorParcela(), 0,
                    java.math.RoundingMode.CEILING);
            if (parcelas.intValue() > 600) {
                throw erro("Esse valor de parcela geraria mais de 600 vencimentos. Aumente o valor da parcela.");
            }
        }
    }

    @Transactional
    public void excluir(Long id) {
        cobrancaRepository.delete(buscarPorId(id));
    }

    @Transactional
    public void alternarAtiva(Long id) {
        Cobranca cobranca = buscarPorId(id);
        cobranca.setAtiva(!cobranca.isAtiva());
    }

    /**
     * Registra dinheiro recebido. O valor abate o saldo da divida — nao precisa
     * casar com o valor da parcela, e por isso que pagamento parcial funciona.
     */
    @Transactional
    public Pagamento registrarPagamento(Long cobrancaId, BigDecimal valor, LocalDate dataPagamento,
                                        String observacao, String usuario) {
        Cobranca cobranca = buscarPorId(cobrancaId);
        if (valor == null || valor.signum() <= 0) {
            throw erro("Informe um valor maior que zero");
        }
        LocalDate data = dataPagamento == null ? LocalDate.now() : dataPagamento;
        if (data.isBefore(cobranca.getDataInicio())) {
            throw erro("O pagamento não pode ser anterior ao início da dívida");
        }
        if (data.isAfter(LocalDate.now())) {
            throw erro("O pagamento não pode ter data futura");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setCobranca(cobranca);
        pagamento.setValorPago(valor);
        pagamento.setDataPagamento(data);
        pagamento.setObservacao(observacao);
        pagamento.setRegistradoPor(usuario);
        return pagamentoRepository.save(pagamento);
    }

    /** Desfaz um pagamento lancado por engano. */
    @Transactional
    public Long estornarPagamento(Long pagamentoId) {
        Pagamento pagamento = pagamentoRepository.findById(pagamentoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pagamento não encontrado"));
        Long clienteId = pagamento.getCobranca().getCliente().getId();
        pagamentoRepository.delete(pagamento);
        return clienteId;
    }

    public List<Pagamento> pagamentosDa(Long cobrancaId) {
        return pagamentoRepository.findByCobrancaIdOrderByDataPagamentoDescIdDesc(cobrancaId);
    }

    public List<Pagamento> historicoDoCliente(Long clienteId) {
        return pagamentoRepository.historicoDoCliente(clienteId);
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }
}
