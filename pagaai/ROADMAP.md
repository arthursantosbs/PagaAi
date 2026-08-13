# Paga aí — próximas versões

Cada item nasceu de uma limitação real do código atual, com o arquivo que
precisaria mudar. Ordem: do que evita prejuízo para o que é conforto.

## Resumo

| # | Item | Bloco | Esforço | Ganho |
|---|---|---|---|---|
| ~~1~~ | ~~Migrations com Flyway~~ — **FEITO** | — | — | — |
| ~~2~~ | ~~Backup automático do banco~~ — **FEITO** | — | — | — |
| 3 | Forçar troca da senha padrão no primeiro login | Antes de usar | pequeno | alto |
| 4 | Teste de tela e de segurança | Antes de usar | médio | alto |
| 5 | Botão "copiar mensagem de cobrança" | Dia a dia | pequeno | **alto** |
| 6 | Registrar venda fiada em 10 segundos | Dia a dia | médio | alto |
| 7 | Fechamento do mês | Dia a dia | pequeno | médio |
| 8 | Desfazer exclusão (lixeira) | Dia a dia | pequeno | médio |
| 9 | Histórico de quem mexeu no quê | Mais gente | pequeno | médio |
| 10 | Paginação e busca nas listas | Mais gente | médio | médio |
| 11 | Permissão real por sócio | Mais gente | médio | baixo |
| 12 | JWT no lugar de HTTP Basic | Mobile | médio | alto |
| 13 | App mobile | Mobile | grande | alto |
| 14 | Lembrete automático de vencimento | Mobile | médio | médio |

---

## Bloco 1 — Antes de usar pra valer

Risco de perder dinheiro ou dado. Faça antes de cadastrar cliente de verdade.

### ~~1. Migrations com Flyway~~ — FEITO

O schema virou `src/main/resources/db/migration/V1__esquema_inicial.sql` e o
`ddl-auto` passou para `validate`. Para alterar o banco, crie um `V2__*.sql`.

### ~~2. Backup automático do banco~~ — FEITO

O serviço `backup` do `docker-compose.yml` gera um `pg_dump` comprimido por dia em
`./backups` e limpa o que passar de `BACKUP_DIAS`. Falta você copiar para fora do
servidor e testar uma restauração — instruções no DEPLOY.md.

<details>
<summary>Texto original dos itens 1 e 2 (para referência)</summary>

### 1. Migrations com Flyway

**Problema hoje:** `application.yml` usa `ddl-auto: update`. O Hibernate cria
coluna nova sozinho, mas **nunca apaga a antiga**. Quando o modelo mudou de
"parcela paga" para "saldo devedor", a coluna `pagamento.data_vencimento` continuou
existindo como `NOT NULL` e todo INSERT novo passou a falhar — o banco de teste teve
de ser apagado. Com clientes reais cadastrados, isso é perda de dados.

**Proposta:** adicionar Flyway. O schema passa a ser uma sequência de arquivos SQL
versionados (`V1__inicial.sql`, `V2__adiciona_cpf.sql`) que o app aplica na ordem,
uma vez cada. Trocar `ddl-auto: update` por `validate`, que recusa subir se o banco
não bater com as entidades — erro na hora certa, e não meses depois.

**Onde mexe:** `pom.xml`, `application.yml`, `application-prod.yml`,
`src/main/resources/db/migration/` (novo).

**Esforço:** médio · **Ganho:** alto

### 2. Backup automático do banco

**Problema hoje:** `DEPLOY.md` ensina o comando de backup, mas ninguém o executa
sozinho. Um servidor sem backup agendado é um servidor sem backup.

**Proposta:** um `cron` diário rodando `pg_dump` para fora do servidor, com
retenção de 30 dias. E — o passo que quase todo mundo pula — **testar a
restauração uma vez**. Backup nunca restaurado não conta como backup.

**Onde mexe:** `docker-compose.yml` (serviço de backup) ou crontab do servidor,
`DEPLOY.md`.

**Esforço:** pequeno · **Ganho:** alto

</details>

### 3. Forçar troca da senha padrão no primeiro login

**Problema hoje:** `VerificacaoDeSeguranca` impede subir em produção com a senha
`12345678`, e o `DataSeeder` grita no log enquanto ela valer. Mas nada obriga a
troca — e log a gente para de ler.

**Proposta:** campo `precisaTrocarSenha` em `Usuario`. Enquanto verdadeiro, todo
acesso redireciona para a tela de troca. Vale também para o login do sócio criado
por você.

**Onde mexe:** `domain/Usuario`, `web/UsuarioController`, um filtro ou
`HandlerInterceptor`, `templates/usuarios/`.

**Esforço:** pequeno · **Ganho:** alto

### 4. Teste de tela e de segurança

**Problema hoje:** os 25 testes cobrem só a aritmética (`CalculadoraDeDivida` e
`CalendarioCobranca`). **Nenhum controller, nenhuma tela e nenhuma regra de
permissão são testados.** Já apareceram dois bugs justamente aí: a API devolvendo
302 em vez de 401, e o `LazyInitializationException` em `/cobrancas/{id}`. Os dois
passaram pela compilação e pelos testes.

**Proposta:** `@WebMvcTest` para os controllers e `spring-security-test` para as
permissões. Os casos que valem mais: SÓCIO não acessa `/usuarios`; API sem
credencial responde 401; registrar pagamento redireciona e grava; ficha do cliente
abre sem estourar lazy loading.

**Onde mexe:** `src/test/java/br/com/pagaai/web/` e `/api/` (novos).

**Esforço:** médio · **Ganho:** alto

---

## Bloco 2 — Faz falta no dia a dia

O que você vai sentir falta na primeira semana de uso real.

### 5. Botão "copiar mensagem de cobrança"

**Problema hoje:** o sistema mostra quem está atrasado, mas o dono ainda abre o
WhatsApp e digita tudo na mão, cliente por cliente. `BRANDING.md` já tem os quatro
modelos de mensagem escritos — eles só não estão no produto.

**Proposta:** na linha de cada devedor, um botão que copia a mensagem já preenchida
com nome, valor e dias, e um link `wa.me/55DDDNUMERO?text=...` que abre a conversa
com o texto pronto. O modelo escolhido muda conforme a situação (a vencer,
atrasado, atraso longo, quitado).

**Onde mexe:** `templates/fragmentos/layout.html` (fragmento novo),
`dto/SituacaoCobranca` (montar o texto), `static/css/app.css`.

**Esforço:** pequeno · **Ganho:** alto — é o item que mais economiza tempo por dia.

### 6. Registrar venda fiada em 10 segundos

**Problema hoje:** para lançar uma venda é preciso ir em Dívidas → Nova dívida,
escolher o cliente num `<select>` que lista todo mundo, e preencher 8 campos.
No balcão, com fila, isso não acontece.

**Proposta:** uma tela "Nova venda" com busca de cliente que autocompleta (ou cria
na hora com só o nome), valor, e um padrão inteligente para o resto. Cliente novo
sem endereço deve ser permitido — o endereço se preenche depois.

**Onde mexe:** `web/CobrancaController`, `templates/cobrancas/`, `service/ClienteService`.

**Esforço:** médio · **Ganho:** alto

### 7. Fechamento do mês

**Problema hoje:** o painel mostra "recebido no mês", e só. Não dá para responder
"quanto entrou em julho?" nem "quanto vendi fiado este mês?".

**Proposta:** tela com filtro por período: total recebido, total vendido fiado,
saldo da carteira no fim do período e a lista de pagamentos. Exportar em CSV
resolve o contador.

**Onde mexe:** `service/CarteiraService`, `repository/PagamentoRepository`,
template novo.

**Esforço:** pequeno · **Ganho:** médio

### 8. Desfazer exclusão (lixeira)

**Problema hoje:** "Excluir cliente" apaga em cascata as dívidas e os pagamentos,
sem volta. Só existe o `confirm()` do navegador entre um clique errado e a perda do
histórico. `Cliente` já tem o campo `ativo`, hoje quase sem uso.

**Proposta:** exclusão passa a marcar `ativo = false` e sumir das listas. Uma tela
de arquivados permite restaurar. Exclusão definitiva fica só para ADMIN.

**Onde mexe:** `service/ClienteService`, `repository/ClienteRepository` (filtrar por
`ativo`), templates.

**Esforço:** pequeno · **Ganho:** médio

---

## Bloco 3 — Quando tiver mais gente usando

### 9. Histórico de quem mexeu no quê

**Problema hoje:** `Pagamento` grava `registradoPor`, mas nada mais. Se um valor
sumir, não há como saber quem editou nem quando. Com dois sócios mexendo, isso
vira discussão.

**Proposta:** tabela de auditoria com quem, quando, o quê e o valor anterior — pelo
menos para pagamento estornado, dívida excluída e valor alterado.

**Onde mexe:** `domain/` (entidade nova), listeners JPA ou os services de escrita.

**Esforço:** pequeno · **Ganho:** médio

### 10. Paginação e busca nas listas

**Problema hoje:** `ClienteRepository#findAllByOrderByNomeAsc` traz **todos** os
clientes, e `CarteiraService#situacoes()` traz **todas** as dívidas ativas em toda
abertura de painel. Com 50 clientes é instantâneo; com 5.000 a página trava.
As 2 consultas fixas ajudam, mas o volume em memória cresce igual.

**Proposta:** `Pageable` nas listagens, busca no banco em vez de filtro em memória,
e no painel mostrar os 20 piores atrasos com link para "ver todos".

**Onde mexe:** `repository/`, `service/CarteiraService`, `web/`, templates.

**Esforço:** médio · **Ganho:** médio — só faz diferença quando a carteira crescer.

### 11. Permissão real por sócio

**Problema hoje:** existem os papéis ADMIN e SOCIO, mas a única diferença é a tela
de usuários. Na prática os dois podem tudo, inclusive excluir cliente.

**Proposta:** decidir o que o sócio não pode (excluir cliente? estornar pagamento?)
e aplicar com `@PreAuthorize` nos services, não só nos controllers.

**Onde mexe:** `service/`, `config/SecurityConfig`.

**Esforço:** médio · **Ganho:** baixo enquanto forem dois sócios de confiança.

---

## Bloco 4 — Mobile e futuro

### 12. JWT no lugar de HTTP Basic

**Problema hoje:** a API usa HTTP Basic, que manda usuário e senha **em toda
requisição**. Funciona, mas obriga o app a guardar a senha no aparelho — se o
celular for perdido, a senha vai junto e não há como revogar sem trocá-la.

**Proposta:** endpoint de login que devolve um token com validade, e um filtro que
o valida. `SecurityConfig#apiFilterChain` já está isolado justamente para essa
troca: o resto da API não muda.

**Onde mexe:** `config/SecurityConfig`, `api/` (filtro e endpoint novos), `pom.xml`.

**Esforço:** médio · **Ganho:** alto se o app existir.

### 13. App mobile

**Problema hoje:** o site é responsivo e funciona bem no celular, mas exige abrir o
navegador e digitar o endereço, e não funciona sem internet.

**Proposta:** antes de partir para app nativo, tente **PWA** — um `manifest.json` e
um service worker transformam o site atual em ícone na tela inicial, abrindo em tela
cheia. É uma tarde de trabalho contra semanas de app nativo, e resolve 90% do
incômodo. App nativo só se precisar de câmera, contatos ou notificação push.

**Onde mexe:** `static/` (manifest e service worker), `fragmentos/layout.html`.

**Esforço:** pequeno para o PWA, grande para nativo · **Ganho:** alto

### 14. Lembrete automático de vencimento

**Problema hoje:** o dono precisa abrir o sistema para lembrar de cobrar.

**Proposta:** uma tarefa agendada (`@Scheduled`) que toda manhã monta a lista de
quem vence hoje e de quem atrasou, e manda por e-mail ou Telegram. Cobrança
automática por WhatsApp exige API oficial paga — comece pelo aviso **para você**,
não para o cliente.

**Onde mexe:** `service/` (classe nova), `pom.xml`, `application-prod.yml`.

**Esforço:** médio · **Ganho:** médio

---

## O que eu não faria

Tentações comuns que, neste projeto, seriam desperdício.

**Trocar Thymeleaf por React.** Ganharia uma API a manter, um build de front, e um
sistema que nem funciona com JavaScript desligado. O site já é responsivo e
carrega instantâneo. Só valeria se a interface ficasse muito interativa —
arrastar, atualizar em tempo real.

**Microserviços.** São dois usuários. Um único jar que sobe em 8 segundos é a
arquitetura certa. Dividir isso multiplicaria os pontos de falha sem resolver nada.

**Calcular juros e multa por atraso.** Parece óbvio, mas muda o produto: entra
regra de contrato, discussão sobre o que foi combinado, e limite legal do CDC.
Hoje o Paga aí registra o que foi combinado, e essa clareza é uma vantagem.
Só faça se o negócio realmente cobrar juros.

**Guardar as parcelas no banco.** A tentação aparece quando alguém pensa "seria
mais fácil consultar". Seria — e aí mudar o dia do vencimento viraria uma migração
de dados, e parcela gravada divergiria da regra da cobrança. O cálculo custa
microssegundos. Não faça.

**Cache no painel.** Ele já custa 2 consultas fixas. Cache aqui adicionaria o
problema mais difícil da computação (invalidar cache) para economizar milissegundos,
e ainda arriscaria mostrar saldo desatualizado — logo depois de registrar um
pagamento, que é exatamente quando o dono confere.
