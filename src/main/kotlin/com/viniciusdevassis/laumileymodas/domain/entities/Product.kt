package com.viniciusdevassis.laumileymodas.domain.entities

import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import jakarta.persistence.CascadeType
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
import jakarta.persistence.OneToMany
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product")
class Product(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "category_id", nullable = false)
	var category: Category,

	@Column(nullable = false, length = 200)
	var name: String,

	@Column(nullable = false, length = 2000)
	var description: String,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	var status: ProductStatus = ProductStatus.INACTIVE,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),

	@Column(nullable = false)
	var updatedAt: Instant = createdAt,
) {
	@OneToMany(mappedBy = "product", cascade = [CascadeType.ALL], orphanRemoval = true)
	private val imageList: MutableList<ProductImage> = mutableListOf()

	val images: List<ProductImage>
		get() = imageList.toList()

	init {
		require(name.isNotBlank()) { "Produto deve possuir nome." }
		require(description.isNotBlank()) { "Produto deve possuir descrição." }
	}

	fun addImage(image: ProductImage) {
		image.product = this
		imageList.add(image)
	}

	fun orderedImages(): List<ProductImage> =
		images.sortedWith(compareByDescending<ProductImage> { it.primary }.thenBy { it.displayOrder })

	fun isActive(): Boolean = status == ProductStatus.ACTIVE

	@PrePersist
	@PreUpdate
	fun validateForPersistence() {
		require(name.isNotBlank()) { "Produto deve possuir nome." }
		require(description.isNotBlank()) { "Produto deve possuir descrição." }

		if (status == ProductStatus.ACTIVE) {
			require(imageList.isNotEmpty()) { "Produto ativo deve possuir ao menos uma imagem." }
			require(imageList.count { it.primary } == 1) { "Produto ativo deve possuir exatamente uma imagem principal." }
		}
	}
}
