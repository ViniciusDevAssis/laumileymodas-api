package com.viniciusdevassis.laumileymodas.presentation.catalog

import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogProductPage
import com.viniciusdevassis.laumileymodas.domain.catalog.Product
import com.viniciusdevassis.laumileymodas.domain.catalog.ProductImage
import org.springframework.stereotype.Component

@Component
class CatalogResponseMapper {

	fun toResponse(page: CatalogProductPage): ProductPageResponse = ProductPageResponse(
		items = page.items.map(::toResponse),
		page = page.page,
		size = page.size,
		totalElements = page.totalElements,
		totalPages = page.totalPages,
	)

	fun toResponse(product: Product): ProductResponse = ProductResponse(
		id = product.id,
		name = product.name,
		description = product.description,
		category = CategoryResponse(product.category.id, product.category.name),
		mainImage = product.mainImage.toResponse(),
		images = product.images.map { it.toResponse() },
	)

	private fun ProductImage.toResponse(): ProductImageResponse = ProductImageResponse(
		id = id,
		url = url,
		primary = primary,
		displayOrder = displayOrder,
	)
}
