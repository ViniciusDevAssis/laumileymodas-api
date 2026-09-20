# Research: Operação Comercial V1

Este documento registra somente decisões técnicas necessárias para implementar a specification.
Cada decisão aplica o teste de necessidade da Constitution: uma abstração só permanece quando
protege uma regra, um limite de segurança, uma integração externa ou reutilização concreta.

## 1. Organização pragmática

**Decision**: usar o fluxo predominante `Controller -> Service -> Spring Data Repository ->
entidade JPA`, organizado pelas responsabilidades `presentation`, `application`, `domain` e
`infrastructure`.

**Rationale**: as quatro responsabilidades continuam claras sem duplicar o mesmo conceito em
modelos diferentes. Services coesos por contexto concentram transações e orquestração; entidades
JPA representam o negócio; repositories Spring Data cuidam da persistência; controllers tratam
HTTP e HATEOAS.

**Alternatives considered**:

- uma classe de caso de uso por operação: rejeitada por fragmentar fluxos simples;
- ports e adapters para cada repository: rejeitados porque existe uma única persistência interna;
- entidades de domínio separadas de entidades JPA: rejeitadas porque os modelos seriam equivalentes;
- arquitetura hexagonal completa ou Clean Architecture estrita: rejeitada por não haver necessidade
  atual que compense o custo.

## 2. Estrutura de código

**Decision**: adotar a estrutura abaixo, permitindo subpackages apenas quando um contexto crescer:

```text
presentation/
  controllers/
  dtos/
  advice/
application/
  AuthService.kt
  GoogleAuthService.kt
  CatalogService.kt
  InterestService.kt
  CrmService.kt
  ProductMediaService.kt
domain/
  entities/
  enums/
  exceptions/
infrastructure/
  repositories/
  security/
  cloudinary/
  config/
```

**Rationale**: esses services correspondem a contextos funcionais reais. Novos services ou helpers
só serão criados quando uma classe deixar de ser coesa ou houver reutilização real.

**Alternatives considered**: packages por camada com subestrutura completa por agregado e packages
por feature com cópias das quatro camadas; ambos foram rejeitados na V1 por multiplicarem arquivos
sem melhorar o comportamento.

## 3. REST e hipermídia

**Decision**: modelar URLs com recursos e semântica HTTP; adicionar Spring HATEOAS e representar
recursos com `EntityModel`, coleções paginadas com `PagedModel` e mídia `application/hal+json`.
Links serão montados nos controllers ou por pequenas funções locais. Um assembler dedicado só será
extraído quando houver duplicação relevante.

**Rationale**: hipermídia permite que o frontend descubra navegação e ações válidas sem transformar
o projeto em uma hierarquia de assemblers. `201 Created` inclui `Location`; alterações parciais e
transições usam `PATCH`; substituições de singleton usam `PUT`; remoções usam `DELETE`; respostas sem
corpo usam `204`.

**Links relevantes**:

- produtos públicos: `self`, coleção, categoria e criação de interesse quando aplicável;
- interesse: `self`, produto e URL externa do WhatsApp;
- recursos administrativos: links somente para ações válidas no estado atual;
- `ContactRecord` pendente: link para conclusão; concluído não anuncia nova conclusão;
- `Reminder`: consulta e contato correspondente, sem link de conclusão independente.

Tokens, respostas CSRF, redirecionamentos OAuth e erros não recebem hipermídia porque são mensagens
de protocolo ou falha, não recursos navegáveis do negócio.

**Alternatives considered**: HATEOAS completo com assembler por DTO, affordances e perfis desde o
início foi rejeitado por cerimônia; JSON sem links foi rejeitado pela decisão explícita do projeto.

Referência: [Spring HATEOAS](https://docs.spring.io/spring-hateoas/docs/current/reference/html/).

## 4. Persistência e modelo

**Decision**: usar entidades JPA como modelo central, repositories Spring Data diretos, PostgreSQL,
Flyway como autoridade do schema e Hibernate com `ddl-auto=validate`.

**Rationale**: elimina modelos, mappers e adapters duplicados sem abrir mão de migrations,
constraints e transações. IDs usam UUID gerado pela aplicação/JPA, sem `IdGenerator`. Regras que
dependem do tempo recebem `java.time.Clock` diretamente no service correspondente.

**Alternatives considered**: `EntityManager` encapsulado em adapters, DAOs manuais e H2 foram
rejeitados. Testes de integração usam o mesmo PostgreSQL da produção por Testcontainers.

## 5. Acompanhamento do interesse

**Decision**: a confirmação idempotente de um `Interest` cria, na mesma transação, exatamente um
`Reminder` e um `ContactRecord PENDING`. `Reminder.interest_id` e
`ContactRecord.interest_id` são únicos; ambos se relacionam pelo interesse e não mantêm referência
circular entre si.

**Rationale**: constraints únicas garantem a cardinalidade exigida e o mesmo `interest_id` fornece
o vínculo necessário. A conclusão do contato atualiza o `ContactRecord` e seu `Reminder` na mesma
transação. Não há endpoint nem service para criar ou concluir Reminder isoladamente.

**Alternatives considered**: `contact_record.reminder_id` mais `reminder.contact_record_id` foi
rejeitado por circularidade; eventos ou mensageria foram rejeitados porque a consistência precisa
ser imediata dentro de um único monólito.

## 6. Imagens e Cloudinary

**Decision**: manter somente a interface `MediaStorage` na aplicação e uma implementação
`CloudinaryMediaStorage` na infraestrutura. `ProductMediaService` coordena upload/exclusão e
persistência das referências `url` e `externalId`.

**Rationale**: Cloudinary é um limite externo real e a Constitution exige isolamento. A relação
permanece `Product 1:N ProductImage`, com ordem e exatamente uma imagem principal. Consultas de
produto retornam as URLs ordenadas.

**Consistência**:

- upload externo ocorre antes da persistência da referência;
- se a persistência falhar, o service tenta remover o asset recém-enviado;
- exclusão local só é confirmada depois da exclusão externa bem-sucedida;
- falhas de compensação são registradas sem expor credenciais ou resposta do provedor;
- nenhuma transação distribuída ou fila é introduzida na V1.

**Alternatives considered**: SDK do Cloudinary dentro da entidade/service, upload direto do
frontend e microsserviço de mídia foram rejeitados pela Constitution.

## 7. Autenticação local e access token

**Decision**: usar Spring Security stateless, `@EnableMethodSecurity`, BCrypt via
`DelegatingPasswordEncoder` com custo 12 e access token JWT curto assinado com HMAC SHA-256 por um
segredo externo forte. O JWT contém somente `sub` da Account, papel, emissor, audiência e tempos.

**Rationale**: apenas o monólito emite e valida o token na V1; HMAC reduz configuração de chaves sem
reduzir a proteção nesse limite. O Resource Server do Spring Security valida assinatura, expiração,
emissor e audiência. Controllers extraem apenas UUID e papel, sem levar tipos do Spring Security às
entidades.

**Alternatives considered**: RSA, `kid` e JWKS foram rejeitados porque nenhum outro serviço valida
os tokens; sessão de servidor foi rejeitada porque a API é stateless.

Referência: [Spring Security JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).

## 8. Refresh token, logout e CSRF

**Decision**: usar refresh token opaco, aleatório e de alta entropia. Apenas seu hash é persistido.
O token fica em cookie `HttpOnly`, `Secure` em produção, `SameSite` configurável e restrito a
`/api/v1/auth`. A rotação consome o token atual e cria sucessor da mesma família; reuso revoga a
família.

`CookieCsrfTokenRepository` publica `XSRF-TOKEN` legível pelo frontend e valida o header
`X-XSRF-TOKEN`. `GET /auth/csrf` materializa o token sem autenticar ou emitir credenciais. CSRF é
exigido apenas nos endpoints que consomem cookies: refresh, logout e troca/conclusão do handoff
Google. Bearer-only endpoints não dependem de cookie e são ignorados pelo matcher CSRF.

**Rationale**: refresh não precisa ser JWT; opacidade reduz claims e chaves. CSRF permanece
necessário porque o navegador envia cookies automaticamente.

**Alternatives considered**: refresh JWT e CSRF desabilitado globalmente foram rejeitados.

Referência: [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

## 9. Google OpenID Connect

**Decision**: usar OAuth2 Login/OIDC do Spring Security com escopos `openid`, `profile` e `email`.
Existem dois pontos de início, `/auth/google/client` e `/auth/google/admin`, cuja intenção é
protegida no `state`. Ambos convergem para o registration Google e para o callback público
`/api/v1/auth/google/callback`.

O `sub` é o identificador confiável. Login só prossegue quando o `sub` já está vinculado. Um novo
`sub` sem Account inicia cadastro de cliente. Se o e-mail já pertence a Account sem aquele vínculo,
o fluxo é rejeitado; não há linking automático nem manual na V1. O fluxo ADMIN exige que o `sub`
seja exatamente o valor configurado no backend e nunca promove uma Account pública.

Após o callback, um handoff opaco de uso único e curta duração é guardado em cookie `HttpOnly`; seu
hash e estado mínimo ficam no PostgreSQL. O backend redireciona somente para URLs configuradas. O
frontend troca o handoff com CSRF; tokens sensíveis nunca aparecem na URL. Cliente Google novo
informa apenas dados adicionais necessários antes da troca final.

**Rationale**: o registro temporário permite uso único e invalidação confiável sem expor identidade
ou token na URL. O domínio persiste apenas identidade externa genérica (`provider`, `subject`).

**Alternatives considered**: vincular por e-mail, callback com JSON, access token na URL e redirect
URL fornecida pelo cliente foram rejeitados por risco de tomada de conta ou vazamento.

Referência: [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html).

## 10. CORS e ambientes

**Decision**: CORS é centralizado no Spring Security. Desenvolvimento aceita apenas as origens
locais configuradas; produção exige lista explícita do domínio oficial. Nunca se combina origem
curinga com credenciais. As configurações variam por ambiente sem duplicar regras de negócio.

**Rationale**: mantém o desenvolvimento simples e torna a restrição de produção explícita antes da
publicação.

## 11. Rate limiting

**Decision**: implementar um limitador em memória para tentativas falhas de autenticação, adequado à
única instância da V1. A chave combina origem e identificador normalizado sem registrar senha. Uma
autenticação bem-sucedida limpa o contador aplicável; excesso retorna `429` no mesmo
`ApiErrorResponse`.

**Rationale**: protege o login sem Redis ou infraestrutura distribuída. A limitação é documentada
como local à instância e pode evoluir somente se houver múltiplas instâncias.

## 12. Erros

**Decision**: usar catálogos `ApiError` com códigos globalmente únicos, uma `ApiException` com status
e erro, `GlobalExceptionHandler` e `ApiErrorResponse`. Validação, `AuthenticationEntryPoint` e
`AccessDeniedHandler` produzem o mesmo payload. Falhas inesperadas retornam `500` com código estável
e sem detalhes internos.

**Rationale**: um contrato único simplifica clientes e mantém mensagens seguras. Não há hierarquia
de exceptions por camada nem RFC ProblemDetail.

## 13. WhatsApp

**Decision**: `InterestService` monta a URL `https://wa.me/{numero}?text={texto}` usando número e
template configurados, codifica os parâmetros e inclui apenas identificação do produto. Não existe
`WhatsappLinkGenerator` porque a lógica é pequena e pertence ao fluxo.

**Rationale**: a V1 só redireciona; não usa WhatsApp Business API nem envia mensagens.

## 14. Testes

**Decision**: testes unitários cobrem regras puras com valor real; testes de integração com
PostgreSQL Testcontainers cobrem migrations, repositories, transações, segurança, REST/HATEOAS e
fluxos críticos. Cloudinary e Google são substituídos nos limites externos. Testcontainers existe
somente no escopo de teste; a aplicação roda pela IDE/Maven.

**Rationale**: os testes protegem comportamento e constraints reais sem fixar quantidade de classes
ou camadas.

## Decisões removidas do plano anterior

- JWT assimétrico, par de chaves e `kid`;
- refresh token JWT;
- ports/adapters para repositories internos;
- modelos de domínio e persistência duplicados;
- mappers triviais;
- `ClockProvider`, `IdGenerator` e `WhatsappLinkGenerator`;
- uma classe de use case por operação;
- referência circular entre `Reminder` e `ContactRecord`;
- metas p95 sem requisito de negócio;
- Spring Session, H2, ProblemDetail, mensageria, cache distribuído, CQRS e microsserviços.
