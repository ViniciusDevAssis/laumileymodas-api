package com.viniciusdevassis.laumileymodas.application

import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ProductRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CatalogService(
	private val productRepository: ProductRepository,
) {
	@Transactional(readOnly = true)
	fun listActiveProducts(categoryId: UUID?, pageable: Pageable): Page<Product> {
		val products = if (categoryId == null) {
			productRepository.findByStatus(ProductStatus.ACTIVE, pageable)
		} else {
			productRepository.findByStatusAndCategoryId(ProductStatus.ACTIVE, categoryId, pageable)
		}
		products.content.forEach { it.orderedImages() }
		return products
	}

	@Transactional(readOnly = true)
	fun getActiveProduct(productId: UUID): Product =
		productRepository.findByIdAndStatus(productId, ProductStatus.ACTIVE)
			?: throw ApiException(ApiError.CATALOG_001, HttpStatus.NOT_FOUND)
}
