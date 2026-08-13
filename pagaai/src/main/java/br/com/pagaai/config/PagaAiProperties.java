package br.com.pagaai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracoes do app (prefixo {@code pagaai} no application.yml). */
@ConfigurationProperties(prefix = "pagaai")
public class PagaAiProperties {

    private final Admin admin = new Admin();

    public Admin getAdmin() {
        return admin;
    }

    public static class Admin {
        /** Login do usuario criado automaticamente no primeiro start. */
        private String login = "HERA123";
        /** Senha em texto puro usada apenas para gerar o hash inicial. */
        private String senha = "12345678";
        private String nome = "Administrador";

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }
    }
}
