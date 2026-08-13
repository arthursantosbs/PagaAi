package br.com.pagaai.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Quem deve. Guarda identificacao, contato e endereco.
 *
 * <p>O contato importa mais do que parece: a cobranca acontece pelo WhatsApp e
 * pelo Instagram, entao telefone e {@code instagram} aparecem direto nas listas
 * de devedores para o dono nao precisar procurar.
 *
 * <p><b>Se voce mexer aqui:</b> adicionar campo pede a mesma mudanca em
 * {@code dto/ClienteForm} (o formulario nao le a entidade direto) e no template
 * {@code clientes/formulario.html}. Ver o roteiro completo em ARQUITETURA.md.
 *
 * <p><b>Cuidado com a exclusao:</b> a colecao {@code cobrancas} usa
 * {@code cascade = ALL} e {@code orphanRemoval}. E isso que faz "Excluir cliente"
 * levar junto as dividas e os pagamentos. Sem isso, o banco recusa a exclusao
 * por causa da chave estrangeira.
 */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Telefone / WhatsApp. */
    @Column(length = 30)
    private String telefone;

    @Column(length = 60)
    private String instagram;

    @Column(length = 150)
    private String logradouro;

    @Column(length = 20)
    private String numero;

    @Column(length = 80)
    private String complemento;

    @Column(length = 80)
    private String bairro;

    @Column(length = 80)
    private String cidade;

    @Column(length = 2)
    private String uf;

    @Column(length = 9)
    private String cep;

    @Column(length = 500)
    private String observacoes;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cobranca> cobrancas = new ArrayList<>();

    /** Endereco em uma linha, para listagens. */
    @Transient
    public String getEnderecoResumido() {
        StringBuilder sb = new StringBuilder();
        if (logradouro != null && !logradouro.isBlank()) {
            sb.append(logradouro);
            if (numero != null && !numero.isBlank()) {
                sb.append(", ").append(numero);
            }
        }
        if (bairro != null && !bairro.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(bairro);
        }
        if (cidade != null && !cidade.isBlank()) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(cidade);
            if (uf != null && !uf.isBlank()) {
                sb.append('/').append(uf);
            }
        }
        return sb.toString();
    }

    /** Handle do instagram sem o @, util para montar links. */
    @Transient
    public String getInstagramHandle() {
        if (instagram == null || instagram.isBlank()) {
            return null;
        }
        return instagram.trim().replaceFirst("^@", "");
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getInstagram() {
        return instagram;
    }

    public void setInstagram(String instagram) {
        this.instagram = instagram;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public void setLogradouro(String logradouro) {
        this.logradouro = logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public void setComplemento(String complemento) {
        this.complemento = complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public void setCidade(String cidade) {
        this.cidade = cidade;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCep() {
        return cep;
    }

    public void setCep(String cep) {
        this.cep = cep;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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

    public List<Cobranca> getCobrancas() {
        return cobrancas;
    }

    public void setCobrancas(List<Cobranca> cobrancas) {
        this.cobrancas = cobrancas;
    }
}
