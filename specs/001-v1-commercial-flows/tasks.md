---

description: "Tarefas de implementação da Operação Comercial V1"
---

# Tasks: Operação Comercial V1

**Input**: Artefatos em `/specs/001-v1-commercial-flows/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml` e `quickstart.md`

**Tests**: Incluídos porque a specification, a Constitution e o plano exigem proteção automatizada para regras de negócio, autorização, persistência e fluxos críticos.

**Organization**: As tarefas estão agrupadas por user story e ordenadas por dependência. Em cada story, os testes vêm antes da implementação correspondente.

## Formato: `[ID] [P?] [Story] Descrição`

- **[P]**: pode ser executada em paralelo com outras tarefas marcadas da mesma etapa porque afeta arquivos diferentes e não depende de trabalho ainda incompleto;
- **[Story]**: identifica a user story atendida;
- todas as tarefas indicam os arquivos concretos afetados.

## Consistency Check

A decisão posterior de permitir cadastro pelo Google tornava ambígua a exigência original de senha em todo cadastro. A specification foi alinhada antes desta lista: senha permanece obrigatória no cadastro tradicional; a identidade Google validada substitui a senha local no cadastro Google, sem dispensar os demais dados obrigatórios. Nenhuma outra inconsistência entre specification, Constitution e plano foi encontrada.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar dependências e configuração do monólito Kotlin/Spring Boot.

- [ ] T001 Atualizar `pom.xml` com Spring Validation, Spring Security, OAuth2 Client, OAuth2 Resource Server/JOSE, Spring Data JPA, Flyway, PostgreSQL, Cloudinary, Spring Security Test e Testcontainers PostgreSQL
- [ ] T002 [P] Configurar datasource, Flyway, `ddl-auto=validate`, origens/URLs fixas do frontend, cookies/CSRF, JWT, Google OAuth2, Cloudinary, WhatsApp e limites de upload em `src/main/resources/application.yaml`
- [ ] T003 [P] Criar configuração de testes sem segredos externos em `src/test/resources/application-test.yaml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Criar a base de persistência, testes, tempo/identidade e tratamento uniforme de erros usada por todas as stories.

**CRITICAL**: Esta fase deve terminar antes da implementação das user stories.

- [ ] T004 Criar a base reutilizável de integração com PostgreSQL Testcontainers e propriedades dinâmicas em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/support/PostgresIntegrationTest.kt`
- [ ] T005 Criar o teste de inicialização do contexto, execução completa do Flyway e validação Hibernate em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/persistence/FlywaySchemaIntegrationTest.kt`
- [ ] T006 Criar a migration de contas, clientes, identidades Google, consentimento, refresh tokens e handoffs OAuth de uso único em `src/main/resources/db/migration/V1__create_identity_customers_and_tokens.sql`
- [ ] T007 [P] Definir ports de relógio e geração de UUID e suas implementações do sistema em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/ClockProvider.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/IdGenerator.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/config/SystemProvidersConfiguration.kt`
- [ ] T008 [P] Definir contrato e exception base de erros de domínio sem dependência de application em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/common/DomainError.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/common/DomainException.kt`
- [ ] T009 [P] Definir contrato e exceptions próprias de application em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/common/ApplicationError.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/common/ApplicationException.kt`
- [ ] T010 [P] Implementar o payload próprio `ApiErrorResponse` e os itens de validação em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/common/error/ApiErrorResponse.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/common/error/ValidationErrorResponse.kt`
- [ ] T011 Implementar mapeamento central de exceptions, validação e fallback `INTERNAL_001` em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/common/error/GlobalExceptionHandler.kt`
- [ ] T012 [P] Implementar `AuthenticationEntryPoint` e `AccessDeniedHandler` com o mesmo contrato de erro em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/security/RestAuthenticationEntryPoint.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/security/RestAccessDeniedHandler.kt`
- [ ] T013 Criar testes do formato de validação, autenticação, autorização e fallback 500 em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/http/ApiErrorResponseIntegrationTest.kt`

**Checkpoint**: Dependências, schema inicial, PostgreSQL real em testes e contrato uniforme de erros estão disponíveis.

---

## Phase 3: User Story 1 - Consultar o catálogo público (Priority: P1) 🎯 MVP

**Goal**: Permitir consulta anônima de produtos ativos, com categoria, imagem principal e URLs ordenadas de todas as imagens.

**Independent Test**: Acessar listagem e detalhe sem autenticação, verificar apenas produtos ativos e conferir resposta vazia válida e imagens ordenadas.

### Tests for User Story 1

- [ ] T014 [P] [US1] Escrever testes das invariantes de ativação, quantidade de imagens e imagem principal em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/domain/catalog/ProductTest.kt`
- [ ] T015 [P] [US1] Escrever testes de persistência, paginação, filtro de status e ordenação de imagens em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/persistence/CatalogPersistenceIntegrationTest.kt`
- [ ] T016 [P] [US1] Escrever testes HTTP anônimos para listagem, detalhe, produto inativo, produto inexistente e catálogo vazio em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/http/PublicCatalogControllerIntegrationTest.kt`

### Implementation for User Story 1

- [ ] T017 [P] [US1] Implementar `Category` e suas regras de nome em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/catalog/Category.kt`
- [ ] T018 [P] [US1] Implementar `ProductImage` com URL, `externalId`, principal e ordem em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/catalog/ProductImage.kt`
- [ ] T019 [US1] Implementar `Product` e `ProductStatus` com invariantes de ativação e mídia em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/catalog/Product.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/catalog/ProductStatus.kt`
- [ ] T020 [P] [US1] Criar tabelas, constraints e índices básicos de categoria, produto e imagem em `src/main/resources/db/migration/V2__create_catalog.sql`
- [ ] T021 [US1] Implementar entidades JPA e mapeamento de catálogo em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/catalog/CategoryJpaEntity.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/catalog/ProductJpaEntity.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/catalog/ProductImageJpaEntity.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/catalog/CatalogPersistenceMapper.kt`
- [ ] T022 [US1] Implementar ports e adapter de leitura paginada do catálogo em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/catalog/CatalogQueryRepository.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/catalog/JpaCatalogQueryRepository.kt`
- [ ] T023 [P] [US1] Criar catálogo de erros estáveis do catálogo em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/catalog/CatalogError.kt`
- [ ] T024 [P] [US1] Implementar os casos de uso de listagem e detalhe público em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/ListPublicCatalogUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/GetPublicProductUseCase.kt`
- [ ] T025 [US1] Implementar DTOs e mapeamento de categoria, produto e imagens sem expor `externalId` em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/catalog/CatalogResponses.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/catalog/CatalogResponseMapper.kt`
- [ ] T026 [US1] Implementar endpoints públicos de listagem e detalhe conforme OpenAPI em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/catalog/PublicCatalogController.kt`

**Checkpoint**: US1 funciona anonimamente e pode ser validada sem autenticação ou operações administrativas.

---

## Phase 4: User Story 2 - Demonstrar interesse e continuar no WhatsApp (Priority: P1)

**Goal**: Permitir cadastro/login de cliente, autenticação Google, tokens próprios, interesse idempotente e redirecionamento `wa.me` com criação atômica do acompanhamento pendente.

**Independent Test**: Cadastrar e autenticar um cliente, confirmar interesse em produto ativo, receber a URL do WhatsApp e verificar exatamente um `Interest`, um `Reminder` e um `ContactRecord PENDING`, inclusive após retry concorrente.

### Tests for User Story 2

- [ ] T027 [P] [US2] Escrever testes unitários de claims, assinatura, audiência, tipo e expiração dos JWT próprios de access e refresh em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/infrastructure/security/JwtTokenServiceTest.kt`
- [ ] T028 [P] [US2] Escrever testes de integração de `GET /auth/csrf`, header/cookie correspondentes, rotação, revogação, logout, expiração e reuso da família de refresh tokens em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/security/RefreshTokenIntegrationTest.kt`
- [ ] T029 [P] [US2] Escrever testes de integração do cadastro sempre CLIENT, senha BCrypt, login válido/inválido, rate limiting com `ApiErrorResponse` HTTP 429, expiração da janela e tentativas válidas normais sem bloqueio indevido em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/security/PasswordAuthenticationIntegrationTest.kt` e `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/security/AuthenticationRateLimitIntegrationTest.kt`
- [ ] T030 [P] [US2] Escrever testes do login/cadastro Google, handoff e redirects fixos sem tokens na URL, vínculo somente por `sub`, rejeição de conflito por e-mail e restrição da única ADMIN em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/security/GoogleAuthenticationIntegrationTest.kt`
- [ ] T031 [P] [US2] Escrever testes da matriz anônimo/CLIENT/ADMIN, respostas 401/403 próprias, JWT ausente/expirado/adulterado, CORS configurado e consentimento sem efeito nas permissões em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/security/SecurityAuthorizationIntegrationTest.kt`
- [ ] T032 [P] [US2] Escrever testes unitários do interesse, produto inativo, idempotência e criação do acompanhamento pendente em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/application/interest/RegisterProductInterestUseCaseTest.kt`
- [ ] T033 [P] [US2] Escrever teste PostgreSQL de retry e concorrência garantindo um único conjunto Interest/Reminder/ContactRecord em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/persistence/InterestFollowUpIntegrationTest.kt`
- [ ] T034 [P] [US2] Escrever testes HTTP de bootstrap por `GET /auth/csrf`, troca/conclusão do handoff com cookie/header CSRF, confirmação de interesse e recuperação do link WhatsApp conforme OpenAPI em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/http/AuthenticationAndInterestControllerIntegrationTest.kt`

### Implementation for User Story 2

- [ ] T035 [P] [US2] Implementar `Account`, `AccountRole` e regras que impedem cadastro público ADMIN em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/account/Account.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/account/AccountRole.kt`
- [ ] T036 [P] [US2] Implementar `Customer` com telefone normalizado e consentimento inicialmente negado em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/customer/Customer.kt`
- [ ] T037 [P] [US2] Implementar vínculo Google por provider/sub em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/account/AccountExternalIdentity.kt`
- [ ] T038 [P] [US2] Implementar estados do refresh token rotativo e do handoff OAuth temporário em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/account/RefreshToken.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/account/OAuthHandoff.kt`
- [ ] T039 [P] [US2] Implementar o registro imutável `Interest` e sua chave idempotente em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/interest/Interest.kt`
- [ ] T040 [P] [US2] Implementar o estado automático pendente de `ContactRecord` vinculado ao interesse em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/crm/ContactRecord.kt`
- [ ] T041 [P] [US2] Implementar Reminder automático, pendente e vinculado obrigatoriamente ao Interest e ContactRecord em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/crm/Reminder.kt`
- [ ] T042 [US2] Criar tabelas e constraints de Interest, ContactRecord e Reminder, incluindo unicidade por interesse em `src/main/resources/db/migration/V3__create_interests_crm_and_follow_up_links.sql`
- [ ] T043 [US2] Criar índices operacionais previstos no plano em `src/main/resources/db/migration/V4__create_operational_indexes.sql`
- [ ] T044 [P] [US2] Criar catálogos globalmente únicos de autenticação, conta, segurança e interesse em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/AuthError.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/account/AccountError.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/security/SecurityError.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/interest/InterestError.kt`
- [ ] T045 [P] [US2] Definir principal autenticado próprio e ports de Account, Customer, identidade externa, refresh token, handoff OAuth e emissão de tokens em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/security/AuthenticatedAccount.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/account/AccountRepository.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/customer/CustomerRepository.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/account/ExternalIdentityRepository.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/security/RefreshTokenRepository.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/security/OAuthHandoffRepository.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/security/TokenIssuer.kt`
- [ ] T046 [US2] Implementar entidades JPA e mapper de Account, Customer e identidade externa em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/account/AccountJpaEntity.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/customer/CustomerJpaEntity.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/account/ExternalIdentityJpaEntity.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/account/IdentityPersistenceMapper.kt`
- [ ] T047 [US2] Implementar persistência de refresh tokens e handoffs com hashes, locks e consumo único em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/account/RefreshTokenJpaEntity.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/account/JpaRefreshTokenRepository.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/account/OAuthHandoffJpaEntity.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/account/JpaOAuthHandoffRepository.kt`
- [ ] T048 [US2] Implementar adapters de Account, Customer e identidade externa em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/account/JpaIdentityRepositories.kt`
- [ ] T049 [US2] Implementar cadastro tradicional e autenticação por senha com `DelegatingPasswordEncoder`/BCrypt em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/RegisterCustomerWithPasswordUseCase.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/LoginWithPasswordUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/PasswordSecurityConfiguration.kt`
- [ ] T050 [US2] Configurar chaves assimétricas, issuer, audiences, `kid` e durações dos tokens em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/JwtProperties.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/JwtKeyConfiguration.kt`
- [ ] T051 [US2] Implementar emissão e validação dos JWT próprios sem claims pessoais em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/JwtTokenService.kt`
- [ ] T052 [US2] Implementar emissão, rotação, reuso e revogação da família de refresh tokens em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/IssueTokenPairUseCase.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/RefreshAccessTokenUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/LogoutUseCase.kt`
- [ ] T053 [US2] Implementar armazenamento temporário assinado/cifrado de state, nonce e intenção OAuth em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/oauth/OAuthAuthorizationRequestCookieRepository.kt`
- [ ] T054 [US2] Implementar autenticação Google, criação/consumo único do handoff, vínculo exclusivo por `sub`, rejeição quando e-mail local já existe, troca para tokens próprios, conclusão de cadastro e restrição da ADMIN em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/CompleteGoogleLoginUseCase.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/ExchangeGoogleHandoffUseCase.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/auth/CompleteGoogleCustomerRegistrationUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/oauth/GoogleIdentityVerifier.kt`
- [ ] T055 [US2] Implementar requests, responses, callback com redirect fixo e endpoints de cadastro, login, CSRF, troca Google, refresh, logout e conta atual em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/auth/AuthRequests.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/auth/AuthResponses.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/auth/AuthController.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/auth/CsrfController.kt`
- [ ] T056 [US2] Configurar API stateless e implementar rate limiting local do login tradicional por origem/identificador, contando falhas, limpando após sucesso ou expiração e respondendo HTTP 429 pelo `ApiErrorResponse`, junto de `@EnableMethodSecurity`, Resource Server JWT, OAuth2 Client, `CookieCsrfTokenRepository`, CORS sem curinga, rotas públicas, handlers 401/403 e autorização por papel em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/SecurityConfiguration.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/AuthenticationRateLimiter.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/security/AuthenticationRateLimitProperties.kt`
- [ ] T057 [P] [US2] Definir port e gerar somente URL `wa.me` com número e texto controlados em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/whatsapp/WhatsappLinkGenerator.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/whatsapp/WhatsappProperties.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/whatsapp/WaMeLinkGenerator.kt`
- [ ] T058 [US2] Implementar entidades JPA e mapper de interesse, contato e lembrete em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/interest/InterestJpaEntity.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/crm/ContactRecordJpaEntity.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/crm/ReminderJpaEntity.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/crm/CrmPersistenceMapper.kt`
- [ ] T059 [US2] Implementar repositórios com idempotência e unicidade do acompanhamento em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/interest/InterestRepository.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/crm/FollowUpRepository.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/interest/JpaInterestRepository.kt`
- [ ] T060 [US2] Implementar transação de confirmação que cria Interest, Reminder e ContactRecord uma única vez em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/interest/RegisterProductInterestUseCase.kt`
- [ ] T061 [US2] Implementar recuperação autorizada do mesmo contexto WhatsApp sem novos registros em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/interest/GetInterestWhatsappLinkUseCase.kt`
- [ ] T062 [US2] Implementar endpoints e DTOs de interesse e link WhatsApp conforme OpenAPI em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/interest/InterestController.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/interest/InterestResponses.kt`

**Checkpoint**: US1 e US2 entregam o fluxo catálogo → identificação → interesse → acompanhamento interno → WhatsApp, sem negociação dentro da aplicação.

---

## Phase 5: User Story 3 - Administrar o catálogo (Priority: P2)

**Goal**: Permitir à ADMIN manter categorias, produtos e imagens com Cloudinary isolado e sem publicar estado parcial.

**Independent Test**: Autenticar a ADMIN autorizada, criar categoria e produto com múltiplas imagens, trocar principal, alterar/inativar o produto e simular falhas de mídia sem referências inválidas.

### Tests for User Story 3

- [ ] T063 [P] [US3] Escrever testes de domínio para adição, troca de principal, remoção e ativação de imagens em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/domain/catalog/ProductImageManagementTest.kt`
- [ ] T064 [P] [US3] Escrever testes dos casos de uso de mídia cobrindo upload, compensação e falhas após commit em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/application/media/ProductMediaUseCasesTest.kt`
- [ ] T065 [P] [US3] Escrever testes HTTP de categorias, produtos, multipart, status e autorização ADMIN em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/http/AdminCatalogControllerIntegrationTest.kt`
- [ ] T066 [P] [US3] Escrever teste isolado do adapter Cloudinary com cliente simulado, sem rede real em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/infrastructure/media/CloudinaryMediaStorageTest.kt`

### Implementation for User Story 3

- [ ] T067 [P] [US3] Criar catálogo globalmente único de erros de mídia e integrar os erros de catálogo administrativo em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/media/MediaError.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/catalog/CatalogError.kt`
- [ ] T068 [P] [US3] Definir tipos neutros e port `MediaStorage` em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/media/MediaStorage.kt`
- [ ] T069 [US3] Implementar propriedades, cliente e adapter Cloudinary restritos a infrastructure em `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/media/CloudinaryProperties.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/media/CloudinaryConfiguration.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/media/CloudinaryMediaStorage.kt`
- [ ] T070 [US3] Implementar port e adapter de escrita de catálogo com lock no produto em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/catalog/CatalogCommandRepository.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/catalog/JpaCatalogCommandRepository.kt`
- [ ] T071 [P] [US3] Implementar criação, listagem administrativa e renomeação de categoria em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/CreateCategoryUseCase.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/ListAdminCategoriesUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/RenameCategoryUseCase.kt`
- [ ] T072 [US3] Implementar criação, consulta administrativa, atualização e mudança de status de produto em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/CreateProductUseCase.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/GetAdminProductUseCase.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/UpdateProductUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/catalog/ChangeProductStatusUseCase.kt`
- [ ] T073 [US3] Implementar adição, definição de principal e remoção compensada de imagens em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/media/AddProductImagesUseCase.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/media/SetPrimaryProductImageUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/media/RemoveProductImageUseCase.kt`
- [ ] T074 [P] [US3] Implementar DTOs e endpoints administrativos de categoria em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/catalog/AdminCategoryRequests.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/catalog/AdminCategoryController.kt`
- [ ] T075 [US3] Implementar DTOs e endpoints administrativos de produto multipart e status em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/catalog/AdminProductRequests.kt`, `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/catalog/AdminProductResponses.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/catalog/AdminProductController.kt`
- [ ] T076 [US3] Implementar endpoints administrativos do ciclo de imagens em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/catalog/AdminProductImageController.kt`

**Checkpoint**: US3 administra o catálogo sem acesso direto do frontend ao Cloudinary e preserva as invariantes do produto.

---

## Phase 6: User Story 4 - Consultar a base de clientes e seus interesses (Priority: P2)

**Goal**: Permitir à ADMIN pesquisar clientes e consultar contexto e interesses sem expor CRM a clientes.

**Independent Test**: Com dados cadastrados, pesquisar por nome/e-mail/telefone, consultar um cliente e seus interesses e verificar `403` para CLIENT.

### Tests for User Story 4

- [ ] T077 [P] [US4] Escrever testes PostgreSQL das consultas paginadas por nome, e-mail, telefone e interesses em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/persistence/CustomerCrmQueryIntegrationTest.kt`
- [ ] T078 [P] [US4] Escrever testes HTTP de contexto vazio, paginação, cliente inexistente e proibição para CLIENT em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/http/AdminCustomerCrmControllerIntegrationTest.kt`

### Implementation for User Story 4

- [ ] T079 [P] [US4] Criar catálogo globalmente único de erros de consulta do CRM em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/CrmQueryError.kt`
- [ ] T080 [US4] Implementar projeções e adapter de leitura do CRM sem carregar agregados desnecessários em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/CustomerCrmViews.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/crm/JpaCustomerCrmQueryRepository.kt`
- [ ] T081 [P] [US4] Implementar pesquisa paginada de clientes em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/SearchCustomersUseCase.kt`
- [ ] T082 [P] [US4] Implementar consulta do contexto comercial e consentimento vigente em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/GetCustomerContextUseCase.kt`
- [ ] T083 [P] [US4] Implementar listagem cronológica dos interesses e produtos de um cliente em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/ListCustomerInterestsUseCase.kt`
- [ ] T084 [US4] Implementar DTOs e endpoints ADMIN de clientes, contexto e interesses em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/crm/CustomerCrmResponses.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/crm/AdminCustomerController.kt`

**Checkpoint**: US4 disponibiliza a base e os interesses apenas à ADMIN, com paginação e estados vazios válidos.

---

## Phase 7: User Story 5 - Registrar contatos e acompanhar atendimentos (Priority: P3)

**Goal**: Manter contatos, lembretes e consentimento, distinguindo acompanhamento pendente de conversa concluída e concluindo contato/lembrete automaticamente vinculados na mesma transação.

**Independent Test**: Registrar um contato manual, consultar o Reminder automático e seu vencimento, revogar consentimento proativo e concluir um ContactRecord pendente verificando a conclusão atômica do Reminder.

### Tests for User Story 5

- [ ] T085 [P] [US5] Escrever testes de domínio para contato pendente/concluído, Reminder automático, vencimento, acionabilidade e conclusão exclusiva via ContactRecord em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/domain/crm/ContactRecordAndReminderTest.kt`
- [ ] T086 [P] [US5] Escrever testes de domínio do opt-in, revogação idempotente e independência do atendimento solicitado em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/domain/customer/ProactiveContactConsentTest.kt`
- [ ] T087 [P] [US5] Escrever testes PostgreSQL de contatos manuais, lembretes automáticos, ordenação, vínculo obrigatório ao Interest e estados derivados em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/persistence/CrmContactReminderIntegrationTest.kt`
- [ ] T088 [P] [US5] Escrever teste transacional de conclusão conjunta, retry e rollback de ContactRecord/Reminder em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/persistence/CompletePendingContactIntegrationTest.kt`
- [ ] T089 [P] [US5] Escrever testes HTTP de consentimento, contatos, listagem de lembretes e conclusão do Reminder somente pela finalização do ContactRecord em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/http/ContactReminderConsentControllerIntegrationTest.kt`

### Implementation for User Story 5

- [ ] T090 [P] [US5] Criar catálogo globalmente único de erros de contatos, lembretes e consentimento em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/crm/CrmError.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/customer/CustomerError.kt`
- [ ] T091 [P] [US5] Completar comportamento de concessão/revogação de consentimento em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/customer/Customer.kt`
- [ ] T092 [P] [US5] Completar regras de contato manual, complementação e transição PENDING→COMPLETED em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/crm/ContactRecord.kt`
- [ ] T093 [P] [US5] Completar regras do Reminder automático vinculado ao Interest, vencimento, acionabilidade enquanto pendente e conclusão conjunta em `src/main/kotlin/com/viniciusdevassis/laumileymodas/domain/crm/Reminder.kt`
- [ ] T094 [US5] Ampliar ports e adapters do CRM para leitura, escrita e locks de conclusão em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/port/crm/CrmRepository.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/infrastructure/persistence/crm/JpaCrmRepository.kt`
- [ ] T095 [P] [US5] Implementar consulta e alteração idempotente do consentimento do próprio CLIENT em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/customer/GetProactiveContactConsentUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/customer/SetProactiveContactConsentUseCase.kt`
- [ ] T096 [P] [US5] Implementar registro manual e listagem de contatos pendentes/concluídos em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/RecordContactUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/ListCustomerContactsUseCase.kt`
- [ ] T097 [P] [US5] Implementar consultas paginadas dos lembretes automáticos pendentes e por cliente em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/ListPendingRemindersUseCase.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/ListCustomerRemindersUseCase.kt`
- [ ] T098 [US5] Implementar conclusão transacional e idempotente do contato pendente e lembrete associado em `src/main/kotlin/com/viniciusdevassis/laumileymodas/application/crm/CompletePendingContactRecordUseCase.kt`
- [ ] T099 [P] [US5] Implementar DTOs e endpoints do consentimento do cliente em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/customer/ContactConsentController.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/customer/ContactConsentDtos.kt`
- [ ] T100 [US5] Implementar DTOs e endpoints administrativos de contatos em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/crm/ContactController.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/crm/ContactDtos.kt`
- [ ] T101 [US5] Implementar DTOs e endpoints administrativos somente de consulta de lembretes em `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/crm/ReminderController.kt` e `src/main/kotlin/com/viniciusdevassis/laumileymodas/presentation/admin/crm/ReminderDtos.kt`

**Checkpoint**: US5 mantém histórico confiável, acompanhamento pendente, lembretes e consentimento sem automação de mensagens.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Validar o conjunto completo, contratos e controles transversais sem ampliar o escopo.

- [ ] T102 [P] Criar teste que percorre todos os catálogos e falha para códigos de erro duplicados em `src/test/kotlin/com/viniciusdevassis/laumileymodas/unit/common/ErrorCodeUniquenessTest.kt`
- [ ] T103 [P] Criar testes de sanitização de logs e respostas para senha, tokens, hashes, Cloudinary e dados de outro cliente em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/security/SensitiveDataExposureIntegrationTest.kt`
- [ ] T104 Validar migrations do zero, constraints parciais, chaves estrangeiras e índices em PostgreSQL real em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/persistence/CompleteSchemaIntegrationTest.kt`
- [ ] T105 Revisar status HTTP, media types, schemas e campos de todos os controllers contra `specs/001-v1-commercial-flows/contracts/openapi.yaml` e consolidar o teste em `src/test/kotlin/com/viniciusdevassis/laumileymodas/integration/http/OpenApiContractRegressionTest.kt`
- [ ] T106 Executar os cenários ponta a ponta e registrar eventuais correções no guia `specs/001-v1-commercial-flows/quickstart.md`
- [ ] T107 Executar `./mvnw test` a partir de `pom.xml` e corrigir somente falhas relacionadas aos requisitos da V1 nos arquivos correspondentes de `src/main/` e `src/test/`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 — Setup**: sem dependências.
- **Phase 2 — Foundational**: depende da Phase 1 e bloqueia todas as user stories.
- **US1 (Phase 3)**: depende da Foundation; entrega o catálogo público e a migration V2.
- **US2 (Phase 4)**: depende da US1 porque o interesse exige produto ativo e porque a migration V3 sucede o catálogo.
- **US3 (Phase 5)**: depende de US1 para o domínio do catálogo e de US2 para autenticação ADMIN.
- **US4 (Phase 6)**: depende de US2 para Account, Customer, Interest e autenticação ADMIN; pode avançar em paralelo com US3.
- **US5 (Phase 7)**: depende de US2 para o acompanhamento automático e de US4 para as consultas do contexto do cliente.
- **Polish (Phase 8)**: depende das stories incluídas na entrega.

### User Story Dependency Graph

```text
Setup → Foundation → US1 → US2 ─┬→ US3 ─┐
                                └→ US4 ─┴→ US5 → Polish
```

### Within Each User Story

1. Escrever os testes indicados e confirmar que falham pelo comportamento ainda ausente.
2. Implementar modelos e regras de domínio.
3. Aplicar migrations antes dos adapters que dependem delas.
4. Implementar ports e adapters de persistência/integração.
5. Implementar casos de uso e limites transacionais.
6. Implementar DTOs e controllers.
7. Executar os testes da story e validar seu checkpoint antes de avançar.

## Parallel Opportunities

- Na Foundation, T007–T010 e T012 podem avançar em arquivos distintos após T001–T006.
- Na US1, T014–T016 e T017–T018 podem ser preparados em paralelo.
- Na US2, T027–T034 e os modelos T035–T041 podem ser divididos; integração começa após seus contratos estarem definidos.
- Na US3, T063–T066 podem ser escritos em paralelo; T067 e T068 também não compartilham arquivos.
- US3 e US4 podem avançar em paralelo depois da US2.
- Na US5, T085–T089 e T090–T093 podem ser divididos entre testes, erros e regras de domínio.
- T102 e T103 podem avançar em paralelo antes da validação final T104–T107.

## Parallel Example: User Story 1

```text
T014 ProductTest.kt
T015 CatalogPersistenceIntegrationTest.kt
T016 PublicCatalogControllerIntegrationTest.kt
T017 Category.kt
T018 ProductImage.kt
```

## Parallel Example: User Story 2

```text
T027 JwtTokenServiceTest.kt
T028 RefreshTokenIntegrationTest.kt
T029 PasswordAuthenticationIntegrationTest.kt
T030 GoogleAuthenticationIntegrationTest.kt
T031 SecurityAuthorizationIntegrationTest.kt
T032 RegisterProductInterestUseCaseTest.kt
T033 InterestFollowUpIntegrationTest.kt
T034 AuthenticationAndInterestControllerIntegrationTest.kt
```

## Parallel Example: User Story 3

```text
T063 ProductImageManagementTest.kt
T064 ProductMediaUseCasesTest.kt
T065 AdminCatalogControllerIntegrationTest.kt
T066 CloudinaryMediaStorageTest.kt
```

## Parallel Example: User Story 4

```text
T077 CustomerCrmQueryIntegrationTest.kt
T078 AdminCustomerCrmControllerIntegrationTest.kt
T081 SearchCustomersUseCase.kt
T082 GetCustomerContextUseCase.kt
T083 ListCustomerInterestsUseCase.kt
```

## Parallel Example: User Story 5

```text
T085 ContactRecordAndReminderTest.kt
T086 ProactiveContactConsentTest.kt
T087 CrmContactReminderIntegrationTest.kt
T088 CompletePendingContactIntegrationTest.kt
T089 ContactReminderConsentControllerIntegrationTest.kt
```

## Implementation Strategy

### MVP First

1. Concluir Setup e Foundation.
2. Implementar US1.
3. Validar catálogo público anonimamente.
4. Este é o menor incremento demonstrável; o fluxo comercial principal exige adicionar US2.

### Commercial Flow Increment

1. Adicionar US2 sobre US1.
2. Validar cadastro/login, interesse idempotente, acompanhamento pendente e URL `wa.me`.
3. Confirmar que nenhuma venda, mensagem automática ou API do WhatsApp foi introduzida.

### Incremental Delivery

1. Setup + Foundation → base técnica validada.
2. US1 → catálogo público.
3. US2 → fluxo comercial principal até WhatsApp.
4. US3 e US4 → administração de catálogo e consulta do CRM.
5. US5 → histórico, lembretes, consentimento e conclusão do atendimento.
6. Polish → validação integral da V1.

## Notes

- O backend é executado normalmente pela IDE/Maven; Docker é exigido apenas pelo PostgreSQL efêmero dos testes Testcontainers.
- Cloudinary e Google devem ser simulados na suíte comum; não usar rede nem segredos reais em testes.
- O `externalId` da mídia permanece interno, embora a API devolva ao frontend as URLs ordenadas de todas as imagens.
- Lembrete automático de interesse não pode ser concluído isoladamente; sua conclusão ocorre com o `ContactRecord`.
- Nenhuma tarefa cria WhatsApp Business API, mensageria, cache distribuído, microsserviço ou automação de comunicação.
- Realizar commits após tarefas ou grupos lógicos pequenos, sempre com mensagem em português do Brasil.
