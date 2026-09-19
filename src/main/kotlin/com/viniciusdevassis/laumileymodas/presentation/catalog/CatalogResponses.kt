package com.viniciusdevassis.laumileymodas.presentation.catalog

import java.net.URI
import java.util.UUID

data class CategoryResponse(
	val id: UUID,
	val name: String,
)

data class ProductImageResponse(
	val id: UUID,
	val url: URI,
	val primary: Boolean,
	val displayOrder: Int,
)

data class ProductResponse(
	val id: UUID,
	val name: String,
	val description: String,
	val category: CategoryResponse,
	val mainImage: ProductImageResponse,
	val images: List<ProductImageResponse>,
)

data class ProductPageResponse(
	val items: List<ProductResponse>,
	val page: Int,
	val size: Int,
	val totalElements: Long,
	val totalPages: Int,
)
