# Paga aí — mapa de manutenção

Este é o documento para consultar **antes de mexer em qualquer coisa**. Ele diz
onde cada coisa mora, o que acontece quando você muda, e onde as armadilhas estão.

---

## 1. As três ideias que explicam o sistema inteiro

Se você entender só isto, já consegue navegar no código.

### Ideia 1 — O sistema controla saldo, não "pago / não pago"

Não existe caixinha de "parcela quitada". Existe conta:

```
saldo devedor      = valor total          − tudo que o cliente pagou
vencido em aberto  = parcelas já vencidas − tudo que o cliente pagou   (nunca negativo)
quitada            = pagou ≥ valor total
```

É por isso que *"devia 100 no dia 20 e pagou 80"* funciona sem nenhum código
especial: sobra 20 de saldo e 20 de atraso.

### Ideia 2 — Parcela não é linha no banco

O banco guarda **4 tabelas**: `usuario`, `cliente`, `cobranca`, `pagamento`.
Parcela **não é uma delas**. As parcelas são recalculadas a cada tela a partir de
`dataInicio + periodicidade + valorParcela`.

Consequência boa: mudar o dia do vencimento é editar um campo, sem migrar nada.
Consequência a respeitar: a geração precisa ser barata, então `CalendarioCobranca`
não pode acessar banco.

### Ideia 3 — Pagamento não pertence a parcela

Um pagamento é só `valor + data`. Ele cai num caixa único da dívida e é alocado
**da parcela mais antiga para a mais nova**. Nenhuma tabela liga pagamento a
parcela — e é justamente essa ausência que faz pagamento parcial, adiantado e a
mais funcionarem sem caso especial.

---

## 2. Mapa das pastas

```
pagaai/
├── pom.xml                      dependências e versão do Java (Maven)
├── run.cmd                      sobe o app nesta máquina (acha o JDK sozinho)
├── Dockerfile                   imagem para produção
├── docker-compose.yml           banco + app + HTTPS, para servidor próprio
├── Caddyfile                    certificado automático (usado pelo compose)
├── render.yaml                  deploy sem servidor (alternativa)
├── .env.example                 modelo dos segredos de produção
│
├── README.md                    visão geral e como rodar
├── ARQUITETURA.md               ESTE arquivo — mapa e matriz de impacto
├── BRANDING.md                  identidade: cores, logo, voz, mensagens
├── DEPLOY.md                    passo a passo para colocar no ar
├── ROADMAP.md                   melhorias das próximas versões
│
├── data/                        banco H2 em arquivo (gerado; fora do git)
├── target/                      resultado do build (gerado; fora do git)
│
└── src/
    ├── main/java/br/com/pagaai/
    │   ├── PagaAiApplication.java    ponto de partida (o main)
    │   ├── domain/                   ← o que existe no banco
    │   ├── repository/               ← consultas
    │   ├── service/                  ← REGRA DE NEGÓCIO (o cérebro)
    │   ├── dto/                      ← objetos que atravessam camadas
    │   ├── web/                      ← controllers das telas
    │   ├── api/                      ← API REST /api/v1 (mobile)
    │   └── config/                   ← segurança e partida
    │
    ├── main/resources/
    │   ├── application.yml           configuração de desenvolvimento
    │   ├── application-prod.yml      configuração de produção
    │   ├── db/migration/             O SCHEMA DO BANCO, em SQL versionado (Flyway)
    │   ├── templates/                as telas (HTML + Thymeleaf)
    │   │   ├── fragmentos/layout.html   menu, marca, formulário de recebimento
    │   │   ├── dashboard.html           o painel
    │   │   ├── login.html
    │   │   ├── clientes/                lista, formulário, ficha
    │   │   ├── cobrancas/               lista, formulário, detalhe
    │   │   └── usuarios/lista.html
    │   └── static/css/app.css        todo o estilo, num arquivo só
    │
    └── test/java/br/com/pagaai/service/
        ├── CalendarioCobrancaTest.java   12 casos — geração de parcelas
        └── CalculadoraDeDividaTest.java  13 casos — alocação e saldo
```

Cada pacote Java também tem um `package-info.java` com a explicação detalhada.
A IDE mostra esse texto quando você abre a pasta.

---

## 3. O caminho de uma requisição

Exemplo real: o dono clica em **Registrar recebimento**.

```
1. navegador  POST /cobrancas/7/pagamentos  valor=80
                    │
2. SecurityConfig ── confere sessão e token CSRF
                    │
3. CobrancaController#pagar ── lê os parâmetros
                    │
4. CobrancaService#registrarPagamento ── VALIDA (valor > 0, data não futura)
                    │                     e grava
5. PagamentoRepository#save ─────────────► banco
                    │
6. redirect: volta para a tela (POST-Redirect-GET, para o F5 não duplicar)
                    │
7. CarteiraService#dividasDoCliente ── 2 consultas
                    │                   ├─ CalendarioCobranca → gera as parcelas
                    │                   └─ CalculadoraDeDivida → aloca os 80
8. templates/clientes/detalhe.html ── desenha o resultado
```

A seta nunca volta: `service` não conhece `web`, `domain` não conhece ninguém.

---

## 4. Matriz de impacto

**Mexeu na coluna da esquerda? Confira tudo na direita.**

| Se você mudar… | Impacta | Como verificar |
|---|---|---|
| `domain/Cobranca` ou `domain/Pagamento` | o banco, todo o cálculo, todas as telas, a API | criar um `V*.sql` novo + `mvn test` + abrir o painel |
| `db/migration/*.sql` | o schema do banco em dev e produção | subir o app; se divergir da entidade, ele recusa iniciar |
| `service/CalculadoraDeDivida` | **todo número do sistema** | `mvn test` (13 casos) |
| `service/CalendarioCobranca` | datas e valores de toda parcela | `mvn test` (12 casos) |
| `service/CarteiraService` | painel, lista de dívidas, ficha do cliente, API | abrir as 3 telas |
| um `record` do `dto/` | as telas **e o JSON da API** | procurar o nome antigo em `templates/` |
| nome de atributo no `model.addAttribute` | a tela correspondente fica vazia, **sem erro** | abrir a tela |
| uma URL num `@GetMapping` / `@PostMapping` | todos os links dos templates | procurar a URL antiga em `templates/` |
| `config/SecurityConfig` | quem entra e o que cada um acessa | entrar como ADMIN e como SÓCIO |
| `templates/fragmentos/layout.html` | **todas as telas de uma vez** | abrir 3 telas diferentes |
| `static/css/app.css` | todas as telas | abrir no desktop e no celular |
| `dto/Severidade` (faixas 7 / 30 dias) | as cores do semáforo | atualizar `BRANDING.md` e o CSS junto |
| `application.yml` | como o app sobe em dev | reiniciar |
| `application-prod.yml` | produção — erro aqui derruba o site | testar antes de publicar |

### Nomes que precisam bater entre arquivos

O compilador **não** verifica estes pares. Se um lado mudar e o outro não, a
falha aparece só quando alguém abre a tela.

| Java | Template |
|---|---|
| `model.addAttribute("dividas", …)` | `th:each="s : ${dividas}"` |
| `record SituacaoCobranca(… saldoDevedor …)` | `${s.saldoDevedor()}` |
| `getSeveridade()` | `${s.severidade}` |
| `isEmAtraso()` | `${s.emAtraso}` |
| `@GetMapping("/clientes/{id}")` | `th:href="@{'/clientes/' + ${c.id}}"` |
| `Severidade` devolve `"atraso-grave"` | `.etiqueta.atraso-grave` no CSS |

---

## 5. Receitas

### Adicionar um campo no cliente (ex.: CPF)

1. `domain/Cliente.java` — campo + getter + setter;
2. `dto/ClienteForm.java` — campo + getter + setter + copiar em `de()` e `aplicarEm()`;
3. `templates/clientes/formulario.html` — o `<input th:field="*{cpf}">`;
4. `templates/clientes/detalhe.html` — mostrar na ficha;
5. `api/ClienteResponse.java` — se o app mobile precisar;
6. reiniciar (o Hibernate cria a coluna sozinho).

### Mudar a faixa de atraso (ex.: vermelho a partir de 15 dias)

1. `dto/Severidade.java` — o `if` das faixas;
2. `static/css/app.css` — as classes `.atraso-*` se mudar de cor;
3. `BRANDING.md` — atualizar a tabela do semáforo.

### Criar uma tela nova

1. método no controller de `web/` devolvendo o nome do template;
2. arquivo em `templates/`, começando por copiar a estrutura de uma tela existente
   (as três primeiras linhas com `th:replace` do layout);
3. link no menu, em `fragmentos/layout.html`;
4. se for restrita, `@PreAuthorize("hasRole('ADMIN')")`.

### Adicionar um endpoint na API

1. método em `api/ApiController.java`;
2. se devolver entidade, crie um `*Response` — **nunca** serialize a entidade JPA
   direto (vaza estrutura interna e estoura lazy loading no meio da resposta);
3. campo novo é seguro; renomear ou remover quebra o app mobile.

---

## 6. Armadilhas conhecidas

Todas já morderam este projeto pelo menos uma vez.

### `LazyInitializationException` ao ler o cliente de uma cobrança

`open-in-view` está desligado. Fora da transação, `cobranca.getCliente().getNome()`
estoura. **Solução:** `CobrancaService#buscarComCliente(id)`, que faz `join fetch`.

### Mudança de schema agora passa pelo Flyway

O `ddl-auto` é **`validate`**: o Hibernate não altera mais nada, só confere se as
entidades batem com o banco. Quem cria e altera tabela são os arquivos SQL de
`src/main/resources/db/migration/`.

Para mudar o banco:

1. crie `V2__o_que_voce_fez.sql` (número novo, nunca edite um já aplicado — o
   Flyway guarda um checksum e recusa subir se o arquivo mudar);
2. altere a entidade em `domain/` para bater com o SQL;
3. suba. Se divergirem, a aplicação **não inicia** e diz qual coluna está errada.

Esse erro na partida é o recurso, não o defeito: antes, uma coluna renomeada
deixava a antiga `NOT NULL` para trás e **todo INSERT novo falhava** — foi o que
custou o banco de teste aqui.

### Acento em texto, nunca em identificador

As telas usam português com acento (*Dívida*, *Cobrança*). Mas **nome de classe CSS,
`id`, `for`, rota e nome de campo continuam sem acento**. Trocar `class="acoes"` por
`class="ações"` quebra o estilo em silêncio.

### Formulário sem `th:action` não envia

O Thymeleaf injeta o token CSRF automaticamente em `<form th:action=...>`.
Formulário escrito com `action=` comum é rejeitado com **403**.

### `@PreAuthorize` só funciona com `@EnableMethodSecurity`

A anotação está em `SecurityConfig`. Sem ela, `@PreAuthorize` é **ignorado em
silêncio** e a tela de usuários fica aberta para qualquer um que esteja logado.

### `COOKIE_SECURE=true` sem HTTPS

O navegador descarta o cookie de sessão e o login entra em loop, sem mensagem de
erro. Ver `DEPLOY.md`.

---

## 7. Rodar e testar

Não há `java` nem `mvn` no PATH desta máquina. O `run.cmd` resolve o JDK sozinho.

```bash
pagaai\run.cmd
```

Para build e testes, com o Maven que vem no IntelliJ:

```bash
mvn -DskipTests package
```

```bash
mvn test
```

**25 testes.** Eles cobrem a parte que erra em silêncio — a aritmética do dinheiro.
Se um quebrar, pare: o número na tela de alguém vai sair errado.

O que os testes **não** cobrem hoje: telas, controllers e segurança. Está no ROADMAP.
