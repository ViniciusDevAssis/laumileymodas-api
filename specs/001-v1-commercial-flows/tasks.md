# Tasks: Operação Comercial V1

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml` e
`quickstart.md` em `specs/001-v1-commercial-flows/`.

**Organization**: tarefas agrupadas em entregas verticais. Cada história inclui schema, modelo,
persistência, service, HTTP/HATEOAS e testes que ela realmente exige. A fundação contém somente
preocupações transversais que bloqueiam todas as entregas.

**Complexity rule**: seguir `Controller -> Service -> Spring Data Repository -> entidade JPA`.
Os pacotes `presentation`, `application`, `domain` e `infrastructure` são organização de
responsabilidades, não Clean Architecture. Não criar ports internos, adapters de repository,
modelos duplicados, mappers triviais ou uma classe por operação. A única interface externa prevista
é `MediaStorage`.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: pode ser executada em paralelo por alterar arquivos independentes;
- **[USn]**: história da specification atendida;
- todas as tarefas possuem caminho explícito e começam desmarcadas porque este é o plano da
  reconstrução simplificada.

## Phase 1: Setup da reconstrução simples

**Goal**: remover a estrutura gerada anteriormente e deixar um baseline mínimo que compile.

- [X] T001 Remover a implementação, testes e migrations gerados pelas fases anteriores em `src/main/kotlin/com/viniciusdevassis/laumileymodas/`, `src/test/kotlin/com/viniciusdevassis/laumileymodas/` e `src/main/resources/db/migration/`, preservando `LaumileyModasApiApplication.kt`
- [X] T002 Atualizar dependências para Web, Validation, Security, OAuth2 Client/Resource Server, Data JPA, Flyway, PostgreSQL, Cloudinary, Spring HATEOAS e dependências de teste em `pom.xml`
- [X] T003 Configurar datasource externo, Flyway, Hibernate validate, context path e limites multipart em `src/main/resources/application.yaml`
- [X] T004 Manter configurações mínimas de frontend, segurança, Cloudinary, mídia e WhatsApp em `src/main/resources/application.yaml`, usando `@Value` diretamente nas classes que precisarem desses valores
- [X] T005 [P] Criar configuração de teste com PostgreSQL Testcontainers em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/PostgresIntegrationTest.kt` e `src/test/resources/application-test.yaml`
- [X] T006 Verificar que o baseline compila e que o contexto de teste inicia sem H2 em `src/test/kotlin/com/viniciusdevassis/laumileymodas/LaumileyModasApiApplicationTests.kt`

**Checkpoint**: projeto mínimo compila, aplicação usa PostgreSQL/Flyway e testes podem compartilhar o
container PostgreSQL.

---

## Phase 2: Fundação transversal mínima

**Goal**: disponibilizar apenas o contrato de erros e configuração comum exigidos por todos os
slices.

- [X] T007 Criar catálogo `ApiError` com códigos globalmente únicos e `ApiException` em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/exceptions/ApiError.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/exceptions/ApiException.kt`
- [X] T008 Criar `ApiErrorResponse` e tratamento de validação, ApiException e fallback 500 em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/advice/GlobalExceptionHandler.kt`
- [X] T009 [P] Disponibilizar `Clock.systemUTC()` diretamente como bean compartilhado em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/config/TimeConfiguration.kt`
- [X] T010 Implementar `ApiAuthenticationEntryPoint` e `ApiAccessDeniedHandler` com o mesmo payload em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/ApiAuthenticationEntryPoint.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/ApiAccessDeniedHandler.kt`
- [X] T011 Configurar a cadeia Spring Security stateless inicial, CORS por origens configuradas e `@EnableMethodSecurity` em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/SecurityConfiguration.kt`
- [X] T012 [P] Testar o contrato de validação, exceção conhecida e fallback seguro em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/ApiErrorIntegrationTest.kt`
- [X] T013 Testar respostas 401 e 403 pelo contrato próprio do Spring Security em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/SecurityErrorIntegrationTest.kt`

**Checkpoint**: erros da aplicação e do Spring Security possuem formato único; nenhuma estrutura de
domínio ou persistência foi antecipada.

---

## Phase 3: User Story 1 - Catálogo público (Priority: P1) 🎯 MVP

**Goal**: visitante lista e consulta detalhes de produtos ativos com imagens, sem autenticação.

**Independent Test**: com dados ativos/inativos no PostgreSQL, `GET /products` e
`GET /products/{id}` expõem apenas ativos em HAL, com imagem principal prioritária e coleção vazia
válida.

### Tests for User Story 1

- [X] T014 [P] [US1] Criar testes de integração do schema, constraints e consultas do catálogo em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/CatalogPersistenceIntegrationTest.kt`
- [X] T015 [P] [US1] Criar testes HTTP do catálogo anônimo, paginação, imagens ordenadas, 404 e links HAL em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/PublicCatalogIntegrationTest.kt`

### Implementation for User Story 1

- [X] T016 [US1] Criar tabelas category, product e product_image com FKs, checks e índices em `src/main/resources/db/migration/V1__create_catalog.sql`
- [X] T017 [P] [US1] Implementar entidades JPA `Category`, `Product` e `ProductImage` com invariantes de ativação/imagem principal em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/entities/Category.kt`, `Product.kt` e `ProductImage.kt`
- [X] T018 [P] [US1] Definir `ProductStatus` em `../../src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/enums/ProductStatus.kt`
- [X] T019 [US1] Criar repositories Spring Data e consultas paginadas de produtos ativos em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/repositories/CatalogRepositories.kt`
- [X] T020 [US1] Implementar consultas públicas coesas em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/CatalogService.kt`
- [X] T021 [P] [US1] Definir DTOs públicos de produto/categoria/imagem em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/dtos/CatalogDtos.kt`
- [X] T022 [US1] Implementar `GET /products` e `GET /products/{id}` com `PagedModel`, `EntityModel` e links em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/controllers/CatalogController.kt`

**Checkpoint**: catálogo público é um incremento utilizável e não depende da autenticação ou CRM.

---

## Phase 4: User Story 2 - Identificação, interesse e WhatsApp (Priority: P1)

**Goal**: cliente cadastra/autentica por senha ou Google, confirma interesse idempotente e recebe o
redirecionamento ao WhatsApp com acompanhamento automático consistente.

**Independent Test**: cliente autenticado cria um único Interest para uma chave, obtém URL WhatsApp
e o PostgreSQL contém exatamente um Reminder e um ContactRecord PENDING para o mesmo Interest.

### Tests for User Story 2

- [ ] T023 [P] [US2] Testar cadastro público sempre CLIENT, BCrypt custo 12 e login local válido/inválido em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/LocalAuthenticationIntegrationTest.kt`
- [ ] T024 [P] [US2] Testar JWT válido/expirado/inválido, refresh rotativo, revogação e reuso em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/TokenIntegrationTest.kt`
- [ ] T025 [P] [US2] Testar `GET /auth/csrf`, proteção seletiva de cookies e CORS configurado em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/CsrfAndCorsIntegrationTest.kt`
- [ ] T026 [P] [US2] Testar endpoints OAuth nativos, callback padrão, login/cadastro Google CLIENT, uso único do handoff e conflito de e-mail sem linking em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/GoogleClientAuthenticationIntegrationTest.kt`
- [ ] T027 [P] [US2] Testar rate limiting 429 e garantir que tentativas válidas normais não são bloqueadas em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/AuthenticationRateLimitIntegrationTest.kt`
- [ ] T028 [P] [US2] Testar interesse idempotente, produto inativo, URL WhatsApp e criação exata do acompanhamento em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/InterestIntegrationTest.kt`

### Implementation for User Story 2

- [ ] T029 [US2] Criar account, external_identity, customer, refresh_token e oauth_handoff em `src/main/resources/db/migration/V2__create_accounts_and_customers.sql`
- [ ] T030 [US2] Criar interest, reminder e contact_record com uniques por interest_id em `src/main/resources/db/migration/V3__create_interests_and_crm.sql`
- [ ] T031 [P] [US2] Implementar entidades JPA de identidade e cliente em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/entities/Account.kt`, `AccountExternalIdentity.kt`, `Customer.kt`, `RefreshToken.kt` e `OAuthHandoff.kt`
- [ ] T032 [P] [US2] Implementar entidades JPA `Interest`, `Reminder` e `ContactRecord` e suas transições em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/entities/Interest.kt`, `Reminder.kt` e `ContactRecord.kt`
- [ ] T033 [P] [US2] Adicionar enums Role, Provider, FollowUpStatus e canais necessários da V1 em `../../src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/enums/ProductStatus.kt`
- [ ] T034 [US2] Criar repositories Spring Data para contas, clientes, tokens e handoffs em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/repositories/AccountRepository.kt` e `CustomerRepository.kt`
- [ ] T035 [US2] Criar repositories Spring Data para Interest, Reminder e ContactRecord em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/repositories/InterestRepository.kt` e `CrmRepositories.kt`
- [ ] T036 [P] [US2] Implementar JWT HMAC com validação de issuer/audience/expiração em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/JwtService.kt`
- [ ] T037 [P] [US2] Implementar cookie de refresh opaco e hash seguro em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/RefreshTokenCookie.kt`
- [ ] T038 [P] [US2] Implementar limitador local de falhas de autenticação em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/AuthenticationRateLimiter.kt`
- [ ] T039 [US2] Implementar cadastro, login, emissão, rotação, reuso e logout em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/AuthService.kt`
- [ ] T040 [US2] Implementar OIDC por sub, ADMIN somente por ADMIN_GOOGLE_SUB, conflito de e-mail e handoff de uso único em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/GoogleAuthService.kt`
- [ ] T041 [US2] Implementar success/failure handlers somente para o comportamento posterior ao OAuth e invalidar a sessão temporária em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/GoogleLoginHandlers.kt`
- [ ] T042 [US2] Configurar cadeias Security OAuth/API com endpoints OAuth nativos, BCrypt 12, Resource Server HMAC e CSRF seletivo sem redirectionEndpoint customizado em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/SecurityConfiguration.kt`
- [ ] T043 [P] [US2] Definir DTOs de cadastro, login, tokens e Google em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/dtos/AuthDtos.kt` e `CustomerDtos.kt`
- [ ] T044 [US2] Implementar cadastro, login, refresh, logout e CSRF em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/controllers/AuthController.kt` e `CustomerController.kt`
- [ ] T045 [US2] Manter o OAuth no fluxo nativo do Spring Security, sem controller/resolver/repository de authorization request customizados, expondo somente exchange e conclusão de cadastro em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/controllers/AuthController.kt`
- [ ] T046 [US2] Implementar confirmação idempotente, URL WhatsApp e criação transacional do trio em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/InterestService.kt`
- [ ] T047 [P] [US2] Adicionar DTO HAL do interesse em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/dtos/CustomerDtos.kt`
- [ ] T048 [US2] Implementar `POST /products/{productId}/interests` com Location e links de produto/WhatsApp em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/controllers/InterestController.kt`

**Checkpoint**: fluxo comercial principal funciona de catálogo a WhatsApp sem implementar venda.

---

## Phase 5: User Story 3 - Administração do catálogo (Priority: P2)

**Goal**: a única ADMIN gerencia categorias, produtos e imagens Cloudinary preservando invariantes.

**Independent Test**: ADMIN Google autorizada cria categoria e produto multipart, altera dados,
principal/ordem e status; catálogo reflete o estado e falha externa não publica referência inválida.

### Tests for User Story 3

- [ ] T049 [P] [US3] Testar Google ADMIN exclusivamente por sub autorizado, tratamento dos demais como CLIENT e impossibilidade de promoção pública em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/AdminAuthenticationIntegrationTest.kt`
- [ ] T050 [P] [US3] Testar matriz anônimo/CLIENT/ADMIN e contrato HAL da gestão do catálogo em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/AdminCatalogIntegrationTest.kt`
- [ ] T051 [P] [US3] Testar upload, exclusão, compensação de falha e invariantes de imagem com fake MediaStorage em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/ProductMediaIntegrationTest.kt`

### Implementation for User Story 3

- [ ] T052 [US3] Completar criação/resolução exclusiva da ADMIN configurada em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/GoogleAuthService.kt`
- [ ] T053 [P] [US3] Definir a pequena interface `MediaStorage` e resultado neutro do provedor em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/MediaStorage.kt`
- [ ] T054 [P] [US3] Implementar integração Cloudinary sem vazar tipos do SDK em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/cloudinary/CloudinaryMediaStorage.kt`
- [ ] T055 [US3] Adicionar gestão de categorias/produtos e invariantes ao service coeso em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/CatalogService.kt`
- [ ] T056 [US3] Implementar coordenação de upload/exclusão e compensação simples em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/ProductMediaService.kt`
- [ ] T057 [US3] Implementar recursos administrativos REST/HAL de categoria, produto e imagem em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/controllers/AdminCatalogController.kt`

**Checkpoint**: administração completa do catálogo funciona sem expor Cloudinary ao frontend.

---

## Phase 6: User Story 4 - Base de clientes e interesses (Priority: P2)

**Goal**: ADMIN pesquisa clientes e navega para seus interesses; CLIENT não acessa o CRM.

**Independent Test**: ADMIN encontra cliente por critérios previstos e consulta interesses/produtos
em HAL; CLIENT e anônimo recebem 403/401 sem conteúdo protegido.

### Tests for User Story 4

- [ ] T058 [P] [US4] Testar pesquisa paginada, detalhes, interesses e estados vazios do CRM em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/CustomerCrmIntegrationTest.kt`
- [ ] T059 [P] [US4] Testar isolamento de dados e matriz 401/403 do CRM em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/CrmAuthorizationIntegrationTest.kt`

### Implementation for User Story 4

- [ ] T060 [US4] Adicionar consultas de pesquisa de cliente e interesses aos Spring Data repositories em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/repositories/CustomerRepository.kt` e `InterestRepository.kt`
- [ ] T061 [US4] Implementar pesquisa e consulta de contexto em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/CrmService.kt`
- [ ] T062 [P] [US4] Definir DTOs HAL de cliente e contexto de interesse em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/dtos/CrmDtos.kt`
- [ ] T063 [US4] Implementar recursos CRM de clientes e interesses com paginação/links em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/controllers/CrmController.kt`

**Checkpoint**: a base inicial do CRM é navegável e protegida sem antecipar automações.

---

## Phase 7: User Story 5 - Contatos e acompanhamento (Priority: P3)

**Goal**: ADMIN registra contatos manuais, consulta pendências e conclui conjuntamente o
ContactRecord/Reminder automático; cliente controla consentimento.

**Independent Test**: Reminder vencido permanece pendente; ContactRecord automático não consta como
contato realizado; seu PATCH de conclusão atualiza os dois registros atomicamente; não existe rota
de criação ou conclusão de Reminder.

### Tests for User Story 5

- [ ] T064 [P] [US5] Testar consentimento opt-in/revogação e independência do atendimento solicitado em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/ContactConsentIntegrationTest.kt`
- [ ] T065 [P] [US5] Testar contato manual, histórico cronológico e distinção PENDING/COMPLETED em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/ContactHistoryIntegrationTest.kt`
- [ ] T066 [P] [US5] Testar listagem/vencimento de Reminder e conclusão conjunta transacional em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/FollowUpIntegrationTest.kt`
- [ ] T067 [P] [US5] Testar que nenhuma rota permite criar ou concluir Reminder diretamente em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/ReminderContractIntegrationTest.kt`

### Implementation for User Story 5

- [ ] T068 [US5] Implementar consulta/substituição idempotente do consentimento em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/AuthService.kt`
- [ ] T069 [US5] Expor GET/PUT do consentimento singleton em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/controllers/CustomerController.kt`
- [ ] T070 [US5] Implementar contato manual, histórico, lembretes, vencimento derivado e conclusão conjunta em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/CrmService.kt`
- [ ] T071 [P] [US5] Completar DTOs de ContactRecord e Reminder com links condicionais em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/dtos/CrmDtos.kt`
- [ ] T072 [US5] Expor contatos, PATCH de conclusão e lembretes somente leitura em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/controllers/CrmController.kt`

**Checkpoint**: CRM V1 completo, sem Reminder manual e sem automação de comunicação.

---

## Phase 8: Validação transversal e documentação

**Goal**: provar que os slices funcionam juntos e que nenhuma complexidade removida retornou.

- [ ] T073 [P] Validar sintaxe e exemplos do contrato em `specs/001-v1-commercial-flows/contracts/openapi.yaml`
- [ ] T074 [P] Sincronizar nomes finais de variáveis e comandos executáveis em `specs/001-v1-commercial-flows/quickstart.md` e `src/main/resources/application.yaml`
- [ ] T075 Executar todos os cenários automatizáveis do quickstart e registrar ajustes necessários em `specs/001-v1-commercial-flows/quickstart.md`
- [ ] T076 Executar `./mvnw clean test` com Docker/Testcontainers e corrigir regressões sem adicionar H2 em `pom.xml` e `src/test/`
- [ ] T077 Revisar `src/main/kotlin/com/viniciusdevassis/laumileymodas/` e remover ports internos, adapters delegadores, modelos duplicados, mappers triviais, use cases unitários e helpers sem justificativa
- [ ] T078 Confirmar ausência de callback/entradas Google customizados, ProblemDetail, JWT assimétrico, linking por e-mail, Reminder manual, `ApplicationProperties` e referências a outros projetos em `src/`, `pom.xml` e `specs/001-v1-commercial-flows/`

## Dependencies & Execution Order

```text
Phase 1 Setup
  -> Phase 2 Fundação
     -> US1 Catálogo público
        -> US2 Cliente + Interesse + WhatsApp
           -> US3 Administração do catálogo
           -> US4 Base de clientes
              -> US5 Contatos e acompanhamento
                 -> Phase 8 Validação final
```

- US1 não depende de autenticação e constitui o primeiro incremento demonstrável.
- US2 depende de Product ativo e cria as bases de Account, Customer e acompanhamento.
- US3 depende da segurança ADMIN e reutiliza o catálogo.
- US4 depende de Customer e Interest.
- US5 depende do acompanhamento criado por US2 e da navegação CRM de US4.

## Parallel Opportunities

- tarefas marcadas `[P]` alteram arquivos independentes dentro da mesma fase;
- testes de uma história podem ser escritos em paralelo antes da implementação;
- entidade/enums/DTOs podem avançar em paralelo quando a migration/contrato já está definido;
- US3 e US4 podem avançar em paralelo depois de US2, embora a sequência acima reduza conflitos em
  `SecurityConfiguration` e services compartilhados.

## Implementation Strategy

1. Concluir Setup e Fundação sem criar todo o domínio antecipadamente.
2. Entregar US1 e validá-la como catálogo público independente.
3. Entregar US2 de ponta a ponta até o WhatsApp e parar para validar idempotência/segurança.
4. Adicionar US3, US4 e US5 uma de cada vez, executando a suíte após cada checkpoint.
5. Só extrair classe ou interface nova se houver regra, risco, integração ou reutilização concreta.
6. Manter commits por slice ou grupo coerente, com mensagens em português do Brasil.

## Task Summary

| Phase | Tasks |
|---|---:|
| Setup | 6 |
| Fundação | 7 |
| US1 Catálogo público | 9 |
| US2 Cliente, autenticação e interesse | 26 |
| US3 Administração do catálogo | 9 |
| US4 Base de clientes | 6 |
| US5 Contatos e acompanhamento | 9 |
| Validação final | 6 |
| **Total** | **78** |

Todas as 78 tarefas seguem o formato de checklist, têm caminho explícito e origem na specification
ou nas decisões técnicas do plano.
