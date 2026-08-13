package br.com.pagaai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de partida. Rodar o {@code main} daqui sobe a aplicacao inteira em
 * http://localhost:8080 — servidor web embutido, banco e telas.
 *
 * <p>As duas anotacoes fazem quase tudo:
 * <ul>
 *   <li>{@code @SpringBootApplication} — varre este pacote e os de baixo procurando
 *       {@code @Component}, {@code @Service}, {@code @Controller} e
 *       {@code @Repository}, e monta os objetos ligando um no outro. E por isso que
 *       nenhuma classe do projeto da {@code new} em service ou repository: quem
 *       constroi e o Spring, entregando pelo construtor.</li>
 *   <li>{@code @ConfigurationPropertiesScan} — encontra
 *       {@link br.com.pagaai.config.PagaAiProperties} e preenche com as chaves
 *       {@code pagaai.*} do application.yml.</li>
 * </ul>
 *
 * <p><b>Se voce mexer aqui:</b> mover esta classe para outro pacote faz o Spring
 * parar de enxergar metade do sistema, e a aplicacao sobe sem controller nenhum.
 * Ela precisa continuar no pacote raiz {@code br.com.pagaai}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PagaAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagaAiApplication.class, args);
    }
}
