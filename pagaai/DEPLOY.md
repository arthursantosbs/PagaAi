# Colocar o Paga aí no ar

O app já está preparado para produção: roda em container, usa PostgreSQL, lê os
segredos do ambiente e **se recusa a subir** com senha padrão ou variável faltando.

Escolha **um** dos caminhos abaixo.

---

## Caminho 1 — Servidor próprio com Docker Compose (recomendado)

Você sobe banco, aplicação e HTTPS automático de uma vez. Não dorme por
inatividade, o backup é seu, e o custo é o do servidor.

**Você vai precisar de:** um servidor Linux pequeno (1 vCPU / 2 GB já roda de
sobra) e um domínio apontando para o IP dele.
Serve qualquer provedor — Hostinger, Contabo, DigitalOcean, Hetzner, ou a camada
Always Free da Oracle Cloud. Confira os preços atuais, eles mudam.

### 1. Apontar o domínio

No painel do seu domínio, crie um registro **A**:

| Tipo | Nome | Valor |
|---|---|---|
| A | `pagaai` | IP do servidor |

Isso te dá `pagaai.seudominio.com.br`. Espere alguns minutos até propagar — o
Caddy só consegue emitir o certificado depois que o domínio resolve.

### 2. Instalar o Docker no servidor

```bash
curl -fsSL https://get.docker.com | sh
```

### 3. Enviar a pasta do projeto

Do seu Windows, dentro de `Desktop\PagaAi`:

```bash
scp -r pagaai usuario@IP_DO_SERVIDOR:~/pagaai
```

### 4. Configurar os segredos

No servidor, dentro de `~/pagaai`:

```bash
cp .env.example .env
nano .env
```

Preencha `DOMINIO`, `DB_PASSWORD` e `ADMIN_SENHA` com valores próprios. Deixe
`COOKIE_SECURE=true`. O `.env` fica só no servidor — está no `.gitignore`.

### 5. Subir

```bash
docker compose up -d --build
```

A primeira vez demora alguns minutos (compila o Java dentro do container).
Acompanhe com `docker compose logs -f app`.

Abra `https://pagaai.seudominio.com.br` e entre com o `ADMIN_LOGIN` e a
`ADMIN_SENHA` que você definiu.

### Manutenção

```bash
docker compose logs -f app          # ver o que está acontecendo
docker compose restart app          # reiniciar
docker compose up -d --build        # aplicar uma alteração no código
```

### Backup

Já é automático. O serviço `backup` do compose roda um `pg_dump` comprimido por
dia em `./backups`, e apaga o que passar de `BACKUP_DIAS` (padrão 30).

```bash
docker compose logs backup     # confirmar que está rodando
ls -lh backups/                # ver os arquivos
```

**Duas coisas que o compose não faz por você:**

1. **Copiar para fora do servidor.** Backup no mesmo disco do banco não protege
   contra perder o disco. Puxe para a sua máquina de tempos em tempos:
   ```bash
   scp usuario@IP_DO_SERVIDOR:~/pagaai/backups/*.sql.gz ./
   ```
2. **Testar a restauração.** Backup nunca restaurado não conta como backup:
   ```bash
   gunzip -c backups/pagaai-2026-08-13-0300.sql.gz | docker compose exec -T db psql -U pagaai -d pagaai
   ```

---

## Caminho 2 — Render (sem mexer em servidor)

Deploy direto do GitHub, sem terminal. O plano gratuito **dorme após inatividade**
(a primeira visita depois de um tempo parado demora cerca de um minuto) e o
PostgreSQL gratuito tem prazo de validade — para uso diário de verdade, vira plano
pago. Confira os preços e limites atuais no site deles.

1. Suba o código para o GitHub. O `.gitignore` já mantém `.env`, banco e
   `target/` fora.
2. No Render: **New → Blueprint** e escolha o repositório.
   - O `render.yaml` fica na **raiz** do repositório — é onde o Render procura.
     Deixe o campo *Blueprint Path* vazio.
   - Dentro dele, `dockerContext: ./pagaai` é o que faz o build achar o código.
   - Preencha só o **Blueprint Name** (qualquer nome) e confirme a branch `main`.
3. Quando ele pedir `ADMIN_SENHA`, digite uma senha sua. É a única que não fica no
   repositório, e é com ela que você vai entrar.
4. O health check já está configurado em `/actuator/health`.

**Se o Blueprint reclamar do formato** (o schema do Render muda de tempos em
tempos), use o caminho manual, que dá no mesmo:

- **New → Postgres**, crie um banco e anote host, porta, base, usuário e senha.
- **New → Web Service → Docker**, aponte para o repositório e configure
  **Root Directory = `pagaai`**.
- Em *Environment*, preencha à mão: `SPRING_PROFILES_ACTIVE=prod`, `DB_HOST`,
  `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `ADMIN_SENHA`.
- Em *Health Check Path*, coloque `/actuator/health`.

O Render entrega HTTPS pronto no domínio `.onrender.com`, então `COOKIE_SECURE`
pode ficar no padrão (`true`).

---

## Variáveis de ambiente

| Variável | Obrigatória | Para que serve |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | sim | tem que ser `prod` |
| `DB_URL` | sim* | URL JDBC completa (`jdbc:postgresql://host:5432/pagaai`) |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | sim* | alternativa ao `DB_URL`, quando a plataforma entrega separado |
| `DB_USER`, `DB_PASSWORD` | sim | credenciais do banco |
| `ADMIN_LOGIN` | não | padrão `HERA123` |
| `ADMIN_SENHA` | **sim** | senha do primeiro acesso; o app não sobe sem ela |
| `COOKIE_SECURE` | não | padrão `true`. Só use `false` para testar por http |
| `PORT` | não | a plataforma injeta; padrão 8080 |

\* Use `DB_URL` **ou** o trio `DB_HOST`/`DB_PORT`/`DB_NAME`.

---

## O que o app recusa fazer

Verificado em teste, não é promessa:

- **Sem `ADMIN_SENHA`** o app não sobe. Isso evita o caso silencioso em que a senha
  do administrador viraria o texto literal `${ADMIN_SENHA}` — uma senha que
  qualquer pessoa que leia o repositório adivinha.
- **Com `ADMIN_SENHA=12345678` no perfil `prod`** o app não sobe. A senha padrão
  serve só para o desenvolvimento na sua máquina.
- **Senha errada na API** devolve `401`, nunca a página de login. Um app mobile que
  recebesse o redirecionamento acharia que o login deu certo.

Enquanto o usuário administrador continuar com a senha padrão, o log grita um aviso
a cada inicialização.

---

## Depois que estiver no ar

1. Entre e troque a senha em **Usuários → Trocar senha**.
2. Crie o login do seu sócio na mesma tela, com papel **Sócio**.
3. Configure o backup do banco (Caminho 1) ou confirme que a plataforma faz
   (Caminho 2).
4. Quando o sistema crescer, troque `ddl-auto: update` por migrations do Flyway —
   hoje o Hibernate altera o schema sozinho, o que é prático agora e arriscado
   quando houver dados que você não pode perder.
