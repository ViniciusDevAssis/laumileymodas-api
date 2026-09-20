package com.viniciusdevassis.laumileymodas.domain.exceptions

enum class ApiError(
	val code: String,
	val message: String,
) {
	AUTH_001("AUTH_001", "Autenticação necessária ou inválida."),
	SEC_001("SEC_001", "Acesso negado."),
	VALIDATION_001("VALIDATION_001", "Dados inválidos."),
	CATALOG_001("CATALOG_001", "Produto não encontrado."),
	INTERNAL_001("INTERNAL_001", "Erro interno inesperado.")
}
