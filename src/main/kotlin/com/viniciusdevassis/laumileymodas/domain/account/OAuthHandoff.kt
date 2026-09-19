package com.viniciusdevassis.laumileymodas.domain.account

import java.time.Instant
import java.util.UUID

enum class OAuthHandoffPurpose { EXISTING_ACCOUNT_LOGIN, CLIENT_REGISTRATION }

data class OAuthHandoff(
	val id: UUID,
	val handleHash: String,
	val purpose: OAuthHandoffPurpose,
	val accountId: UUID?,
	val providerSubject: String,
	val verifiedEmail: String,
	val givenName: String?,
	val familyName: String?,
	val expiresAt: Instant,
	val consumedAt: Instant? = null,
	val createdAt: Instant,
) {
	fun isUsable(now: Instant) = consumedAt == null && now.isBefore(expiresAt)
	fun consume(now: Instant) = copy(consumedAt = now)
}
