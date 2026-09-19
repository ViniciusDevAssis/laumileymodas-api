# Quickstart: validar a Operação Comercial V1

Este guia descreve a validação esperada após a implementação. O contrato detalhado está em
[contracts/openapi.yaml](./contracts/openapi.yaml) e o schema em [data-model.md](./data-model.md).

## Prerequisites

- Java 21
- PostgreSQL acessível para executar a aplicação pela IDE
- Docker disponível somente para os containers temporários dos testes de integração
- Credenciais de um ambiente de desenvolvimento do Cloudinary
- Cliente OAuth 2.0 Google de desenvolvimento e URI de callback registrada
- Par de chaves de desenvolvimento para assinar e validar os JWT próprios
- Número de WhatsApp da loja em formato E.164

## 1. Configure PostgreSQL

Crie uma base PostgreSQL local ou compartilhada pelo meio usual do ambiente e configure a aplicação
sem versionar segredos. O backend continuará sendo executado diretamente pela IDE ou Maven:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/laumiley'
$env:SPRING_DATASOURCE_USERNAME='laumiley'
$env:SPRING_DATASOURCE_PASSWORD='local-only'
$env:CLOUDINARY_CLOUD_NAME='<cloud-name>'
$env:CLOUDINARY_API_KEY='<api-key>'
$env:CLOUDINARY_API_SECRET='<api-secret>'
$env:LAUMILEY_WHATSAPP_NUMBER='<numero-e164-sem-sinal>'
$env:LAUMILEY_ALLOWED_ORIGINS='http://localhost:3000'
$env:LAUMILEY_FRONTEND_AUTH_SUCCESS_URL='http://localhost:3000/auth/google/success'
$env:LAUMILEY_FRONTEND_REGISTRATION_URL='http://localhost:3000/auth/google/complete-registration'
$env:LAUMILEY_FRONTEND_AUTH_ERROR_URL='http://localhost:3000/auth/google/error'
$env:GOOGLE_CLIENT_ID='<google-client-id>'
$env:GOOGLE_CLIENT_SECRET='<google-client-secret>'
$env:LAUMILEY_JWT_PRIVATE_KEY='<chave-privada-de-desenvolvimento>'
$env:LAUMILEY_JWT_PUBLIC_KEY='<chave-publica-de-desenvolvimento>'
$env:LAUMILEY_ADMIN_GOOGLE_SUB='<sub-da-conta-google-da-loja>'
$env:LAUMILEY_ADMIN_GOOGLE_EMAIL='admin@example.com'
```

Resultado esperado: ao iniciar, Flyway aplica todas as migrations e Hibernate valida o schema sem
criá-lo ou alterá-lo automaticamente.

## 2. Configure a única identidade administrativa

Obtenha o `sub` da conta Google controlada pela loja e configure-o junto do e-mail verificado
esperado. Não configure senha administrativa. No primeiro login Google válido dessa identidade, a
aplicação cria o único `Account` `ADMIN`; qualquer outro `sub` no fluxo administrativo deve receber
negação e redirecionamento para a rota fixa de erro, sem criar handoff, mesmo que informe um e-mail
parecido. O segredo do cliente Google e as chaves JWT não devem ser versionados.

## 3. Execute verificações automatizadas

```powershell
.\mvnw.cmd test
```

A suíte deve:

- subir PostgreSQL real com Testcontainers e executar todo o histórico Flyway;
- verificar catálogo público e matriz anônimo/`CLIENT`/`ADMIN`;
- verificar cadastro público sempre como `CLIENT`, login local válido/inválido, login Google de
  cliente e `ADMIN`, JWT válido, expirado e adulterado, rotação, revogação, reuso, logout e negação padrão;
- verificar handoff Google por redirecionamento fixo, consumo único, ausência de tokens na URL,
  proteção CSRF nos endpoints de cookie e CORS apenas para as origens configuradas;
- verificar que `sub` inédito com e-mail já cadastrado é rejeitado sem account linking automático;
- provar idempotência concorrente do interesse e do conjunto `Reminder`/`ContactRecord`;
- preservar as invariantes de imagens em sucesso, rollback e falha externa;
- validar consentimento, contatos pendentes/concluídos e conclusão conjunta do lembrete;
- validar o mesmo `ApiErrorResponse` em domínio, validação, autenticação, autorização e fallback 500;
- executar sem acesso real ao Cloudinary ou Google.

O Docker é usado pela suíte apenas para o PostgreSQL efêmero do Testcontainers. O backend não
precisa ser empacotado ou executado em container durante o desenvolvimento.

## 4. Inicie a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

Use um objeto `WebRequestSession` para preservar cookies `HttpOnly`. Inicialize o CSRF pelo endpoint
explícito e, nas operações que consomem refresh ou handoff, envie o valor obtido no header
`X-XSRF-TOKEN`:

```powershell
$api='http://localhost:8080/api/v1'
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
Invoke-WebRequest -Method Get -Uri "$api/auth/csrf" -WebSession $session | Out-Null
$csrfToken=($session.Cookies.GetCookies([Uri]"$api/auth/csrf") |
  Where-Object Name -eq 'XSRF-TOKEN').Value
$cookieHeaders=@{Origin='http://localhost:3000';'X-XSRF-TOKEN'=$csrfToken}
```

`GET /auth/csrf` é público e apenas materializa ou renova o cookie legível `XSRF-TOKEN`; ele não
autentica nem emite access, refresh ou handoff. O frontend deve chamá-lo na inicialização, após o
retorno do OAuth e novamente antes de repetir uma operação rejeitada por CSRF. O valor do cookie é
enviado em `X-XSRF-TOKEN`; o refresh permanece em outro cookie `HttpOnly`.

## 5. Valide catálogo anônimo

```powershell
Invoke-RestMethod -Method Get -Uri "$api/catalog/products?page=0&size=20"
```

Resultado esperado: `200`; nenhuma autenticação é solicitada; a lista contém apenas produtos ativos
ou fica vazia de forma válida. Respostas nunca contêm preço, estoque ou tamanho.

## 6. Cadastre e autentique um cliente

```powershell
$registration=@{
  firstName='Cliente'
  lastName='Teste'
  email='cliente@example.com'
  password='<senha-de-teste>'
  whatsappPhone='+5511999999999'
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri "$api/auth/customers" `
  -ContentType 'application/json' -Body $registration

$login=@{email='cliente@example.com';password='<senha-de-teste>'} | ConvertTo-Json
$auth=Invoke-RestMethod -Method Post -Uri "$api/auth/login" `
  -WebSession $session -ContentType 'application/json' -Body $login
$headers=@{Authorization="Bearer $($auth.accessToken)"}
```

Resultado esperado: cadastro `201`, login `200`, access token JWT retornado, refresh token apenas no
cookie seguro e papel `CLIENT`. A
autorização de contato proativo começa como `false` e o cliente não consegue acessar `/admin/**`.

Valide também `POST /auth/refresh`, incluindo cookie, `Origin` autorizado e header CSRF: o access
token é renovado e o refresh token é rotacionado.
Reutilizar o valor anterior deve retornar `401` e revogar a família. Para Google, abra
`/auth/google/client`, conclua o fluxo e confirme que o callback configura apenas o handoff temporário
e redireciona para uma URL conhecida do frontend, sem JSON ou tokens na URL. Para conta existente, o
frontend chama `POST /auth/google/exchange` com cookie, `Origin` e CSRF para receber o access token no
corpo e o refresh token em cookie. Um cliente novo é redirecionado à rota fixa de conclusão e envia
somente nome, sobrenome e WhatsApp a `POST /auth/google/customers`; o handoff não pode ser reutilizado.
Crie também uma conta tradicional e simule um retorno Google com `sub` ainda não vinculado e o mesmo
e-mail: o callback deve seguir para a rota fixa de erro sem criar identidade externa. A conta continua
acessível somente pela autenticação que já possuía; a V1 não oferece vinculação manual.

## 7. Valide consentimento

```powershell
Invoke-RestMethod -Method Get -Uri "$api/customers/me/proactive-contact-consent" `
  -Headers $headers

$grant=@{authorized=$true} | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "$api/customers/me/proactive-contact-consent" `
  -Headers $headers -ContentType 'application/json' -Body $grant

$revoke=@{authorized=$false} | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "$api/customers/me/proactive-contact-consent" `
  -Headers $headers -ContentType 'application/json' -Body $revoke
```

Resultado esperado: concessão exige ação afirmativa, revogação vale imediatamente e nenhuma das duas
operações bloqueia catálogo, autenticação ou atendimento solicitado pelo cliente.

## 8. Valide interesse e WhatsApp

Escolha um `productId` ativo retornado pelo catálogo:

```powershell
$key=[guid]::NewGuid().ToString()
$interest=Invoke-RestMethod -Method Post -Uri "$api/products/<productId>/interests" `
  -Headers ($headers + @{'Idempotency-Key'=$key})

$interest
```

Resultado esperado: `201`, um único `interestId` e uma `whatsappUrl` cujo destino é o número oficial
e cujo texto identifica somente o produto. Repetir a chamada com a mesma chave retorna o mesmo
interesse; usar a chave para outro produto retorna `409`. Produto inativo não cria interesse. A
primeira confirmação também cria exatamente um lembrete automático acionável e um `ContactRecord`
`PENDING`; retries não criam novos acompanhamentos.

## 9. Valide administração do catálogo

Revogue o refresh token do cliente e autentique a administradora pelo Google autorizado:

```powershell
Invoke-RestMethod -Method Post -Uri "$api/auth/logout" -WebSession $session -Headers $cookieHeaders
```

Use `/auth/google/admin` e o access token administrativo no header Bearer. Confirme antes que outra
conta Google é redirecionada para a rota fixa de erro, sem handoff, e não cria nem promove `Account`;
ela continua podendo usar o fluxo de cliente. Depois do login administrativo, valide pelo contrato:

1. criar e renomear uma categoria;
2. criar produto multipart com uma ou mais imagens e uma principal;
   confirmar que listagem e detalhe retornam as URLs ordenadas de todas as imagens do produto;
3. trocar a imagem principal;
4. tentar remover a última imagem e receber `422`;
5. inativar o produto e confirmar que ele some do catálogo e rejeita novo interesse;
6. simular falha do adapter de mídia e confirmar que não há estado parcial publicado.

## 10. Valide CRM

Como `ADMIN`, execute os fluxos documentados em `/admin`:

1. localizar o cliente por nome, e-mail ou telefone;
2. consultar seus interesses;
3. localizar o lembrete automático e seu `ContactRecord` `PENDING`, com canal WhatsApp, data,
   cliente/código e produto corretos;
4. confirmar que o contato pendente não aparece como conversa concluída;
5. completar o `ContactRecord` com data e descrição e confirmar que o lembrete relacionado muda
   para `COMPLETED` no mesmo commit;
6. registrar separadamente um contato manual com data, canal e descrição;
7. confirmar que não existem operações para criar ou concluir lembretes diretamente;
8. revogar o consentimento como cliente e confirmar que o lembrete originado pelo interesse continua
   acionável, sem apagar histórico nem bloquear o acompanhamento solicitado pelo cliente.

Resultado esperado: apenas `ADMIN` acessa o CRM; lembrete vencido continua `PENDING`; conclusão não
remove o registro.

## 11. Valide caminhos negativos de segurança

- Bearer ausente, JWT expirado ou assinatura/claims inválidos em recurso protegido retornam `401`.
- Refresh token ausente, expirado, revogado ou reutilizado retorna `401` e não emite access token.
- Cliente autenticado em `/admin/**` retorna `403`.
- Cadastro público que tente informar papel administrativo é rejeitado e nunca cria `ADMIN`.
- Login com e-mail inexistente e senha incorreta retorna o mesmo formato genérico.
- Conta Google não autorizada nunca recebe papel `ADMIN`.
- `sub` Google inédito com e-mail já usado é rejeitado sem vincular ou assumir a conta local.
- A conta Google cujo `sub` corresponde à configuração segura autentica a única `ADMIN`.
- Retirar consentimento proativo não altera autenticação, papel ou acesso do cliente às próprias operações.
- Origem CORS não configurada é rejeitada; nenhuma resposta usa origem curinga com credenciais.
- Refresh, logout e troca/conclusão do handoff sem token CSRF válido são rejeitados.
- `GET /auth/csrf` apenas renova `XSRF-TOKEN` e nunca cria autenticação ou credenciais.
- Erros usam `application/json` com `ApiErrorResponse` e não incluem stack trace, senha, token, hash,
  resposta do Cloudinary ou dados de outro cliente. Validação, `AuthenticationEntryPoint` e
  `AccessDeniedHandler` preservam esse mesmo formato; falhas inesperadas usam `INTERNAL_001`.

## Completion Gate

A V1 está pronta para implementação concluída quando a suíte passa, as migrations sobem um banco
vazio, os cenários acima produzem os resultados esperados e não há divergência do contrato OpenAPI.
