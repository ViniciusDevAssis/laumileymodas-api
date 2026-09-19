# Research: Operação Comercial V1

## 1. Arquitetura do monólito

**Decision**: manter um único módulo e processo Spring Boot, organizado pelas responsabilidades
`presentation`, `application`, `domain` e `infrastructure`. Controllers chamam casos de uso
concretos; ports existem somente para persistência, mídia, relógio, segurança e geração do contexto
do WhatsApp.

**Rationale**: atende aos limites definidos pela Constitution, preserva regras testáveis e evita
interfaces, módulos e deploys sem mais de uma implementação ou consumidor.

**Alternatives considered**: módulos Maven separados, hexagonal completa com interface para cada
caso de uso e microsserviços por contexto. Foram rejeitados porque não resolvem uma necessidade da
V1 e aumentam coordenação, mapeamento e operação.

## 2. Persistência e modelo de domínio

**Decision**: usar Spring Data JPA nos adapters de `infrastructure`, PostgreSQL como fonte de verdade
e modelos Kotlin no domínio. Mapear somente limites que protegem comportamento; consultas podem
retornar projeções próprias da aplicação.

**Rationale**: JPA reduz o código de persistência, enquanto a separação impede que DTOs HTTP ou
tipos do Cloudinary definam o domínio. O modelo rico fica concentrado nas invariantes de produto,
interesse, consentimento e lembrete.

**Alternatives considered**: SQL manual para todo acesso, modelos JPA serializados diretamente e
uma camada de repositório genérico. SQL manual ampliaria o trabalho da V1; expor JPA pela API
acoplaria contratos; repositório genérico esconderia consultas específicas sem benefício concreto.

## 3. JWT, refresh token e Google OpenID Connect

**Decision**: usar Spring Security de forma stateless para a API. A aplicação emite access token JWT
assinado assimetricamente, válido inicialmente por 15 minutos, e refresh token JWT próprio, com tipo
e audiência distintos, válido por 30 dias. O hash do `jti` do refresh é persistido e rotacionado a
cada uso. O refresh token fica em cookie `HttpOnly`, `Secure` em produção, `SameSite=Strict` e
restrito às rotas de autenticação; refresh e logout validam a origem permitida. Reuso do token
anterior revoga sua família.

Clientes podem autenticar por e-mail/senha ou por Google OAuth 2.0 Authorization Code com OpenID
Connect. A aplicação valida integralmente o retorno Google, usa o `sub` como identificador externo
imutável, cria ou vincula seu próprio `Account` e então emite os tokens da aplicação. Tokens Google
nunca autorizam diretamente a API. O handshake mantém `state`, nonce e a intenção cliente/admin em
cookie curto assinado/cifrado. Novo cliente que ainda precisa informar telefone recebe apenas um
registration token JWT de 10 minutos, restrito à conclusão do cadastro, antes do par definitivo.

**Rationale**: access tokens curtos limitam a janela de uma credencial vazada; refresh stateful e
rotativo permite logout e revogação sem blacklist de todos os access tokens. O `sub` é estável mesmo
quando o e-mail Google muda, enquanto o `Account` local mantém autorização e domínio independentes
do provedor.

**Alternatives considered**: sessão persistida, refresh JWT sem estado, access token longo e uso do ID
token Google como bearer da API. Sessão foi substituída pela decisão do projeto; refresh sem estado
não permite rotação/reuso confiáveis; token longo amplia risco; token Google acopla a autorização
interna ao provedor.

**Sources**:

- [Spring Security OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html)
- [Spring Security Resource Server JWT](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Google OpenID Connect](https://developers.google.com/identity/openid-connect/openid-connect)

## 4. Senhas e autorização

**Decision**: persistir senhas com `DelegatingPasswordEncoder` e BCrypt, custo inicial 12 calibrado
no ambiente. Limitar a entrada a 64 caracteres e 72 bytes UTF-8 para não ultrapassar o limite do
algoritmo. Usar apenas `ROLE_CLIENT` e `ROLE_ADMIN`, negar por padrão e combinar regras de rota com
proteção nos casos de uso sensíveis.

**Rationale**: BCrypt já é suportado pelo Spring Security, inclui salt e evita uma dependência
criptográfica adicional. O encoder delegador permite evolução futura. A autorização em duas
camadas impede que uma rota nova exponha operação e mantém o backend como autoridade.

**Alternatives considered**: Argon2id e autorização apenas no controller. Argon2id é uma boa evolução,
mas requer uma dependência adicional; autorização apenas na borda não protege chamadas internas ou
erros de mapeamento.

**Source**: [Spring Security password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html)

## 5. Provisionamento administrativo

**Decision**: separar o início do login Google de cliente e de administradora, protegendo a intenção
no `state`. A única administradora fica restrita ao `sub` Google autorizado e ao e-mail verificado da
loja, ambos fornecidos por segredo operacional. O primeiro callback administrativo válido cria o
`Account` `ADMIN` e a identidade externa em uma transação protegida pela constraint de uma única
administradora. Não há senha administrativa nem endpoint de cadastro ou promoção.

**Rationale**: o privilégio depende da intenção administrativa explícita, do identificador imutável
emitido pelo Google e de configuração explícita da loja, não de simples coincidência de e-mail. Isso
permite o primeiro acesso sem seed de senha e impede que qualquer conta Google obtenha `ADMIN`.

**Alternatives considered**: permitir uma lista de e-mails, promover cliente existente, seed com
senha ou criar administrador em todo startup. E-mail isolado pode mudar ou colidir; promoção pública
abre elevação de privilégio; senha/seed contradiz a autenticação Google administrativa definida.

## 6. Flyway e schema

**Decision**: Flyway é a única autoridade para schema; Hibernate usa `ddl-auto=validate`. Migrations
incluem tabelas de negócio, identidades externas, refresh tokens, índices e constraints.

**Rationale**: ambientes reproduzíveis e mudanças auditáveis são exigidos pela Constitution. Usar
PostgreSQL real nos testes cobre índices parciais, locks e tipos que H2 não reproduz.

**Alternatives considered**: `ddl-auto=update`, scripts manuais e H2 nos testes. Foram rejeitados por
produzirem drift de schema ou comportamento diferente da produção.

## 7. Upload e referências de mídia

**Decision**: a aplicação expõe a porta `MediaStorage`; o adapter Cloudinary faz upload pelo backend
e devolve `externalId` e `secureUrl`. A chave externa é gerada antes do upload e não pode sobrescrever
asset existente.

**Rationale**: o domínio não conhece o fornecedor, o banco mantém somente as referências exigidas e
o prefixo previsível facilita compensação e auditoria.

**Alternatives considered**: upload direto do frontend, armazenamento binário no PostgreSQL e
filesystem local. Todos contradizem as decisões do projeto ou dificultam segurança e operação.

**Sources**:

- [Cloudinary Java upload](https://cloudinary.com/documentation/java_image_and_video_upload)
- [Cloudinary Upload API and destroy](https://cloudinary.com/documentation/image_upload_api_reference)

## 8. Consistência entre PostgreSQL e Cloudinary

**Decision**: usar ordenação de operações e compensação síncrona em melhor esforço. Novos assets são
enviados antes do commit local e removidos se upload ou persistência falhar. Assets substituídos ou
removidos são apagados somente depois que a referência local deixa de ser publicada.

**Rationale**: não existe transação distribuída com o Cloudinary. A ordem escolhida prioriza nunca
deixar o catálogo apontando para conteúdo removido. Falhas raras podem deixar asset órfão, que é um
risco menor e auditável.

**Alternatives considered**: outbox com worker de limpeza, mensageria, sobrescrita do mesmo asset e
exclusão externa antes do commit. Retry persistente amplia a arquitetura sem demanda operacional;
sobrescrita e exclusão antecipada podem quebrar referências publicadas.

## 9. Idempotência do interesse

**Decision**: exigir `Idempotency-Key` em cada confirmação. A combinação cliente/chave é única; a
mesma chave com o mesmo produto retorna o resultado existente e com produto diferente retorna
conflito. A primeira confirmação persiste atomicamente `Interest`, `Reminder` e `ContactRecord`
pendente; unicidades por `interest_id` impedem acompanhamento duplicado. Um novo interesse
intencional usa nova chave.

**Rationale**: protege contra duplo clique, retry e concorrência sem impedir que o cliente demonstre
novo interesse no mesmo produto em outro momento, e garante que todo interesse confirmado tenha um
único acompanhamento completo.

**Alternatives considered**: unicidade eterna cliente/produto e deduplicação por janela de tempo. A
primeira altera o requisito; a segunda é ambígua e sujeita a relógio.

## 10. Contexto do WhatsApp

**Decision**: gerar no backend uma URL `wa.me` com número oficial em formato E.164 e mensagem mínima
percent-encoded contendo nome e referência pública do produto. A URL é devolvida como dado, sem
redirect HTTP, WhatsApp Business API ou envio automático. Antes da resposta, a transação de
confirmação cria lembrete `CUSTOMER_REQUEST_FOLLOW_UP` e `ContactRecord` `PENDING`; o contato só vira
histórico concluído quando a administradora registra o resultado, concluindo o lembrete associado na
mesma transação.

**Rationale**: mantém destino e conteúdo sob controle do backend, evita open redirect e não inclui
dados pessoais. O registro e seu acompanhamento independem de o dispositivo conseguir abrir o
WhatsApp, sem alegar que uma conversa ocorreu.

**Alternatives considered**: URL montada no frontend, destino enviado pelo cliente e integração com
API de mensagens. As duas primeiras perdem autoridade; a última adiciona automação fora da V1.

## 11. Contratos e erros

**Decision**: documentar o contrato em OpenAPI 3.1, usar `/api/v1`, paginação limitada e um payload
próprio `ApiErrorResponse` em `application/json`, preservando o padrão do Fidelizei. Catálogos de
erros por contexto fornecem código globalmente único e mensagem segura; exceptions específicas
carregam esses erros e o `GlobalExceptionHandler` os traduz para HTTP. Violações de validação,
`AuthenticationEntryPoint` e `AccessDeniedHandler` usam o mesmo payload. O fallback 500 expõe apenas
`INTERNAL_001` e uma mensagem genérica. Uploads usam multipart, e respostas públicas nunca incluem
identificador externo do Cloudinary, hash, token ou dados internos do CRM.

**Rationale**: um contrato único orienta frontend, testes e validação, mantém a identidade já usada
no projeto de referência e elimina formatos divergentes entre MVC, validação e Spring Security. A
separação dos catálogos impede que exceptions de `application` dependam incorretamente de erros do
`domain`; um teste de catálogo garante unicidade global dos códigos.

**Alternatives considered**: formato RFC 9457, contratos apenas em controllers ou documento
narrativo. O formato RFC foi descartado por decisão explícita; as demais alternativas permitem drift
e são menos verificáveis.

## 12. Estratégia de testes

**Decision**: regras puras e orquestração usam testes unitários com fakes; contratos, segurança,
migrations e persistência usam Spring Boot Test, MockMvc e PostgreSQL Testcontainers. Docker é
necessário para os containers efêmeros da suíte de integração, não para executar a aplicação pela
IDE. Cloudinary e Google não são chamados pela suíte comum.

**Rationale**: os testes protegem comportamento e riscos reais, incluindo constraints e transações
do PostgreSQL, sem ficarem instáveis por rede ou credenciais externas.

**Alternatives considered**: somente mocks, H2 ou Cloudinary real no CI. Eles não validam o banco de
produção ou tornam a suíte lenta e instável.

## Resolved Unknowns and Residual Trade-offs

Não restam decisões técnicas pendentes. Specification, Constitution e decisões técnicas não se
contradizem. O trade-off residual é a possibilidade de asset órfão quando o processo encerra entre
Cloudinary e PostgreSQL; referências quebradas no catálogo continuam sendo evitadas. Recuperação
automática persistente será considerada apenas se a operação real demonstrar necessidade.
