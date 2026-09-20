<!--
Sync Impact Report
- Version change: 1.0.0 -> 1.1.0
- Modified principles: II, V, VI, VII, X e Development Workflow and Quality Gates
- Added sections: none
- Removed sections: none
- Follow-up TODOs: none
-->
# Laumiley Modas Constitution

## Core Principles

### I. Spec First

Toda funcionalidade relevante DEVE ser especificada antes da implementação. A especificação
DEVE descrever o comportamento esperado, as regras de negócio, as necessidades do usuário e os
critérios de aceitação. Decisões de implementação NÃO DEVEM substituir requisitos de negócio.
Mudanças relevantes descobertas durante o desenvolvimento DEVEM ser refletidas nos artefatos de
especificação antes de a funcionalidade ser considerada concluída.

Este princípio mantém produto, implementação e validação alinhados e permite avaliar o resultado
por seu comportamento observável.

### II. Simplicidade da V1

A implementação DEVE atender somente a necessidades reais do escopo atual. Abstrações,
infraestrutura, serviços e funcionalidades NÃO DEVEM ser criados apenas para uma possibilidade
futura. Entre soluções adequadas, DEVE ser escolhida inicialmente a alternativa mais simples que
preserve clareza, segurança, testabilidade e uma possibilidade razoável de evolução.

Complexidade adicional DEVE ter uma necessidade atual identificável e uma justificativa registrada
no artefato de planejamento correspondente. A evolução futura do produto não autoriza antecipar
problemas que ainda não existem.

Uma classe, interface ou camada nova DEVE justificar sua existência por uma regra de negócio, um
limite de segurança, uma integração externa ou reutilização concreta. Organização visual,
simetria entre camadas e possível uso futuro NÃO constituem justificativa suficiente.

### III. Backend como Autoridade

O backend DEVE aplicar as regras de negócio, a autorização, a validação de dados e toda mudança de
estado relevante. O frontend DEVE ser tratado como uma entrada não confiável para decisões
críticas. Nenhuma regra importante ou restrição de acesso PODE depender exclusivamente de
validação realizada no cliente.

Essa autoridade garante comportamento consistente para qualquer consumidor da API e impede que
controles críticos sejam contornados pela manipulação do cliente.

### IV. Segurança e Privacidade dos Dados

Dados pessoais de clientes e informações internas do CRM DEVEM ser tratados como protegidos. A
aplicação DEVE autenticar e autorizar cada operação conforme o tipo de usuário e o recurso
acessado. Clientes NÃO PODEM acessar informações administrativas, dados de outros clientes ou
informações internas do CRM.

APIs, logs e mensagens de erro NÃO DEVEM expor informações sensíveis sem necessidade. A coleta e
a persistência de dados pessoais DEVEM se limitar a informações com finalidade definida no
produto. Quando dados forem usados para relacionamento comercial, lembretes ou ofertas
personalizadas, a aplicação DEVE respeitar as preferências de contato, as autorizações aplicáveis e
a possibilidade de interrupção dessas comunicações.

### V. Regras de Negócio Claras e Testáveis

Regras relevantes do domínio DEVEM ser expressas de forma explícita e verificável. Regras,
permissões e fluxos críticos DEVEM possuir testes automatizados que protejam seu comportamento
contra regressões. Um teste NÃO DEVE existir apenas para reproduzir detalhes internos da
implementação sem proteger um requisito ou risco observável.

Os testes DEVEM tornar mudanças seguras e comunicar as invariantes do negócio, sem acoplar a suíte
a uma estrutura interna que possa evoluir sem alterar o comportamento.

Testes de arquitetura que imponham quantidade de camadas, interfaces, mappers ou classes NÃO DEVEM
substituir testes do comportamento observável. A estrutura interna PODE ser simplificada sem exigir
reescrita dos testes quando o contrato e as regras permanecerem iguais.

### VI. Integrações Externas Isoladas

WhatsApp, e-mail, armazenamento de mídia e outros serviços externos NÃO DEVEM definir o modelo de
negócio interno. Tipos, SDKs e detalhes específicos de fornecedores DEVEM permanecer isolados na
infraestrutura. Uma interface própria DEVE existir quando representar um limite externo relevante,
como armazenamento de mídia. Interfaces para persistência interna ou delegações sem alternativa
real NÃO DEVEM ser criadas apenas para reproduzir um padrão arquitetural.

Trocar uma integração externa NÃO DEVE exigir alterações desnecessárias nas regras centrais do
sistema.

### VII. Arquitetura com Responsabilidades Claras, sem Dogmatismo

O sistema DEVE separar responsabilidades conceituais da seguinte forma:

- `presentation`: entrada e saída, incluindo controllers, requests, responses e exposição da API;
- `application`: services coesos que orquestram os fluxos da aplicação;
- `domain`: regras, comportamentos, conceitos e modelos centrais do negócio;
- `infrastructure`: persistência, integrações, configuração técnica e implementações dependentes de
  frameworks ou serviços externos.

Essa divisão orienta responsabilidades e NÃO representa Clean Architecture, arquitetura hexagonal
ou obrigação de pureza entre camadas. O fluxo padrão PODE ser
`Controller -> Service -> Spring Data Repository -> entidade JPA`.
Entidades de negócio PODEM ser também entidades JPA, e repositórios internos PODEM ser interfaces
Spring Data usadas diretamente pelos services. Duplicar entidade de domínio e entidade de
persistência, criar mapper entre modelos equivalentes ou criar adapter que apenas delega ao
repositório é PROIBIDO sem benefício concreto documentado.

Fluxos DEVEM ser coordenados por services coesos na camada `application`. Um service PODE agrupar
várias operações do mesmo contexto; não se exige uma classe de use case ou comando por operação. Controllers
DEVEM permanecer pequenos, transações pertencem aos services e detalhes de persistência permanecem
nos repositórios. Tipos de Spring Security e SDKs externos NÃO DEVEM contaminar regras de negócio.

DDD DEVE ser aplicado de forma leve. Entidades, value objects, serviços de domínio, agregados,
interfaces e camadas adicionais somente DEVEM ser introduzidos quando melhorarem concretamente a
clareza, a testabilidade, a manutenção ou a evolução. Uma única classe NÃO PODE acumular regras de
negócio, persistência, autorização, integrações e orquestração a ponto de formar um God Service.

O número de classes NÃO é uma medida de qualidade. Construtores ou atribuições diretas DEVEM ser
preferidos a mappers triviais; `java.time.Clock` PODE ser injetado diretamente quando o tempo fizer
parte de uma regra; UUIDs PODEM ser gerados diretamente pela aplicação ou persistência. Uma
abstração adicional exige um segundo comportamento real, um limite externo ou uma regra que ela
torne materialmente mais clara.

Configurações da V1 DEVEM permanecer declaradas em `src/main/resources/application.yaml`. Quando
uma classe precisar de um valor configurável, ela DEVE recebê-lo diretamente, preferencialmente por
injeção de construtor com `@Value`. Uma classe central de propriedades, como `ApplicationProperties`
ou estrutura equivalente com `@ConfigurationProperties`, NÃO DEVE ser criada sem autorização
explícita do responsável pelo projeto.

### VIII. Banco de Dados Versionado

Toda alteração estrutural do banco de dados DEVE ser registrada em uma migration versionada.
Mudanças de schema NÃO PODEM depender de execução manual direta em um ambiente. As migrations
DEVEM permitir que ambientes compatíveis alcancem o mesmo schema de forma repetível e auditável.

### IX. Qualidade antes da Conclusão

Uma funcionalidade somente PODE ser considerada concluída quando satisfizer seus critérios de
aceitação e, conforme os riscos e comportamentos definidos na especificação, contemplar validações,
cenários de erro, autorização, persistência, testes e consistência dos dados. O fluxo principal
executar com sucesso, isoladamente, NÃO é evidência suficiente de conclusão.

Os controles aplicáveis DEVEM ser identificados durante specification e planning e verificados
antes da entrega.

### X. Armazenamento e Gerenciamento de Mídia

Imagens do catálogo NÃO PODEM ser armazenadas como conteúdo binário no banco de dados nem no
sistema de arquivos da aplicação. Na V1, o Cloudinary DEVE armazenar e entregar as imagens dos
produtos, sem introduzir dependência das regras centrais em relação ao provedor.

O fluxo de mídia DEVE obedecer às seguintes responsabilidades:

1. a administradora envia a imagem pela aplicação;
2. a API recebe o arquivo e coordena o envio ao provedor externo;
3. o provedor retorna as referências necessárias para identificar e acessar o asset;
4. a API persiste a URL de acesso e um identificador externo adequado ao gerenciamento;
5. o frontend utiliza as informações fornecidas pela API para exibir a imagem.

O frontend NÃO PODE executar upload ou exclusão diretamente no Cloudinary. O ciclo de vida dos
assets DEVE ser coordenado por um service coeso em `application`, e a comunicação com o provedor
DEVE permanecer em `infrastructure`. `domain` e `application` NÃO PODEM depender diretamente de
SDKs, APIs ou tipos do Cloudinary. Uma abstração pequena para armazenamento de mídia DEVE manter o
provedor isolado.

O sistema DEVE preservar referências suficientes para administrar os assets e DEVE coordenar
persistência e operações externas para evitar, dentro das garantias disponíveis, referências
inconsistentes e arquivos órfãos. O gerenciamento de mídia DEVE permanecer na aplicação principal.
Um serviço independente de mídia somente PODE ser considerado mediante requisitos concretos, como
múltiplas aplicações consumidoras, processamento intensivo, volume elevado ou necessidade de
escalabilidade independente.

## Product and Technology Constraints

A V1 apoia o fluxo comercial **redes sociais -> catálogo -> interesse em um produto -> WhatsApp ->
negociação e fechamento da venda**. Seu escopo compreende catálogo público, cadastro e autenticação
de clientes e da administradora, identificação do cliente ao demonstrar interesse, encaminhamento
para atendimento pelo WhatsApp, administração do catálogo e o início de um CRM com base de
clientes, histórico de contatos e lembretes. A venda NÃO será concluída dentro da aplicação na V1.

A stack inicial é Kotlin, Java 21, Spring Boot 3.5.16, Spring Web, PostgreSQL, Flyway, API REST e
testes unitários e de integração. Uma mudança de stack DEVE apresentar justificativa técnica ou de
negócio e considerar impacto, migração e compatibilidade.

Esta constituição NÃO define entidades, tabelas, endpoints, contratos de API, estrutura definitiva
de packages, regras detalhadas de funcionalidades ou uma arquitetura além das responsabilidades
estabelecidas. Também NÃO autoriza funcionalidades futuras fora da V1. Essas decisões DEVEM ser
tomadas nos artefatos de specification, clarification e planning correspondentes.

## Development Workflow and Quality Gates

O desenvolvimento DEVE seguir a sequência specification, clarification quando necessária,
planning, tasks, implementation e validation. Cada etapa DEVE usar os artefatos aprovados das
etapas anteriores como fonte de requisitos, mantendo rastreabilidade entre comportamento esperado,
implementação e validação.

Antes da implementação, a equipe DEVE verificar se o trabalho pertence à V1, se os critérios de
aceitação são testáveis e se decisões ainda indefinidas foram encaminhadas à etapa correta. Durante
a implementação, mudanças relevantes de escopo ou comportamento DEVEM retornar aos artefatos de
especificação e planejamento.

Antes da conclusão, a revisão DEVE verificar conformidade com esta constituição, critérios de
aceitação, autorização, privacidade, migrations quando houver mudança de schema e testes
proporcionais ao risco. Complexidade ou exceções aos princípios DEVEM ser justificadas por escrito
no plano ou na revisão da mudança.

O planejamento e a revisão DEVEM aplicar um teste de necessidade a cada abstração: qual requisito,
risco ou integração exige esta classe ou interface? Se não houver resposta concreta, a solução DEVE
ser simplificada antes da implementação.

## Governance

Esta constituição prevalece sobre práticas informais e decisões técnicas conflitantes. Todo plano,
implementação e revisão DEVE demonstrar conformidade com seus princípios. Divergências temporárias
exigem justificativa explícita, avaliação de risco e um plano de correção; elas não alteram a
constituição por si mesmas.

Emendas DEVEM ser propostas por escrito, apresentar motivação e impacto, ser revisadas pelo
responsável pelo projeto e registrar eventuais necessidades de migração. Uma emenda entra em vigor
quando aprovada e incorporada a este documento.

O versionamento segue Semantic Versioning para governança:

- MAJOR: remoção ou redefinição incompatível de um princípio ou regra de governança;
- MINOR: inclusão de princípio, seção ou expansão material de orientação;
- PATCH: esclarecimentos e ajustes editoriais sem mudança de obrigação.

Toda emenda DEVE atualizar a versão, a data de última alteração e o Sync Impact Report. A
conformidade DEVE ser revisada em cada planejamento e antes de cada entrega relevante. A
constituição também DEVE ser revisitada quando o escopo do produto ou a arquitetura mudar de forma
material.

**Version**: 1.1.0 | **Ratified**: 2026-09-18 | **Last Amended**: 2026-09-20
