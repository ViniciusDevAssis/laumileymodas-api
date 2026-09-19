# Implementation Plan: Operação Comercial V1

**Branch**: `main` | **Feature context**: `001-v1-commercial-flows` | **Date**: 2026-09-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-v1-commercial-flows/spec.md`

## Summary

Implementar a V1 como uma única API REST em Kotlin e Spring Boot, com PostgreSQL como fonte de
verdade para contas, catálogo e CRM. O sistema seguirá as responsabilidades `presentation`,
`application`, `domain` e `infrastructure`; controllers chamarão casos de uso concretos e apenas
dependências externas ou de persistência receberão portas próprias.

A autenticação usará Spring Security, access tokens JWT próprios e refresh tokens rotativos, com
login de clientes por e-mail/senha ou Google OpenID Connect e login da única `ADMIN` somente pela
identidade Google autorizada da loja. Imagens serão recebidas pela API, armazenadas no Cloudinary por um adapter de
`infrastructure` e referenciadas localmente por URL e identificador externo. Interesses serão
idempotentes, criarão atomicamente seu lembrete e `ContactRecord` pendente e retornarão um link do
WhatsApp gerado pelo backend após a persistência.

## Technical Context

**Language/Version**: Kotlin 2.3.21 executando em Java 21

**Primary Dependencies**: Spring Boot 3.5.16, Spring Web MVC, Spring Validation, Spring Security,
Spring Security OAuth2 Client, Spring Security OAuth2 Resource Server/JOSE, Spring Data JPA, Flyway,
PostgreSQL Driver, Jackson Kotlin e Cloudinary Java SDK restrito à infraestrutura

**Storage**: PostgreSQL para dados, identidades externas e refresh tokens; Cloudinary para o conteúdo
das imagens; nenhum binário de imagem no banco ou no sistema de arquivos da aplicação

**Testing**: JUnit 5, Kotlin Test, Spring Boot Test, Spring Security Test, MockMvc, Testcontainers com
PostgreSQL e adapters falsos para integrações externas

**Target Platform**: serviço JVM executado em ambiente Linux com HTTPS no ponto de entrada

**Project Type**: API web monolítica

**Performance Goals**: leituras paginadas do catálogo e CRM com p95 de até 500 ms e comandos sem
upload com p95 de até 1 s sob a carga inicial; latência de upload é medida separadamente por depender
do provedor externo

**Constraints**: uma única aplicação backend; uma conta administrativa vinculada exclusivamente à
identidade Google autorizada; access token de curta duração e refresh token rotativo; paginação
máxima de 100 itens; uploads somente de imagens em formatos permitidos e com limite configurável;
nenhum preço, estoque, tamanho, checkout, pagamento ou automação comercial

**Scale/Scope**: uma instância de aplicação na V1, dezenas de acessos concorrentes, até 10 mil
clientes, 5 mil produtos e histórico compatível com uma única loja; os limites são premissas de
capacidade, não novos requisitos funcionais

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Gate | Pre-design result | Evidence |
|------|-------------------|----------|
| Spec First | PASS | O plano deriva de `spec.md`; nenhuma regra funcional nova substitui a specification. |
| Simplicidade da V1 | PASS | Um monólito, uma base PostgreSQL e integrações síncronas; sem mensageria, CQRS ou microsserviços. |
| Backend como autoridade | PASS | Autorização, invariantes, idempotência e mudanças de estado são aplicadas nos casos de uso. |
| Segurança e privacidade | PASS | JWT de curta duração, refresh token rotativo, conta Google administrativa restrita, papéis mínimos, consentimento opt-in e respostas sem dados sensíveis. |
| Regras testáveis | PASS | Regras de catálogo, interesse, autorização, consentimento e lembretes têm estratégia de testes. |
| Integrações isoladas | PASS | Cloudinary e geração do link do WhatsApp ficam atrás de adapters de `infrastructure`. |
| Responsabilidades claras | PASS | As quatro responsabilidades têm contratos explícitos e casos de uso pequenos. |
| Banco versionado | PASS | Flyway é a única fonte de criação e evolução do schema. |
| Qualidade antes da conclusão | PASS | Testes unitários, integração real com PostgreSQL e cenários ponta a ponta são gates. |
| Mídia externa | PASS | A API controla upload/exclusão; o banco mantém apenas referências; não há serviço separado. |

Não há violações que bloqueiem a pesquisa ou exijam exceção à Constitution.

## Architecture and Layer Contracts

### `presentation`

- Expõe controllers REST, requests, responses, paginação e o payload próprio `ApiErrorResponse`.
- Centraliza exceptions HTTP no `GlobalExceptionHandler`; validação, `AuthenticationEntryPoint` e
  `AccessDeniedHandler` escrevem o mesmo contrato de erro.
- Converte multipart em um tipo neutro de conteúdo antes de chamar `application`.
- Obtém a identidade do principal autenticado; nunca aceita do cliente um papel ou identidade para
  decidir autorização.
- Valida forma, tamanho e sintaxe de entrada. Não contém regras de negócio, transações, acesso a
  repositórios ou chamadas ao Cloudinary.

### `application`

- Contém um caso de uso por intenção relevante e coordena autorização contextual, domínio,
  persistência e integrações.
- Define os limites transacionais locais e os ports de saída necessários para repositórios,
  armazenamento de mídia, relógio e geração do contexto do WhatsApp.
- Usa classes concretas para casos de uso; interfaces de entrada só serão criadas quando houver
  mais de um consumidor ou benefício de teste que justifique a abstração.
- Não conhece DTOs HTTP, `MultipartFile`, SDK do Cloudinary ou detalhes JPA.

### `domain`

- Mantém modelos Kotlin e regras para status de produto, imagens principais, idempotência do
  interesse, consentimento e estados coordenados de `ContactRecord` e lembrete.
- Não depende de Spring, HTTP, JPA, Cloudinary ou formatos de resposta.
- Usa entidades e value objects somente onde protegem uma regra; projeções de leitura simples não
  precisam virar agregados ricos.

### `infrastructure`

- Implementa persistência JPA/PostgreSQL, migrations, Spring Security, validação dos JWT próprios,
  cliente Google OpenID Connect, Cloudinary, configuração e geração da URL do WhatsApp.
- Converte modelos persistidos e respostas de fornecedor para tipos definidos pela aplicação.
- Mantém credenciais exclusivamente em configuração externa e sanitiza logs de integrações.

Controllers dependem de casos de uso, casos de uso dependem do domínio e de ports pequenos, e
adapters de infraestrutura implementam esses ports. Modelos de persistência e DTOs não atravessam
seus limites como contratos públicos.

## Main Use Cases

### Identidade e cliente

- `RegisterCustomerWithPassword`: normaliza e-mail/telefone, cria somente papel `CLIENT`, codifica a
  senha e inicia o consentimento proativo como não autorizado.
- `LoginWithPassword`: autentica somente contas com credencial local e não revela qual dado foi
  incorreto.
- `CompleteGoogleLogin`: valida a identidade Google, autentica conta já vinculada ou emite uma
  credencial curta para completar o cadastro de cliente; no fluxo administrativo, cria/autentica o
  `Account` próprio e impede elevação quando o `sub` não corresponde à conta autorizada.
- `CompleteGoogleCustomerRegistration`: valida o registration token e os dados obrigatórios e cria
  `Account`, identidade Google e `Customer` atomicamente antes de emitir access/refresh.
- `IssueTokenPair`, `RefreshAccessToken` e `Logout`: emitem access token JWT, rotacionam refresh token
  de uso único e revogam a credencial renovável atual.
- `GetCurrentCustomer`: retorna a visão pessoal mínima do principal autenticado.
- `SetProactiveContactConsent`: registra opt-in afirmativo ou revogação do próprio cliente e faz a
  nova elegibilidade valer imediatamente.

### Catálogo e mídia

- `ListPublicCatalog` e `GetPublicProduct`: retornam somente produtos ativos e referências públicas
  das imagens.
- `CreateCategory` e `RenameCategory`: mantêm a classificação sem adicionar exclusão não prevista.
- `CreateProduct`: valida produto e imagens, coordena uploads e persiste produto completo com uma
  única imagem principal.
- `UpdateProduct` e `ChangeProductStatus`: alteram somente campos previstos e impedem ativação sem
  as invariantes de mídia.
- `AddProductImages`, `SetPrimaryProductImage` e `RemoveProductImage`: preservam pelo menos uma
  imagem e exatamente uma principal.

### Interesse e WhatsApp

- `RegisterProductInterest`: exige `CLIENT`, valida produto ativo e aplica idempotência por
  confirmação; na mesma transação persiste o interesse, um lembrete acionável de acompanhamento e
  um `ContactRecord` pendente relacionado aos dois, e devolve o link do WhatsApp após o commit.
- `GetInterestWhatsAppLink`: permite ao próprio cliente recuperar o link de um interesse já criado
  sem gerar outro registro.

### CRM

- `SearchCustomers` e `GetCustomerContext`: fornecem à administradora a base, consentimento vigente
  e contexto necessário, sem expor esses dados a clientes.
- `ListCustomerInterests`: lista interesses de um cliente em ordem cronológica.
- `RecordContact` e `ListCustomerContacts`: mantêm contatos manuais e listam separadamente registros
  pendentes e concluídos.
- `CompletePendingContactRecord`: exige dados finais do atendimento, conclui o `ContactRecord` e o
  lembrete automático relacionado na mesma transação.
- `CreateReminder`, `ListPendingReminders` e `CompleteManualReminder`: mantêm lembretes manuais;
  atraso e acionabilidade são derivados no momento da leitura. O lembrete automático não aceita
  conclusão independente.
- Lembrete com propósito `PROACTIVE_CONTACT` só é acionável com consentimento vigente. O propósito
  `CUSTOMER_REQUEST_FOLLOW_UP` permanece acionável porque responde a uma iniciativa do cliente.

Nenhum caso de uso de venda, preço, estoque, tamanho, exclusão de produto/categoria ou envio
automático de comunicação será criado na V1.

## Authentication and Authorization Strategy

- Spring Security autenticará a API de forma stateless pelo header `Authorization: Bearer`. A
  aplicação assinará access tokens JWT assimétricos e validará assinatura, algoritmo, `iss`, `aud`,
  `exp` e `nbf`. O token terá duração inicial de 15 minutos e conterá somente `sub` com o ID do
  `Account`, papel, `iat`, `exp` e `jti`; e-mail e dados do cliente não serão claims.
- A chave privada de assinatura e as chaves públicas de validação virão de segredos externos, nunca
  do repositório ou banco. O header `kid` identificará a chave ativa; uma rotação operacional poderá
  manter a chave pública anterior apenas durante a janela máxima dos tokens já emitidos. CORS
  aceitará somente origens configuradas explicitamente.
- O refresh token também será um JWT próprio, com `typ=refresh`, `sub`, `jti`, `family_id`, `iat` e
  `exp`, duração inicial de 30 dias e hash do `jti` persistido. Será enviado apenas em cookie
  `HttpOnly`, `Secure` em produção, `SameSite=Strict` e
  `Path=/api/v1/auth`; `/auth/refresh` e `/auth/logout` aceitarão somente `POST` e validarão a origem
  configurada. Cada uso rotaciona o token em uma transação e invalida o anterior; reuso revoga toda
  a família. Logout revoga a família atual e remove o cookie. Access tokens já emitidos permanecem
  válidos até o curto `exp`, sem blacklist na V1.
- Senhas serão armazenadas pelo `DelegatingPasswordEncoder` usando BCrypt com custo inicial 12,
  calibrado no ambiente. A entrada será limitada a 64 caracteres e 72 bytes UTF-8 para não exceder
  o limite seguro do BCrypt. Senha, hash, tokens e credenciais nunca entram em respostas ou logs.
- O login Google usará OAuth 2.0 Authorization Code com OpenID Connect pelo Spring Security OAuth2
  Client. A aplicação validará `state`, nonce, assinatura, emissor, audiência, expiração e
  `email_verified`, identificará a pessoa por `(provider=GOOGLE, subject=sub)` e nunca aceitará token
  Google como credencial da API. Após o callback, ela trabalhará com o `Account` local e emitirá o
  mesmo par access/refresh da autenticação por senha.
- A requisição de autorização Google será mantida apenas durante o handshake em cookie curto,
  assinado/cifrado, `HttpOnly`, `Secure` em produção e `SameSite=Lax`, sem sessão persistida no
  servidor. Quando um novo cliente ainda precisar informar telefone, o callback emitirá um
  registration token JWT de
  uso restrito e duração de 10 minutos; somente após completar os dados serão emitidos access e
  refresh tokens.
- O início do OAuth é explícito para cliente ou administradora e a intenção é protegida no `state`.
  No fluxo administrativo, qualquer identidade que não corresponda ao `sub` e e-mail autorizados é
  negada, sem cair no cadastro de cliente. Para `CLIENT`, uma identidade Google inédita só cria
  `Account`, identidade externa e `Customer`, com consentimento proativo negado, depois que os dados
  obrigatórios ausentes, como telefone, forem coletados. Um Google `sub` já vinculado autentica a
  conta existente. E-mail Google
  verificado só poderá vincular uma conta `CLIENT` existente em fluxo transacional sem conflito; uma
  tentativa de vincular ou assumir `ADMIN` por coincidência de e-mail será negada.
- A matriz é: catálogo, cadastro, login, início/callback Google e refresh públicos nos limites de
  cada fluxo; interesse e consentimento para `CLIENT`; catálogo administrativo e CRM para `ADMIN`;
  negação por padrão para qualquer rota não classificada. Regras contextuais também permanecem nos
  casos de uso.
- `401` representa autenticação ausente, inválida ou expirada; `403`, papel autenticado sem permissão.
  O `AuthenticationEntryPoint` e o `AccessDeniedHandler` usam o mesmo `ApiErrorResponse` do restante
  da API. Erros de login e OAuth são genéricos para impedir enumeração de contas.
- Uma limitação simples e configurável de tentativas de login por origem e identificador será
  mantida na instância local, sem bloqueio permanente e sem infraestrutura distribuída.

### Provisionamento da única administradora

A identidade administrativa será configurada por segredos operacionais contendo o `sub` Google
autorizado e o e-mail esperado da loja. Não haverá senha administrativa, endpoint de cadastro de
administrador, credencial padrão ou segredo em migration. No primeiro callback Google válido que
corresponder simultaneamente ao `sub` autorizado e ao e-mail verificado esperado, a aplicação cria,
em transação, o único `Account` `ADMIN` e sua identidade externa; callbacks posteriores autenticam
essa mesma conta.

Uma restrição parcial única no PostgreSQL garantirá no máximo uma conta `ADMIN`, e a unicidade de
`(provider, subject)` impedirá reutilização da identidade. No fluxo administrativo, qualquer outro
`sub`, inclusive com e-mail parecido ou já cadastrado como cliente, recebe acesso negado e nunca é
promovido. Alterar a conta Google autorizada exige procedimento operacional explícito e revogação
das famílias de refresh
tokens da administradora.

## Persistence and Migration Strategy

- Spring Data JPA implementará os adapters de persistência; classes JPA ficarão em
  `infrastructure.persistence` e serão mapeadas para modelos ou resultados da aplicação.
- Flyway será a única fonte de schema. Hibernate usará `ddl-auto=validate`; migrations aplicadas não
  serão alteradas.
- IDs serão UUIDs gerados pela aplicação, e datas serão persistidas como instantes UTC em
  `timestamptz`.
- A ordem inicial será: `V1` contas/clientes/identidades externas/consentimento e refresh tokens;
  `V2` catálogo/imagens; `V3` interesses/CRM e vínculos do acompanhamento; `V4` índices operacionais.
- Restrições do banco cobrirão e-mail normalizado único, uma administradora, nomes normalizados de
  categoria, identificador externo de mídia único, no máximo uma imagem principal por produto,
  chaves estrangeiras, identidade externa única, refresh token único, idempotência do interesse e
  um único conjunto automático de acompanhamento por interesse.
- Regras entre linhas que exigiriam triggers, como “produto tem ao menos uma imagem”, permanecerão
  no domínio e no caso de uso. Operações concorrentes de mídia bloquearão a linha do produto
  durante a transação.
- Listagens serão paginadas e usarão índices por status do produto, cliente/data de interesse,
  cliente/status/data do contato e status/data do lembrete.

## Cloudinary Consistency Strategy

A aplicação definirá a porta `MediaStorage` com `upload` e `delete`, usando tipos neutros. O adapter
Cloudinary armazenará cada arquivo sob uma chave previsível e única baseada nos IDs gerados pela
aplicação, sem permitir sobrescrita. Apenas `externalId` e `secureUrl` serão persistidos.

Criação e inclusão de imagem seguem: validar tudo, fazer upload, persistir referências em uma única
transação e compensar os novos uploads se qualquer upload ou commit falhar. Troca de conteúdo envia
um novo asset, troca a referência local e só então tenta remover o antigo. Alterar somente a imagem
principal é uma transação local.

Remoção segue: validar invariantes e remover a referência local em transação; após o commit, excluir
o asset externo de forma idempotente. Se a remoção for da imagem principal, a requisição deve indicar
outra imagem existente como principal na mesma transação. Falha na exclusão externa não reverte o
estado local nem publica URL quebrada; ela gera log estruturado para limpeza operacional.

Sem transação distribuída ou fila persistente, uma interrupção entre etapas pode deixar um asset
órfão. A V1 aceita esse risco documentado e prioriza nunca manter no catálogo uma referência para
asset removido. O prefixo organizado de assets e os logs com IDs permitem auditoria manual. Retry
persistente só será adicionado mediante necessidade operacional concreta.

## WhatsApp Strategy

- O backend gera `https://wa.me/{numeroE164}?text={mensagemCodificada}`; não há chamada à API do
  WhatsApp, uso de WhatsApp Business API ou envio automático de mensagens na V1.
- Número e texto-base vêm de configuração validada na inicialização. A requisição nunca fornece
  destino ou URL de redirecionamento.
- A mensagem inclui apenas nome e referência pública do produto, sem e-mail, telefone, token ou
  dados do CRM. Conteúdo variável é codificado e limitado.
- A confirmação persiste `Interest`, `Reminder` e `ContactRecord` pendente na mesma transação antes
  da geração da resposta. Uma constraint única por `interest_id` em cada acompanhamento é a última
  barreira contra concorrência; repetir a chave idempotente ou consultar o interesse recupera o
  mesmo contexto sem novos registros.
- O lembrete automático nasce como `CUSTOMER_REQUEST_FOLLOW_UP`, imediatamente acionável mesmo sem
  consentimento proativo, e mantém canal WhatsApp, data, cliente, identificador e produto por meio
  dos vínculos persistidos. O `ContactRecord` continua `PENDING` e não compõe o histórico concluído
  até a administradora registrar data efetiva, descrição e resultado do atendimento.
- Ao finalizar o `ContactRecord`, o caso de uso bloqueia os dois registros e conclui o lembrete na
  mesma transação. O endpoint de conclusão manual de lembrete rejeita lembretes automáticos.
- O consentimento proativo continua modelado para CRM e possíveis versões futuras, mas não aciona
  API de mensagens nem automação nesta versão.

## REST Contract Strategy

O contrato canônico será [contracts/openapi.yaml](./contracts/openapi.yaml): prefixo `/api/v1`, JSON
UTF-8, Bearer JWT, multipart para mídia, datas ISO-8601, IDs opacos, paginação determinística e
`application/json` com `ApiErrorResponse` para erros. A chave `Idempotency-Key` será obrigatória na
confirmação do interesse; sua repetição pelo mesmo cliente e produto devolve o resultado existente,
e reutilização para outro produto retorna conflito.

O padrão de exceções preservará a identidade adotada no Fidelizei: catálogos de erros por contexto e
pela camada que é dona da falha,
cada item com código estável e mensagem segura; exceptions específicas carregando um erro;
`GlobalExceptionHandler` como tradutor central; e um único payload `ApiErrorResponse` com
`timestamp`, `status`, `code`, `message`, `path`, `traceId` opcional e `errors` para violações de
campos. Códigos terão prefixos globais por contexto (`AUTH_`, `ACCOUNT_`, `CATALOG_`, `MEDIA_`,
`INTEREST_`, `CRM_`, `VALIDATION_`, `SECURITY_`, `INTERNAL_`) e unicidade verificada por teste.
Exceptions de `domain` carregam somente tipos e erros declarados em `domain`; exceptions de
`application` carregam somente seu catálogo e podem traduzir uma falha do domínio sem criar a
dependência inversa. Falhas técnicas da infraestrutura são traduzidas no limite do caso de uso e não
vazam para o contrato HTTP. `presentation` conhece esses erros apenas para mapeá-los. Erros
inesperados retornam `INTERNAL_001`, HTTP 500 e mensagem genérica, registrando detalhes apenas no
servidor.

O contrato não terá operações públicas de preço, estoque, tamanho, checkout, administração ou
criação de `ADMIN`. Produtos/categorias não terão exclusão porque a specification não define esses
fluxos.

## Test Strategy

### Unit tests

- Domínio: ativação do produto, quantidade de principais, remoção/troca de imagem, status de
  lembrete, transição do contato pendente, conclusão coordenada e consentimento.
- Aplicação: idempotência do interesse, derivação da identidade do principal, opt-in/revogação,
  criação atômica de acompanhamento, acionabilidade de lembretes, conclusão conjunta e compensações
  de mídia em cada ponto de falha.
- Infraestrutura pura: geração segura do link WhatsApp, normalização e codificação de valores.
- Segurança: emissão/validação de claims JWT, rotação e detecção de reuso de refresh token, mapeamento
  do Google `sub`, recusa de elevação administrativa e unicidade global dos códigos de erro.
- Fakes pequenos substituirão ports; não serão testados getters, mapeamentos triviais ou detalhes do
  framework sem risco comportamental.

### Integration tests

- `@SpringBootTest`, MockMvc e PostgreSQL real via Testcontainers; H2 não será usado.
- Flyway deve construir um banco vazio e Hibernate deve validar o schema.
- O callback Google autorizado deve criar/autenticar uma única administradora; outro `sub`, mesmo
  com e-mail semelhante, deve receber negação. A constraint parcial será testada sob concorrência.
- Matriz anônimo/`CLIENT`/`ADMIN`, login local e Google, Bearer JWT válido/expirado/adulterado,
  refresh rotativo, logout, respostas `401`/`403` e negação padrão.
- Restrições, transações, paginação, idempotência concorrente e invariantes de imagem serão testadas
  no PostgreSQL.
- Concorrência e retry da confirmação devem produzir exatamente um `Interest`, um `Reminder` e um
  `ContactRecord`; concluir o contato deve concluir o lembrete no mesmo commit, e rollback deve
  preservar ambos pendentes.
- O port de mídia será falso nos testes de fluxo; o adapter Cloudinary terá teste isolado com client
  simulado, sem rede ou segredo no CI.
- Cenários de contrato validarão JSON, multipart, `ApiErrorResponse`, validações de campo, erros do
  Spring Security, fallback 500, datas, URLs de todas as imagens e ausência de dados sensíveis.
- Testcontainers exige Docker somente durante a suíte de integração; executar a aplicação pela IDE
  continua permitido com um PostgreSQL configurado, sem containerizar o backend.

### End-to-end validation

[quickstart.md](./quickstart.md) cobrirá catálogo anônimo, cadastro/login local e Google, rotação de
tokens, interesse/WhatsApp/acompanhamento, administração de catálogo, CRM, consentimento e
autorização negativa. Todos os testes e migrations devem passar antes da funcionalidade ser
considerada concluída.

## Project Structure

### Documentation (this feature)

```text
specs/001-v1-commercial-flows/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md                 # criado posteriormente por $speckit-tasks
```

### Source Code (repository root)

```text
src/main/kotlin/com/viniciusdevassis/laumileymodas/
├── LaumileyModasApiApplication.kt
├── presentation/
│   ├── auth/
│   ├── catalog/
│   ├── customer/
│   ├── admin/
│   ├── crm/
│   └── common/
├── application/
│   ├── auth/
│   ├── catalog/
│   ├── customer/
│   ├── interest/
│   ├── crm/
│   ├── media/
│   └── port/
├── domain/
│   ├── account/
│   ├── customer/
│   ├── catalog/
│   ├── interest/
│   └── crm/
└── infrastructure/
    ├── config/
    ├── security/
    ├── persistence/
    ├── media/
    └── whatsapp/

src/main/resources/
├── application.yaml
└── db/migration/

src/test/kotlin/com/viniciusdevassis/laumileymodas/
├── unit/
│   ├── domain/
│   └── application/
└── integration/
    ├── http/
    ├── persistence/
    ├── security/
    └── media/
```

**Structure Decision**: um único módulo Maven e um único processo. Os diretórios representam
responsabilidades, e cada responsabilidade é subdividida pelo fluxo de negócio. Não haverá módulos
físicos, serviços separados ou hierarquias de ports/interfaces além das dependências reais.

## Post-Design Constitution Check

Todos os gates permanecem `PASS` após o desenho. O modelo não adiciona venda, API de mensagens, automação,
microsserviço de mídia ou dependência do domínio em fornecedor. A autenticação, as migrations, a
matriz de autorização e os testes tornam explícitas as exigências de segurança e qualidade.

O risco residual de asset órfão foi aceito como trade-off operacional limitado da V1; ele não viola
a exigência de evitar inconsistências “sempre que possível”, pois o fluxo compensa falhas síncronas,
registra falhas de limpeza e prioriza não publicar referências quebradas.

## Complexity Tracking

Nenhuma violação da Constitution exige justificativa. As dependências adicionais atendem requisitos
concretos: Spring Security OAuth2 Client/Resource Server para Google e JWT, JPA/PostgreSQL para
persistência e refresh tokens, Flyway para versionamento, Cloudinary para mídia e Testcontainers
para validar comportamento real do banco.
