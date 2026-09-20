package com.viniciusdevassis.laumileymodas.domain.entities

import com.viniciusdevassis.laumileymodas.domain.enums.Role
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "account")
class Account(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	email: String,

	@Column(length = 255)
	var passwordHash: String? = null,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	val role: Role,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),

	@Column(nullable = false)
	var updatedAt: Instant = createdAt,
) {
	@Column(nullable = false, unique = true, length = 320)
	var email: String = normalizeEmail(email)
		set(value) {
			field = normalizeEmail(value)
		}

	init {
		require(this.email.isNotBlank()) { "Conta deve possuir e-mail." }
	}

	companion object {
		fun normalizeEmail(email: String): String = email.trim().lowercase()
	}
}
