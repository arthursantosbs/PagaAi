package br.com.pagaai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Tudo que um cliente deve, somando as dividas dele.
 * Todo mundo com saldo e devedor; quem tem {@code totalEmAtraso} maior que zero
 * e o que precisa de cobranca hoje.
 */
public record PendenciaCliente(
        Long clienteId,
        String clienteNome,
        String telefone,
        String instagram,
        BigDecimal totalDevido,
        BigDecimal totalEmAtraso,
        BigDecimal totalAVencer,
        BigDecimal totalJaPago,
        long diasAtraso,
        int dividasAbertas,
        LocalDate proximoVencimento,
        BigDecimal proximoValor,
        List<SituacaoCobranca> cobrancas) {

    public boolean isEmAtraso() {
        return totalEmAtraso.signum() > 0;
    }

    /**
     * Link que abre a conversa deste cliente no WhatsApp, ou {@code null} quando
     * nao ha telefone utilizavel.
     *
     * <p><b>Por que a limpeza do numero vive aqui, e nao no template:</b> a
     * primeira versao montava o link direto no HTML, com
     * {@code #strings.replaceAll(telefone, '\D', '')}. A linguagem de template
     * nao trata a expressao regular como o Java trata, a avaliacao estourava, e
     * o Painel inteiro caia na tela de erro. Regex e trabalho de Java.
     *
     * <p>O wa.me exige so digitos, com o codigo do pais na frente. Numero com
     * ate 11 digitos e tratado como brasileiro sem DDI, entao ganha o 55.
     */
    public String getWhatsapp() {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() < 10) {
            return null;   // nao da para montar link com numero incompleto
        }
        if (digitos.length() <= 11) {
            digitos = "55" + digitos;
        }
        return "https://wa.me/" + digitos;
    }

    public String getSeveridade() {
        return Severidade.porDiasDeAtraso(diasAtraso);
    }
}
