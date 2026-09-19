package com.viniciusdevassis.laumileymodas.domain.account

import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import java.time.Instant
import java.util.UUID

enum class ExternalIdentityProvider { GOOGLE }

data class AccountExternalIdentity(
	val id: UUID,
	val accountId: UUID,
	val provider: ExternalIdentityProvider,
	val subject: String,
	val emailAtLink: String,
	val createdAt: Instant,
	val lastLoginAt: Instant,
) {
	init {
		if (subject.isBlank() || emailAtLink.isBlank()) throw DomainException(AccountError.INVALID_EXTERNAL_IDENTITY)
	}
}
