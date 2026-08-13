package br.com.pagaai.repository;

import br.com.pagaai.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Consultas de usuario do sistema.
 *
 * <p>O {@code IgnoreCase} nos dois metodos e proposital: quem digita "hera123" no
 * login entra do mesmo jeito. Se um dia voce tirar isso, o {@code DataSeeder}
 * passa a criar um usuario duplicado a cada start com caixa diferente.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLoginIgnoreCase(String login);

    boolean existsByLoginIgnoreCase(String login);
}
