package com.viniciusdevassis.laumileymodas.infrastructure.persistence.catalog

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product")
open class ProductJpaEntity(
	@Id
	@Column(nullable = false)
	open var id: UUID = UUID(0, 0),

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	open var category: CategoryJpaEntity? = null,

	@Column(nullable = false, length = 160)
	open var name: String = "",

	@Column(nullable = false, columnDefinition = "text")
	open var description: String = "",

	@Column(nullable = false, length = 20)
	open var status: String = "INACTIVE",

	@Column(name = "created_at", nullable = false)
	open var createdAt: Instant = Instant.EPOCH,

	@Column(name = "updated_at", nullable = false)
	open var updatedAt: Instant = Instant.EPOCH,

	@OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
	@OrderBy("displayOrder ASC")
	open var images: MutableList<ProductImageJpaEntity> = mutableListOf(),
)
