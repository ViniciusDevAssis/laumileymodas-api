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

## 3. Autenticação por sessão

**Decision**: usar Spring Security com sessão opaca persistida no PostgreSQL por Spring Session JDBC.
O identificador fica em cookie `HttpOnly`, `Secure` em produção e `SameSite=Lax`; CSRF permanece
habilitado em comandos. A sessão expira após 30 minutos de inatividade.

**Rationale**: a V1 tem um backend monolítico e um navegador como consumidor principal. Sessões
oferecem logout e revogação imediatos sem emissão, refresh e blacklist de JWT. Persistir no banco já
existente evita perder sessões em todo reinício e mantém aberta a possibilidade de mais de uma
instância sem introduzir cache distribuído.

**Alternatives considered**: JWT com refresh token, bearer opaco próprio, HTTP Basic e sessão apenas
em memória. JWT e token próprio exigiriam infraestrutura de credenciais já resolvida pelo Spring
Session; Basic reenvia credenciais; memória perde todas as sessões em reinícios.

**Sources**:

- [Spring Security session management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)

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

**Decision**: criar a única administradora por comando/perfil operacional explícito no mesmo
artefato. Credenciais vêm de segredos do ambiente; não há endpoint, migration com senha ou usuário
padrão. Uma restrição parcial única no PostgreSQL impede duas contas `ADMIN`.

**Rationale**: separa bootstrap operacional do cadastro público, permite auditoria e protege também
contra concorrência. A execução falha se já houver administradora e nunca redefine credenciais
silenciosamente.

**Alternatives considered**: seed em migration, criação automática em todo startup e endpoint de
cadastro administrativo. Todas mantêm segredo versionado, repetem efeitos ou aumentam a superfície
de ataque.

## 6. Flyway e schema

**Decision**: Flyway é a única autoridade para schema; Hibernate usa `ddl-auto=validate`. Migrations
incluem tabelas de negócio, índices, constraints e o schema oficial do Spring Session.

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
conflito. Um novo interesse intencional usa nova chave.

**Rationale**: protege contra duplo clique, retry e concorrência sem impedir que o cliente demonstre
novo interesse no mesmo produto em outro momento.

**Alternatives considered**: unicidade eterna cliente/produto e deduplicação por janela de tempo. A
primeira altera o requisito; a segunda é ambígua e sujeita a relógio.

## 10. Contexto do WhatsApp

**Decision**: gerar no backend uma URL `wa.me` com número oficial em formato E.164 e mensagem mínima
percent-encoded contendo nome e referência pública do produto. A URL é devolvida como dado, sem
redirect HTTP nem chamada a uma API do WhatsApp.

**Rationale**: mantém destino e conteúdo sob controle do backend, evita open redirect e não inclui
dados pessoais. O registro do interesse independe de o dispositivo conseguir abrir o WhatsApp.

**Alternatives considered**: URL montada no frontend, destino enviado pelo cliente e integração com
API de mensagens. As duas primeiras perdem autoridade; a última adiciona automação fora da V1.

## 11. Contratos e erros

**Decision**: documentar o contrato em OpenAPI 3.1, usar `/api/v1`, paginação limitada e erros RFC
9457 `application/problem+json`. Uploads usam multipart, e respostas públicas nunca incluem
identificador externo do Cloudinary, hash ou dados internos do CRM.

**Rationale**: um contrato único orienta frontend, testes e validação. Problem Details evita formatos
de erro diferentes e permite sanitização consistente.

**Alternatives considered**: contratos apenas em controllers ou documento narrativo. Ambos permitem
drift e são menos verificáveis.

## 12. Estratégia de testes

**Decision**: regras puras e orquestração usam testes unitários com fakes; contratos, segurança,
migrations e persistência usam Spring Boot Test, MockMvc e PostgreSQL Testcontainers. Cloudinary não
é chamado pela suíte comum.

**Rationale**: os testes protegem comportamento e riscos reais, incluindo constraints e transações
do PostgreSQL, sem ficarem instáveis por rede ou credenciais externas.

**Alternatives considered**: somente mocks, H2 ou Cloudinary real no CI. Eles não validam o banco de
produção ou tornam a suíte lenta e instável.

## Resolved Unknowns and Residual Trade-offs

Não restam decisões técnicas pendentes. Specification, Constitution e decisões técnicas não se
contradizem. O trade-off residual é a possibilidade de asset órfão quando o processo encerra entre
Cloudinary e PostgreSQL; referências quebradas no catálogo continuam sendo evitadas. Recuperação
automática persistente será considerada apenas se a operação real demonstrar necessidade.
