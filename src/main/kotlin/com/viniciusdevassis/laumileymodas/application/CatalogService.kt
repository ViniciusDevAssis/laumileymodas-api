package com.viniciusdevassis.laumileymodas.application

import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ProductRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CategoryRepository
import com.viniciusdevassis.laumileymodas.domain.entities.Category
import java.time.Clock
import java.time.Instant
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CatalogService(
	private val productRepository: ProductRepository,
	private val categoryRepository: CategoryRepository,
	private val clock: Clock,
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

	@Transactional(readOnly = true)
	fun listAdminProducts(status: ProductStatus?, pageable: Pageable): Page<Product> {
		val products = if (status == null) productRepository.findAllWithDetails(pageable) else productRepository.findAllByStatus(status, pageable)
		products.content.forEach { it.orderedImages() }
		return products
	}

	@Transactional(readOnly = true)
	fun getAdminProduct(productId: UUID): Product = productRepository.findWithDetailsById(productId)
		?: throw ApiException(ApiError.CATALOG_001, HttpStatus.NOT_FOUND)

	@Transactional(readOnly = true)
	fun listCategories(): List<Category> = categoryRepository.findAll().sortedBy { it.name.lowercase() }

	@Transactional
	fun createCategory(name: String): Category {
		val normalized = Category.normalize(name)
		if (normalized.isBlank()) invalid()
		if (categoryRepository.findByNormalizedName(normalized) != null) conflictCategory()
		return categoryRepository.save(Category(name = name.trim(), normalizedName = normalized, createdAt = now(), updatedAt = now()))
	}

	@Transactional
	fun replaceCategory(categoryId: UUID, name: String): Category {
		val category = categoryRepository.findById(categoryId).orElseThrow { ApiException(ApiError.CATALOG_002, HttpStatus.NOT_FOUND) }
		val normalized = Category.normalize(name)
		if (normalized.isBlank()) invalid()
		val duplicate = categoryRepository.findByNormalizedName(normalized)
		if (duplicate != null && duplicate.id != categoryId) conflictCategory()
		category.rename(name.trim(), now())
		category.normalizedName = normalized
		return category
	}

	@Transactional
	fun createProduct(name: String, description: String, categoryId: UUID): Product {
		val category = categoryRepository.findById(categoryId).orElseThrow { ApiException(ApiError.CATALOG_002, HttpStatus.NOT_FOUND) }
		if (name.isBlank() || description.isBlank()) invalid()
		return productRepository.save(Product(category = category, name = name.trim(), description = description.trim(), status = ProductStatus.INACTIVE, createdAt = now(), updatedAt = now()))
	}

	@Transactional
	fun replaceProduct(productId: UUID, name: String, description: String, categoryId: UUID): Product {
		val product = productRepository.findWithDetailsByIdForUpdate(productId) ?: productNotFound()
		val category = categoryRepository.findById(categoryId).orElseThrow { ApiException(ApiError.CATALOG_002, HttpStatus.NOT_FOUND) }
		product.replaceDetails(name, description, category, now())
		return product
	}

	@Transactional
	fun changeProductStatus(productId: UUID, status: ProductStatus): Product {
		val product = productRepository.findWithDetailsByIdForUpdate(productId) ?: productNotFound()
		product.status = status
		product.updatedAt = now()
		try {
			product.validateForPersistence()
		} catch (exception: IllegalArgumentException) {
			invalid()
		}
		return productRepository.saveAndFlush(product)
	}

	fun saveProduct(product: Product): Product = productRepository.saveAndFlush(product)

	fun findProductForUpdate(productId: UUID): Product = productRepository.findWithDetailsByIdForUpdate(productId) ?: productNotFound()

	fun flushProduct(product: Product): Product = productRepository.saveAndFlush(product)

	private fun productNotFound(): Nothing = throw ApiException(ApiError.CATALOG_001, HttpStatus.NOT_FOUND)
	private fun conflictCategory(): Nothing = throw ApiException(ApiError.CATALOG_003, HttpStatus.CONFLICT)
	private fun invalid(): Nothing = throw ApiException(ApiError.CATALOG_004, HttpStatus.UNPROCESSABLE_ENTITY)
	private fun now(): Instant = Instant.now(clock)
}
