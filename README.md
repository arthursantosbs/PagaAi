# Paga aí

> O caderninho do fiado que faz a conta sozinho.

Sistema web para controlar quem te deve: cadastro de clientes, dívidas com valor
total (fiado, venda parcelada) ou cobrança recorrente, e um painel que mostra
**quem está devendo, quanto e há quantos dias**.

O código fica em **[`pagaai/`](pagaai/)**.

## O que ele faz

- **Dívida com valor total.** Cliente comprou R$ 500 fiado para pagar R$ 150 por
  mês? O sistema gera as parcelas, e quando a soma dos pagamentos chega em 500 a
  cobrança quita sozinha.
- **Pagamento parcial de verdade.** Devia R$ 100 no dia 20 e pagou R$ 80? Sobram
  R$ 20 de saldo e R$ 20 em atraso, contando desde o dia 20. Sem gambiarra.
- **Atrasado é tratado à parte.** Todo mundo com saldo é devedor, mas quem passou
  da data aparece em bloco separado no topo, ordenado pelo maior atraso.
- **Login por usuário e senha**, com senha em BCrypt e papéis (Administrador e
  Sócio).
- **API REST** em `/api/v1` pronta para um app mobile.

## Como rodar

Precisa de JDK 17 ou superior.

```bash
cd pagaai && mvn -DskipTests package && java -jar target/paga-ai-0.1.0.jar
```

No Windows, `pagaai\run.cmd` acha o JDK sozinho. Abra <http://localhost:8080>.

## Documentação

| Arquivo | Para quê |
|---|---|
| [pagaai/ARQUITETURA.md](pagaai/ARQUITETURA.md) | mapa das pastas, matriz de impacto, receitas e armadilhas |
| [pagaai/DEPLOY.md](pagaai/DEPLOY.md) | colocar no ar, com Docker ou em plataforma |
| [pagaai/BRANDING.md](pagaai/BRANDING.md) | cores, logo, voz e modelos de mensagem de cobrança |
| [pagaai/ROADMAP.md](pagaai/ROADMAP.md) | próximas versões, priorizadas |

## Tecnologia

Java 17 · Spring Boot 3.3 · Thymeleaf · Spring Security · Flyway ·
PostgreSQL em produção, H2 em desenvolvimento · Docker
