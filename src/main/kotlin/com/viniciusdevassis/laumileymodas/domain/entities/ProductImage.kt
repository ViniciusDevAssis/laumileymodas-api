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
@Table(name = "product_image")
class ProductImage(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@Column(nullable = false, length = 2048)
	val url: String,

	@Column(nullable = false, unique = true, length = 255)
	val externalId: String,

	@Column(name = "is_primary", nullable = false)
	var primary: Boolean,

	@Column(nullable = false)
	var displayOrder: Int,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),
) {
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	lateinit var product: Product

	init {
		require(url.isNotBlank()) { "Imagem deve possuir URL." }
		require(externalId.isNotBlank()) { "Imagem deve possuir identificador externo." }
		require(displayOrder >= 0) { "Ordem da imagem deve ser maior ou igual a zero." }
	}
}
