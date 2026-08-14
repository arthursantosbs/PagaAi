# Paga aí — manual de uso

Para você e seu sócio. Nada de termo técnico.

**Endereço:** <https://paga-ai.onrender.com>

---

## 1. Entrar

1. Abra o endereço acima no navegador (celular ou computador, funciona nos dois)
2. Digite seu **usuário** e sua **senha**
3. Clique em **Entrar**

> **A primeira vez do dia demora até 1 minuto.** Não é travamento. O sistema
> "dorme" quando ninguém usa, para não custar nada, e leva esse tempo para
> acordar. Depois disso fica rápido.

**Dica:** salve o site na tela inicial do celular. No Chrome, menu ⋮ →
*Adicionar à tela inicial*. Fica com ícone, igual a um aplicativo.

---

## 2. As quatro telas

| Tela | Para quê |
|---|---|
| **Painel** | a primeira coisa que você vê: quem está devendo e quanto |
| **Clientes** | cadastro e a ficha de cada pessoa |
| **Dívidas** | tudo que está em aberto e a agenda do mês |
| **Usuários** | criar o login do sócio e trocar senhas |

---

## 3. Cadastrar um cliente

**Clientes** → **Novo cliente**

Só o **nome** é obrigatório. O resto você preenche se tiver e quiser.

O que vale a pena preencher:

- **Telefone** e **Instagram** — aparecem na lista de atrasados, para você cobrar
  sem precisar procurar o contato
- **Endereço** — se você entrega ou vai cobrar pessoalmente
- **Observações** — o que te ajudar a lembrar ("cliente do seu João", "paga
  sempre no dia 5")

Clique em **Salvar**. Cliente cadastrado não deve nada ainda — a dívida é o
próximo passo.

---

## 4. Lançar uma venda fiada

**Dívidas** → **Nova dívida** (ou, na ficha do cliente, **Nova dívida** — já vem
com ele selecionado)

Preencha:

- **Cliente** — quem comprou
- **O que ele comprou** — ex: "2 caixas de produto"
- **Tipo** — escolha **Dívida com valor total (fiado, venda parcelada)**
- **Valor total da dívida** — quanto ele deve no total, ex: `500`
- **Valor de cada pagamento** — quanto ele vai pagar por vez, ex: `100`.
  **Deixe em branco se ele vai pagar tudo de uma vez**
- **Com que frequência ele paga** — Mensal ou Semanal
- **Dia do mês** (ou dia da semana) — quando vence
- **Primeiro vencimento** — a data da primeira cobrança

Clique em **Salvar**.

### O que o sistema faz sozinho

Se você lançou **R$ 500, pagando R$ 100 por mês**, ele monta as 5 parcelas
sozinho. Quando a soma dos pagamentos chegar em R$ 500, **a dívida quita e para
de cobrar**. Você não precisa fechar nada na mão.

Se o valor não dividir certinho — R$ 500 em parcelas de R$ 150 — a última vem
quebrada: 150, 150, 150 e **50**. Nunca sobra nem falta centavo.

### E a mensalidade?

Se for algo sem fim, tipo uma mensalidade, escolha o tipo **Cobrança recorrente
sem fim**. Aí não tem valor total: ele cobra o mesmo valor todo mês, para sempre,
até você pausar.

---

## 5. Receber um pagamento

Este é o botão que você mais vai usar.

**Na ficha do cliente** (Clientes → clique no nome), cada dívida em aberto tem um
quadro **Valor recebido**.

1. O campo **já vem preenchido** com quanto falta para ele ficar em dia
2. **Se ele pagou menos, apague e digite o valor real**
3. Confira a data (vem a de hoje)
4. Clique em **Registrar recebimento**

### Pagamento parcial funciona de verdade

Ele devia R$ 100 e te pagou R$ 80? Digite `80`. O sistema:

- baixa os R$ 80
- deixa **R$ 20 em aberto**
- continua contando o atraso desde a data do vencimento

Não precisa inventar nada nem fazer conta na mão.

### Errou o lançamento?

Na mesma tela, embaixo, em **Pagamentos recebidos**, cada linha tem o botão
**Estornar**. Ele apaga aquele pagamento e as contas voltam ao que eram.

---

## 6. Ver quem cobrar hoje

Abra o **Painel**. Ele já vem organizado por urgência:

**Em cima, "Atrasados"** — quem passou da data e não pagou, do mais atrasado para
o menos. É aqui que você olha todo dia. Cada linha mostra:

- quanto está **atrasado**
- quanto ele deve **no total**
- **há quantos dias**
- o telefone e o Instagram, para você já chamar

**Embaixo, "Devedores em dia"** — quem deve mas ainda não venceu. Não precisa
cobrar, mas é bom saber que existe.

**Por último, "Vence nos próximos 7 dias"** — para você mandar um lembrete antes
de vencer. Cobrar antes evita cobrar depois.

### Os números do topo

| Indicador | O que quer dizer |
|---|---|
| **Total a receber** | tudo que o mundo te deve, vencido ou não |
| **Vencido e não pago** | só o que já passou da data — o problema de hoje |
| **Ainda vai vencer** | o que está combinado mas não chegou o dia |
| **Recebido no mês** | quanto entrou de dinheiro este mês |
| **Devedores** | quantas pessoas te devem |
| **Maior atraso** | o caso mais antigo, em dias |

### As cores

O sistema pinta por gravidade, mas **sempre escreve os dias junto** — você nunca
depende só da cor:

- **verde** — pago
- **amarelo** — até 7 dias de atraso
- **laranja** — de 8 a 30 dias
- **vermelho** — mais de 30 dias

---

## 7. Acompanhar uma dívida específica

Clique no nome da dívida, em qualquer lista. A tela mostra:

- uma **barra de progresso**: quanto já foi pago do total
- **quanto falta**, quanto está vencido, qual o próximo vencimento
- a lista de **todas as parcelas**, com quanto entrou em cada uma
- todos os **pagamentos** já registrados

No fim tem **Pausar cobrança** (para de gerar vencimento, mas guarda o histórico)
e **Excluir dívida** (apaga tudo, sem volta).

---

## 8. Criar o login do seu sócio

Só quem é Administrador vê essa tela.

**Usuários** → em **Adicionar sócio**:

- **Login** — o nome que ele vai digitar para entrar
- **Nome** — o nome dele
- **Senha** — mínimo 8 caracteres
- **Papel** — escolha **Sócio**

Clique em **Criar usuário** e passe o login e a senha para ele.

Vocês dois veem **exatamente as mesmas informações**. O que um lançar, o outro vê
na hora. Cada pagamento fica registrado com o nome de quem lançou.

Para trocar uma senha, use **Trocar senha** na mesma tela.

---

## 9. Dúvidas comuns

**Demorou quase um minuto para abrir.**
Normal. O sistema dorme quando ninguém usa. A partir do segundo acesso fica
rápido.

**Cadastrei o cliente e ele não aparece nos devedores.**
Cliente cadastrado não deve nada. Você precisa lançar uma **dívida** para ele
aparecer.

**A dívida sumiu da lista.**
Ela foi quitada — a soma dos pagamentos alcançou o valor total. Ela continua na
ficha do cliente, em **Dívidas quitadas**.

**O cliente pagou a mais.**
Pode registrar. O sistema quita a dívida e mostra separado quanto ele pagou a
mais.

**Lancei o pagamento no cliente errado.**
Abra a ficha dele, vá em **Pagamentos recebidos** e clique em **Estornar**.
Depois lance no certo.

**Esqueci minha senha.**
Se for a do sócio, você entra como Administrador e usa **Trocar senha**. Se for a
do Administrador, hoje só resolve mexendo no banco — fale com quem cuida do
sistema.

---

## 10. Uma coisa importante ao cobrar

O Código de Defesa do Consumidor proíbe cobrança que exponha ou constranja o
devedor. Na prática:

- cobre **em conversa privada**, nunca em grupo, nunca em story
- não fale da dívida com parente, vizinho ou patrão dele
- não ameace

Tem quatro modelos de mensagem prontos em `BRANDING.md` — lembrete antes de
vencer, primeira cobrança, atraso longo e agradecimento pela quitação. Todos
escritos dentro dessa regra.
