package com.viniciusdevassis.laumileymodas.domain.account

import java.time.Instant
import java.util.UUID

data class RefreshToken(
	val id: UUID,
	val accountId: UUID,
	val familyId: UUID,
	val jtiHash: String,
	val expiresAt: Instant,
	val consumedAt: Instant? = null,
	val revokedAt: Instant? = null,
	val replacedById: UUID? = null,
	val createdAt: Instant,
) {
	fun isActive(now: Instant) = consumedAt == null && revokedAt == null && now.isBefore(expiresAt)
	fun consume(now: Instant, replacementId: UUID) = copy(consumedAt = now, replacedById = replacementId)
	fun revoke(now: Instant) = if (revokedAt == null) copy(revokedAt = now) else this
}
