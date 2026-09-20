package com.viniciusdevassis.laumileymodas.domain.entities

import com.viniciusdevassis.laumileymodas.domain.enums.Provider
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
@Table(name = "account_external_identity")
class AccountExternalIdentity(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	var account: Account,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	val provider: Provider,

	@Column(nullable = false, length = 255)
	val subject: String,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),
) {
	init {
		require(subject.isNotBlank()) { "Identidade externa deve possuir subject." }
	}
}
