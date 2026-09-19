# Quickstart: validar a Operação Comercial V1

Este guia descreve a validação esperada após a implementação. O contrato detalhado está em
[contracts/openapi.yaml](./contracts/openapi.yaml) e o schema em [data-model.md](./data-model.md).

## Prerequisites

- Java 21
- Docker com PostgreSQL disponível para desenvolvimento e testes
- Credenciais de um ambiente de desenvolvimento do Cloudinary
- Número de WhatsApp da loja em formato E.164

## 1. Configure PostgreSQL

Exemplo descartável para desenvolvimento:

```powershell
docker run --name laumiley-postgres `
  -e POSTGRES_DB=laumiley `
  -e POSTGRES_USER=laumiley `
  -e POSTGRES_PASSWORD=local-only `
  -p 5432:5432 `
  -d postgres:17
```

Configure a aplicação sem versionar segredos:

```powershell
$env:SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/laumiley'
$env:SPRING_DATASOURCE_USERNAME='laumiley'
$env:SPRING_DATASOURCE_PASSWORD='local-only'
$env:CLOUDINARY_CLOUD_NAME='<cloud-name>'
$env:CLOUDINARY_API_KEY='<api-key>'
$env:CLOUDINARY_API_SECRET='<api-secret>'
$env:LAUMILEY_WHATSAPP_NUMBER='<numero-e164-sem-sinal>'
$env:LAUMILEY_ALLOWED_ORIGINS='http://localhost:3000'
```

Resultado esperado: ao iniciar, Flyway aplica todas as migrations e Hibernate valida o schema sem
criá-lo ou alterá-lo automaticamente.

## 2. Provisione a única administradora

```powershell
$env:LAUMILEY_ADMIN_EMAIL='admin@example.com'
$env:LAUMILEY_ADMIN_PASSWORD='<senha-inicial-forte>'
.\mvnw.cmd -Dspring-boot.run.profiles=admin-bootstrap spring-boot:run
```

Resultado esperado: a primeira execução cria exatamente uma conta `ADMIN` e encerra. Uma segunda
execução falha sem criar ou alterar conta. Depois do sucesso, remova as variáveis e não execute o
perfil no funcionamento normal.

## 3. Execute verificações automatizadas

```powershell
.\mvnw.cmd test
```

A suíte deve:

- subir PostgreSQL real com Testcontainers e executar todo o histórico Flyway;
- verificar catálogo público e matriz anônimo/`CLIENT`/`ADMIN`;
- verificar CSRF, sessão, logout e negação padrão;
- provar idempotência concorrente do interesse;
- preservar as invariantes de imagens em sucesso, rollback e falha externa;
- validar consentimento, contatos e estados derivados dos lembretes;
- executar sem acesso real ao Cloudinary.

## 4. Inicie a aplicação

```powershell
.\mvnw.cmd spring-boot:run
```

Use uma sessão HTTP para preservar os cookies. O exemplo abaixo obtém o token CSRF:

```powershell
$api='http://localhost:8080/api/v1'
$session=New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrf=Invoke-RestMethod -Method Get -Uri "$api/auth/csrf" -WebSession $session
$headers=@{'X-CSRF-TOKEN'=$csrf.token}
```

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
  -WebSession $session -Headers $headers -ContentType 'application/json' -Body $registration

$login=@{email='cliente@example.com';password='<senha-de-teste>'} | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$api/auth/login" `
  -WebSession $session -Headers $headers -ContentType 'application/json' -Body $login
```

Resultado esperado: cadastro `201`, login `200`, cookie de sessão criado e papel `CLIENT`. A
autorização de contato proativo começa como `false` e o cliente não consegue acessar `/admin/**`.

## 7. Valide consentimento

```powershell
Invoke-RestMethod -Method Get -Uri "$api/customers/me/proactive-contact-consent" `
  -WebSession $session

$grant=@{authorized=$true} | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "$api/customers/me/proactive-contact-consent" `
  -WebSession $session -Headers $headers -ContentType 'application/json' -Body $grant

$revoke=@{authorized=$false} | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "$api/customers/me/proactive-contact-consent" `
  -WebSession $session -Headers $headers -ContentType 'application/json' -Body $revoke
```

Resultado esperado: concessão exige ação afirmativa, revogação vale imediatamente e nenhuma das duas
operações bloqueia catálogo, autenticação ou atendimento solicitado pelo cliente.

## 8. Valide interesse e WhatsApp

Escolha um `productId` ativo retornado pelo catálogo:

```powershell
$key=[guid]::NewGuid().ToString()
$interest=Invoke-RestMethod -Method Post -Uri "$api/products/<productId>/interests" `
  -WebSession $session -Headers ($headers + @{'Idempotency-Key'=$key})

$interest
```

Resultado esperado: `201`, um único `interestId` e uma `whatsappUrl` cujo destino é o número oficial
e cujo texto identifica somente o produto. Repetir a chamada com a mesma chave retorna o mesmo
interesse; usar a chave para outro produto retorna `409`. Produto inativo não cria interesse.

## 9. Valide administração do catálogo

Encerre a sessão do cliente, autentique a administradora e mantenha o cookie/CSRF atualizados:

```powershell
Invoke-RestMethod -Method Post -Uri "$api/auth/logout" -WebSession $session -Headers $headers
```

Depois do login administrativo, valide pelo contrato:

1. criar e renomear uma categoria;
2. criar produto multipart com uma ou mais imagens e uma principal;
3. trocar a imagem principal;
4. tentar remover a última imagem e receber `422`;
5. inativar o produto e confirmar que ele some do catálogo e rejeita novo interesse;
6. simular falha do adapter de mídia e confirmar que não há estado parcial publicado.

## 10. Valide CRM

Como `ADMIN`, execute os fluxos documentados em `/admin`:

1. localizar o cliente por nome, e-mail ou telefone;
2. consultar seus interesses;
3. registrar contato com data, canal e descrição;
4. criar lembrete `CUSTOMER_REQUEST_FOLLOW_UP` e concluí-lo;
5. criar lembrete `PROACTIVE_CONTACT` com consentimento vigente;
6. revogar o consentimento como cliente e confirmar que o lembrete proativo fica `actionable=false`,
   sem apagar histórico nem bloquear o acompanhamento solicitado pelo cliente.

Resultado esperado: apenas `ADMIN` acessa o CRM; lembrete vencido continua `PENDING`; conclusão não
remove o registro.

## 11. Valide caminhos negativos de segurança

- Requisição mutável sem `X-CSRF-TOKEN` retorna `403`.
- Sessão ausente ou expirada em recurso protegido retorna `401`.
- Cliente autenticado em `/admin/**` retorna `403`.
- Login com e-mail inexistente e senha incorreta retorna o mesmo formato genérico.
- Erros usam `application/problem+json` e não incluem stack trace, senha, hash, resposta do Cloudinary
  ou dados de outro cliente.

## Completion Gate

A V1 está pronta para implementação concluída quando a suíte passa, as migrations sobem um banco
vazio, os cenários acima produzem os resultados esperados e não há divergência do contrato OpenAPI.
