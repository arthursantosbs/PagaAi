package br.com.pagaai.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Quem entra no sistema — voce e seu socio. Nao confundir com
 * {@link Cliente}, que e quem deve.
 *
 * <p><b>A senha nunca e gravada.</b> O campo e {@code senhaHash}, um BCrypt de 60
 * caracteres. BCrypt e mao unica: da para conferir se uma senha digitada bate com
 * o hash, mas nao da para descobrir a senha a partir dele. Por isso o sistema nao
 * tem "recuperar senha", so "definir nova senha".
 *
 * <p><b>Se voce mexer aqui:</b> trocar o algoritmo no {@code SecurityConfig}
 * invalida todos os hashes ja gravados e ninguem mais entra.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String login;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel = Papel.SOCIO;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = papel;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
