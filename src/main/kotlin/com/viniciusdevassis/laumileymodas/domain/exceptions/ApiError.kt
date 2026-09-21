package com.viniciusdevassis.laumileymodas.domain.exceptions

enum class ApiError(
	val code: String,
	val message: String,
) {
	AUTH_001("AUTH_001", "Autenticação necessária ou inválida."),
	AUTH_002("AUTH_002", "E-mail ou senha inválidos."),
	AUTH_003("AUTH_003", "E-mail já cadastrado."),
	AUTH_004("AUTH_004", "Limite de tentativas de autenticação excedido."),
	AUTH_005("AUTH_005", "A identidade Google não pôde ser validada."),
	AUTH_006("AUTH_006", "A identidade Google conflita com uma conta existente."),
	SEC_001("SEC_001", "Acesso negado."),
	VALIDATION_001("VALIDATION_001", "Dados inválidos."),
	CATALOG_001("CATALOG_001", "Produto não encontrado."),
	INTEREST_001("INTEREST_001", "Produto indisponível para iniciar um interesse."),
	INTEREST_002("INTEREST_002", "Chave de idempotência já utilizada para outro produto."),
	CUSTOMER_001("CUSTOMER_001", "Cliente não encontrado."),
	INTERNAL_001("INTERNAL_001", "Erro interno inesperado.")
}
