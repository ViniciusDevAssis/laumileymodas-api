package com.viniciusdevassis.laumileymodas.domain.exceptions

enum class ApiError(
	val code: String,
	val message: String,
) {
	AUTH_001("AUTH_001", "Autenticação necessária ou inválida."),
	AUTH_002("AUTH_002", "E-mail ou senha inválidos."),
	AUTH_003("AUTH_003", "E-mail já cadastrado."),
	AUTH_004("AUTH_004", "Limite de tentativas de autenticação excedido."),
	SEC_001("SEC_001", "Acesso negado."),
	VALIDATION_001("VALIDATION_001", "Dados inválidos."),
	CATALOG_001("CATALOG_001", "Produto não encontrado."),
	INTERNAL_001("INTERNAL_001", "Erro interno inesperado.")
}
