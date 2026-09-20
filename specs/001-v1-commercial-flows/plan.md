# Implementation Plan: Operação Comercial V1

**Branch**: `001-v1-commercial-flows` | **Date**: 2026-09-20
**Spec**: [spec.md](./spec.md)
**Research**: [research.md](./research.md)

## Summary

Implementar a V1 como um monólito Spring Boot simples, com API REST e HATEOAS. O fluxo padrão será
`Controller -> Service -> Spring Data Repository -> entidade JPA`. Os pacotes `presentation`,
`application`, `domain` e `infrastructure` organizam responsabilidades; eles não representam Clean
Architecture. As entidades de negócio também serão entidades JPA; não haverá ports internos,
adapters de persistência, modelos duplicados, mappers triviais nem uma classe por operação.

Services coesos cuidam de transações e regras do próprio contexto. Controllers traduzem HTTP e
adicionam links. Spring Security centraliza autenticação e autorização. A única abstração externa
obrigatória é `MediaStorage`, implementada pela integração Cloudinary.

## Technical Context

**Language/Version**: Kotlin, Java 21
**Framework**: Spring Boot 3.5.16, Spring Web, Spring Validation, Spring Security
**Hypermedia**: Spring HATEOAS (`EntityModel`, `PagedModel`, HAL)
**Authentication**: JWT access token HMAC, refresh token opaco, Google OAuth2/OIDC
**Persistence**: Spring Data JPA, PostgreSQL, Flyway; Hibernate `ddl-auto=validate`
**Media**: Cloudinary por meio de `MediaStorage`
**Testing**: JUnit 5, Spring Boot Test, Spring Security Test, Testcontainers PostgreSQL
**Application type**: monólito REST; APIs stateless por Bearer JWT e sessão temporária somente no
protocolo OAuth2 Login nativo
**Performance**: sem limite quantitativo arbitrário na V1; consultas paginadas e índices orientados
pelos acessos previstos
**Deployment scope**: uma única instância na V1
**Out of scope**: checkout, pagamento, estoque, tamanhos, WhatsApp Business API, mensagens
automáticas, microsserviços, mensageria, cache distribuído, CQRS e automações de CRM

## Constitution Check

| Principle | Design response | Gate |
|---|---|---|
| Spec First | O plano implementa FR-001 a FR-075 sem alterar a specification. | PASS |
| Simplicidade da V1 | Entidade JPA única, Spring Data direto e seis services por contexto. | PASS |
| Backend como autoridade | Services e entidades validam regras; Security valida acesso. | PASS |
| Segurança e privacidade | JWT, refresh rotativo, OIDC por `sub`, CSRF seletivo e dados mínimos. | PASS |
| Regras testáveis | Testes focam invariantes, autorização e fluxos críticos. | PASS |
| Integrações isoladas | Apenas Cloudinary recebe uma interface própria; Google fica em security. | PASS |
| Responsabilidades claras | Controllers, services, entidades e infraestrutura têm papéis distintos. | PASS |
| Banco versionado | Flyway cria e evolui todo o schema; Hibernate apenas valida. | PASS |
| Qualidade | Erros, rollback, idempotência e casos negativos fazem parte das entregas. | PASS |
| Mídia | Backend controla upload/exclusão e persiste somente referências. | PASS |

O check permanece válido após o design. Não há exceção constitucional planejada.

## Project Structure

```text
src/main/kotlin/com/viniciusdevassis/laumileymodas/
├── LaumileyModasApiApplication.kt
├── presentation/
│   ├── controllers/
│   │   ├── AuthController.kt
│   │   ├── CustomerController.kt
│   │   ├── CatalogController.kt
│   │   ├── InterestController.kt
│   │   ├── AdminCatalogController.kt
│   │   └── CrmController.kt
│   ├── dtos/
│   │   ├── AuthDtos.kt
│   │   ├── CustomerDtos.kt
│   │   ├── CatalogDtos.kt
│   │   └── CrmDtos.kt
│   └── advice/
│       └── GlobalExceptionHandler.kt
├── application/
│   ├── AuthService.kt
│   ├── GoogleAuthService.kt
│   ├── CatalogService.kt
│   ├── InterestService.kt
│   ├── CrmService.kt
│   ├── ProductMediaService.kt
│   └── MediaStorage.kt
├── domain/
│   ├── entities/
│   │   ├── Account.kt
│   │   ├── AccountExternalIdentity.kt
│   │   ├── RefreshToken.kt
│   │   ├── OAuthHandoff.kt
│   │   ├── Customer.kt
│   │   ├── Category.kt
│   │   ├── Product.kt
│   │   ├── ProductImage.kt
│   │   ├── Interest.kt
│   │   ├── Reminder.kt
│   │   └── ContactRecord.kt
│   ├── enums/
│   │   └── DomainEnums.kt
│   └── exceptions/
│       ├── ApiError.kt
│       └── ApiException.kt
└── infrastructure/
    ├── repositories/
    │   ├── AccountRepository.kt
    │   ├── CustomerRepository.kt
    │   ├── CatalogRepositories.kt
    │   ├── InterestRepository.kt
    │   └── CrmRepositories.kt
    ├── security/
    │   ├── SecurityConfiguration.kt
    │   ├── JwtService.kt
    │   ├── RefreshTokenCookie.kt
    │   ├── GoogleLoginHandlers.kt
    │   ├── AuthenticationRateLimiter.kt
    │   ├── ApiAuthenticationEntryPoint.kt
    │   └── ApiAccessDeniedHandler.kt
    ├── cloudinary/
    │   └── CloudinaryMediaStorage.kt
    └── config/
        └── ApplicationProperties.kt

src/main/resources/
├── application.yaml
└── db/migration/
    ├── V1__create_catalog.sql
    ├── V2__create_accounts_and_customers.sql
    └── V3__create_interests_and_crm.sql

src/test/kotlin/com/viniciusdevassis/laumileymodas/
├── integration/
└── unit/
```

Arquivos agrupam tipos pequenos do mesmo contexto quando isso reduz cerimônia. Eles só serão
separados se a legibilidade ou coesão exigir. A estrutura é um alvo pragmático, não uma quota de
classes.

## Architecture and Boundaries

### Presentation

- recebe e valida formato das requisições;
- aplica semântica HTTP, paginação, `Location` e media types;
- converte resultados em DTOs com `EntityModel`/`PagedModel`;
- não abre transações nem contém regra de negócio;
- extrai da autenticação apenas `accountId` e papel necessários ao service.

### Application

- services públicos representam contextos funcionais, não operações isoladas;
- `@Transactional` delimita mudanças de estado;
- repositories Spring Data são usados diretamente;
- regras simples podem permanecer no service; invariantes do próprio objeto ficam na entidade;
- `Clock` é injetado diretamente em services com regras temporais;
- `MediaStorage` é a única interface externa exigida neste plano.

### Domain

- entidades JPA representam estado e comportamento do negócio;
- enums representam papéis e estados fechados;
- `ApiError` e `ApiException` mantêm códigos globais e resposta consistente;
- não depende de DTOs HTTP, HATEOAS, objetos OAuth ou SDK Cloudinary.

### Infrastructure

- repositories são interfaces Spring Data, sem adapters intermediários;
- security contém configuração e detalhes Spring Security/OIDC/JWT;
- Cloudinary implementa `MediaStorage` e converte respostas do SDK para resultado simples;
- configuração externa mapeia propriedades tipadas.

## REST and HATEOAS Strategy

O contrato exato está em [contracts/openapi.yaml](./contracts/openapi.yaml). Os recursos centrais são
`customers`, `products`, `categories`, `interests`, `contact-records` e `reminders`.

- `GET` é seguro e não altera estado;
- `POST` cria recursos e responde `201` com `Location`;
- `PUT` substitui o consentimento singleton ou categoria editável;
- `PATCH` altera parcialmente produto/imagem e conclui `ContactRecord`;
- `DELETE` remove imagem quando a regra permite;
- `400`, `401`, `403`, `404`, `409`, `422`, `429` e `500` usam `ApiErrorResponse`;
- coleções usam `page`, `size`, ordenação estável e `PagedModel`;
- recursos incluem apenas links disponíveis ao consumidor/estado atual;
- erros e respostas de autenticação permanecem `application/json` sem `_links`.

Não haverá endpoint para criar ou concluir `Reminder`. A única transição é o `PATCH` do
`ContactRecord PENDING`, que conclui ambos.

## Domain and Persistence Design

O modelo completo está em [data-model.md](./data-model.md). Decisões principais:

- `Account` possui papel imutável `CLIENT` ou `ADMIN`; cadastro público sempre cria `CLIENT`;
- `AccountExternalIdentity(provider, subject)` identifica Google sem linking por e-mail;
- `Customer` contém dados comerciais e estado do consentimento;
- `Product 1:N ProductImage`, com URL, `externalId`, principal e ordem;
- `Interest` tem chave idempotente única por cliente;
- `Reminder.interest_id` e `ContactRecord.interest_id` são únicos e compartilham o mesmo interesse;
- `ContactRecord` manual nasce concluído; o automático nasce pendente;
- `Reminder` nasce automaticamente e só termina com o contato automático;
- refresh tokens persistem apenas hash e família; handoffs persistem apenas handle hash e estado
  mínimo temporário.

As migrations mantêm FKs, uniques, checks e índices necessários. Migrations aplicadas não são
reescritas em ambientes compartilhados; durante a reconstrução local anterior ao primeiro release,
o histórico ainda pode ser recriado de forma limpa conforme a execução das tasks.

## Service Responsibilities

### `AuthService`

- cadastro tradicional de cliente;
- login por e-mail/senha com rate limiting;
- emissão de access token e refresh opaco;
- rotação, detecção de reuso, revogação e logout;
- consulta e substituição do consentimento do cliente.

### `GoogleAuthService`

- validar identidade OIDC normalizada recebida do handler;
- decidir ADMIN exclusivamente quando `sub == ADMIN_GOOGLE_SUB`; demais identidades seguem CLIENT;
- resolver vínculo exclusivamente por `provider + sub`;
- rejeitar conflito de e-mail sem linking automático;
- criar/consumir handoff de uso único;
- concluir cadastro Google ou login e delegar emissão de tokens ao `AuthService`.

### `CatalogService`

- consultar catálogo público e detalhes ativos;
- criar/renomear categorias;
- criar, consultar e alterar produtos;
- validar status, categoria e invariantes das imagens.

### `ProductMediaService`

- enviar/remover assets por `MediaStorage`;
- persistir/remover `ProductImage` preservando principal e ordem;
- executar compensação simples quando persistência falhar após upload.

### `InterestService`

- confirmar interesse de cliente em produto ativo;
- aplicar `Idempotency-Key`;
- criar `Interest`, `Reminder` e `ContactRecord PENDING` atomicamente;
- montar a URL de redirecionamento do WhatsApp.

### `CrmService`

- pesquisar clientes e consultar seus interesses;
- listar lembretes automáticos pendentes e calcular vencimento;
- registrar contato manual concluído;
- listar histórico distinguindo pendentes e concluídos;
- finalizar contato automático e Reminder na mesma transação.

## Security Design

### Access token

- JWT HMAC SHA-256 com segredo de pelo menos 256 bits vindo de variável de ambiente;
- curta duração, padrão de 15 minutos;
- claims: `sub`, `role`, `iss`, `aud`, `iat`, `exp`;
- enviado apenas em `Authorization: Bearer`, nunca em URL ou cookie.

### Refresh token

- 256 bits aleatórios, codificados de forma URL-safe;
- somente hash SHA-256 persistido;
- cookie `HttpOnly`, `Secure` em produção, `SameSite` e path configuráveis;
- rotação dentro de transação com lock; reuso revoga a família;
- logout revoga o token/família aplicável e expira o cookie.

### CSRF

- `CookieCsrfTokenRepository` com `XSRF-TOKEN` e `X-XSRF-TOKEN`;
- `GET /auth/csrf` materializa ou renova o token;
- refresh, logout e consumo/conclusão de handoff exigem CSRF;
- endpoints Bearer e callback OIDC não são protegidos por cookie CSRF;
- token CSRF é renovado após autenticação/logout quando o Spring o invalidar.

### Google OIDC and ADMIN

- escopos `openid profile email`;
- um único registrationId `google` usa os endpoints padrão do Spring Security;
- início: `/oauth2/authorization/google`;
- callback processado pelo Spring: `/login/oauth2/code/google`;
- externamente em desenvolvimento, com o context path, o callback é
  `http://localhost:8080/api/v1/login/oauth2/code/google`;
- não há configuração de `authorizationEndpoint`, `redirectionEndpoint` ou `redirect-uri`;
- o `state` e a authorization request são geridos pelo mecanismo padrão do Spring;
- uma cadeia OAuth permite sessão temporária `IF_REQUIRED` apenas durante o protocolo e os handlers
  invalidam essa sessão; a cadeia da API permanece `STATELESS`;
- `sub` administrativo vem exclusivamente de configuração secreta;
- a única ADMIN é criada/resolvida somente quando o `sub` validado coincide com a configuração;
- cadastro público nunca aceita papel;
- URLs de frontend são allowlist fixa em configuração; não há redirect arbitrário;
- success/failure handlers customizam somente o comportamento após autenticação;
- handoff de uso único fica em cookie e seu hash no banco; nenhum token vai na URL.

### Authorization, CORS and errors

- `/products/**`, cadastro, login, CSRF e endpoints OAuth padrão são públicos conforme contrato;
- interesse e consentimento exigem `CLIENT`;
- `/admin/**` exige `ADMIN` na URL e, quando útil, method security;
- CORS usa origens configuradas por ambiente, nunca `*` com credenciais;
- `AuthenticationEntryPoint` e `AccessDeniedHandler` escrevem o mesmo `ApiErrorResponse`;
- nenhum erro expõe stack trace, segredo, token, hash ou payload do provedor.

### Rate limiting

- contador local por janela para falhas de login;
- resposta `429` com código estável;
- sucesso normal limpa o contador correspondente;
- nenhuma dependência em Redis na V1.

## Media Consistency

O Cloudinary não participa da transação PostgreSQL. O service usa ordem explícita e compensação:

1. valida regras locais e autorização;
2. envia o novo asset;
3. persiste a referência na transação;
4. se a persistência falhar, tenta apagar o novo asset;
5. ao excluir, confirma a exclusão externa antes de remover a referência local;
6. nunca publica produto ativo sem imagem válida e principal.

Falha de compensação é registrada para intervenção operacional. A V1 não adiciona fila, outbox ou
job automático sem requisito concreto.

## Error Contract

`ApiErrorResponse` contém:

- `timestamp`;
- `status`;
- `code` globalmente único;
- `message` segura;
- `path`;
- `fieldErrors`, somente para validação, como mapa campo -> mensagem.

`ApiError` cataloga códigos por prefixos globais (`AUTH`, `SEC`, `CUSTOMER`, `CATALOG`, `MEDIA`,
`INTEREST`, `CRM`, `VALIDATION`, `INTERNAL`). `ApiException` carrega erro e status. Não haverá
ProblemDetail nem exceptions equivalentes por camada.

## Testing Strategy

### Unit tests

Somente para comportamento que pode ser validado com valor sem iniciar Spring:

- transições/invariantes de Product, ContactRecord e Reminder;
- geração/validação JWT e hash/rotação de refresh;
- regra do rate limiter;
- composição e codificação da URL WhatsApp quando extraída dentro do service.

### Integration tests

Usam `@SpringBootTest`/MockMvc e PostgreSQL Testcontainers para:

- aplicar todas as migrations e validar Hibernate;
- cadastro público sempre CLIENT;
- login local válido/inválido e limite `429` sem bloquear uso normal;
- JWT válido, expirado, inválido; refresh rotativo, revogação e reuso;
- CSRF nos endpoints por cookie e `GET /auth/csrf`;
- CORS por origem configurada;
- Google CLIENT/ADMIN, `sub` autorizado e conflito de e-mail sem linking;
- catálogo público HAL e autorização administrativa;
- `Product 1:N ProductImage` e compensação de mídia com fake `MediaStorage`;
- idempotência concorrente do interesse;
- criação exata e conclusão conjunta do acompanhamento;
- consentimento e isolamento do CRM;
- contrato uniforme de erros de aplicação e Spring Security.

Não haverá teste que imponha ports, adapters, mappers ou quantidade de classes.

## Configuration and Secrets

Variáveis externas principais:

- `LAUMILEY_DB`, `AIVEN_DB_USER`, `AIVEN_DB_PASS`;
- `LAUMILEY_JWT_SECRET`, issuer, audience e TTLs;
- `LAUMILEY_OAUTH_CLIENT_ID`, `LAUMILEY_OAUTH_CLIENT_SECRET`;
- `LAUMILEY_ADMIN_GOOGLE_SUB`;
- URLs e origens autorizadas do frontend;
- configurações dos cookies;
- credenciais Cloudinary;
- número/template do WhatsApp;
- limites de mídia e autenticação.

Segredos não possuem valor real padrão e não são versionados. O quickstart lista os nomes exatos.

## Delivery Strategy

As tasks são organizadas em slices verticais:

1. baseline simples e fundação transversal mínima;
2. catálogo público completo;
3. cliente, autenticação e interesse até o WhatsApp;
4. administração do catálogo e mídia;
5. consulta da base de clientes e interesses;
6. histórico, lembretes automáticos, conclusão conjunta e consentimento;
7. validação transversal final.

Cada slice inclui migration, entidade, repository, service, controller/HATEOAS e testes necessários,
evitando construir toda a infraestrutura antecipadamente.

## Complexity Tracking

| Abstraction | Why it exists now | Simpler option rejected because |
|---|---|---|
| `MediaStorage` | Isola o Cloudinary conforme a Constitution. | SDK no service misturaria regra da aplicação com detalhe de provedor externo. |
| `OAuthHandoff` | Garante troca única sem tokens na URL. | Cookie autocontido não permite invalidar reuso confiavelmente. |
| `ApiError`/`ApiException` | Mantém códigos estáveis e contrato único. | Exceptions ad hoc quebrariam consistência. |

Nenhuma outra abstração própria está pré-aprovada. Sua criação exige atualizar esta tabela ou
demonstrar reutilização local evidente durante a implementação.
