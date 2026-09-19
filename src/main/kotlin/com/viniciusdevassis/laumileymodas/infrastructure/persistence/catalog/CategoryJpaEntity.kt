package com.viniciusdevassis.laumileymodas.infrastructure.persistence.catalog

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "category")
open class CategoryJpaEntity(
	@Id
	@Column(nullable = false)
	open var id: UUID = UUID(0, 0),

	@Column(nullable = false, length = 120)
	open var name: String = "",

	@Column(name = "normalized_name", nullable = false, length = 120, unique = true)
	open var normalizedName: String = "",

	@Column(name = "created_at", nullable = false)
	open var createdAt: Instant = Instant.EPOCH,

	@Column(name = "updated_at", nullable = false)
	open var updatedAt: Instant = Instant.EPOCH,
)
