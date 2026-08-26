package br.com.pagaai.web;

import br.com.pagaai.domain.Papel;
import br.com.pagaai.repository.UsuarioRepository;
import br.com.pagaai.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela de usuarios ({@code /usuarios}): criar o login do socio e trocar senhas.
 *
 * <p>Restrita a ADMIN por {@code @PreAuthorize} em cada metodo. A anotacao so
 * funciona porque {@code SecurityConfig} tem {@code @EnableMethodSecurity} — sem
 * ela, o {@code @PreAuthorize} e ignorado <b>em silencio</b> e a tela fica aberta
 * para qualquer usuario logado.
 *
 * <p>O menu esconde o link com {@code sec:authorize="hasRole('ADMIN')"}, mas
 * esconder link nao e seguranca: quem digitar a URL tambem precisa ser barrado, e
 * e o {@code @PreAuthorize} que faz isso.
 */
@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute("pagina")
    public String pagina() {
        return "usuarios";
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("papeis", Papel.values());
        return "usuarios/lista";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String criar(@RequestParam String login, @RequestParam String nome,
                        @RequestParam String senha, @RequestParam Papel papel,
                        RedirectAttributes flash) {
        if (senha.length() < 8) {
            flash.addFlashAttribute("erro", "A senha precisa ter pelo menos 8 caracteres. "
                    + "Uma frase curta funciona bem e é fácil de lembrar, como \"padaria do ze\".");
            return "redirect:/usuarios";
        }
        if (usuarioRepository.existsByLoginIgnoreCase(login)) {
            flash.addFlashAttribute("erro", "Já existe um usuário com o login \"" + login
                    + "\". Escolha outro — por exemplo, o primeiro nome junto do sobrenome.");
            return "redirect:/usuarios";
        }
        usuarioService.criar(login.trim(), nome.trim(), senha, papel);
        flash.addFlashAttribute("sucesso", "Usuário \"" + login.trim() + "\" criado. "
                + "Passe o login e a senha para essa pessoa — de preferência pessoalmente "
                + "ou por mensagem privada, não por escrito em grupo.");
        return "redirect:/usuarios";
    }

    @PostMapping("/senha")
    @PreAuthorize("hasRole('ADMIN')")
    public String trocarSenha(@RequestParam String login, @RequestParam String novaSenha,
                              RedirectAttributes flash) {
        if (novaSenha.length() < 8) {
            flash.addFlashAttribute("erro", "A senha precisa ter pelo menos 8 caracteres. "
                    + "Uma frase curta funciona bem e é fácil de lembrar, como \"padaria do ze\".");
            return "redirect:/usuarios";
        }

        // Conferir aqui, ANTES de chamar o servico, nao e detalhe de estilo.
        // O UsuarioService lanca UsernameNotFoundException, que e uma excecao de
        // AUTENTICACAO: o Spring Security a intercepta, limpa a sessao e joga o
        // usuario no login. Ou seja, um erro de digitacao no login deslogava o
        // administrador em silencio, e ele nunca sabia se a senha tinha trocado.
        if (!usuarioRepository.existsByLoginIgnoreCase(login)) {
            flash.addFlashAttribute("erro", "Não existe usuário com o login \"" + login
                    + "\". Confira a lista aqui em cima e digite o login exatamente "
                    + "como aparece lá.");
            return "redirect:/usuarios";
        }

        usuarioService.trocarSenha(login, novaSenha);
        flash.addFlashAttribute("sucesso", "Senha de \"" + login + "\" atualizada. "
                + "Avise essa pessoa para entrar com a senha nova.");
        return "redirect:/usuarios";
    }
}
