package br.com.pagaai.service;

import br.com.pagaai.domain.Cliente;
import br.com.pagaai.domain.Cobranca;
import br.com.pagaai.dto.*;
import br.com.pagaai.repository.ClienteRepository;
import br.com.pagaai.repository.CobrancaRepository;
import br.com.pagaai.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Monta a carteira inteira — todas as dividas com saldo — em duas consultas:
 * uma traz as cobrancas ativas com o cliente, outra traz a soma paga por cobranca
 * ja agregada no banco. O resto e aritmetica em memoria.
 *
 * <p>Painel, lista de cobrancas e API compartilham o mesmo retrato, entao abrir
 * qualquer uma dessas telas custa o mesmo: duas consultas, independente de quantos
 * pagamentos existam no historico.
 */
@Service
@Transactional(readOnly = true)
public class CarteiraService {

    private final CobrancaRepository cobrancaRepository;
    private final PagamentoRepository pagamentoRepository;
    private final ClienteRepository clienteRepository;
    private final CalculadoraDeDivida calculadora;

    public CarteiraService(CobrancaRepository cobrancaRepository,
                           PagamentoRepository pagamentoRepository,
                           ClienteRepository clienteRepository,
                           CalculadoraDeDivida calculadora) {
        this.cobrancaRepository = cobrancaRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.clienteRepository = clienteRepository;
        this.calculadora = calculadora;
    }

    /** Situacao de todas as cobrancas ativas, quitadas inclusive. */
    public List<SituacaoCobranca> situacoes() {
        LocalDate hoje = LocalDate.now();
        Map<Long, BigDecimal> pagos = pagosPorCobranca(pagamentoRepository.totaisPagos());

        return cobrancaRepository.findAtivasComCliente().stream()
                .map(c -> calculadora.calcular(c, c.getCliente(), pagos.get(c.getId()), hoje))
                .toList();
    }

    /**
     * Situacao de TODAS as cobrancas, pausadas inclusive.
     *
     * <p>Existe para a tela conseguir provar que nada sumiu: cobranca pausada
     * nao aparece em {@link #situacoes()} e, sem isto, ficaria invisivel — o que
     * o usuario le como "o cadastro nao salvou".
     */
    public List<SituacaoCobranca> todas() {
        LocalDate hoje = LocalDate.now();
        Map<Long, BigDecimal> pagos = pagosPorCobranca(pagamentoRepository.totaisPagos());

        return cobrancaRepository.findTodasComCliente().stream()
                .map(c -> calculadora.calcular(c, c.getCliente(), pagos.get(c.getId()), hoje))
                .toList();
    }

    /** So o que ainda tem saldo a receber. */
    public List<SituacaoCobranca> emAberto() {
        return situacoes().stream()
                .filter(SituacaoCobranca::isDevedor)
                .sorted(Comparator.comparing(SituacaoCobranca::diasAtraso).reversed()
                        .thenComparing(s -> s.proximoVencimento() == null ? LocalDate.MAX : s.proximoVencimento()))
                .toList();
    }

    /**
     * Um item por cliente devedor. Todos entram; quem esta em atraso vem primeiro,
     * do atraso maior para o menor.
     */
    public List<PendenciaCliente> devedores() {
        Map<Long, List<SituacaoCobranca>> porCliente = emAberto().stream()
                .collect(Collectors.groupingBy(SituacaoCobranca::clienteId, LinkedHashMap::new, Collectors.toList()));

        List<PendenciaCliente> resultado = new ArrayList<>(porCliente.size());
        for (List<SituacaoCobranca> dividas : porCliente.values()) {
            resultado.add(consolidar(dividas));
        }

        resultado.sort(Comparator
                // Atrasados primeiro, depois quem so tem parcela a vencer.
                .comparing(PendenciaCliente::isEmAtraso).reversed()
                .thenComparing(Comparator.comparingLong(PendenciaCliente::diasAtraso).reversed())
                .thenComparing(Comparator.comparing(PendenciaCliente::totalDevido).reversed())
                .thenComparing(PendenciaCliente::clienteNome));
        return resultado;
    }

    /** Devedores que ja passaram da data — os que precisam de cobranca hoje. */
    public List<PendenciaCliente> emAtraso() {
        return devedores().stream().filter(PendenciaCliente::isEmAtraso).toList();
    }

    /** Parcelas que ainda vao vencer nos proximos dias, em ordem de data. */
    public List<ItemAgenda> agenda(int dias) {
        LocalDate hoje = LocalDate.now();
        LocalDate ate = hoje.plusDays(dias);
        List<ItemAgenda> itens = new ArrayList<>();
        for (SituacaoCobranca situacao : situacoes()) {
            for (ParcelaView parcela : situacao.parcelas()) {
                if (parcela.saldo().signum() > 0
                        && !parcela.vencimento().isBefore(hoje)
                        && !parcela.vencimento().isAfter(ate)) {
                    itens.add(new ItemAgenda(situacao, parcela));
                }
            }
        }
        itens.sort(Comparator.comparing(i -> i.parcela().vencimento()));
        return itens;
    }

    private PendenciaCliente consolidar(List<SituacaoCobranca> dividas) {
        SituacaoCobranca primeira = dividas.get(0);

        BigDecimal devido = soma(dividas, SituacaoCobranca::saldoDevedor);
        BigDecimal atrasado = soma(dividas, SituacaoCobranca::valorEmAtraso);
        BigDecimal aVencer = soma(dividas, SituacaoCobranca::valorAVencer);
        BigDecimal jaPago = soma(dividas, SituacaoCobranca::totalPago);
        long diasAtraso = dividas.stream().mapToLong(SituacaoCobranca::diasAtraso).max().orElse(0);

        SituacaoCobranca proxima = dividas.stream()
                .filter(s -> s.proximoVencimento() != null)
                .min(Comparator.comparing(SituacaoCobranca::proximoVencimento))
                .orElse(null);

        return new PendenciaCliente(
                primeira.clienteId(),
                primeira.clienteNome(),
                primeira.clienteTelefone(),
                primeira.clienteInstagram(),
                devido,
                atrasado,
                aVencer,
                jaPago,
                diasAtraso,
                dividas.size(),
                proxima == null ? null : proxima.proximoVencimento(),
                proxima == null ? null : proxima.proximoValor(),
                dividas);
    }

    /** Todas as dividas de um cliente, quitadas inclusive, da mais recente para a mais antiga. */
    public List<SituacaoCobranca> dividasDoCliente(Cliente cliente) {
        LocalDate hoje = LocalDate.now();
        Map<Long, BigDecimal> pagos = pagosPorCobranca(pagamentoRepository.totaisPagosDoCliente(cliente.getId()));

        return cobrancaRepository.findByClienteIdOrderByCriadoEmDesc(cliente.getId()).stream()
                .map(c -> calculadora.calcular(c, cliente, pagos.get(c.getId()), hoje))
                .toList();
    }

    public SituacaoCobranca situacaoDa(Cobranca cobranca) {
        return calculadora.calcular(cobranca, cobranca.getCliente(),
                pagamentoRepository.totalPagoDaCobranca(cobranca.getId()), LocalDate.now());
    }

    public ResumoDashboard resumo() {
        List<SituacaoCobranca> situacoes = situacoes();
        List<SituacaoCobranca> abertas = situacoes.stream().filter(SituacaoCobranca::isDevedor).toList();

        Set<Long> devedores = new HashSet<>();
        Set<Long> atrasados = new HashSet<>();
        for (SituacaoCobranca s : abertas) {
            devedores.add(s.clienteId());
            if (s.isEmAtraso()) {
                atrasados.add(s.clienteId());
            }
        }

        YearMonth mes = YearMonth.now();
        BigDecimal recebido = pagamentoRepository.totalRecebidoEntre(mes.atDay(1), mes.atEndOfMonth());

        return new ResumoDashboard(
                soma(abertas, SituacaoCobranca::saldoDevedor),
                soma(abertas, SituacaoCobranca::valorEmAtraso),
                soma(abertas, SituacaoCobranca::valorAVencer),
                devedores.size(),
                atrasados.size(),
                abertas.size(),
                (int) situacoes.stream().filter(SituacaoCobranca::quitada).count(),
                recebido == null ? BigDecimal.ZERO : recebido,
                clienteRepository.count(),
                abertas.stream().mapToLong(SituacaoCobranca::diasAtraso).max().orElse(0));
    }

    private Map<Long, BigDecimal> pagosPorCobranca(List<TotalPorCobranca> totais) {
        Map<Long, BigDecimal> mapa = new HashMap<>();
        for (TotalPorCobranca t : totais) {
            mapa.put(t.cobrancaId(), t.total());
        }
        return mapa;
    }

    private BigDecimal soma(List<SituacaoCobranca> itens,
                            java.util.function.Function<SituacaoCobranca, BigDecimal> campo) {
        return itens.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
