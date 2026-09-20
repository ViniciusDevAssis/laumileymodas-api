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
@Table(name = "interest")
class Interest(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	val customer: Customer,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	val product: Product,

	@Column(nullable = false, length = 100)
	val idempotencyKey: String,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),
) {
	init {
		require(idempotencyKey.isNotBlank()) { "Interesse deve possuir chave de idempotência." }
		require(idempotencyKey.length <= 100) { "Chave de idempotência deve possuir no máximo 100 caracteres." }
	}
}
