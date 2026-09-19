package com.viniciusdevassis.laumileymodas.infrastructure.persistence.catalog

import com.viniciusdevassis.laumileymodas.domain.catalog.Category
import com.viniciusdevassis.laumileymodas.domain.catalog.Product
import com.viniciusdevassis.laumileymodas.domain.catalog.ProductImage
import com.viniciusdevassis.laumileymodas.domain.catalog.ProductStatus
import org.springframework.stereotype.Component
import java.net.URI

@Component
class CatalogPersistenceMapper {

	fun toDomain(entity: ProductJpaEntity): Product = Product(
		id = entity.id,
		category = entity.category?.toDomain()
			?: error("Produto ${entity.id} sem categoria persistida"),
		name = entity.name,
		description = entity.description,
		status = ProductStatus.valueOf(entity.status),
		images = entity.images.map { it.toDomain() },
		createdAt = entity.createdAt,
		updatedAt = entity.updatedAt,
	)

	fun toEntity(product: Product): ProductJpaEntity {
		val entity = ProductJpaEntity(
			id = product.id,
			category = product.category.toEntity(),
			name = product.name,
			description = product.description,
			status = product.status.name,
			createdAt = product.createdAt,
			updatedAt = product.updatedAt,
		)
		entity.images = product.images.map { it.toEntity(entity) }.toMutableList()
		return entity
	}

	private fun CategoryJpaEntity.toDomain(): Category = Category(id, name, createdAt, updatedAt)

	private fun Category.toEntity(): CategoryJpaEntity = CategoryJpaEntity(
		id = id,
		name = name,
		normalizedName = normalizedName,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)

	private fun ProductImageJpaEntity.toDomain(): ProductImage = ProductImage(
		id = id,
		externalId = externalId,
		url = URI(secureUrl),
		primary = primary,
		displayOrder = displayOrder,
		createdAt = createdAt,
	)

	private fun ProductImage.toEntity(product: ProductJpaEntity): ProductImageJpaEntity = ProductImageJpaEntity(
		id = id,
		product = product,
		externalId = externalId,
		secureUrl = url.toASCIIString(),
		primary = primary,
		displayOrder = displayOrder,
		createdAt = createdAt,
	)
}
