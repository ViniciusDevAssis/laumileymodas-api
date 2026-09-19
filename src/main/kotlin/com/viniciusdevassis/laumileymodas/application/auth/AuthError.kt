package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.common.ApplicationError

enum class AuthError(
	override val code: String,
	override val message: String,
	override val type: ApplicationError.Type,
) : ApplicationError {
	INVALID_CREDENTIALS("AUTH_001", "E-mail ou senha inválidos.", ApplicationError.Type.UNAUTHORIZED),
	EMAIL_ALREADY_EXISTS("AUTH_002", "Já existe uma conta com este e-mail.", ApplicationError.Type.CONFLICT),
	INVALID_REFRESH_TOKEN("AUTH_003", "Refresh token inválido ou expirado.", ApplicationError.Type.UNAUTHORIZED),
	REFRESH_REUSE("AUTH_004", "Reutilização de refresh token detectada.", ApplicationError.Type.UNAUTHORIZED),
	INVALID_HANDOFF("AUTH_005", "Handoff OAuth inválido ou expirado.", ApplicationError.Type.UNAUTHORIZED),
	GOOGLE_EMAIL_CONFLICT("AUTH_006", "O e-mail já pertence a uma conta local. Utilize a autenticação existente.", ApplicationError.Type.CONFLICT),
	GOOGLE_ADMIN_NOT_ALLOWED("AUTH_007", "Conta Google não autorizada para administração.", ApplicationError.Type.FORBIDDEN),
	RATE_LIMITED("AUTH_008", "Muitas tentativas de autenticação. Tente novamente mais tarde.", ApplicationError.Type.TOO_MANY_REQUESTS),
	ACCOUNT_DISABLED("AUTH_009", "Conta desabilitada.", ApplicationError.Type.UNAUTHORIZED),
	INVALID_PASSWORD("AUTH_010", "Senha inválida.", ApplicationError.Type.INVALID),
}

