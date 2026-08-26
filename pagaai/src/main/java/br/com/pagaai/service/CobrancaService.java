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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CobrancaService {

    /** Teto de vencimentos por divida; alem disso o carne vira ruido e a tela trava. */
    private static final int MAX_PARCELAS = 600;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Essa dívida não existe mais — ela pode ter sido excluída por você ou "
                                + "pelo seu sócio. Volte em Dívidas e escolha na lista."));
    }

    /** Usar sempre que o cliente da cobranca for lido fora da transacao. */
    public Cobranca buscarComCliente(Long id) {
        return cobrancaRepository.findByIdComCliente(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Essa dívida não existe mais — ela pode ter sido excluída por você ou "
                                + "pelo seu sócio. Volte em Dívidas e escolha na lista."));
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
        // "ativa" NAO vem do formulario. Divida nova nasce ativa; editar uma
        // existente preserva o estado atual. Quem pausa e retoma e o botao da
        // tela de detalhe, que e explicito sobre o efeito.
        if (form.getId() == null) {
            cobranca.setAtiva(true);
        }
        return cobrancaRepository.save(cobranca);
    }

    /**
     * Regra das mensagens deste servico: toda recusa diz o que aconteceu E o que
     * fazer em seguida, com exemplo quando ajuda. Quem le nao e programador e
     * esta no balcao, muitas vezes com o cliente esperando.
     */
    private void validar(CobrancaForm form) {
        if (form.getTipo() == TipoCobranca.VALOR_FECHADO) {
            if (form.getValorTotal() == null || form.getValorTotal().signum() <= 0) {
                throw erro("Falta o valor total da dívida. Digite quanto o cliente deve no total — "
                        + "por exemplo 500. Se for uma mensalidade que não acaba, troque o tipo "
                        + "para \"Cobrança recorrente\".");
            }
        } else if (form.getValorParcela() == null) {
            throw erro("Falta o valor de cada cobrança. Numa cobrança recorrente é quanto você cobra "
                    + "a cada vencimento — por exemplo 150 por mês.");
        }

        if (form.getValorParcela() == null || form.getValorParcela().signum() <= 0) {
            throw erro("O valor de cada pagamento precisa ser maior que zero. "
                    + "Se o cliente vai pagar tudo de uma vez, deixe esse campo em branco.");
        }

        if (form.getPeriodicidade() == Periodicidade.MENSAL && form.getDiaDoMes() == null) {
            throw erro("Escolha o dia do mês em que vence, de 1 a 31. "
                    + "Se o mês não tiver esse dia, o vencimento cai no último dia do mês.");
        }
        if (form.getPeriodicidade() == Periodicidade.SEMANAL && form.getDiaDaSemana() == null) {
            throw erro("Escolha o dia da semana em que vence — por exemplo, toda sexta-feira.");
        }
        if (form.getDataFim() != null && form.getDataFim().isBefore(form.getDataInicio())) {
            throw erro("A data de término está antes do primeiro vencimento. "
                    + "Corrija uma das duas datas, ou deixe o término em branco "
                    + "para a cobrança não ter fim.");
        }

        // Trava contra parcelamento absurdo (10.000 em parcelas de 1 real = 10.000 vencimentos).
        // A mensagem calcula uma parcela que funciona, em vez de so reclamar.
        if (form.getValorTotal() != null) {
            BigDecimal parcelas = form.getValorTotal().divide(form.getValorParcela(), 0, RoundingMode.CEILING);
            if (parcelas.intValue() > MAX_PARCELAS) {
                BigDecimal sugerida = form.getValorTotal()
                        .divide(BigDecimal.valueOf(MAX_PARCELAS), 2, RoundingMode.UP);
                throw erro("Uma dívida de " + reais(form.getValorTotal()) + " em parcelas de "
                        + reais(form.getValorParcela()) + " daria " + parcelas + " vencimentos, "
                        + "o que o sistema não comporta. Aumente o valor da parcela para "
                        + reais(sugerida) + " ou mais.");
            }
        }
    }

    /** Formata em reais para caber no meio de uma frase. */
    private String reais(BigDecimal valor) {
        return "R$ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Data no formato que o usuario ve na tela, para a mensagem citar a data dele. */
    private String porExtenso(LocalDate data) {
        return data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
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
            throw erro("Digite quanto o cliente pagou. Para oitenta reais, digite 80 ou 80,00.");
        }

        LocalDate data = dataPagamento == null ? LocalDate.now() : dataPagamento;

        // NAO ha trava para pagamento anterior ao inicio da divida, de proposito.
        //
        // Ela existia e atrapalhava sem proteger nada: o comerciante recebe uma
        // entrada antes de lancar a venda, ou cadastra a divida dias depois de ela
        // ter comecado. A trava o obrigava a mentir a data para conseguir salvar,
        // o que e pior do que aceitar a data verdadeira.
        //
        // E seguro: a alocacao do dinheiro entre as parcelas usa apenas o VALOR
        // pago (ver CalculadoraDeDivida), nunca a data. A data serve so para o
        // historico e para o relatorio de quanto entrou no mes.

        if (data.isAfter(LocalDate.now())) {
            throw erro("A data " + porExtenso(data) + " ainda não chegou, então esse dinheiro "
                    + "ainda não entrou. Se o cliente já pagou, use a data de hoje ou o dia em "
                    + "que ele pagou. Se ele ainda vai pagar, registre quando o dinheiro cair.");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Esse pagamento já foi estornado — talvez pelo seu sócio, em outro "
                                + "aparelho. Atualize a página para ver a lista de agora."));
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
