package com.viniciusdevassis.laumileymodas.domain.customer

import com.viniciusdevassis.laumileymodas.domain.account.AccountError
import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import java.time.Instant
import java.util.UUID

data class Customer(
	val id: UUID,
	val accountId: UUID,
	val firstName: String,
	val lastName: String,
	val whatsappPhone: String,
	val proactiveContactAuthorized: Boolean = false,
	val contactConsentChangedAt: Instant? = null,
	val createdAt: Instant,
	val updatedAt: Instant,
) {
	init {
		if (firstName.isBlank() || firstName.length > 100 || lastName.isBlank() || lastName.length > 100) {
			throw DomainException(AccountError.INVALID_NAME)
		}
		if (!PHONE.matches(whatsappPhone)) throw DomainException(AccountError.INVALID_PHONE)
	}

	companion object {
		private val PHONE = Regex("^\\+[1-9][0-9]{7,14}$")
		fun normalizePhone(value: String): String {
			val digits = value.filter(Char::isDigit)
			val normalized = "+$digits"
			if (!PHONE.matches(normalized)) throw DomainException(AccountError.INVALID_PHONE)
			return normalized
		}
	}
}
