package com.viniciusdevassis.laumileymodas.presentation.common.error

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiErrorResponse(
	val timestamp: OffsetDateTime,
	val status: Int,
	val code: String,
	val message: String,
	val path: String,
	val traceId: String? = null,
	val errors: List<ValidationErrorResponse>? = null,
)

enum class PresentationError(
	val code: String,
	val message: String,
) {
	VALIDATION("VALIDATION_001", "Um ou mais campos são inválidos."),
	AUTHENTICATION_REQUIRED("SECURITY_001", "Autenticação necessária ou inválida."),
	ACCESS_DENIED("SECURITY_002", "Você não possui permissão para realizar esta operação."),
	INTERNAL("INTERNAL_001", "Ocorreu um erro interno inesperado."),
}
