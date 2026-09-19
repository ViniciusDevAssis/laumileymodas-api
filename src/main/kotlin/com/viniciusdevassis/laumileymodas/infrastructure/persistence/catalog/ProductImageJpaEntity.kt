package com.viniciusdevassis.laumileymodas.infrastructure.persistence.catalog

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_image")
open class ProductImageJpaEntity(
	@Id
	@Column(nullable = false)
	open var id: UUID = UUID(0, 0),

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	open var product: ProductJpaEntity? = null,

	@Column(name = "external_id", nullable = false, length = 255, unique = true)
	open var externalId: String = "",

	@Column(name = "secure_url", nullable = false, length = 2048)
	open var secureUrl: String = "",

	@Column(name = "is_primary", nullable = false)
	open var primary: Boolean = false,

	@Column(name = "display_order", nullable = false)
	open var displayOrder: Int = 0,

	@Column(name = "created_at", nullable = false)
	open var createdAt: Instant = Instant.EPOCH,
)
