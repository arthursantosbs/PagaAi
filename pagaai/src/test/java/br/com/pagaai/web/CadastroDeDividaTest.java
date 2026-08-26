package br.com.pagaai.web;

import br.com.pagaai.dto.ClienteForm;
import br.com.pagaai.dto.SituacaoCobranca;
import br.com.pagaai.service.CarteiraService;
import br.com.pagaai.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicacao inteira e cadastra uma divida pelo formulario, do jeito que o
 * navegador envia.
 *
 * <p>Existe por causa de um bug real: o formulario tinha uma caixa "Cobranca
 * ativa" marcada por padrao, dentro do {@code <label>}. Clicar no texto
 * desmarcava. Uma divida salva assim sumia do Painel e da lista — mas continuava
 * abrindo pela URL. O usuario via "Divida salva", nao encontrava mais nada, e
 * concluia que o sistema nao tinha salvado.
 *
 * <p>Os testes de calculo nao pegavam isso: a aritmetica estava certa. O buraco
 * era entre o formulario e a tela.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        // Banco em memoria: nao encosta no ./data de desenvolvimento.
        "spring.datasource.url=jdbc:h2:mem:teste-cadastro;DB_CLOSE_DELAY=-1"
})
// Cada teste roda numa transacao que e desfeita no fim. Sem isto, o que um teste
// cadastra aparece no seguinte e as contagens quebram — o Spring reaproveita o
// mesmo contexto e o mesmo banco entre os metodos.
@Transactional
class CadastroDeDividaTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private CarteiraService carteira;

    private Long cadastrarCliente(String nome) {
        ClienteForm form = new ClienteForm();
        form.setNome(nome);
        form.setAtivo(true);
        return clienteService.salvar(form).getId();
    }

    @Test
    @WithMockUser(username = "HERA123", roles = "ADMIN")
    void dividaCadastradaPeloFormularioApareceNoPainel() throws Exception {
        Long clienteId = cadastrarCliente("Zezao");

        mvc.perform(post("/cobrancas").with(csrf())
                        .param("clienteId", clienteId.toString())
                        .param("descricao", "VAPE")
                        .param("tipo", "VALOR_FECHADO")
                        .param("valorTotal", "365")
                        .param("periodicidade", "MENSAL")
                        .param("diaDoMes", "22")
                        .param("dataInicio", LocalDate.now().toString()))
                // Repare no que NAO e enviado: nenhum parametro "ativa".
                // E exatamente assim que o navegador envia quando a caixa esta
                // desmarcada — e era o caso que fazia a divida nascer invisivel.
                .andExpect(status().is3xxRedirection());

        assertThat(carteira.emAberto())
                .as("a divida recem-cadastrada precisa aparecer nas listas")
                .hasSize(1);

        assertThat(carteira.emAberto().get(0).ativa())
                .as("divida nova nasce ativa, sem depender de caixa marcada")
                .isTrue();

        assertThat(carteira.resumo().totalAReceber()).isEqualByComparingTo("365.00");
        assertThat(carteira.devedores()).hasSize(1);
    }

    @Test
    @WithMockUser(username = "HERA123", roles = "ADMIN")
    void aceitaPagamentoComDataAnteriorAoInicioDaDivida() throws Exception {
        // Caso real do balcao: o cliente deu uma entrada antes de a venda ser
        // lancada no sistema, ou a divida foi cadastrada dias depois de comecar.
        // O sistema ja recusou isso, e a recusa obrigava o dono a mentir a data
        // para conseguir salvar — pior do que aceitar a data verdadeira.
        Long clienteId = cadastrarCliente("Cliente da entrada");
        LocalDate inicio = LocalDate.now().plusDays(10);

        mvc.perform(post("/cobrancas").with(csrf())
                        .param("clienteId", clienteId.toString())
                        .param("descricao", "Compra com entrada")
                        .param("tipo", "VALOR_FECHADO")
                        .param("valorTotal", "300")
                        .param("periodicidade", "MENSAL")
                        .param("diaDoMes", String.valueOf(inicio.getDayOfMonth()))
                        .param("dataInicio", inicio.toString()))
                .andExpect(status().is3xxRedirection());

        Long cobrancaId = carteira.emAberto().stream()
                .filter(s -> s.descricao().equals("Compra com entrada"))
                .findFirst().orElseThrow().cobrancaId();

        // Pagamento datado ANTES do inicio da divida.
        mvc.perform(post("/cobrancas/" + cobrancaId + "/pagamentos").with(csrf())
                        .param("valor", "100")
                        .param("dataPagamento", LocalDate.now().minusDays(5).toString()))
                .andExpect(status().is3xxRedirection());

        SituacaoCobranca depois = carteira.emAberto().stream()
                .filter(s -> s.cobrancaId().equals(cobrancaId))
                .findFirst().orElseThrow();

        assertThat(depois.totalPago())
                .as("a entrada precisa entrar na conta, mesmo datada antes do inicio")
                .isEqualByComparingTo("100.00");
        assertThat(depois.saldoDevedor()).isEqualByComparingTo("200.00");
    }

    @Test
    @WithMockUser(username = "HERA123", roles = "ADMIN")
    void recusaPagamentoComDataFuturaExplicandoOMotivo() throws Exception {
        // Esta trava fica: dinheiro que ainda nao entrou nao e recebimento.
        // Mas a mensagem precisa dizer o que fazer.
        Long clienteId = cadastrarCliente("Cliente do futuro");

        mvc.perform(post("/cobrancas").with(csrf())
                        .param("clienteId", clienteId.toString())
                        .param("descricao", "Compra qualquer")
                        .param("tipo", "VALOR_FECHADO")
                        .param("valorTotal", "100")
                        .param("periodicidade", "MENSAL")
                        .param("diaDoMes", "10")
                        .param("dataInicio", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection());

        Long cobrancaId = carteira.emAberto().stream()
                .filter(s -> s.descricao().equals("Compra qualquer"))
                .findFirst().orElseThrow().cobrancaId();

        mvc.perform(post("/cobrancas/" + cobrancaId + "/pagamentos").with(csrf())
                        .param("valor", "50")
                        .param("dataPagamento", LocalDate.now().plusMonths(1).toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erro",
                        org.hamcrest.Matchers.containsString("ainda não chegou")));

        assertThat(carteira.emAberto().stream()
                .filter(s -> s.cobrancaId().equals(cobrancaId))
                .findFirst().orElseThrow().totalPago())
                .as("nada deve ter sido gravado")
                .isEqualByComparingTo("0.00");
    }

    @Test
    @WithMockUser(username = "HERA123", roles = "ADMIN")
    void pausarTiraDasListasMasNaoApagaADivida() throws Exception {
        Long clienteId = cadastrarCliente("Maria");

        mvc.perform(post("/cobrancas").with(csrf())
                        .param("clienteId", clienteId.toString())
                        .param("descricao", "Fiado da Maria")
                        .param("tipo", "VALOR_FECHADO")
                        .param("valorTotal", "200")
                        .param("periodicidade", "MENSAL")
                        .param("diaDoMes", "10")
                        .param("dataInicio", LocalDate.now().toString()))
                .andExpect(status().is3xxRedirection());

        Long cobrancaId = carteira.emAberto().stream()
                .filter(s -> s.descricao().equals("Fiado da Maria"))
                .findFirst()
                .orElseThrow()
                .cobrancaId();

        mvc.perform(post("/cobrancas/" + cobrancaId + "/alternar").with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(carteira.emAberto())
                .as("pausada sai das listas normais")
                .noneMatch(s -> s.cobrancaId().equals(cobrancaId));

        assertThat(carteira.todas())
                .as("mas continua existindo e localizavel — nada desaparece")
                .anyMatch(s -> s.cobrancaId().equals(cobrancaId) && !s.ativa());
    }
}
