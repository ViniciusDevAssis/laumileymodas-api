# Data Model: Operação Comercial V1

O modelo usa as próprias entidades JPA como modelo de negócio. Não existem cópias `DomainModel`,
`JpaEntity`, mappers ou adapters de persistência. DTOs existem somente na borda HTTP.

## Conventions

- IDs: UUID.
- Instantes: `timestamptz`, manipulados como `Instant`.
- E-mails: valor original para exibição e valor normalizado em minúsculas para unicidade.
- Textos obrigatórios: trimmed e não vazios.
- Datas de criação/alteração: preenchidas pelo backend.
- Enums persistidos como texto com checks no banco.
- Exclusão física só é usada onde o produto a prevê; histórico comercial não é apagado na V1.

## `Account`

Credencial e autoridade local da aplicação.

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `email` | varchar(320) | obrigatório |
| `passwordHash` | varchar(255) | nullable; obrigatório para cadastro tradicional |
| `role` | `CLIENT`, `ADMIN` | obrigatório e imutável |
| `createdAt` | instant | obrigatório |
| `updatedAt` | instant | obrigatório |

Rules:

- cadastro público sempre define `CLIENT` e não recebe papel no request;
- `ADMIN` só é criada/resolvida pelo fluxo Google administrativo cujo `sub` corresponde à
  configuração do backend;
- índice único parcial permite no máximo uma linha `ADMIN`;
- Account Google pode não possuir senha local;
- coincidência de e-mail nunca cria vínculo de identidade externo.

## `AccountExternalIdentity`

Vínculo genérico com um provedor autenticado.

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `accountId` | UUID | FK Account, obrigatório |
| `provider` | varchar(30) | `GOOGLE` na V1 |
| `subject` | varchar(255) | `sub` do provedor, obrigatório |
| `createdAt` | instant | obrigatório |

Constraints: unique `(provider, subject)` e unique `(account_id, provider)`.

## `RefreshToken`

Estado servidor do refresh token opaco.

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `accountId` | UUID | FK Account, obrigatório |
| `tokenHash` | char(64) | SHA-256 em hexadecimal, unique |
| `familyId` | UUID | obrigatório |
| `expiresAt` | instant | obrigatório |
| `consumedAt` | instant | nullable |
| `revokedAt` | instant | nullable |
| `createdAt` | instant | obrigatório |

Derived state:

- active: não consumido, não revogado e ainda não expirado;
- consumed: `consumedAt != null`;
- revoked: `revokedAt != null`;
- expired: `expiresAt <= now`.

Rotação consome o atual e cria outro na mesma família. Reuso de um token consumido revoga todos os
tokens ainda ativos da família.
`n## `Customer`

Dados comerciais do cliente.

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK e código público do cliente |
| `accountId` | UUID | FK Account, unique, obrigatório |
| `firstName` | varchar(120) | obrigatório |
| `lastName` | varchar(120) | obrigatório |
| `whatsappPhone` | varchar(20) | E.164, obrigatório |
| `proactiveContactAuthorized` | boolean | obrigatório, default false |
| `consentGrantedAt` | instant | nullable |
| `consentRevokedAt` | instant | nullable |
| `createdAt` | instant | obrigatório |
| `updatedAt` | instant | obrigatório |

Rules:

- consentimento começa `false` e depende de ação afirmativa;
- concessão e revogação são idempotentes;
- revogação não altera autenticação, histórico, interesses ou lembretes de atendimento solicitado.

## `Category`

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `name` | varchar(120) | obrigatório |
| `normalizedName` | varchar(120) | unique, obrigatório |
| `createdAt` | instant | obrigatório |
| `updatedAt` | instant | obrigatório |

A V1 permite criar e renomear. Exclusão não foi especificada.

## `Product`

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `categoryId` | UUID | FK Category, obrigatório |
| `name` | varchar(200) | obrigatório |
| `description` | varchar(2000) | obrigatório |
| `status` | `ACTIVE`, `INACTIVE` | obrigatório |
| `createdAt` | instant | obrigatório |
| `updatedAt` | instant | obrigatório |

Rules:

- produto ativo deve possuir ao menos uma imagem e exatamente uma principal;
- apenas ativo aparece no catálogo e aceita novo interesse;
- preço, estoque, tamanho e disponibilidade não pertencem ao modelo.

## `ProductImage`

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `productId` | UUID | FK Product, obrigatório |
| `url` | varchar(2048) | obrigatório |
| `externalId` | varchar(255) | obrigatório, unique |
| `primary` | boolean | obrigatório |
| `displayOrder` | integer | >= 0, obrigatório |
| `createdAt` | instant | obrigatório |

Constraints: unique `(product_id, display_order)` e índice único parcial em `product_id` quando
`primary = true`. O service garante ao menos uma principal antes de ativar/publicar.

## `Interest`

Confirmação do cliente para seguir ao WhatsApp.

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `customerId` | UUID | FK Customer, obrigatório |
| `productId` | UUID | FK Product, obrigatório |
| `idempotencyKey` | varchar(100) | obrigatório |
| `createdAt` | instant | data do interesse, obrigatório |

Constraint: unique `(customer_id, idempotency_key)`. Repetição com mesma chave e mesmo produto
retorna o resultado existente; mesma chave para produto diferente retorna conflito.

## `Reminder`

Acompanhamento criado exclusivamente pelo interesse.

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `interestId` | UUID | FK Interest, obrigatório, unique |
| `description` | varchar(1000) | obrigatório |
| `dueAt` | instant | obrigatório; inicialmente data do interesse |
| `status` | `PENDING`, `COMPLETED` | obrigatório |
| `completedAt` | instant | nullable; obrigatório quando concluído |
| `createdAt` | instant | obrigatório |
| `updatedAt` | instant | obrigatório |

Rules:

- sempre nasce `PENDING` junto do Interest e do ContactRecord;
- `overdue = status == PENDING && dueAt < now`;
- não existe criação manual, edição, exclusão ou conclusão independente;
- só muda para `COMPLETED` na conclusão do ContactRecord do mesmo `interestId`;
- canal, cliente/código e produto exigidos pela resposta são obtidos pelo Interest relacionado.

## `ContactRecord`

Contato comercial manual concluído ou acompanhamento automático pendente.

| Field | Type | Rules |
|---|---|---|
| `id` | UUID | PK |
| `customerId` | UUID | FK Customer, obrigatório |
| `interestId` | UUID | FK Interest, nullable, unique quando presente |
| `status` | `PENDING`, `COMPLETED` | obrigatório |
| `occurredAt` | instant | nullable enquanto pendente; obrigatório ao concluir |
| `channel` | varchar(40) | obrigatório; automático nasce `WHATSAPP` |
| `description` | varchar(2000) | nullable enquanto pendente; obrigatório ao concluir |
| `completedAt` | instant | nullable; obrigatório quando concluído |
| `createdAt` | instant | obrigatório |
| `updatedAt` | instant | obrigatório |

Rules:

- registro manual não tem Interest e nasce `COMPLETED`;
- registro automático tem Interest e nasce `PENDING`, sem afirmar que houve conversa;
- concluir registro automático exige `occurredAt`, canal e descrição, muda o ContactRecord e o
  Reminder do mesmo Interest para `COMPLETED` na mesma transação;
- não há reabertura, edição posterior ou exclusão na V1.

## Relationships

```text
Account 1 ─── 0..* AccountExternalIdentity
Account 1 ─── 0..* RefreshToken
Account 1 ─── 0..1 Customer
Category 1 ─── * Product
Product 1 ─── 1..* ProductImage
Customer 1 ─── * Interest * ─── 1 Product
Customer 1 ─── * ContactRecord
Interest 1 ─── 1 Reminder
Interest 1 ─── 1 ContactRecord (automático)
```

`Reminder` e `ContactRecord` automático são relacionados pelo mesmo `Interest`; não existe FK
entre eles.

## State Transitions

### Product

```text
INACTIVE -- activate with valid images and one primary --> ACTIVE
ACTIVE   -- deactivate --------------------------------> INACTIVE
```

### Contact consent

```text
NOT_AUTHORIZED -- affirmative grant --> AUTHORIZED
AUTHORIZED     -- revoke -----------> NOT_AUTHORIZED
```

Repetir o estado desejado é idempotente.

### ContactRecord and Reminder

```text
Interest confirmed
  -> ContactRecord PENDING + Reminder PENDING
  -> admin completes ContactRecord
  -> ContactRecord COMPLETED + Reminder COMPLETED (same transaction)
```

`OVERDUE` é derivado e não altera `PENDING`.

### RefreshToken

```text
ACTIVE -- refresh --> CONSUMED + new ACTIVE in same family
ACTIVE -- logout --> REVOKED
CONSUMED -- reuse --> active family members REVOKED
ACTIVE -- time passes --> EXPIRED (derived)
```
`n## Transaction Boundaries

- Account tradicional e Customer são criados juntos.
- Vínculo Google, criação/resolução de Account/Customer e emissão do refresh token são atômicos conforme o fluxo.
- Rotação de refresh usa lock e cria sucessor na mesma transação.
- Product e referências de imagens são persistidos preservando suas invariantes; operação externa
  Cloudinary usa compensação simples descrita no plan.
- Interest, Reminder e ContactRecord PENDING são inseridos na mesma transação.
- Conclusão automática altera ContactRecord e Reminder do mesmo Interest na mesma transação.
- Não existe transação de criação ou conclusão isolada de Reminder.

## Indexes

- `account(email)` unique e unique parcial para papel ADMIN;
- `account_external_identity(provider, subject)` unique;
- `refresh_token(token_hash)` unique, índices em `family_id`, `account_id`, `expires_at`;
- `customer(whatsapp_phone)` e campos normalizados usados na pesquisa;
- `category(normalized_name)` unique;
- `product(status, created_at desc)` e `product(category_id)`;
- `product_image(product_id, display_order)` e principal parcial;
- `interest(customer_id, created_at desc)` e unique idempotente;
- `reminder(interest_id)` unique e `(status, due_at)`;
- `contact_record(interest_id)` unique quando presente e
  `(customer_id, status, occurred_at desc)`.

## Migration Plan

1. `V1__create_catalog.sql`
2. `V2__create_accounts_and_customers.sql`
3. `V3__create_interests_and_crm.sql`

Cada migration inclui suas constraints e índices diretamente relacionados. Não existe migration
separada apenas para criar estrutura cerimonial. Depois de aplicada em ambiente compartilhado, uma
migration não é editada; mudanças recebem nova versão.
