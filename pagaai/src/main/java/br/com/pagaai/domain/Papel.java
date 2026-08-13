package br.com.pagaai.domain;

/** Papel do usuario dentro da empresa. */
public enum Papel {
    ADMIN("Administrador"),
    SOCIO("Sócio");

    private final String descricao;

    Papel(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
