# Feature Specification: Operação Comercial V1

**Feature Branch**: não criada
**Created**: 2026-09-18
**Status**: Draft
**Input**: Catálogo público, identificação de clientes, registro de interesse com continuidade no WhatsApp,
administração do catálogo e CRM inicial para a Laumiley Modas.

## User Scenarios & Testing

### User Story 1 - Consultar o catálogo público (Priority: P1)

Como visitante, quero acessar o catálogo e consultar os detalhes e imagens das peças sem criar uma conta, para decidir
se tenho interesse em algum produto.

**Why this priority**: O catálogo é a entrada principal do fluxo comercial vindo das redes sociais e precisa entregar
valor mesmo sem autenticação.

**Independent Test**: Pode ser testado acessando o catálogo como visitante, consultando os produtos visíveis e abrindo
os detalhes de um deles sem autenticação.

**Acceptance Scenarios**:

1. **Given** que existem produtos ativos no catálogo, **When** um visitante acessa o catálogo, **Then** ele consegue
   visualizar esses produtos sem autenticação.
2. **Given** que um produto está ativo, **When** o visitante seleciona esse produto, **Then** ele visualiza nome,
   descrição, categoria e suas imagens.
3. **Given** que um produto possui várias imagens, **When** ele é apresentado na listagem do catálogo, **Then** sua
   imagem principal é utilizada como representação prioritária.
4. **Given** que não existem produtos ativos, **When** o visitante acessa o catálogo, **Then** ele recebe uma indicação
   clara de que não há produtos disponíveis no momento.

---

### User Story 2 - Demonstrar interesse e continuar no WhatsApp (Priority: P1)

Como cliente, quero me identificar, demonstrar interesse em um produto e seguir para o WhatsApp, para iniciar o
atendimento comercial sobre a peça escolhida.

**Why this priority**: Esse fluxo conecta o catálogo ao canal no qual a loja negocia e conclui suas vendas. “Demonstrar
interesse” e “pedir orçamento” representam a mesma ação de negócio.

**Independent Test**: Pode ser testado criando uma conta, autenticando o cliente, selecionando um produto, demonstrando
interesse e verificando o registro da interação e a continuidade para o WhatsApp.

**Acceptance Scenarios**:

1. **Given** que um visitante não está autenticado, **When** ele tenta demonstrar interesse em um produto, **Then** o
   sistema solicita sua identificação antes de continuar.
2. **Given** que o visitante ainda não possui conta, **When** ele conclui um cadastro válido, **Then** passa a poder se
   autenticar como cliente.
3. **Given** que um cliente autenticado selecionou um produto ativo, **When** confirma seu interesse, **Then** o sistema
   registra uma única interação relacionada ao cliente e ao produto.
4. **Given** que o interesse foi registrado, **When** o cliente prossegue com o atendimento, **Then** o sistema permite
   abrir o WhatsApp com contexto suficiente para identificar o produto escolhido.
5. **Given** que o cliente foi encaminhado ao WhatsApp, **When** a negociação avança, **Then** preço, tamanho,
   disponibilidade, pagamento e fechamento permanecem fora da aplicação.

---

### User Story 3 - Administrar o catálogo (Priority: P2)

Como administradora autenticada, quero manter produtos, categorias e imagens, para que o catálogo público apresente
informações atuais e organizadas aos visitantes.

**Why this priority**: O catálogo só sustenta o fluxo comercial se puder ser atualizado pela loja.

**Independent Test**: Pode ser testado autenticando a administradora, cadastrando uma categoria, cadastrando um produto
com múltiplas imagens, definindo sua imagem principal e verificando sua apresentação no catálogo público.

**Acceptance Scenarios**:

1. **Given** que a administradora está autenticada, **When** registra um produto válido associado a uma categoria e
   envia uma ou mais imagens, **Then** o produto pode ser disponibilizado no catálogo.
2. **Given** que um produto possui múltiplas imagens, **When** a administradora define uma delas como principal,
   **Then** essa imagem passa a ser utilizada prioritariamente na apresentação do produto.
3. **Given** que um produto já existe, **When** a administradora altera suas informações, categoria ou imagens, **Then**
   a versão atualizada passa a ser apresentada conforme seu status.
4. **Given** que um produto está ativo, **When** a administradora o torna inativo, **Then** ele deixa de aparecer no
   catálogo público e não pode iniciar novos fluxos de interesse.
5. **Given** que uma pessoa não possui autorização administrativa, **When** tenta executar uma operação de gestão do
   catálogo, **Then** o acesso é negado sem exposição de dados internos.
6. **Given** que o envio ou alteração de uma imagem não é concluído com sucesso, **When** a operação falha, **Then** o
   catálogo não apresenta uma atualização parcial ou uma referência inválida.

---

### User Story 4 - Consultar a base de clientes e seus interesses (Priority: P2)

Como administradora, quero localizar clientes e consultar os interesses que demonstraram, para compreender o
relacionamento comercial e dar continuidade ao atendimento.

**Why this priority**: A base de clientes e os interesses registrados formam o primeiro contexto do CRM e transformam as
interações do catálogo em informação útil para a loja.

**Independent Test**: Pode ser testado com clientes e interesses previamente registrados, localizando um cliente e
verificando que apenas a administradora acessa seu contexto comercial.

**Acceptance Scenarios**:

1. **Given** que há clientes cadastrados, **When** a administradora acessa a base de clientes, **Then** consegue
   consultar e localizar os registros necessários ao relacionamento comercial.
2. **Given** que um cliente demonstrou interesse em produtos, **When** a administradora consulta seu contexto, **Then**
   visualiza os interesses relacionados ao cliente e aos produtos correspondentes.
3. **Given** que um cliente autenticado tenta acessar informações internas do CRM, **When** solicita esses dados,
   **Then** o acesso é negado.

---

### User Story 5 - Registrar contatos e lembretes (Priority: P3)

Como administradora, quero registrar contatos realizados e ações futuras de relacionamento com um cliente, para
acompanhar o histórico e não perder oportunidades de acompanhamento.

**Why this priority**: Histórico e lembretes ampliam o valor da base de clientes, mas dependem da identificação dos
clientes e do contexto comercial já registrado.

**Independent Test**: Pode ser testado selecionando um cliente, registrando um contato e um lembrete futuro e depois
consultando essas informações no contexto do mesmo cliente.

**Acceptance Scenarios**:

1. **Given** que a administradora acessou um cliente, **When** registra uma interação contendo data, canal e descrição,
   **Then** ela passa a constar no histórico desse cliente.
2. **Given** que existem múltiplas interações, **When** a administradora consulta o histórico, **Then** consegue
   compreender sua sequência cronológica.
3. **Given** que a administradora pretende realizar uma ação futura, **When** registra um lembrete com descrição e data
   prevista, **Then** ele fica associado ao cliente como pendente.
4. **Given** que um lembrete pendente ultrapassou sua data prevista, **When** a administradora consulta os lembretes,
   **Then** ele continua pendente e é identificado como vencido.
5. **Given** que um lembrete foi atendido, **When** a administradora o conclui, **Then** ele deixa de aparecer como
   pendente sem perder o contexto do relacionamento.

## Edge Cases

* Se um produto se tornar inativo entre a consulta e a demonstração de interesse, o sistema deve impedir a criação de um
  novo interesse e informar a mudança ao cliente.
* Se a autenticação do cliente expirar durante o fluxo de interesse, o sistema deve solicitar nova autenticação e
  preservar, quando possível, o produto que originou a ação.
* Se o WhatsApp não puder ser aberto, o interesse já confirmado deve permanecer registrado sem ser duplicado.
* Se o envio, substituição ou exclusão de uma imagem falhar, o catálogo não deve apresentar referência inválida ou
  atualização parcial.
* Um produto não deve ser disponibilizado no catálogo sem pelo menos uma imagem válida.
* Um produto com múltiplas imagens deve possuir exatamente uma imagem definida como principal.
* A remoção da imagem principal não deve deixar um produto ativo sem outra imagem principal definida.
* Se não houver clientes, contatos, interesses ou lembretes, a área correspondente deve apresentar um estado vazio
  compreensível.
* Um lembrete vencido não deve desaparecer ou ser concluído automaticamente.
* Tentativas de acesso de clientes ou visitantes às informações administrativas devem ser negadas sem revelar conteúdo
  protegido.

## Requirements

### Functional Requirements

#### Catálogo

* **FR-001**: O sistema DEVE permitir acesso público ao catálogo sem exigir autenticação.
* **FR-002**: O catálogo DEVE apresentar somente produtos ativos.
* **FR-003**: Cada produto exibido no catálogo DEVE apresentar, no mínimo, nome, descrição, categoria e uma ou mais
  imagens.
* **FR-004**: O visitante DEVE conseguir acessar os detalhes de um produto a partir do catálogo.
* **FR-005**: O sistema DEVE informar claramente quando não houver produtos ativos.
* **FR-006**: Produtos inativos NÃO DEVEM ser utilizados para iniciar novos fluxos de interesse.

#### Clientes e autenticação

* **FR-007**: O sistema DEVE permitir que um visitante crie uma conta de cliente.
* **FR-008**: A criação da conta DEVE exigir nome, sobrenome, e-mail, senha e telefone/WhatsApp.
* **FR-009**: Os dados coletados no cadastro DEVEM ser utilizados apenas para identificação, autenticação e
  relacionamento comercial previsto na V1.
* **FR-010**: O sistema DEVE permitir que um cliente cadastrado se autentique posteriormente.
* **FR-011**: O catálogo DEVE permanecer acessível quando o visitante optar por não criar uma conta.
* **FR-012**: O sistema DEVE exigir identificação do cliente somente quando ele iniciar o fluxo de interesse em um
  produto.
* **FR-013**: O sistema DEVE restringir informações e operações administrativas à conta administrativa autenticada e
  autorizada.
* **FR-014**: A V1 DEVE possuir apenas uma conta administrativa válida.
* **FR-015**: O cadastro público NÃO DEVE permitir a criação de contas com privilégios administrativos.
* **FR-016**: A forma técnica de provisionamento da conta administrativa DEVE ser definida posteriormente no
  planejamento técnico.

#### Interesse e WhatsApp

* **FR-017**: “Demonstrar interesse” e “pedir orçamento” DEVEM representar um único fluxo funcional.
* **FR-018**: O sistema DEVE registrar cada interesse confirmado por um cliente autenticado.
* **FR-019**: Cada interesse registrado DEVE estar relacionado ao cliente identificado e ao produto selecionado.
* **FR-020**: Uma única confirmação válida NÃO DEVE produzir múltiplos registros de interesse.
* **FR-021**: Após registrar o interesse, o sistema DEVE permitir que o cliente prossiga para o atendimento da loja pelo
  WhatsApp.
* **FR-022**: A continuidade para o WhatsApp DEVE fornecer contexto suficiente para identificar o produto que motivou o
  contato.
* **FR-023**: Uma falha ao abrir o WhatsApp NÃO DEVE remover nem duplicar um interesse já confirmado.
* **FR-024**: A V1 NÃO DEVE realizar venda, checkout, pagamento, negociação ou fechamento da compra.

#### Produtos e categorias

* **FR-025**: A administradora DEVE conseguir cadastrar produtos contendo nome, descrição, categoria, imagens e status.
* **FR-026**: A administradora DEVE conseguir alterar nome, descrição, categoria, imagens e status de um produto.
* **FR-027**: Cada produto DEVE estar associado a uma categoria.
* **FR-028**: A administradora DEVE conseguir cadastrar categorias utilizadas para organizar os produtos.
* **FR-029**: A administradora DEVE conseguir alterar o nome de uma categoria cadastrada.
* **FR-030**: A administradora DEVE conseguir definir um produto como ativo ou inativo.
* **FR-031**: Produtos ativos DEVEM poder ser exibidos no catálogo público.
* **FR-032**: Produtos inativos NÃO DEVEM ser exibidos no catálogo nem utilizados para iniciar novos fluxos de
  interesse.
* **FR-033**: A V1 NÃO DEVE controlar preço, estoque, tamanhos disponíveis ou outras informações de disponibilidade
  comercial das peças.
* **FR-034**: Dúvidas sobre preço, tamanho, estoque ou disponibilidade DEVEM permanecer no atendimento realizado pelo
  WhatsApp.

#### Imagens

* **FR-035**: Cada produto DEVE possuir uma ou mais imagens.
* **FR-036**: A administradora DEVE enviar as imagens através da aplicação.
* **FR-037**: Quando um produto possuir múltiplas imagens, exatamente uma DEVE ser identificada como imagem principal.
* **FR-038**: A administradora DEVE conseguir alterar qual imagem é considerada principal.
* **FR-039**: O sistema DEVE impedir que um produto ativo fique sem pelo menos uma imagem válida e sem uma imagem
  principal.
* **FR-040**: O sistema DEVE informar falhas no gerenciamento de imagens sem publicar alterações incompletas ou
  referências inutilizáveis.
* **FR-041**: Pessoas sem autorização administrativa NÃO DEVEM criar, alterar ou remover imagens de produtos.

#### CRM e interesses

* **FR-042**: A administradora DEVE conseguir acessar a base de clientes cadastrados.
* **FR-043**: A administradora DEVE conseguir localizar um cliente na base para dar continuidade ao relacionamento
  comercial.
* **FR-044**: A administradora DEVE conseguir consultar os interesses registrados de um cliente e os respectivos
  produtos.
* **FR-045**: Clientes NÃO DEVEM acessar dados de outros clientes, informações administrativas ou conteúdo interno do
  CRM.

#### Histórico de contatos

* **FR-046**: A administradora DEVE conseguir registrar uma interação comercial realizada com um cliente.
* **FR-047**: Cada registro de contato DEVE conter, no mínimo, a data da interação, o canal utilizado e uma descrição do
  contato.
* **FR-048**: Cada registro de contato DEVE permanecer associado ao cliente correspondente.
* **FR-049**: A administradora DEVE conseguir consultar o histórico de contatos de um cliente em ordem que permita
  compreender a sequência cronológica das interações.

#### Lembretes

* **FR-050**: A administradora DEVE conseguir registrar um lembrete de ação futura relacionado a um cliente.
* **FR-051**: Cada lembrete DEVE conter, no mínimo, uma descrição da ação e a data prevista para sua realização.
* **FR-052**: Um lembrete DEVE possuir estado que permita distinguir pelo menos `pendente` e `concluído`.
* **FR-053**: A administradora DEVE conseguir consultar os lembretes pendentes.
* **FR-054**: Um lembrete pendente cuja data prevista tenha passado DEVE ser identificado como vencido sem que seu
  estado seja automaticamente alterado.
* **FR-055**: A administradora DEVE conseguir marcar um lembrete como concluído.
* **FR-056**: A conclusão de um lembrete NÃO DEVE apagar o registro necessário para preservar o contexto do
  relacionamento.
* **FR-057**: Um lembrete vencido NÃO DEVE ser ocultado automaticamente enquanto permanecer pendente.

#### Segurança, privacidade e limites da V1

* **FR-058**: O sistema DEVE apresentar somente dados pessoais necessários à operação autorizada.
* **FR-059**: O sistema NÃO DEVE expor informações sensíveis em mensagens de erro apresentadas aos usuários.
* **FR-060**: A V1 NÃO DEVE enviar automaticamente lembretes, ofertas ou mensagens personalizadas aos clientes.
* **FR-061**: A V1 NÃO DEVE incluir funcionalidades futuras de personalização ou automação que não estejam
  explicitamente definidas nesta especificação.

## Success Criteria

### Measurable Outcomes

* **SC-001**: Um visitante consegue acessar o catálogo e consultar os detalhes de produtos ativos sem autenticação.
* **SC-002**: Um visitante consegue navegar pelo catálogo sem criar conta, e a identificação somente é exigida ao
  iniciar o fluxo de interesse.
* **SC-003**: Produtos inativos não aparecem no catálogo público nem permitem a criação de novos interesses.
* **SC-004**: Cada confirmação válida de interesse produz um único registro relacionado ao cliente identificado e ao
  produto selecionado.
* **SC-005**: Após registrar o interesse, o cliente consegue prosseguir para o WhatsApp com contexto suficiente para
  identificar o produto escolhido.
* **SC-006**: Nenhum visitante ou cliente consegue executar operações administrativas ou acessar informações internas do
  CRM.
* **SC-007**: Um produto ativo apresenta nome, descrição, categoria e pelo menos uma imagem válida.
* **SC-008**: Produtos com múltiplas imagens possuem exatamente uma imagem principal utilizada prioritariamente na
  apresentação do catálogo.
* **SC-009**: Falhas no gerenciamento de imagens não deixam referências inválidas ou alterações parcialmente publicadas
  no catálogo.
* **SC-010**: A administradora consegue localizar um cliente e consultar os interesses registrados para ele e os
  respectivos produtos.
* **SC-011**: Um contato registrado aparece no histórico do cliente correto com data, canal e descrição, preservando a
  sequência das interações.
* **SC-012**: Um lembrete criado permanece pendente até ser concluído manualmente pela administradora.
* **SC-013**: Um lembrete pendente cuja data prevista tenha passado continua visível e é identificado como vencido.
* **SC-014**: A conclusão de um lembrete remove seu estado pendente sem apagar o contexto necessário do relacionamento.
* **SC-015**: Nenhum fluxo da V1 permite checkout, pagamento ou conclusão da venda dentro da aplicação.
* **SC-016**: O catálogo não apresenta preço, estoque ou tamanhos como informações controladas pela aplicação.
* **SC-017**: Falhas em autenticação, imagens ou encaminhamento ao WhatsApp não produzem interesses duplicados nem
  deixam o sistema em estado inconsistente.

## Assumptions

* A V1 atende exclusivamente à loja Laumiley Modas.
* A V1 utiliza um único destino oficial da loja no WhatsApp.
* Haverá apenas uma conta administrativa.
* O catálogo tem como objetivo apresentar visualmente os produtos e gerar interesse.
* O catálogo não representa uma fonte autoritativa de preço, estoque, tamanhos ou disponibilidade comercial.
* Preço, tamanho, estoque, disponibilidade, negociação, pagamento e fechamento da venda são tratados pelo WhatsApp.
* Cada produto pertence a uma única categoria na V1.
* As categorias são administradas pela própria loja e não são informadas livremente durante cada cadastro de produto.
* Cada produto possui uma ou mais imagens e exatamente uma delas é considerada principal.
* O histórico de contatos é alimentado manualmente pela administradora.
* Os interesses originados pelo catálogo são registrados automaticamente pelo fluxo correspondente e ficam disponíveis
  no contexto do cliente.
* Os lembretes são criados e concluídos manualmente pela administradora.
* A V1 não envia notificações ou mensagens automáticas aos clientes.
* Um lembrete vencido continua sendo funcionalmente um lembrete pendente até sua conclusão manual.
* O funcionamento do encaminhamento depende da disponibilidade do WhatsApp no dispositivo ou navegador do cliente.
* A gestão técnica do armazenamento das imagens segue a Constitution e não redefine o comportamento funcional descrito
  nesta especificação.
* A forma técnica de criação da conta administrativa será definida durante o planning.
* Políticas detalhadas de retenção e exclusão de dados poderão ser refinadas quando existirem requisitos legais ou
  operacionais suficientes, mantendo desde a V1 os princípios de minimização e proteção de dados.
