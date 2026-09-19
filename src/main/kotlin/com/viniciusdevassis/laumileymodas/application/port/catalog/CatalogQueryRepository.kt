package com.viniciusdevassis.laumileymodas.application.port.catalog

import com.viniciusdevassis.laumileymodas.domain.catalog.Product
import java.util.UUID

interface CatalogQueryRepository {
	fun findActive(page: Int, size: Int): CatalogProductPage
	fun findActiveById(productId: UUID): Product?
}

data class CatalogProductPage(
	val items: List<Product>,
	val page: Int,
	val size: Int,
	val totalElements: Long,
	val totalPages: Int,
)
