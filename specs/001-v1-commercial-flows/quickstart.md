# Quickstart: validar a Operação Comercial V1

Este guia descreve a configuração e os cenários que provam a V1. O contrato completo está em
[contracts/openapi.yaml](./contracts/openapi.yaml) e o modelo em [data-model.md](./data-model.md).

## Prerequisites

- Java 21;
- PostgreSQL acessível para execução normal, local ou Aiven;
- Docker Desktop somente para o PostgreSQL temporário dos testes de integração;
- credenciais Google OIDC de desenvolvimento;
- credenciais Cloudinary de desenvolvimento;
- número oficial da loja no WhatsApp em E.164.

A aplicação roda diretamente pela IDE ou Maven. Dockerizar a aplicação não faz parte desta etapa.

## 1. Configure o ambiente

Exemplo PowerShell para desenvolvimento:

```powershell
$env:LAUMILEY_DB='jdbc:postgresql://<host>:<port>/<database>?sslmode=require'
$env:AIVEN_DB_USER='<usuario>'
$env:AIVEN_DB_PASS='<senha>'

$env:LAUMILEY_JWT_SECRET='<base64-com-pelo-menos-256-bits-aleatorios>'
$env:LAUMILEY_JWT_ISSUER='laumiley-modas-api'
$env:LAUMILEY_JWT_AUDIENCE='laumiley-api'
$env:LAUMILEY_ACCESS_TOKEN_TTL='15m'
$env:LAUMILEY_REFRESH_TOKEN_TTL='30d'

$env:LAUMILEY_OAUTH_CLIENT_ID='<google-client-id>'
$env:LAUMILEY_OAUTH_CLIENT_SECRET='<google-client-secret>'
$env:LAUMILEY_ADMIN_GOOGLE_SUB='<sub-da-conta-google-da-loja>'

$env:LAUMILEY_FRONTEND_ALLOWED_ORIGINS='http://localhost:3000'
$env:LAUMILEY_FRONTEND_AUTH_SUCCESS_URL='http://localhost:3000/auth/google/success'
$env:LAUMILEY_FRONTEND_REGISTRATION_URL='http://localhost:3000/auth/google/complete-registration'
$env:LAUMILEY_FRONTEND_AUTH_ERROR_URL='http://localhost:3000/auth/google/error'
$env:LAUMILEY_COOKIE_SECURE='false'
$env:LAUMILEY_COOKIE_SAME_SITE='Lax'

$env:CLOUDINARY_CLOUD_NAME='<cloud-name>'
$env:CLOUDINARY_API_KEY='<api-key>'
$env:CLOUDINARY_API_SECRET='<api-secret>'
$env:LAUMILEY_WHATSAPP_NUMBER='<numero-e164-sem-sinal>'
```

Em produção, use `LAUMILEY_COOKIE_SECURE=true`, segredo JWT novo e forte, credenciais armazenadas no
ambiente e somente a origem oficial em `LAUMILEY_FRONTEND_ALLOWED_ORIGINS`. Não use `*` com
credenciais.

No cliente Google, registre exatamente este callback de desenvolvimento:

```text
http://localhost:8080/api/v1/login/oauth2/code/google
```

O context path `/api/v1` já faz parte de `{baseUrl}`. O template padrão do Spring Boot resolve
`{action}` como `login` e produz a URL acima; não configure `redirect-uri` nem
`redirectionEndpoint` customizados.

## 2. Execute os testes

Com Docker Desktop ativo:

```powershell
.\mvnw.cmd clean test
```

Resultado esperado:

- Testcontainers inicia PostgreSQL efêmero;
- Flyway cria o schema e Hibernate o valida;
- nenhum teste acessa Google ou Cloudinary reais;
- testes unitários e de integração passam;
- o container é encerrado ao fim da suíte.

## 3. Inicie a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

Base URL:

```powershell
$api='http://localhost:8080/api/v1'
```

Ao iniciar com banco vazio, Flyway aplica as migrations. Hibernate não cria nem altera tabelas.

## 4. Valide o catálogo público e HAL

```powershell
$catalog=Invoke-RestMethod -Method Get -Uri "$api/products?page=0&size=20" `
  -Headers @{Accept='application/hal+json'}
$catalog
```

Resultado esperado: `200`, somente produtos ativos, `_embedded.products`, metadados `page` e links
`_links`. Cada produto contém nome, descrição, categoria e URLs de imagens ordenadas, sem preço,
estoque ou tamanho. O catálogo vazio continua respondendo `200` com coleção vazia.

## 5. Cadastre e autentique um cliente

```powershell
$registration=@{
  firstName='Cliente'
  lastName='Teste'
  email='cliente@example.com'
  password='<senha-de-teste>'
  whatsappPhone='+5511999999999'
} | ConvertTo-Json

$customer=Invoke-RestMethod -Method Post -Uri "$api/customers" `
  -ContentType 'application/json' -Body $registration

$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
$login=@{email='cliente@example.com';password='<senha-de-teste>'} | ConvertTo-Json
$auth=Invoke-RestMethod -Method Post -Uri "$api/auth/login" -WebSession $session `
  -ContentType 'application/json' -Body $login
$bearer=@{Authorization="Bearer $($auth.accessToken)";Accept='application/hal+json'}
```

Resultado esperado: cadastro `201` com `Location`, papel sempre `CLIENT`, login `200`, access token
JWT no corpo e refresh opaco somente em cookie `HttpOnly`. O consentimento começa desautorizado.

Cadastros que incluam papel administrativo são rejeitados. Cliente recebe `403` em `/admin/**`.
Falhas repetidas de login chegam a `429`; tentativas válidas normais não são bloqueadas.

## 6. Inicialize CSRF e valide refresh/logout

```powershell
Invoke-WebRequest -Method Get -Uri "$api/auth/csrf" -WebSession $session | Out-Null
$xsrf=($session.Cookies.GetCookies([Uri]$api) |
  Where-Object Name -eq 'XSRF-TOKEN').Value
$cookieHeaders=@{
  Origin='http://localhost:3000'
  'X-XSRF-TOKEN'=$xsrf
}

Invoke-RestMethod -Method Post -Uri "$api/auth/refresh" `
  -WebSession $session -Headers $cookieHeaders
```

`GET /auth/csrf` apenas materializa `XSRF-TOKEN`. Refresh rotaciona o cookie e reutilizar o valor
anterior retorna `401` e revoga a família. Refresh, logout e handoff sem CSRF válido são rejeitados.
Após autenticação ou logout, obtenha novo token CSRF quando o Spring limpar o anterior.

## 7. Valide Google OIDC

No navegador, inicie sempre pelo endpoint nativo:

```text
http://localhost:8080/api/v1/oauth2/authorization/google
```

Valide separadamente:

1. cliente cujo `sub` já está vinculado;
2. novo cliente Google, que retorna à rota configurada para completar nome, sobrenome e WhatsApp;
3. e-mail já existente com `sub` Google ainda não vinculado, que deve ir para erro sem linking;
4. conta cujo `sub` difere de `LAUMILEY_ADMIN_GOOGLE_SUB`, que segue sempre como CLIENT;
5. conta cujo `sub` coincide com a configuração, que autentica a única ADMIN.

O Spring Security gera a authorization request e processa o callback padrão
`/api/v1/login/oauth2/code/google`. O success handler apenas resolve o comportamento local, cria
handoff opaco em cookie e redireciona para URL fixa do frontend. O frontend chama
`POST /auth/google/exchange` ou `POST /auth/google/customers` com cookie e CSRF. Access token,
refresh token, identidade Google e handoff não aparecem na URL.

## 8. Valide consentimento

```powershell
Invoke-RestMethod -Method Get `
  -Uri "$api/customers/me/proactive-contact-consent" -Headers $bearer

@{authorized=$true} | ConvertTo-Json | ForEach-Object {
  Invoke-RestMethod -Method Put `
    -Uri "$api/customers/me/proactive-contact-consent" `
    -Headers $bearer -ContentType 'application/json' -Body $_
}
```

Conceder e revogar são idempotentes. A revogação remove elegibilidade para novo contato proativo,
mas não altera login, interesse, histórico ou acompanhamento iniciado pelo cliente.

## 9. Valide o interesse e WhatsApp

Use um `productId` ativo do catálogo:

```powershell
$key=[guid]::NewGuid().ToString()
$interest=Invoke-RestMethod -Method Post `
  -Uri "$api/products/<productId>/interests" `
  -Headers ($bearer + @{'Idempotency-Key'=$key})
$interest
```

Resultado esperado: `201`, `Location`, URL `wa.me` com contexto do produto e links HAL úteis. Na
mesma transação são criados um Interest, um Reminder `PENDING` e um ContactRecord `PENDING`.
Repetir a mesma chave e produto retorna `200` com o mesmo resultado; reutilizar a chave para outro
produto retorna `409`. Produto inativo retorna `404` ou resposta indistinguível de indisponível e
não cria dados. Falha ao abrir o WhatsApp não desfaz nem duplica o conjunto.

## 10. Valide a administração do catálogo

Autentique a ADMIN pelo fluxo Google autorizado e use seu Bearer token. Pelo contrato OpenAPI:

1. crie categoria com `POST /admin/categories` e renomeie com `PUT`;
2. crie produto multipart com imagens e índice da principal;
3. confirme URLs ordenadas de todas as imagens na resposta e no catálogo;
4. adicione imagem e altere principal/ordem com os recursos de imagem;
5. tente remover a última imagem válida de produto ativo e receba `422`;
6. altere o status com `PATCH /admin/products/{id}` e confirme que inativo some do catálogo;
7. simule falha de `MediaStorage` em teste e confirme ausência de referência parcial.

Cada criação responde `201` com `Location`. Links administrativos anunciam apenas operações válidas
para o estado atual.

## 11. Valide o CRM

Como ADMIN:

1. pesquise `GET /admin/customers?query=...`;
2. consulte cliente e seus interesses;
3. liste `/admin/reminders` e localize o acompanhamento automático com canal WhatsApp, data,
   cliente/código e produto;
4. confirme que o ContactRecord automático está `PENDING` e não aparece como conversa concluída;
5. conclua-o com `PATCH /admin/contact-records/{id}` e confirme que o Reminder do mesmo Interest
   muda para `COMPLETED` na mesma ação;
6. registre um contato manual concluído com
   `POST /admin/customers/{customerId}/contact-records`;
7. confirme que nenhum endpoint cria ou conclui Reminder diretamente;
8. confirme que lembrete vencido permanece pendente e visível;
9. revogue consentimento do cliente e confirme que o acompanhamento solicitado continua acionável.

As representações de ContactRecord pendente incluem link de conclusão; as concluídas não. Reminder
inclui link para o contato correspondente, sem ação própria de conclusão.

## 12. Valide erros e segurança

- requisição protegida sem Bearer, com JWT expirado ou inválido: `401`;
- CLIENT em operação ADMIN: `403`;
- recurso inexistente ou não acessível: `404`;
- conflito de e-mail/idempotência/estado: `409`;
- invariante de imagem: `422`;
- excesso de tentativas de login: `429`;
- origem CORS fora da configuração: rejeitada;
- conta Google não autorizada: nunca recebe ADMIN;
- `sub` Google novo com e-mail já usado: nunca é vinculado automaticamente;
- falha inesperada: `500` com `INTERNAL_001`.

Todos usam `ApiErrorResponse` com `timestamp`, `status`, `code`, `message`, `path` e, quando houver,
`fieldErrors`. Nenhuma resposta contém stack trace, senha, token, hash, segredo ou payload bruto de
provedor. `AuthenticationEntryPoint` e `AccessDeniedHandler` mantêm o mesmo contrato.

## Completion Gate

A implementação está pronta quando:

- a suíte completa passa com Testcontainers;
- todas as migrations sobem banco PostgreSQL vazio e Hibernate valida o schema;
- os cenários acima respeitam o OpenAPI e os links HAL;
- os requisitos funcionais permanecem cobertos sem estruturas excluídas no research;
- não existem Spring Session, H2, ProblemDetail, linking por e-mail, Reminder manual, ports internos,
  entidades duplicadas, mappers triviais ou `ApplicationProperties`.
