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

`Account` representa credenciais e papel. `Customer` representa a pessoa e seu contexto de
relacionamento. A única conta `ADMIN` não precisa de um registro `Customer`.

### Catalog

`Product` é a raiz responsável por status e invariantes de suas `ProductImage`. `Category` organiza
produtos, mas mantém ciclo de vida independente.

### Interest

`Interest` é um registro imutável de uma confirmação feita por cliente autenticado para produto
ativo. Ele preserva o vínculo mesmo se o produto se tornar inativo depois.

### CRM

`ContactRecord` registra contatos passados. `Reminder` representa uma ação futura e calcula atraso
e acionabilidade usando data, estado, propósito e consentimento atual do cliente.

## Persistent Models

### `account`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `email` | varchar(320) | Valor original normalizado para apresentação |
| `normalized_email` | varchar(320) | Lowercase/trim, unique, required |
| `password_hash` | varchar(255) | Hash com identificador do encoder, required |
| `role` | varchar(20) | `CLIENT` ou `ADMIN`, required |
| `enabled` | boolean | Default `true`; desativação não ganha fluxo público na V1 |
| `created_at` | timestamptz | Required |
| `updated_at` | timestamptz | Required |

Constraints:

- `UNIQUE(normalized_email)`.
- Check de papel limitado a `CLIENT` e `ADMIN`.
- Índice único parcial para no máximo uma linha com `role = 'ADMIN'`.
- Cadastro público sempre força `CLIENT`; papel recebido na entrada é ignorado/rejeitado.

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
caso de uso garante pelo menos uma. A URL e o identificador externo não são expostos juntos: o
catálogo recebe somente URL, enquanto operações administrativas usam o ID interno da imagem.

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
| `occurred_at` | timestamptz | Data informada da interação, required |
| `channel` | varchar(80) | Texto controlado/validado, required |
| `description` | varchar(2000) | Required, trimmed |
| `created_at` | timestamptz | Momento do registro, required |

Registros são manuais e ordenados por `occurred_at DESC, id DESC`. Edição e exclusão não fazem
parte da V1.

### `reminder`

| Field | Type | Rules |
|-------|------|-------|
| `id` | UUID | Primary key |
| `customer_id` | UUID | FK para `customer`, required |
| `description` | varchar(1000) | Required, trimmed |
| `due_at` | timestamptz | Required |
| `purpose` | varchar(40) | `PROACTIVE_CONTACT` ou `CUSTOMER_REQUEST_FOLLOW_UP` |
| `status` | varchar(20) | `PENDING` ou `COMPLETED`, required |
| `completed_at` | timestamptz | Required somente em `COMPLETED` |
| `created_at` | timestamptz | Required |
| `updated_at` | timestamptz | Required |

Derivations:

- `overdue = status == PENDING && due_at < now`.
- `actionable = status == PENDING && (purpose != PROACTIVE_CONTACT || customer consent is true)`.
- Revogar consentimento não altera `status`; muda imediatamente `actionable` para `false` nos
  lembretes proativos.

### Spring Session tables

As tabelas oficiais `SPRING_SESSION` e `SPRING_SESSION_ATTRIBUTES` são infraestrutura, não entidades
do domínio. Elas entram por migration Flyway e têm a inicialização automática do Spring Session
desabilitada. Sessões são localizáveis pelo principal para logout e revogação operacional.

## Relationships

```text
Account 1 ─── 0..1 Customer
Category 1 ─── * Product
Product 1 ─── 1..* ProductImage
Customer 1 ─── * Interest * ─── 1 Product
Customer 1 ─── * ContactRecord
Customer 1 ─── * Reminder
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

`OVERDUE` e `NOT_ACTIONABLE` são condições derivadas, não estados persistidos. Reabertura, edição e
exclusão não pertencem à V1.

### Interest

Interesse não muda de estado nem é removido na V1. A repetição idempotente recupera o mesmo registro.

## Transaction Boundaries

- Conta e cliente são criados atomicamente.
- Consentimento é alterado em uma transação curta; leituras de lembretes calculam acionabilidade
  com o valor já confirmado.
- Produto e referências de todas as imagens da criação são persistidos juntos.
- Troca de principal remove e define a flag na mesma transação, com lock no produto.
- Interesse é persistido com a constraint de idempotência como autoridade final contra concorrência.
- Conclusão do lembrete altera `status` e `completed_at` juntos.

## Indexes

- `account(normalized_email)` unique.
- `account(role) WHERE role = 'ADMIN'` unique partial.
- `category(normalized_name)` unique.
- `product(status, created_at DESC)` para catálogo.
- `product(category_id)` para gestão.
- `product_image(product_id, display_order)` e índice parcial da principal.
- `product_image(external_id)` unique.
- `interest(customer_id, created_at DESC)` e unique de idempotência.
- `contact_record(customer_id, occurred_at DESC)`.
- `reminder(status, due_at)` e `reminder(customer_id, due_at DESC)`.

## Migration Plan

1. `V1__create_identity_and_customers.sql`
2. `V2__create_catalog.sql`
3. `V3__create_interests_and_crm.sql`
4. `V4__create_spring_session_and_operational_indexes.sql`

Após serem aplicadas fora do ambiente local descartável, migrations não são editadas. Qualquer
ajuste recebe uma nova versão.
