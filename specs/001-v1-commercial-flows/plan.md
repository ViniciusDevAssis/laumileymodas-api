# Implementation Plan: Operação Comercial V1

**Branch**: `main` | **Feature context**: `001-v1-commercial-flows` | **Date**: 2026-09-19 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-v1-commercial-flows/spec.md`

## Summary

Implementar a V1 como uma única API REST em Kotlin e Spring Boot, com PostgreSQL como fonte de
verdade para contas, catálogo e CRM. O sistema seguirá as responsabilidades `presentation`,
`application`, `domain` e `infrastructure`; controllers chamarão casos de uso concretos e apenas
dependências externas ou de persistência receberão portas próprias.

A autenticação usará sessões opacas persistidas no PostgreSQL, com autorização por `CLIENT` e
`ADMIN`. Imagens serão recebidas pela API, armazenadas no Cloudinary por um adapter de
`infrastructure` e referenciadas localmente por URL e identificador externo. Interesses serão
idempotentes e retornarão um link do WhatsApp gerado pelo backend após a persistência.

## Technical Context

**Language/Version**: Kotlin 2.3.21 executando em Java 21

**Primary Dependencies**: Spring Boot 3.5.16, Spring Web MVC, Spring Validation, Spring Security,
Spring Session JDBC, Spring Data JPA, Flyway, PostgreSQL Driver, Jackson Kotlin e Cloudinary Java
SDK restrito à infraestrutura

**Storage**: PostgreSQL para dados e sessões; Cloudinary para o conteúdo das imagens; nenhum binário
de imagem no banco ou no sistema de arquivos da aplicação

**Testing**: JUnit 5, Kotlin Test, Spring Boot Test, Spring Security Test, MockMvc, Testcontainers com
PostgreSQL e adapters falsos para integrações externas

**Target Platform**: serviço JVM executado em ambiente Linux com HTTPS no ponto de entrada

**Project Type**: API web monolítica

**Performance Goals**: leituras paginadas do catálogo e CRM com p95 de até 500 ms e comandos sem
upload com p95 de até 1 s sob a carga inicial; latência de upload é medida separadamente por depender
do provedor externo

**Constraints**: uma única aplicação backend; uma conta administrativa; sessões de 30 minutos de
inatividade; paginação máxima de 100 itens; uploads somente de imagens em formatos permitidos e com
limite configurável; nenhum preço, estoque, tamanho, checkout, pagamento ou automação comercial

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
| Segurança e privacidade | PASS | Sessão protegida, papéis mínimos, consentimento opt-in e respostas sem dados sensíveis. |
| Regras testáveis | PASS | Regras de catálogo, interesse, autorização, consentimento e lembretes têm estratégia de testes. |
| Integrações isoladas | PASS | Cloudinary e geração do link do WhatsApp ficam atrás de adapters de `infrastructure`. |
| Responsabilidades claras | PASS | As quatro responsabilidades têm contratos explícitos e casos de uso pequenos. |
| Banco versionado | PASS | Flyway é a única fonte de criação e evolução do schema. |
| Qualidade antes da conclusão | PASS | Testes unitários, integração real com PostgreSQL e cenários ponta a ponta são gates. |
| Mídia externa | PASS | A API controla upload/exclusão; o banco mantém apenas referências; não há serviço separado. |

Não há violações que bloqueiem a pesquisa ou exijam exceção à Constitution.

## Architecture and Layer Contracts

### `presentation`

- Expõe controllers REST, requests, responses, paginação e `ProblemDetail`.
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
  interesse, consentimento e estados de lembrete.
- Não depende de Spring, HTTP, JPA, Cloudinary ou formatos de resposta.
- Usa entidades e value objects somente onde protegem uma regra; projeções de leitura simples não
  precisam virar agregados ricos.

### `infrastructure`

- Implementa persistência JPA/PostgreSQL, migrations, Spring Security, Spring Session JDBC,
  Cloudinary, configuração e geração da URL do WhatsApp.
- Converte modelos persistidos e respostas de fornecedor para tipos definidos pela aplicação.
- Mantém credenciais exclusivamente em configuração externa e sanitiza logs de integrações.

Controllers dependem de casos de uso, casos de uso dependem do domínio e de ports pequenos, e
adapters de infraestrutura implementam esses ports. Modelos de persistência e DTOs não atravessam
seus limites como contratos públicos.

## Main Use Cases

### Identidade e cliente

- `RegisterCustomer`: normaliza e-mail/telefone, cria somente papel `CLIENT`, codifica a senha e
  inicia o consentimento proativo como não autorizado.
- `Login` e `Logout`: autenticam credenciais, criam ou invalidam sessão e não revelam qual dado foi
  incorreto.
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

- `RegisterProductInterest`: exige `CLIENT`, valida produto ativo, aplica idempotência por
  confirmação, persiste o interesse e devolve o link do WhatsApp após o commit.
- `GetInterestWhatsAppLink`: permite ao próprio cliente recuperar o link de um interesse já criado
  sem gerar outro registro.

### CRM

- `SearchCustomers` e `GetCustomerContext`: fornecem à administradora a base, consentimento vigente
  e contexto necessário, sem expor esses dados a clientes.
- `ListCustomerInterests`: lista interesses de um cliente em ordem cronológica.
- `RecordContact` e `ListContactHistory`: mantêm o histórico manual.
- `CreateReminder`, `ListPendingReminders` e `CompleteReminder`: mantêm lembretes manuais; atraso e
  acionabilidade são derivados no momento da leitura.
- Lembrete com propósito `PROACTIVE_CONTACT` só é acionável com consentimento vigente. O propósito
  `CUSTOMER_REQUEST_FOLLOW_UP` permanece acionável porque responde a uma iniciativa do cliente.

Nenhum caso de uso de venda, preço, estoque, tamanho, exclusão de produto/categoria ou envio
automático de comunicação será criado na V1.

## Authentication and Authorization Strategy

- Spring Security autenticará e-mail e senha e persistirá o contexto em sessão opaca via Spring
  Session JDBC. REST não exige autenticação stateless, e essa escolha evita refresh tokens e listas
  próprias de revogação.
- O cookie de sessão será `HttpOnly`, `Secure` em produção, `SameSite=Lax`, limitado ao host e sem
  `Max-Age`. CSRF continuará habilitado para toda operação mutável, inclusive login, logout e
  upload; CORS aceitará somente origens configuradas explicitamente.
- Senhas serão armazenadas pelo `DelegatingPasswordEncoder` usando BCrypt com custo inicial 12,
  calibrado no ambiente. A entrada será limitada a 64 caracteres e 72 bytes UTF-8 para não exceder
  o limite seguro do BCrypt. Senha, hash e credenciais nunca entram em respostas ou logs.
- A sessão expira após 30 minutos de inatividade. Logout, redefinição operacional da senha
  administrativa ou desativação futura invalidam as sessões do principal.
- A matriz é: catálogo e obtenção de CSRF públicos; cadastro e login públicos com CSRF; interesse e
  consentimento para `CLIENT`; catálogo administrativo e CRM para `ADMIN`; negação por padrão para
  qualquer rota não classificada.
- `401` representa ausência ou falha de autenticação; `403`, papel autenticado sem permissão. Erros
  de login são genéricos para impedir enumeração de contas.
- Uma limitação simples e configurável de tentativas de login por origem e e-mail será mantida na
  instância local, sem bloqueio permanente e sem infraestrutura distribuída.

### Provisionamento da única administradora

Um comando operacional no mesmo artefato será ativado apenas por perfil explícito de bootstrap. Ele
receberá e-mail e senha por segredo do ambiente, codificará a senha e criará `ADMIN` em uma
transação somente quando nenhuma conta administrativa existir. Não haverá endpoint de criação de
administradores, credencial padrão ou senha em migration.

Uma restrição parcial única no PostgreSQL garantirá no máximo uma conta `ADMIN`, inclusive em
execuções concorrentes. Se já existir administradora, o comando falhará sem alterá-la. Após o
primeiro uso, o perfil será desabilitado e o segredo inicial removido. Um comando operacional
separado poderá redefinir a senha e invalidar todas as sessões da administradora.

## Persistence and Migration Strategy

- Spring Data JPA implementará os adapters de persistência; classes JPA ficarão em
  `infrastructure.persistence` e serão mapeadas para modelos ou resultados da aplicação.
- Flyway será a única fonte de schema. Hibernate usará `ddl-auto=validate`; migrations aplicadas não
  serão alteradas.
- IDs serão UUIDs gerados pela aplicação, e datas serão persistidas como instantes UTC em
  `timestamptz`.
- A ordem inicial será: `V1` identidade/clientes/consentimento; `V2` catálogo/imagens; `V3`
  interesses/CRM; `V4` tabelas oficiais do Spring Session e índices operacionais.
- Restrições do banco cobrirão e-mail normalizado único, uma administradora, nomes normalizados de
  categoria, identificador externo de mídia único, no máximo uma imagem principal por produto,
  chaves estrangeiras e idempotência do interesse.
- Regras entre linhas que exigiriam triggers, como “produto tem ao menos uma imagem”, permanecerão
  no domínio e no caso de uso. Operações concorrentes de mídia bloquearão a linha do produto
  durante a transação.
- Listagens serão paginadas e usarão índices por status do produto, cliente/data de interesse,
  cliente/data do contato e status/data do lembrete.

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
  WhatsApp na V1.
- Número e texto-base vêm de configuração validada na inicialização. A requisição nunca fornece
  destino ou URL de redirecionamento.
- A mensagem inclui apenas nome e referência pública do produto, sem e-mail, telefone, token ou
  dados do CRM. Conteúdo variável é codificado e limitado.
- O interesse é confirmado antes da geração da resposta. Falha ao abrir o aplicativo não desfaz o
  registro; repetir a mesma chave idempotente ou consultar o interesse recupera o mesmo contexto.

## REST Contract Strategy

O contrato canônico será [contracts/openapi.yaml](./contracts/openapi.yaml): prefixo `/api/v1`, JSON
UTF-8, multipart para mídia, datas ISO-8601, IDs opacos, paginação determinística e
`application/problem+json` para erros. A chave `Idempotency-Key` será obrigatória na confirmação do
interesse; sua repetição pelo mesmo cliente e produto devolve o resultado existente, e reutilização
para outro produto retorna conflito.

O contrato não terá operações públicas de preço, estoque, tamanho, checkout, administração ou
criação de `ADMIN`. Produtos/categorias não terão exclusão porque a specification não define esses
fluxos.

## Test Strategy

### Unit tests

- Domínio: ativação do produto, quantidade de principais, remoção/troca de imagem, status de
  lembrete, vencimento e consentimento.
- Aplicação: idempotência do interesse, derivação da identidade do principal, opt-in/revogação,
  acionabilidade de lembretes e compensações de mídia em cada ponto de falha.
- Infraestrutura pura: geração segura do link WhatsApp, normalização e codificação de valores.
- Fakes pequenos substituirão ports; não serão testados getters, mapeamentos triviais ou detalhes do
  framework sem risco comportamental.

### Integration tests

- `@SpringBootTest`, MockMvc e PostgreSQL real via Testcontainers; H2 não será usado.
- Flyway deve construir um banco vazio e Hibernate deve validar o schema.
- O bootstrap deve criar uma única administradora, rejeitar segunda execução e falhar sem segredo
  válido; a constraint parcial será testada também sob concorrência.
- Matriz anônimo/`CLIENT`/`ADMIN`, sessão, CSRF, logout, expiração, respostas `401`/`403` e negação
  padrão.
- Restrições, transações, paginação, idempotência concorrente e invariantes de imagem serão testadas
  no PostgreSQL.
- O port de mídia será falso nos testes de fluxo; o adapter Cloudinary terá teste isolado com client
  simulado, sem rede ou segredo no CI.
- Cenários de contrato validarão JSON, multipart, `ProblemDetail`, datas e ausência de dados
  sensíveis.

### End-to-end validation

[quickstart.md](./quickstart.md) cobrirá catálogo anônimo, cadastro/login, interesse e WhatsApp,
administração de catálogo, CRM, consentimento e autorização negativa. Todos os testes e migrations
devem passar antes da funcionalidade ser considerada concluída.

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

Todos os gates permanecem `PASS` após o desenho. O modelo não adiciona venda, automação,
microsserviço de mídia ou dependência do domínio em fornecedor. A autenticação, as migrations, a
matriz de autorização e os testes tornam explícitas as exigências de segurança e qualidade.

O risco residual de asset órfão foi aceito como trade-off operacional limitado da V1; ele não viola
a exigência de evitar inconsistências “sempre que possível”, pois o fluxo compensa falhas síncronas,
registra falhas de limpeza e prioriza não publicar referências quebradas.

## Complexity Tracking

Nenhuma violação da Constitution exige justificativa. As dependências adicionais atendem requisitos
concretos: Security/Session para autenticação revogável, JPA/PostgreSQL para persistência, Flyway
para versionamento, Cloudinary para mídia e Testcontainers para validar comportamento real do banco.
