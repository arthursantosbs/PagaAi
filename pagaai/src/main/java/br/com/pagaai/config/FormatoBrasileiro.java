package br.com.pagaai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Locale;

/**
 * Faz os campos de dinheiro entenderem o jeito que o brasileiro digita.
 *
 * <p><b>Por que existe:</b> os campos eram {@code <input type="number">}, que so
 * aceita ponto como separador decimal. No teclado do celular brasileiro a pessoa
 * digita <b>365,00</b> — e o navegador simplesmente descartava o valor, deixando
 * o campo vazio. O dono via "falta o valor total" num formulario onde ele tinha
 * acabado de digitar o valor.
 *
 * <p>Agora os campos sao texto com teclado numerico, e este conversor aceita
 * todas as formas que aparecem na pratica:
 *
 * <pre>
 *   365        -> 365.00
 *   365,00     -> 365.00
 *   365.00     -> 365.00
 *   1.234,56   -> 1234.56
 *   R$ 1.234,56 -> 1234.56
 * </pre>
 *
 * <p><b>Se voce mexer aqui:</b> vale para todo campo BigDecimal dos formularios e
 * para os {@code @RequestParam} dos controllers. A API REST nao passa por aqui —
 * ela usa Jackson e continua falando JSON com ponto.
 */
@Configuration
public class FormatoBrasileiro implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new DinheiroDoBrasil());
    }

    static class DinheiroDoBrasil implements Formatter<BigDecimal> {

        @Override
        public BigDecimal parse(String texto, Locale locale) throws ParseException {
            if (texto == null) {
                return null;
            }
            String limpo = texto.trim()
                    .replace("R$", "")
                    .replace(" ", "")
                    .replace(" ", "");   // espaco fixo, que vem de copiar e colar
            if (limpo.isEmpty()) {
                return null;
            }

            // Com virgula, ela e o separador decimal e o ponto e milhar: 1.234,56
            // Sem virgula, o ponto ja e o separador decimal: 1234.56
            if (limpo.indexOf(',') >= 0) {
                limpo = limpo.replace(".", "").replace(',', '.');
            }

            try {
                return new BigDecimal(limpo);
            } catch (NumberFormatException e) {
                // A mensagem chega ao usuario quando o campo tem validacao.
                throw new ParseException("Digite só números, como 365 ou 365,00", 0);
            }
        }

        /** Devolve para a tela no formato que o brasileiro le. */
        @Override
        public String print(BigDecimal valor, Locale locale) {
            if (valor == null) {
                return "";
            }
            return valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
        }
    }
}
