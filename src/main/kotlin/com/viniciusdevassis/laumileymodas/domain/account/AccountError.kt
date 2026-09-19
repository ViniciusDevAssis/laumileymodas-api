package com.viniciusdevassis.laumileymodas.domain.account

import com.viniciusdevassis.laumileymodas.domain.common.DomainError

enum class AccountError(
	override val code: String,
	override val message: String,
	override val type: DomainError.Type,
) : DomainError {
	INVALID_EMAIL("ACCOUNT_001", "E-mail inválido.", DomainError.Type.INVALID),
	INVALID_NAME("ACCOUNT_002", "Nome inválido.", DomainError.Type.INVALID),
	INVALID_PHONE("ACCOUNT_003", "Telefone/WhatsApp inválido.", DomainError.Type.INVALID),
	ADMIN_PUBLIC_REGISTRATION("ACCOUNT_004", "Cadastro público de administrador não é permitido.", DomainError.Type.INVALID),
	INVALID_EXTERNAL_IDENTITY("ACCOUNT_005", "Identidade externa inválida.", DomainError.Type.INVALID),
}
