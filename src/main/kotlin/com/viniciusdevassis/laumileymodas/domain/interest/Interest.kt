package com.viniciusdevassis.laumileymodas.domain.interest

import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import java.time.Instant
import java.util.UUID

data class Interest(
	val id: UUID,
	val customerId: UUID,
	val productId: UUID,
	val idempotencyKey: String,
	val createdAt: Instant,
) {
	init {
		if (idempotencyKey.isBlank() || idempotencyKey.length > 128) throw DomainException(InterestError.INVALID_IDEMPOTENCY_KEY)
	}
}
