# Paga aí

> O caderninho do fiado que faz a conta sozinho.

Controle de quem te deve: cadastro de clientes, dívidas com valor total (fiado,
venda parcelada) ou cobrança recorrente, e o painel mostrando **quem está devendo,
quanto e há quantos dias**.

Java 17 + Spring Boot 3.3, com API REST pronta para o app mobile no futuro.

## Documentação

| Arquivo | Para quê |
|---|---|
| **[ARQUITETURA.md](ARQUITETURA.md)** | **comece por aqui para manutenção** — mapa das pastas, matriz de impacto, receitas e armadilhas |
| **[PROXIMA-VERSAO.md](PROXIMA-VERSAO.md)** | **para codar a v2** — melhorias com passo a passo, na ordem sugerida |
| [MANUAL.md](MANUAL.md) | manual de uso, para o dono e o sócio |
| [BRANDING.md](BRANDING.md) | cores, logo, tipografia, voz e modelos de mensagem de cobrança |
| [ROADMAP.md](ROADMAP.md) | melhorias das próximas versões, priorizadas |
| [DEPLOY.md](DEPLOY.md) | como colocar no ar |

Cada pacote Java também tem um `package-info.java` explicando o que vive nele e
onde uma alteração ali repercute.

---

## Como rodar

Precisa de um JDK 17 ou superior. Se `java` não estiver no PATH, o `run.cmd`
já aponta para o JDK que está instalado nesta máquina (`%USERPROFILE%\.jdks\ms-17.0.20`).

Build:

```bash
mvn -DskipTests package
```

Subir a aplicação:

```bash
run.cmd
```

Depois abra <http://localhost:8080>.

**Login padrão:** usuário `HERA123`, senha `12345678`.
Troque essa senha em **Usuários → Trocar senha** antes de colocar em produção —
ela está escrita em texto puro no `application.yml` e no README.

---

## Banco de dados

Por padrão usa **H2 em arquivo** (`data/pagaai.mv.db`, criado na pasta de onde você
roda o app). Zero instalação, os dados sobrevivem ao restart.

Console do H2 (só para o usuário ADMIN): <http://localhost:8080/h2-console>
— JDBC URL `jdbc:h2:file:./data/pagaai`, usuário `sa`, senha vazia.

Em produção o perfil `prod` usa **PostgreSQL**, cookie seguro e segredos por
variável de ambiente. O passo a passo para colocar no ar está em
[DEPLOY.md](DEPLOY.md).

O schema é criado pelo Hibernate (`ddl-auto: update`) — quando o sistema virar a
gestão da empresa de verdade, vale trocar isso por Flyway com migrations versionadas.

---

## Como o cálculo de pendência funciona

O foco é **receber o valor todo**, não marcar mensalidade como paga.

Uma **dívida** guarda o valor total e o ritmo combinado de pagamento (R$ 100 por
semana, por exemplo). As parcelas não existem no banco: `CalendarioCobranca` as
calcula e para de gerar assim que a soma cobre o total — a última vem quebrada
com o resto (500 em parcelas de 150 vira 150 + 150 + 150 + 50).

Um **pagamento** é só um valor e uma data. Ele não pertence a uma parcela: entra
no caixa da dívida e `CalculadoraDeDivida` aloca da parcela mais antiga para a
mais nova. É isso que faz o caso real funcionar sem gambiarra:

> Devia R$ 100 no dia 20, pagou R$ 80 → saldo R$ 20, atraso R$ 20, contando
> desde o dia 20.

As contas que saem daí:

| Conta | Como é calculada |
|---|---|
| Saldo devedor | `valor total − tudo que pagou` |
| Vencido e não pago | `parcelas já vencidas − tudo que pagou` (nunca negativo) |
| Ainda vai vencer | `saldo − vencido` |
| Dias de atraso | da parcela mais antiga que ainda tem saldo |
| Quitada | pagou ≥ valor total → a cobrança acaba sozinha |

Consequências práticas:

- pagamento parcial, adiantado ou a mais funciona sem caso especial;
- **todo cliente com saldo é devedor**; quem passou da data é tratado à parte,
  com bloco próprio no painel e ordenação pelo maior atraso;
- mudou o dia do vencimento? é só editar a dívida, sem remontar parcela nenhuma;
- mensal no dia 31 cai no último dia dos meses curtos (28/02, 30/04...);
- mensalidade sem fim continua possível: é só não informar o valor total.

Painel, lista e API compartilham o mesmo retrato da carteira, montado por
`CarteiraService` em **duas consultas** — uma traz as dívidas ativas, outra traz a
soma paga por dívida já agregada no banco. O custo não cresce com o histórico de
pagamentos.

A regra tem testes: `CalculadoraDeDividaTest` (13 casos) e `CalendarioCobrancaTest`
(7 casos).

```bash
mvn test
```

---

## Estrutura

```
src/main/java/br/com/pagaai/
  domain/       entidades JPA (Cliente, Cobranca, Pagamento, Usuario)
  repository/   Spring Data JPA
  service/      regra de negócio — CalculadoraDeDivida e CarteiraService são o núcleo
  web/          controllers das telas (Thymeleaf)
  api/          API REST /api/v1 (para o mobile)
  config/       segurança, propriedades, usuário padrão
src/main/resources/
  templates/    telas
  static/css/   estilo (responsivo, já funciona no celular)
```

---

## Telas

| Rota | O que faz |
|---|---|
| `/` | Painel: a receber, vencido, a vencer, recebido no mês. **Atrasados** em bloco separado, depois os devedores em dia |
| `/clientes` | Lista e busca por nome, telefone ou Instagram |
| `/clientes/{id}` | Ficha do cliente, uma dívida por bloco com barra de progresso e campo de recebimento, dívidas quitadas e histórico |
| `/cobrancas` | Dívidas atrasadas, todas as dívidas em aberto e a agenda de 30 dias |
| `/cobrancas/{id}` | Detalhe da dívida: progresso, parcelas com quanto entrou em cada uma, registrar/estornar pagamento |
| `/usuarios` | Só ADMIN: criar o sócio e trocar senhas |

---

## API REST (para o app mobile)

Base `/api/v1`, autenticação **HTTP Basic**, sem redirect para tela de login —
responde `401` quando não autenticado. Sessão é stateless.

| Método | Rota | O que faz |
|---|---|---|
| GET | `/api/v1/me` | usuário logado e papéis |
| GET | `/api/v1/resumo` | números do painel |
| GET | `/api/v1/devedores` | todos os devedores, atrasados primeiro |
| GET | `/api/v1/devedores/atrasados` | só quem passou da data |
| GET | `/api/v1/dividas` | dívidas com saldo, com o detalhe das parcelas |
| GET | `/api/v1/agenda?dias=7` | o que vai vencer no período |
| GET/POST | `/api/v1/clientes` | listar (`?q=`) / criar |
| GET/PUT/DELETE | `/api/v1/clientes/{id}` | ler / atualizar / excluir |
| GET | `/api/v1/clientes/{id}/dividas` | dívidas do cliente, quitadas inclusive |
| GET/POST | `/api/v1/cobrancas` | listar ativas / criar |
| GET/PUT/DELETE | `/api/v1/cobrancas/{id}` | situação da dívida / atualizar / excluir |
| POST | `/api/v1/cobrancas/{id}/pagamentos` | registrar recebimento; devolve a dívida recalculada |
| DELETE | `/api/v1/pagamentos/{id}` | estornar |

Registrar um recebimento parcial:

```bash
curl -u HERA123:12345678 -X POST http://localhost:8080/api/v1/cobrancas/1/pagamentos -H "Content-Type: application/json" -d "{\"valor\":80.00}"
```

A resposta já traz `saldoDevedor`, `valorEmAtraso`, `diasAtraso`, `quitada` e as
parcelas com quanto entrou em cada uma.

Quando o app mobile sair do papel, troque Basic por **JWT**: é adicionar um filtro
na cadeia `apiFilterChain` do `SecurityConfig` — o resto da API não muda.

---

## Colocar no ar

Veja [DEPLOY.md](DEPLOY.md). Resumo do que já está pronto:

- `Dockerfile` (build e runtime separados, roda sem privilégio de root);
- `docker-compose.yml` + `Caddyfile` — banco, app e HTTPS automático em um comando;
- `render.yaml` para quem não quer administrar servidor;
- perfil `prod` com PostgreSQL, cookie `Secure`/`HttpOnly` e `h2-console` desligado;
- `/actuator/health` público para o health check da hospedagem.

O que ainda depende de você: escolher a hospedagem, apontar um domínio, definir
`ADMIN_SENHA` e configurar o backup do banco.
