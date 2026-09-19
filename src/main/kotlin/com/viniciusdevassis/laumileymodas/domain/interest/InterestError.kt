package com.viniciusdevassis.laumileymodas.domain.interest

import com.viniciusdevassis.laumileymodas.domain.common.DomainError

enum class InterestError(
	override val code: String,
	override val message: String,
	override val type: DomainError.Type,
) : DomainError {
	INVALID_IDEMPOTENCY_KEY("INTEREST_001", "Chave de idempotência inválida.", DomainError.Type.INVALID),
	IDEMPOTENCY_CONFLICT("INTEREST_002", "A chave de idempotência já foi usada para outro produto.", DomainError.Type.CONFLICT),
	NOT_FOUND("INTEREST_003", "Interesse não encontrado.", DomainError.Type.NOT_FOUND),
}
