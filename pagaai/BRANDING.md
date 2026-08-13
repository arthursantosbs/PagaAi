# Paga aí — identidade

Guia curto e aplicável. Tudo que está aqui já está no código, em
`src/main/resources/static/css/app.css` e `templates/fragmentos/layout.html`.

---

## 1. Posicionamento

> **Paga aí é o caderninho do fiado que faz a conta sozinho.**

Para quem vende fiado e cobra pelo WhatsApp: o dono e o sócio, não uma equipe de
cobrança. Duas pessoas, um celular, uma pergunta o dia inteiro — *quem me deve, e
há quanto tempo?*

**O que o Paga aí não é:**

- não é banco — não emite boleto, não calcula juros, não faz análise de crédito;
- não é ERP — não controla estoque nem emite nota;
- não é robô de cobrança — ele te diz quem cobrar; quem manda a mensagem é você.

Essa fronteira é o que mantém o sistema simples. Toda ideia nova deveria passar
por ela antes de virar código.

---

## 2. Nome

| Onde | Como escrever |
|---|---|
| Texto corrido, telas, documentação | **Paga aí** (com acento, duas palavras) |
| Nome técnico, pacote, arquivo, domínio | `pagaai` (tudo junto, minúsculo, sem acento) |
| Assinatura visual | **Paga**<span style="color:#0f766e">aí</span> — "Paga" neutro, "aí" na cor da marca |

Nunca: "PagaAí" camelCase em texto, "PAGA AÍ" em caixa alta, "Paga Ai" sem acento
em texto voltado ao usuário.

---

## 3. Paleta

O verde-azulado é a marca. O resto é **semáforo de atraso** — e essa é a decisão
de cor mais importante do produto: a cor não é decoração, é informação.

### Marca

| Token CSS | Hex | Uso |
|---|---|---|
| `--marca` | `#0f766e` | botão principal, links, "aí" da assinatura, arco do símbolo |
| `--marca-escura` | `#0b5c55` | hover, texto sobre fundo claro da marca |
| `--marca-clara` | `#e6f4f1` | fundo do item ativo no menu, trilho do símbolo |

### Semáforo de atraso

| Token | Hex | Significa |
|---|---|---|
| `--ok` | `#15803d` | quitado, pago, dinheiro que entrou |
| `--alerta` | `#b45309` | atraso de 1 a 7 dias |
| `--atencao` | `#ea580c` | atraso de 8 a 30 dias |
| `--perigo` | `#b91c1c` | atraso acima de 30 dias, ações destrutivas |

A escada vive em `dto/Severidade.java`. **Mudou a cor aqui, muda lá também** — ou
o CSS e o Java passam a discordar sobre o que é "grave".

### Neutros

| Token | Hex | Uso |
|---|---|---|
| `--texto` | `#14181f` | texto principal |
| `--texto-fraco` | `#667085` | rótulo, dado secundário |
| `--borda` | `#e3e6ea` | linha de tabela, contorno de campo |
| `--superficie` | `#ffffff` | cartão, bloco |
| `--fundo` | `#f6f7f9` | fundo da página |

### Contraste

Calculado par a par pela fórmula WCAG 2.1. O mínimo para texto normal é **4.5:1**.

| Par | Razão | AA |
|---|---|---|
| `--marca` sobre branco | 5.47:1 | ✅ |
| branco sobre `--marca` (botão) | 5.47:1 | ✅ |
| `--texto` sobre `--fundo` | ~16:1 | ✅ |
| `--texto-fraco` sobre branco | 5.04:1 | ✅ |
| `--ok` sobre branco | 5.02:1 | ✅ |
| `--alerta` sobre branco | 5.02:1 | ✅ |
| `--perigo` sobre branco | 6.47:1 | ✅ |
| etiqueta grave (`#991b1b` sobre `#fee2e2`) | 6.80:1 | ✅ |

Duas passam raspando (5.02:1). Se você escurecer algum fundo, **recalcule** —
não confie no olho.

E a regra que sustenta tudo: **nunca use só a cor para dizer algo**. Toda linha
colorida no sistema também traz o número de dias escrito. Uma pessoa daltônica
(8% dos homens) precisa da informação do mesmo jeito.

---

## 4. Tipografia

Mantida a *font stack* nativa:

```css
system-ui, -apple-system, "Segoe UI", Roboto, sans-serif
```

Defesa: zero download, zero atraso na primeira pintura, e o app já parece nativo
no celular — que é onde o dono vai abrir. Fonte customizada aqui só teria custo.

| Papel | Tamanho | Peso |
|---|---|---|
| Número de indicador | 1.5rem | 700 |
| Título de bloco | 1.05rem | 600 |
| Corpo | 15px | 400 |
| Rótulo, dado secundário | 0.8–0.88rem | 400–600 |
| Cabeçalho de tabela | 0.78rem, caixa alta, `letter-spacing: .04em` | 600 |

**Regra do dinheiro:** todo valor em coluna usa `font-variant-numeric: tabular-nums`
e alinhamento à direita. Sem isso os algarismos têm larguras diferentes, a vírgula
não alinha entre as linhas e comparar R$ 1.250,00 com R$ 980,00 de relance fica
impossível — que é justamente o que o dono faz o dia inteiro.

---

## 5. Logo

**Conceito:** o símbolo é a própria métrica do produto. Um anel = o valor total da
dívida; o arco preenchido = quanto já foi pago. É a mesma barra de progresso que
aparece em cada dívida, fechada em círculo.

### Símbolo

```svg
<svg viewBox="0 0 32 32" role="img" aria-label="Paga aí">
  <circle cx="16" cy="16" r="13" fill="none" stroke="currentColor"
          stroke-opacity=".22" stroke-width="6"/>
  <circle cx="16" cy="16" r="13" fill="none" stroke="currentColor" stroke-width="6"
          stroke-linecap="round" stroke-dasharray="57 82" transform="rotate(-90 16 16)"/>
</svg>
```

Usa `currentColor` nos dois traços: herda a cor de quem o contém, então funciona
em fundo claro e escuro sem segunda versão. O trilho é o mesmo traço com 22% de
opacidade. A 24px continua legível porque só tem duas formas.

`stroke-dasharray="57 82"` = 70% do perímetro (2π×13 ≈ 82). Não é número mágico:
é "a maior parte paga, ainda falta um pedaço" — a situação normal de quem usa.

### Assinatura horizontal

Símbolo + palavra, usada na barra de navegação e no login. A palavra é **texto
HTML**, não caminho em SVG: escala com o zoom, é lida por leitor de tela e é
selecionável.

```html
<a class="marca" href="/">
  <svg class="marca-simbolo" ...>…</svg>
  <span>Paga<b>aí</b></span>
</a>
```

### Favicon

O mesmo símbolo, embutido como `data:` URI no `<head>` — sem arquivo, sem
requisição extra, sem CDN.

---

## 6. Voz

Direto, sem juridiquês, sem susto. O sistema fala como um sócio experiente
falaria: constata o fato e diz o que fazer.

| Não escreva | Escreva | Por quê |
|---|---|---|
| "Inadimplente" | "Atrasado" | ninguém fala "inadimplente" no balcão |
| "Não há registros" | "Ninguém atrasado. Tudo dentro do prazo." | diz o que aconteceu, não o que faltou |
| "Título liquidado" | "Dívida quitada. Não gera mais cobrança." | fala o efeito prático |
| "Baixa efetuada" | "Pagamento registrado." | verbo que o dono usa |
| "Aging da carteira" | "Há quantos dias está atrasado" | sem jargão importado |
| "Valor em aberto" | "Falta pagar" | o dono pensa em "falta", não em "aberto" |
| "Cliente possui pendências" | "Deve R$ 320,00, sendo R$ 20,00 vencido" | número em vez de adjetivo |
| "Deseja realmente excluir?" | "Excluir esta dívida e todos os pagamentos dela?" | diz o que se perde |
| "Erro de validação" | "Informe um valor maior que zero" | diz como consertar |
| "Processando…" | "Registrar recebimento" | rótulo de botão é o que ele faz |

Duas frases já no sistema que definem o tom, e valem de referência:

- *"Passaram da data e ainda não pagaram. Cobre estes primeiro."* — prioriza para o dono.
- *"O valor pode ser parcial. O que faltar continua na conta."* — explica a regra antes do erro.

---

## 7. Modelos de mensagem de cobrança

Para o dono copiar e mandar no WhatsApp. Placeholders: `{cliente}`, `{valor}`,
`{data}`, `{dias}`, `{descricao}`.

> ⚖️ **Limite legal.** O Código de Defesa do Consumidor (art. 42) proíbe expor o
> devedor ao ridículo ou constrangê-lo. Na prática: cobre em conversa privada,
> nunca em grupo, nunca no story, nunca falando com parente ou vizinho, e nunca
> ameace. Os textos abaixo respeitam isso — se for reescrever, mantenha a regra.

**Lembrete, 2 dias antes**
> Oi {cliente}, tudo bem? Passando pra lembrar que {descricao} vence dia {data},
> R$ {valor}. Qualquer coisa é só me chamar. 👍

**Primeira cobrança, logo depois de vencer**
> Oi {cliente}! O pagamento de {descricao} venceu dia {data} e ainda não caiu
> aqui — R$ {valor}. Consegue acertar essa semana? Se ficar apertado, me fala que
> a gente combina.

**Atraso longo**
> {cliente}, tudo bem? Estou com R$ {valor} em aberto do {descricao}, atrasado há
> {dias} dias. Preciso resolver isso. Você prefere pagar de uma vez ou dividir?
> Me diz o que dá pra fazer que eu te ajudo a encaixar.

**Quitação**
> {cliente}, recebido! Sua conta do {descricao} está quitada, zerada aqui. 🙌
> Obrigado pela parceria — quando precisar, é só chamar.

O tom da terceira é a chave: firme no fato ("preciso resolver isso"), aberto na
saída ("o que dá pra fazer"). Cliente que some é cliente sem opção — oferecer
parcelamento recupera mais do que ameaça.
