package br.com.pagaai.api;

import br.com.pagaai.domain.Cliente;

/** Representacao de cliente exposta na API (sem entidades JPA cruas). */
public record ClienteResponse(
        Long id,
        String nome,
        String telefone,
        String instagram,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String cep,
        String observacoes,
        boolean ativo) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getTelefone(), c.getInstagram(),
                c.getLogradouro(), c.getNumero(), c.getComplemento(), c.getBairro(), c.getCidade(),
                c.getUf(), c.getCep(), c.getObservacoes(), c.isAtivo());
    }
}
