package br.com.pagaai.dto;

import br.com.pagaai.domain.Cliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ClienteForm {

    private Long id;

    @NotBlank(message = "Informe o nome do cliente")
    @Size(max = 120)
    private String nome;

    @Size(max = 30)
    private String telefone;

    @Size(max = 60)
    private String instagram;

    @Size(max = 150)
    private String logradouro;

    @Size(max = 20)
    private String numero;

    @Size(max = 80)
    private String complemento;

    @Size(max = 80)
    private String bairro;

    @Size(max = 80)
    private String cidade;

    @Size(max = 2, message = "Use a sigla do estado, ex: PB")
    private String uf;

    @Size(max = 9)
    private String cep;

    @Size(max = 500)
    private String observacoes;

    private boolean ativo = true;

    public static ClienteForm de(Cliente cliente) {
        ClienteForm form = new ClienteForm();
        form.id = cliente.getId();
        form.nome = cliente.getNome();
        form.telefone = cliente.getTelefone();
        form.instagram = cliente.getInstagram();
        form.logradouro = cliente.getLogradouro();
        form.numero = cliente.getNumero();
        form.complemento = cliente.getComplemento();
        form.bairro = cliente.getBairro();
        form.cidade = cliente.getCidade();
        form.uf = cliente.getUf();
        form.cep = cliente.getCep();
        form.observacoes = cliente.getObservacoes();
        form.ativo = cliente.isAtivo();
        return form;
    }

    public void aplicarEm(Cliente cliente) {
        cliente.setNome(nome == null ? null : nome.trim());
        cliente.setTelefone(telefone);
        cliente.setInstagram(instagram);
        cliente.setLogradouro(logradouro);
        cliente.setNumero(numero);
        cliente.setComplemento(complemento);
        cliente.setBairro(bairro);
        cliente.setCidade(cidade);
        cliente.setUf(uf == null || uf.isBlank() ? null : uf.trim().toUpperCase());
        cliente.setCep(cep);
        cliente.setObservacoes(observacoes);
        cliente.setAtivo(ativo);
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
}
