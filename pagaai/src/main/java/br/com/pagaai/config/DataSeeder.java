package br.com.pagaai.config;

import br.com.pagaai.domain.Papel;
import br.com.pagaai.domain.Usuario;
import br.com.pagaai.repository.UsuarioRepository;
import br.com.pagaai.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** Cria o usuario padrao no primeiro start e avisa enquanto a senha padrao continuar valendo. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final PagaAiProperties properties;

    public DataSeeder(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
                      PasswordEncoder passwordEncoder, PagaAiProperties properties) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        String login = properties.getAdmin().getLogin();
        Optional<Usuario> existente = usuarioRepository.findByLoginIgnoreCase(login);

        if (existente.isEmpty()) {
            usuarioService.criar(login, properties.getAdmin().getNome(),
                    properties.getAdmin().getSenha(), Papel.ADMIN);
            log.info("Usuario '{}' criado.", login);
        }

        alertarSenhaPadrao(login);
    }

    /** Roda a cada start: enquanto a senha for a padrao, o log grita. */
    private void alertarSenhaPadrao(String login) {
        usuarioRepository.findByLoginIgnoreCase(login).ifPresent(usuario -> {
            if (passwordEncoder.matches(VerificacaoDeSeguranca.SENHA_PADRAO, usuario.getSenhaHash())) {
                log.warn("=================================================================");
                log.warn(" ATENCAO: o usuario '{}' ainda usa a senha padrao.", login);
                log.warn(" Troque agora em /usuarios -> Trocar senha.");
                log.warn("=================================================================");
            }
        });
    }
}
