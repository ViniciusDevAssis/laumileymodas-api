package com.viniciusdevassis.laumileymodas.domain.account

import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import java.time.Instant
import java.util.UUID

data class Account(
	val id: UUID,
	val email: String,
	val passwordHash: String?,
	val role: AccountRole,
	val enabled: Boolean,
	val createdAt: Instant,
	val updatedAt: Instant,
) {
	val normalizedEmail: String = normalizeEmail(email)

	init {
		if (!EMAIL.matches(normalizedEmail) || normalizedEmail.length > 320) throw DomainException(AccountError.INVALID_EMAIL)
		if (role == AccountRole.ADMIN && passwordHash != null) throw DomainException(AccountError.ADMIN_PUBLIC_REGISTRATION)
	}

	companion object {
		private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
		fun normalizeEmail(value: String): String = value.trim().lowercase()

		fun publicClient(id: UUID, email: String, passwordHash: String, now: Instant) = Account(
			id, email.trim(), passwordHash, AccountRole.CLIENT, true, now, now,
		)
	}
}
