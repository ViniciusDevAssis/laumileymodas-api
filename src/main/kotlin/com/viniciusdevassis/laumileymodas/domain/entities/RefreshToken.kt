package com.viniciusdevassis.laumileymodas.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_token")
class RefreshToken(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	val account: Account,

	@Column(nullable = false, unique = true, length = 64)
	val tokenHash: String,

	@Column(nullable = false)
	val familyId: UUID,

	@Column(nullable = false)
	val expiresAt: Instant,

	@Column
	var consumedAt: Instant? = null,

	@Column
	var revokedAt: Instant? = null,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),
) {
	init {
		require(tokenHash.matches(HEX_64)) { "Hash do refresh token deve possuir 64 caracteres hexadecimais." }
	}

	fun isActive(now: Instant): Boolean = consumedAt == null && revokedAt == null && expiresAt.isAfter(now)

	companion object {
		private val HEX_64 = Regex("^[0-9a-f]{64}$")
	}
}
