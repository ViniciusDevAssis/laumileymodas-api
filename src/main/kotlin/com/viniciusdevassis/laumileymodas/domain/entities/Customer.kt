package com.viniciusdevassis.laumileymodas.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "customer")
class Customer(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false, unique = true)
	val account: Account,

	@Column(nullable = false, length = 120)
	var firstName: String,

	@Column(nullable = false, length = 120)
	var lastName: String,

	@Column(length = 20)
	var whatsappPhone: String? = null,

	@Column(nullable = false)
	var proactiveContactAuthorized: Boolean = false,

	@Column
	var consentGrantedAt: Instant? = null,

	@Column
	var consentRevokedAt: Instant? = null,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),

	@Column(nullable = false)
	var updatedAt: Instant = createdAt,
) {
	init {
		require(firstName.isNotBlank()) { "Cliente deve possuir nome." }
		require(lastName.isNotBlank()) { "Cliente deve possuir sobrenome." }
		require(whatsappPhone?.isNotBlank() != false) { "WhatsApp não pode ficar em branco." }
	}
}
