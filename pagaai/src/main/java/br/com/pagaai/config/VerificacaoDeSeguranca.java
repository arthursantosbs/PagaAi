package br.com.pagaai.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Barreira de partida: derruba a aplicacao antes de servir qualquer requisicao
 * quando a configuracao de acesso esta insegura.
 *
 * <p>Existe porque um placeholder nao resolvido (ex.: ADMIN_SENHA sem valor no
 * ambiente) nao explode sozinho — o valor fica literalmente {@code ${ADMIN_SENHA}}
 * e viraria a senha do administrador, que qualquer um que leia o repositorio adivinha.
 */
@Component
public class VerificacaoDeSeguranca {

    static final String SENHA_PADRAO = "12345678";

    private final PagaAiProperties properties;
    private final Environment environment;

    public VerificacaoDeSeguranca(PagaAiProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    @PostConstruct
    void verificar() {
        String senha = properties.getAdmin().getSenha();

        if (senha == null || senha.isBlank()) {
            throw new IllegalStateException(
                    "pagaai.admin.senha esta vazia. Defina a variavel de ambiente ADMIN_SENHA.");
        }

        if (senha.startsWith("${")) {
            throw new IllegalStateException(
                    "A variavel de ambiente " + nomeDaVariavel(senha) + " nao foi definida. "
                            + "A senha do administrador ficaria com o texto do placeholder. "
                            + "Defina essa variavel no ambiente antes de subir a aplicacao.");
        }

        if (ehProducao() && SENHA_PADRAO.equals(senha)) {
            throw new IllegalStateException(
                    "Em producao a senha padrao nao e aceita. Defina ADMIN_SENHA com uma senha propria.");
        }
    }

    private boolean ehProducao() {
        return Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }

    private String nomeDaVariavel(String placeholder) {
        return placeholder.replaceAll("^\\$\\{", "").replaceAll("[:}].*$", "");
    }
}
