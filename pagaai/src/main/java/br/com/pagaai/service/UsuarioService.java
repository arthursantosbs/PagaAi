package br.com.pagaai.service;

import br.com.pagaai.domain.Papel;
import br.com.pagaai.domain.Usuario;
import br.com.pagaai.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * A ponte entre a tabela {@code usuario} e o Spring Security.
 *
 * <p>Ao implementar {@code UserDetailsService}, esta classe passa a ser chamada
 * pelo proprio Spring toda vez que alguem tenta entrar. Nos nao escrevemos
 * comparacao de senha em lugar nenhum: entregamos o hash e o framework confere.
 *
 * <p>O papel do usuario vira autoridade no formato {@code ROLE_ADMIN} /
 * {@code ROLE_SOCIO} — o prefixo {@code ROLE_} e obrigatorio para
 * {@code hasRole('ADMIN')} funcionar no {@code SecurityConfig} e no
 * {@code sec:authorize} dos templates.
 *
 * <p><b>Se voce mexer aqui:</b> mudar o formato da autoridade quebra em silencio
 * as regras de permissao — as telas de ADMIN simplesmente somem do menu, sem erro.
 */
@Service
@Transactional(readOnly = true)
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = repository.findByLoginIgnoreCase(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário ou senha inválidos"));
        return User.withUsername(usuario.getLogin())
                .password(usuario.getSenhaHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name())))
                .disabled(!usuario.isAtivo())
                .build();
    }

    public List<Usuario> listar() {
        return repository.findAll();
    }

    @Transactional
    public Usuario criar(String login, String nome, String senha, Papel papel) {
        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setNome(nome);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setPapel(papel);
        return repository.save(usuario);
    }

    @Transactional
    public void trocarSenha(String login, String novaSenha) {
        Usuario usuario = repository.findByLoginIgnoreCase(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
    }
}
