package com.viniciusdevassis.laumileymodas.application.security

import com.viniciusdevassis.laumileymodas.application.common.ApplicationError

enum class SecurityError(
	override val code: String,
	override val message: String,
	override val type: ApplicationError.Type,
) : ApplicationError {
	AUTHENTICATION_REQUIRED("SECURITY_003", "Autenticação necessária ou inválida.", ApplicationError.Type.UNAUTHORIZED),
	ACCESS_DENIED("SECURITY_004", "Você não possui permissão para realizar esta operação.", ApplicationError.Type.FORBIDDEN),
}
