# Paga aí — guia da próxima versão

Escrito para **você codar e me consultar quando travar**. Cada item tem o
problema real, por que importa, o passo a passo e como saber que deu certo.

Ordem sugerida: comece pela **M1**, que é curta e te dá o ciclo completo
(branch → código → teste → PR → deploy) sem risco nenhum.

---

## Como trabalhar

Sempre o mesmo ciclo. Vale a pena decorar:

```bash
git checkout main
git pull origin main
git checkout -b nome-da-mudanca
```

Codar. Depois:

```bash
mvn test
```

Se verde, commit, e no GitHub Desktop → **Publish branch** → abrir o PR →
mergear. O Render redeploya sozinho.

**Regra que salva:** nunca commite direto na `main`. A branch te dá o
desfazer de graça.

### Quando me consultar

Me manda **estas três coisas** e eu resolvo rápido:

1. o que você queria fazer,
2. o erro completo (a mensagem inteira, não só a última linha),
3. o arquivo que você mexeu.

Se for erro em produção, o log do Render. Se for tela errada, um print.

---

# Parte 1 — Melhorias no código

## M1. A tela de Dívidas faz 2 consultas para nada

**Onde:** `web/CobrancaController.java`, no método `lista`.

Linha 85 busca os devedores e linha 98 coloca no modelo:

```java
List<PendenciaCliente> devedores = carteira.devedores();
...
model.addAttribute("devedores", devedores);
```

**Nenhum template usa `${devedores}`.** Confira você mesmo:

```bash
git grep "devedores" -- "*.html"
```

Aquele `carteira.devedores()` dispara duas consultas ao banco toda vez que
alguém abre a tela, e o resultado é jogado fora.

### Passo a passo

1. Abra `web/CobrancaController.java`
2. Apague a linha `List<PendenciaCliente> devedores = carteira.devedores();`
3. Apague a linha `model.addAttribute("devedores", devedores);`
4. O import de `PendenciaCliente` provavelmente fica sem uso — apague também
   (a IDE marca em cinza)
5. `mvn test`
6. Abra a tela de Dívidas e confirme que nada sumiu

**Ganho:** duas consultas a menos por acesso. **Esforço:** 5 minutos.

> Lição: código morto não é neutro. Este custava banco toda vez.

---

## M2. O painel repete a mesma consulta 3 vezes

**Onde:** `service/CarteiraService.java` e os controllers que o usam.

Todo método público do `CarteiraService` começa refazendo o mesmo trabalho:

```java
public List<SituacaoCobranca> situacoes() {
    Map<Long, BigDecimal> pagos = pagosPorCobranca(pagamentoRepository.totaisPagos());
    return cobrancaRepository.findAtivasComCliente().stream()...
}
```

E `emAberto()`, `devedores()`, `emAtraso()` e `agenda()` **todos chamam
`situacoes()`**. Então:

| Tela | Chamadas | Consultas |
|---|---|---|
| Painel | `resumo()` + `devedores()` + `agenda(7)` | **7** |
| Dívidas | 6 chamadas diferentes | **12** |

Deveriam ser 2 e 2. Com 50 clientes ninguém sente; com 2.000 a tela começa a
arrastar.

### Passo a passo

A ideia: montar o retrato **uma vez** e derivar tudo dele.

1. Crie `dto/Carteira.java` — um record que guarda a lista pronta:

   ```java
   public record Carteira(List<SituacaoCobranca> situacoes) {
       public List<SituacaoCobranca> emAberto() { ... }
       public List<PendenciaCliente> devedores() { ... }
       public List<ItemAgenda> agenda(int dias) { ... }
       public ResumoDashboard resumo(long totalClientes, BigDecimal recebidoNoMes) { ... }
   }
   ```

2. **Mova** a lógica de filtro e ordenação do `CarteiraService` para dentro
   dele. Repare que nada disso toca o banco — é só aritmética sobre a lista.

3. No `CarteiraService`, deixe **um** método que vai ao banco:

   ```java
   public Carteira montar() { /* as 2 consultas, e devolve new Carteira(...) */ }
   ```

4. Nos controllers, chame `carteira.montar()` **uma vez** e derive:

   ```java
   Carteira c = carteiraService.montar();
   model.addAttribute("atrasadas", c.emAberto().stream().filter(...).toList());
   model.addAttribute("abertas", c.emAberto());
   ```

5. `mvn test` e abra as duas telas

### Como provar que funcionou

Ligue o log de SQL em `application.yml`:

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
```

Abra o Painel, conte os `select` no console, e compare antes e depois.
**Meça, não confie na impressão.** Depois desligue.

**Ganho:** de 7 e 12 consultas para 2 e 2. **Esforço:** algumas horas.

> Esta é a melhoria que mais ensina: separa "buscar dados" de "calcular sobre
> dados", que é o que deixa o segundo fácil de testar.

---

## M3. Adicionar um campo agora exige uma migration

Isso mudou nesta versão e é **a regra mais importante para não quebrar o banco**.
Antes o Hibernate criava coluna sozinho; agora `ddl-auto` é `validate`, ele só
confere.

### Exemplo completo: adicionar CPF no cliente

1. Crie `src/main/resources/db/migration/V2__adiciona_cpf_no_cliente.sql`:

   ```sql
   ALTER TABLE cliente ADD COLUMN cpf VARCHAR(14);
   ```

   O número (`V2`) tem que ser maior que o último. **Nunca edite um arquivo
   `V*` já aplicado** — o Flyway guarda um checksum e recusa subir.

2. Em `domain/Cliente.java`, o campo com o mesmo tipo e tamanho:

   ```java
   @Column(length = 14)
   private String cpf;
   ```
   Mais o getter e o setter.

3. Em `dto/ClienteForm.java`: campo, getter, setter, e copiar em `de()` e
   `aplicarEm()`.

4. Em `templates/clientes/formulario.html`, o input:

   ```html
   <div class="campo">
       <label for="cpf">CPF</label>
       <input id="cpf" type="text" th:field="*{cpf}">
   </div>
   ```

5. Suba. Se você errar o tipo ou o tamanho, **a aplicação não inicia** e diz
   qual coluna está diferente. Esse erro é o recurso funcionando.

> Se travar o banco local em desenvolvimento, apague a pasta `pagaai/data` e
> suba de novo: o Flyway recria tudo do zero. **Em produção isso apaga os
> dados** — lá o caminho é sempre uma migration nova.

---

## M4. Faltam testes de tela

Hoje são 29 testes: 27 de cálculo e **2 de tela**. Os dois bugs sérios desta
versão passaram pela compilação e pelos 27 — o buraco era entre o formulário e
a tela.

O molde já existe: `src/test/java/br/com/pagaai/web/CadastroDeDividaTest.java`.
Copie e adapte.

### Os quatro que eu escreveria primeiro

1. **Sócio não acessa `/usuarios`** — `@WithMockUser(roles = "SOCIO")`,
   espera `status().isForbidden()`
2. **API sem credencial responde 401** — não a página de login
3. **Registrar pagamento parcial** — posta o valor, confere que o saldo caiu
4. **Ficha do cliente abre** — pega `LazyInitializationException` na hora certa

### Passo a passo do primeiro

1. Crie `src/test/java/br/com/pagaai/web/PermissoesTest.java`
2. Copie as anotações de `CadastroDeDividaTest`
3. Escreva:

   ```java
   @Test
   @WithMockUser(username = "socio", roles = "SOCIO")
   void socioNaoAcessaTelaDeUsuarios() throws Exception {
       mvc.perform(get("/usuarios")).andExpect(status().isForbidden());
   }
   ```

4. `mvn test`

**Ganho:** alto — é o que impede a próxima regressão. **Esforço:** 1h os quatro.

---

## M5. Nada tem paginação

`ClienteRepository.findAllByOrderByNomeAsc()` traz **todos** os clientes.
`CarteiraService` traz **todas** as dívidas ativas. Funciona muito bem hoje.

**Quando mexer:** quando a lista de clientes passar de umas 500 linhas e a tela
começar a demorar. Antes disso é otimização adiantada.

**O caminho, quando chegar a hora:** trocar `List<Cliente>` por
`Page<Cliente>` com `Pageable` nos repositórios, e no painel mostrar os 20
piores atrasos com link para "ver todos".

---

# Parte 2 — Funcionalidades da v2

## F1. Botão "cobrar no WhatsApp" ⭐ comece por esta

**Por que primeiro:** é a que mais economiza seu tempo por dia, mexe só em
template e numa classe nova, e não tem risco de quebrar nada.

Hoje o sistema te diz quem cobrar, e você abre o WhatsApp e digita tudo na mão.
Os quatro textos prontos já existem em `BRANDING.md` — só não estão no produto.

### Passo a passo

1. Crie `service/MensagemDeCobranca.java`, anotada com `@Component`

2. Um método que monta o texto:

   ```java
   public String paraAtrasado(PendenciaCliente p) {
       return "Oi " + p.clienteNome() + "! ...";
   }
   ```
   Use os modelos do `BRANDING.md`, seção 7.

3. Um método que monta o link, e é aqui que mora a dificuldade:

   ```java
   public String linkWhatsApp(PendenciaCliente p) {
       String numero = p.telefone().replaceAll("\\D", "");  // só dígitos
       if (numero.length() <= 11) numero = "55" + numero;   // DDI do Brasil
       String texto = URLEncoder.encode(paraAtrasado(p), StandardCharsets.UTF_8);
       return "https://wa.me/" + numero + "?text=" + texto;
   }
   ```

4. No `templates/dashboard.html`, na tabela de atrasados, um botão:

   ```html
   <a class="btn btn-ok" target="_blank" rel="noopener"
      th:href="${@mensagemDeCobranca.linkWhatsApp(d)}">Cobrar</a>
   ```

   O `@nomeDoBean` chama um componente Spring direto do template. Truque útil.

5. **Trate o telefone vazio** — `p.telefone()` pode ser `null`. Esconda o botão
   com `th:if="${d.telefone() != null}"`.

6. Um teste para a normalização do número:

   ```java
   assertThat(mensagem.linkWhatsApp(cliente("(83) 99999-8888")))
       .startsWith("https://wa.me/5583999998888");
   ```

**Esforço:** uma tarde. **Ganho:** alto.

---

## F2. Fechamento do mês

Hoje o painel mostra "recebido no mês" e só. Não dá para responder "quanto
entrou em julho?".

### Passo a passo

1. Em `PagamentoRepository`, uma consulta por período:

   ```java
   @Query("select p from Pagamento p join fetch p.cobranca c join fetch c.cliente " +
          "where p.dataPagamento between :de and :ate order by p.dataPagamento desc")
   List<Pagamento> doPeriodo(LocalDate de, LocalDate ate);
   ```
   Repare no `join fetch` — sem ele dá `LazyInitializationException` ao ler o
   nome do cliente no template.

2. Um método no `CarteiraService` que soma e devolve o total mais a lista

3. `web/RelatorioController.java`, com `@GetMapping("/relatorios")` e dois
   parâmetros de data

4. `templates/relatorios/mensal.html`, copiando a estrutura de outra tela

5. Link no menu, em `fragmentos/layout.html`

**Extra que vale:** botão de exportar CSV. É um `@GetMapping` que devolve
`text/csv` com `Content-Disposition: attachment`. Resolve o contador.

**Esforço:** um ou dois dias.

---

## F3. Obrigar a troca da senha no primeiro acesso

Hoje o `DataSeeder` cria o usuário e grita no log enquanto a senha for a padrão.
Log a gente para de ler.

### Passo a passo

1. Migration `V3__usuario_precisa_trocar_senha.sql`:

   ```sql
   ALTER TABLE usuario ADD COLUMN precisa_trocar_senha BOOLEAN NOT NULL DEFAULT FALSE;
   ```

2. Campo em `domain/Usuario.java`, com getter e setter

3. `UsuarioService.criar(...)` passa a marcar `true`

4. Um `HandlerInterceptor` que, se o usuário logado tiver a marca, redireciona
   tudo para `/trocar-senha` (menos a própria tela e os arquivos estáticos —
   senão você cria um laço infinito)

5. Tela simples: senha nova, confirmação, e desmarcar a flag ao salvar

**Cuidado:** esse é o item com mais chance de te trancar para fora. Teste
**local** antes de mandar para produção.

**Esforço:** um dia.

---

## F4. Lixeira em vez de exclusão definitiva

`ClienteService.excluir` apaga o cliente, as dívidas e os pagamentos, sem volta.
Só o `confirm()` do navegador separa um clique errado da perda do histórico.
E `Cliente` já tem o campo `ativo`, hoje quase sem uso.

### Passo a passo

1. `ClienteService.excluir` passa a fazer `cliente.setAtivo(false)`
2. `ClienteRepository`: filtrar por `ativo = true` nas listagens
3. Tela de arquivados, com botão restaurar
4. Exclusão definitiva só para ADMIN, com `@PreAuthorize`

**Esforço:** meio dia.

---

# A ordem que eu sugiro

| # | O quê | Tempo | Por que nessa ordem |
|---|---|---|---|
| 1 | **M1** — apagar as 2 consultas mortas | 5 min | ciclo completo sem risco |
| 2 | **F1** — botão WhatsApp | 1 tarde | o que mais te ajuda no dia a dia |
| 3 | **M4** — 4 testes de tela | 1 h | segura as regressões antes de crescer |
| 4 | **M2** — carteira montada uma vez | horas | a que mais ensina |
| 5 | **F2** — fechamento do mês | 1–2 dias | você vai querer no fim do mês |
| 6 | **F4** — lixeira | meio dia | rede de proteção |
| 7 | **F3** — troca de senha obrigatória | 1 dia | segurança, quando houver mais gente |

Os itens 8 em diante estão no [ROADMAP.md](ROADMAP.md): auditoria, permissão
por sócio, JWT, PWA e lembrete automático.

---

## Três coisas que eu não faria agora

**Multi-tenant.** Só quando você tiver cliente pagando. Enquanto for você e seu
sócio, é complexidade sem retorno — e o caminho mais barato até lá é uma
instância por cliente, com o `docker-compose.yml` que já está pronto.

**Trocar Thymeleaf por React.** Ganharia um build de front e uma API a manter,
e o site já é responsivo e carrega instantâneo.

**Guardar as parcelas no banco.** A tentação vem quando alguém pensa "seria mais
fácil consultar". Seria — e aí mudar o dia do vencimento viraria migração de
dados, e parcela gravada passaria a divergir da regra. O cálculo custa
microssegundos. Não faça.
