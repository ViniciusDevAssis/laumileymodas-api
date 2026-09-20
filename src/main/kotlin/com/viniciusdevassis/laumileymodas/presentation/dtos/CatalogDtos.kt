package com.viniciusdevassis.laumileymodas.presentation.dtos

import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import org.springframework.hateoas.server.core.Relation
import java.util.UUID

data class CategorySummary(
	val id: UUID,
	val name: String,
)

@Relation(collectionRelation = "images", itemRelation = "image")
data class ProductImageRepresentation(
	val id: UUID,
	val url: String,
	val primary: Boolean,
	val displayOrder: Int,
)

@Relation(collectionRelation = "products", itemRelation = "product")
data class ProductRepresentation(
	val id: UUID,
	val name: String,
	val description: String,
	val category: CategorySummary,
	val status: ProductStatus,
	val images: List<ProductImageRepresentation>,
)
