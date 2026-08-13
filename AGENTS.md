# Paga aí

O sistema fica em `pagaai/` — Java 17 + Spring Boot 3.3, Thymeleaf nas telas,
H2 em arquivo no dev e PostgreSQL no perfil `prod`. Leia `pagaai/README.md` e
`pagaai/ARQUITETURA.md` antes de mexer.

Regras do projeto:

- O modelo é de **saldo**, não de parcela marcada como paga. `Cobranca` guarda
  `valorTotal` + `valorParcela`; as parcelas são calculadas por `CalendarioCobranca`
  e param quando cobrem o total. `Pagamento` é só valor + data, sem vínculo com
  parcela — `CalculadoraDeDivida` aloca da mais antiga para a mais nova. É isso
  que faz pagamento parcial funcionar. Antes de criar tabela de parcelas, entenda isso.
- Mudou a regra de vencimento ou de alocação? Atualize `CalculadoraDeDividaTest` e
  `CalendarioCobrancaTest` junto.
- O schema é do **Flyway** (`src/main/resources/db/migration/`), não do Hibernate —
  `ddl-auto` é `validate`. Para alterar o banco, crie um `V2__descricao.sql` novo e
  ajuste a entidade junto. Nunca edite migration já aplicada: o checksum trava.
- `open-in-view` está desligado. Ao ler `cobranca.getCliente()` fora da transação,
  use `CobrancaService#buscarComCliente` — senão dá `LazyInitializationException`.
- Leituras de painel passam por `CarteiraService`, que monta tudo em duas consultas.
  Não faça consulta por cobrança dentro de laço.
- `/api/v1/**` é a API do futuro app mobile: cadeia de segurança própria
  (HTTP Basic, stateless, sem redirect para login). Não misture com a cadeia web.
- Texto de tela vai acentuado ("Dívida", "Cobrança"). Nome de classe CSS, `id`,
  `for`, rota e campo Java continuam sem acento.
- Build e testes:
  ```
  mvn -DskipTests package
  mvn test
  ```
  Nesta máquina não há `java` nem `mvn` no PATH: use `JAVA_HOME=%USERPROFILE%\.jdks\ms-17.0.20`
  e o Maven que vem com o IntelliJ, ou rode `pagaai\run.cmd`.
