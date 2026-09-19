# Data Model: Operação Comercial V1

## Modeling Principles

- PostgreSQL é a fonte de verdade do estado publicado; Cloudinary mantém somente o conteúdo dos
  assets.
- IDs são UUIDs gerados pela aplicação. Instantes são gravados em UTC com `timestamptz`.
- O modelo não contém preço, estoque, tamanho, pedido, pagamento ou venda.
- Exclusão de produto e categoria não faz parte da V1. Produto sai do catálogo por status.
- Campos derivados, como lembrete vencido ou acionável, não são persistidos.

## Domain Boundaries

### Identity and Customer

`Account` representa a identidade e o papel internos. `AccountExternalIdentity` vincula uma conta ao
Google sem transferir ao provedor a autoridade sobre papéis. `RefreshToken` representa uma
credencial renovável rotativa. `OAuthHandoff` representa somente a passagem temporária e de uso único
entre o callback Google e o frontend. `Customer` representa a pessoa e seu contexto de relacionamento.
A única conta `ADMIN` não precisa de um registro `Customer`.

### Catalog

`Product` é a raiz responsável por status e invariantes de suas `ProductImage`. `Category` organiza
produtos, mas mantém ciclo de vida independente.

### Interest

`Interest` é um registro imutável de uma confirmação feita por cliente autenticado para produto
ativo. Ele preserva o vínculo mesmo se o produto se tornar inativo depois.

### CRM

`ContactRecord` representa tanto o registro pendente de um atendimento solicitado quanto um contato
já concluído. `Reminder` representa uma ação futura e calcula atraso e acionabilidade usando data,
estado, origem, propósito e consentimento atual do cliente.

## Persistent Models

### `account`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `email` | varchar(320) | Valor original normalizado para apresentação |
| `normalized_email` | varchar(320) | Lowercase/trim, unique, required |
| `password_hash` | varchar(255) | Hash com identificador do encoder; nullable para conta somente Google |
| `role` | varchar(20) | `CLIENT` ou `ADMIN`, required |
| `enabled` | boolean | Default `true`; desativação não ganha fluxo público na V1 |
| `created_at` | timestamptz | Required |
| `updated_at` | timestamptz | Required |

Constraints:

- `UNIQUE(normalized_email)`.
- Check de papel limitado a `CLIENT` e `ADMIN`.
- Índice único parcial para no máximo uma linha com `role = 'ADMIN'`.
- Cadastro público sempre força `CLIENT`; papel recebido na entrada é ignorado/rejeitado.

### `account_external_identity`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `account_id` | UUID | FK para `account`, required |
| `provider` | varchar(30) | `GOOGLE` na V1, required |
| `subject` | varchar(255) | Valor `sub` validado no provedor, required |
| `email_at_link` | varchar(320) | E-mail verificado observado no vínculo, required |
| `created_at` | timestamptz | Required |
| `last_login_at` | timestamptz | Required |

`UNIQUE(provider, subject)` impede que a mesma identidade autentique mais de uma conta. O papel nunca
é derivado do e-mail: `ADMIN` só pode ser criado/autenticado quando `subject` e e-mail verificado
correspondem à configuração operacional autorizada.

Para clientes, `email_at_link` é evidência de auditoria do momento do vínculo, não uma chave de
identidade. Um `subject` inédito nunca é associado automaticamente a uma `Account` existente pela
coincidência desse e-mail. Se o e-mail já estiver ocupado sem vínculo com o `subject`, o fluxo é
rejeitado; vinculação manual não pertence à V1.

### `refresh_token`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key e identificador interno |
| `account_id` | UUID | FK para `account`, required |
| `family_id` | UUID | Identifica a cadeia de rotações, required |
| `jti_hash` | varchar(255) | Hash do `jti` do refresh JWT, unique, required; token bruto nunca persistido |
| `expires_at` | timestamptz | Required |
| `consumed_at` | timestamptz | Preenchido no primeiro refresh; nullable |
| `revoked_at` | timestamptz | Preenchido no logout, reuso ou revogação operacional; nullable |
| `replaced_by_id` | UUID | FK nullable para o token sucessor |
| `created_at` | timestamptz | Required |

Um token é utilizável somente se não expirou, não foi consumido nem revogado e a conta está ativa.
A rotação consome o atual e cria o sucessor na mesma transação. Reuso de token consumido revoga todos
os tokens ainda ativos da família.

### `oauth_handoff`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key interno |
| `handle_hash` | varchar(255) | Hash do valor opaco enviado somente em cookie, unique, required |
| `purpose` | varchar(40) | `EXISTING_ACCOUNT_LOGIN` ou `CLIENT_REGISTRATION`, required |
| `account_id` | UUID | FK nullable; required para login de conta existente |
| `provider` | varchar(30) | `GOOGLE` na V1, required |
| `provider_subject` | varchar(255) | `sub` validado, required |
| `verified_email` | varchar(320) | E-mail validado no provedor, required |
| `given_name` | varchar(100) | Nullable; sugestão para concluir cadastro |
| `family_name` | varchar(100) | Nullable; sugestão para concluir cadastro |
| `expires_at` | timestamptz | Required; no máximo 10 minutos após criação |
| `consumed_at` | timestamptz | Preenchido atomicamente no uso; nullable |
| `created_at` | timestamptz | Required |

O valor bruto nunca é persistido, retornado em JSON ou colocado na URL. O handoff não concede acesso
a recursos e só pode ser consumido pelo endpoint compatível com seu `purpose`, acompanhado da
proteção CSRF. Expiração ou consumo impede reutilização.

`verified_email` permite preencher o novo cadastro ou detectar conflito com uma conta existente; ele
não autoriza a criação de handoff para essa conta nem a criação de `AccountExternalIdentity`.

### `customer`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `account_id` | UUID | FK unique para `account`, required e papel `CLIENT` validado pela aplicação |
| `first_name` | varchar(100) | Required, trimmed |
| `last_name` | varchar(100) | Required, trimmed |
| `whatsapp_phone` | varchar(20) | Required, normalizado para E.164 |
| `proactive_contact_authorized` | boolean | Required, default `false`; nunca concedido por cadastro |
| `contact_consent_changed_at` | timestamptz | Instante da última decisão explícita; nullable até a primeira ação |
| `created_at` | timestamptz | Required |
| `updated_at` | timestamptz | Required |

O atendimento solicitado pelo cliente não depende de `proactive_contact_authorized`. A flag é
consultada somente para novos contatos comerciais iniciados pela loja.

### `category`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `name` | varchar(120) | Required, trimmed |
| `normalized_name` | varchar(120) | Unique, required |
| `created_at` | timestamptz | Required |
| `updated_at` | timestamptz | Required |

Não há exclusão na V1. Renomear deve manter a unicidade após normalização.

### `product`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `category_id` | UUID | FK para `category`, required, `ON DELETE RESTRICT` |
| `name` | varchar(160) | Required, trimmed |
| `description` | text | Required, conteúdo não vazio, limite de aplicação |
| `status` | varchar(20) | `ACTIVE` ou `INACTIVE`, required |
| `created_at` | timestamptz | Required |
| `updated_at` | timestamptz | Required |

Um produto só pode ser persistido ou permanecer `ACTIVE` com uma ou mais imagens válidas e
exatamente uma principal. Essas regras entre linhas ficam no domínio e no caso de uso.

### `product_image`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `product_id` | UUID | FK para `product`, required, `ON DELETE RESTRICT` |
| `external_id` | varchar(255) | Identificador do asset no Cloudinary, unique, required |
| `secure_url` | varchar(2048) | URL HTTPS retornada pelo provedor, required |
| `is_primary` | boolean | Required, default `false` |
| `display_order` | integer | Required, non-negative |
| `created_at` | timestamptz | Required |

Um índice único parcial em `product_id WHERE is_primary = true` garante no máximo uma principal. O
caso de uso garante pelo menos uma. Cada `ProductImage` mantém seu `Product`, URL, `externalId`, flag
principal e ordem. Consultas de produto entregam ao frontend as URLs ordenadas; o identificador
externo permanece interno e operações administrativas usam o ID local da imagem.

### `interest`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `customer_id` | UUID | FK para `customer`, required |
| `product_id` | UUID | FK para `product`, required |
| `idempotency_key` | varchar(128) | Required, valor opaco validado |
| `created_at` | timestamptz | Required |

`UNIQUE(customer_id, idempotency_key)` protege uma confirmação contra repetição. Ao repetir a chave,
o caso de uso compara `product_id`: se for igual, devolve o registro existente; se for diferente,
retorna conflito. Não há unicidade em cliente/produto, pois um novo interesse futuro é permitido.

### `contact_record`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `customer_id` | UUID | FK para `customer`, required |
| `interest_id` | UUID | FK unique nullable; preenchida no registro automático |
| `reminder_id` | UUID | FK unique nullable; preenchida no registro automático |
| `status` | varchar(20) | `PENDING` ou `COMPLETED`, required |
| `occurred_at` | timestamptz | Data efetiva da interação; nullable enquanto pendente |
| `channel` | varchar(80) | Texto controlado/validado, required |
| `description` | varchar(2000) | Nullable enquanto pendente; required e trimmed ao concluir |
| `completed_at` | timestamptz | Required somente em `COMPLETED` |
| `created_at` | timestamptz | Momento do registro, required |
| `updated_at` | timestamptz | Required |

Registros manuais nascem `COMPLETED` com data, canal e descrição. O fluxo de interesse cria um
registro `PENDING`, canal `WHATSAPP`, sem afirmar que houve conversa. Ao concluir, a administradora
informa os dados finais e o registro passa ao histórico. Listagens distinguem status; concluídos são
ordenados por `occurred_at DESC, id DESC`, e pendentes por `created_at DESC, id DESC`. Edição posterior,
reabertura e exclusão não fazem parte da V1.

### `reminder`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `customer_id` | UUID | FK para `customer`, required |
| `interest_id` | UUID | FK unique nullable; preenchida somente no acompanhamento automático |
| `description` | varchar(1000) | Required, trimmed |
| `due_at` | timestamptz | Required |
| `purpose` | varchar(40) | `PROACTIVE_CONTACT` ou `CUSTOMER_REQUEST_FOLLOW_UP` |
| `origin` | varchar(20) | `MANUAL` ou `INTEREST`, required |
| `status` | varchar(20) | `PENDING` ou `COMPLETED`, required |
| `completed_at` | timestamptz | Required somente em `COMPLETED` |
| `created_at` | timestamptz | Required |
| `updated_at` | timestamptz | Required |

Derivations:

- `overdue = status == PENDING && due_at < now`.
- `actionable = status == PENDING && (purpose != PROACTIVE_CONTACT || customer consent is true)`.
- Revogar consentimento não altera `status`; muda imediatamente `actionable` para `false` nos
  lembretes proativos.
- Um lembrete `INTEREST` usa `CUSTOMER_REQUEST_FOLLOW_UP`, nasce pendente e acionável com `due_at`
  igual à data do interesse e só é concluído junto com seu `ContactRecord`.

## Relationships

```text
Account 1 ─── 0..* AccountExternalIdentity
Account 1 ─── 0..* RefreshToken
Account 1 ─── 0..* OAuthHandoff (somente login de conta existente)
Account 1 ─── 0..1 Customer
Category 1 ─── * Product
Product 1 ─── 1..* ProductImage
Customer 1 ─── * Interest * ─── 1 Product
Customer 1 ─── * ContactRecord
Customer 1 ─── * Reminder
Interest 1 ─── 1 Reminder (acompanhamento automático)
Interest 1 ─── 1 ContactRecord (acompanhamento automático)
Reminder 1 ─── 1 ContactRecord (quando originado por interesse)
```

## State Transitions

### Product

```text
INACTIVE ── activate (images valid + exactly one primary) ──> ACTIVE
ACTIVE   ── deactivate ─────────────────────────────────────> INACTIVE
```

Produto ativo pode receber alterações desde que a transação preserve as invariantes. Produto
inativo não aparece no catálogo e não aceita novo interesse.

### Contact consent

```text
NOT_AUTHORIZED (default) ── affirmative opt-in ──> AUTHORIZED
AUTHORIZED ── revoke ───────────────────────────> NOT_AUTHORIZED
NOT_AUTHORIZED ── revoke again ─────────────────> NOT_AUTHORIZED (idempotent)
AUTHORIZED ── grant again ──────────────────────> AUTHORIZED (idempotent)
```

### Reminder

```text
PENDING ── complete ──> COMPLETED
```

`OVERDUE` e `NOT_ACTIONABLE` são condições derivadas, não estados persistidos. Lembrete `MANUAL` é
concluído diretamente; lembrete `INTEREST` só muda para `COMPLETED` na transição conjunta do contato.
Reabertura, edição e exclusão não pertencem à V1.

### ContactRecord

```text
PENDING ── complete with occurredAt + description ──> COMPLETED
```

Contato manual nasce `COMPLETED`. Um contato automático nasce `PENDING`, vinculado ao interesse e
ao lembrete, e sua conclusão também conclui esse lembrete. O estado pendente não é evidência de
conversa realizada.

### RefreshToken

```text
ACTIVE ── refresh ──> CONSUMED + successor ACTIVE
ACTIVE ── logout/administrative revoke ──> REVOKED
CONSUMED ── reuse detected ──> family REVOKED
ACTIVE ── expires ──> EXPIRED (derived)
```

### OAuthHandoff

```text
ACTIVE ── exchange/complete registration ──> CONSUMED
ACTIVE ── timeout ─────────────────────────> EXPIRED (derived)
```

Não existe transição de handoff para sessão ou autenticação de recurso. Somente seu consumo bem
sucedido permite emitir os tokens próprios da aplicação.

### Interest

Interesse não muda de estado nem é removido na V1. A repetição idempotente recupera o mesmo registro.

## Transaction Boundaries

- Conta e cliente são criados atomicamente.
- Vínculo Google e eventual criação de `Account`/`Customer` são atômicos; a constraint da
  administradora permanece a autoridade final contra concorrência.
- Criação do handoff ocorre somente após validação completa do callback Google. Seu consumo usa lock,
  marca `consumed_at` e emite tokens ou cria `Account`/`Customer` na mesma transação lógica, impedindo
  reutilização concorrente.
- Rotação consome o refresh token atual e cria seu sucessor em uma única transação com lock; detecção
  de reuso revoga a família atomicamente.
- Consentimento é alterado em uma transação curta; leituras de lembretes calculam acionabilidade
  com o valor já confirmado.
- Produto e referências de todas as imagens da criação são persistidos juntos.
- Troca de principal remove e define a flag na mesma transação, com lock no produto.
- Interesse, lembrete `INTEREST` e `ContactRecord` pendente são persistidos na mesma transação; as
  constraints de idempotência e unicidade por interesse são a autoridade final contra concorrência.
- Conclusão de `ContactRecord` automático preenche seus dados finais e altera contato e lembrete para
  `COMPLETED`, com os dois `completed_at`, na mesma transação.
- Conclusão direta de lembrete é permitida apenas para `origin = MANUAL`.

## Indexes

- `account(normalized_email)` unique.
- `account(role) WHERE role = 'ADMIN'` unique partial.
- `account_external_identity(provider, subject)` unique e índice por `account_id`.
- `refresh_token(jti_hash)` unique; índices por `account_id`, `family_id` e `expires_at`.
- `oauth_handoff(handle_hash)` unique; índices por `expires_at` e `account_id` quando não nulo.
- `category(normalized_name)` unique.
- `product(status, created_at DESC)` para catálogo.
- `product(category_id)` para gestão.
- `product_image(product_id, display_order)` e índice parcial da principal.
- `product_image(external_id)` unique.
- `interest(customer_id, created_at DESC)` e unique de idempotência.
- `contact_record(interest_id)` e `contact_record(reminder_id)` unique quando não nulos;
  `contact_record(customer_id, status, occurred_at DESC)`.
- `reminder(status, due_at)` e `reminder(customer_id, due_at DESC)`.
- `reminder(interest_id)` unique quando não nulo.

## Migration Plan

1. `V1__create_identity_customers_and_tokens.sql`
2. `V2__create_catalog.sql`
3. `V3__create_interests_crm_and_follow_up_links.sql`
4. `V4__create_operational_indexes.sql`

`V1` inclui identidades externas, refresh tokens e handoffs OAuth temporários; `V4` inclui os índices
de expiração e busca operacional desses registros.

Após serem aplicadas fora do ambiente local descartável, migrations não são editadas. Qualquer
ajuste recebe uma nova versão.
