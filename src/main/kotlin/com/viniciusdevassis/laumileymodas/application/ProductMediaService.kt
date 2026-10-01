package com.viniciusdevassis.laumileymodas.application

import com.viniciusdevassis.laumileymodas.domain.entities.ProductImage
import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class ProductMediaService(
	private val catalogService: CatalogService,
	private val mediaStorage: MediaStorage,
	private val clock: Clock,
) {
	@Transactional
	fun createProduct(
		name: String,
		description: String,
		categoryId: UUID,
		status: ProductStatus,
		primaryImageIndex: Int,
		files: List<Pair<ByteArray, String>>,
	): Product {
		if (files.isEmpty() || primaryImageIndex !in files.indices) invalidMedia()
		val assets = uploadAll(files)
		try {
			val product = catalogService.createProduct(name, description, categoryId)
			assets.forEachIndexed { index, asset ->
				product.addImage(ProductImage(url = asset.url, externalId = asset.externalId, primary = index == primaryImageIndex, displayOrder = index, createdAt = now()))
			}
			product.status = status
			product.validateForPersistence()
			return catalogService.flushProduct(product)
		} catch (exception: RuntimeException) {
			compensate(assets.map { it.externalId })
			throw exception
		}
	}

	@Transactional
	fun addImage(productId: UUID, content: ByteArray, contentType: String, primary: Boolean?, displayOrder: Int?): ProductImage {
		if (content.isEmpty() || !contentType.startsWith("image/")) invalidMedia()
		val product = catalogService.findProductForUpdate(productId)
		val order = displayOrder ?: (product.images.maxOfOrNull { it.displayOrder }?.plus(1) ?: 0)
		if (order < 0 || product.images.any { it.displayOrder == order }) invalidMedia()
		val asset = upload(content, contentType)
		try {
			val image = ProductImage(url = asset.url, externalId = asset.externalId, primary = primary == true, displayOrder = order, createdAt = now())
			if (image.primary) {
				val originalStatus = product.status
				if (originalStatus == ProductStatus.ACTIVE) {
					product.status = ProductStatus.INACTIVE
					catalogService.flushProduct(product)
				}
				product.images.firstOrNull { it.primary }?.let {
					it.primary = false
					catalogService.flushProduct(product)
				}
				product.addImage(image)
				product.status = originalStatus
			} else {
				product.addImage(image)
			}
			val savedProduct = catalogService.flushProduct(product)
			return savedProduct.images.single { it.externalId == asset.externalId }
		} catch (exception: RuntimeException) {
			compensate(listOf(asset.externalId))
			throw exception
		}
	}

	@Transactional
	fun updateImage(productId: UUID, imageId: UUID, primary: Boolean?, displayOrder: Int?): ProductImage {
		if (primary == false || primary == null && displayOrder == null || displayOrder != null && displayOrder < 0) invalidMedia()
		val product = catalogService.findProductForUpdate(productId)
		val image = product.images.find { it.id == imageId } ?: throw ApiException(ApiError.CATALOG_006, HttpStatus.NOT_FOUND)
		if (primary == true && !image.primary) {
			val originalStatus = product.status
			if (originalStatus == ProductStatus.ACTIVE) {
				product.status = ProductStatus.INACTIVE
				catalogService.flushProduct(product)
			}
			product.images.firstOrNull { it.primary }?.let {
				it.primary = false
				catalogService.flushProduct(product)
			}
			product.setPrimaryImage(image)
			product.status = originalStatus
		}
		if (displayOrder != null && displayOrder != image.displayOrder) {
			val other = product.images.find { it.displayOrder == displayOrder }
			if (other == null) image.displayOrder = displayOrder else {
				val oldOrder = image.displayOrder
				val temporaryOrder = product.images.maxOf { it.displayOrder } + 1
				image.displayOrder = temporaryOrder
				catalogService.flushProduct(product)
				other.displayOrder = oldOrder
				catalogService.flushProduct(product)
				image.displayOrder = displayOrder
			}
		}
		product.updatedAt = now()
		catalogService.flushProduct(product)
		return image
	}

	@Transactional
	fun deleteImage(productId: UUID, imageId: UUID) {
		val product = catalogService.findProductForUpdate(productId)
		val image = product.images.find { it.id == imageId } ?: throw ApiException(ApiError.CATALOG_006, HttpStatus.NOT_FOUND)
		if (product.images.size == 1) invalidMedia()
		val wasPrimary = image.primary
		try {
			mediaStorage.delete(image.externalId)
		} catch (exception: RuntimeException) {
			throw ApiException(ApiError.CATALOG_005, HttpStatus.UNPROCESSABLE_ENTITY)
		}
		val originalStatus = product.status
		if (wasPrimary) {
			if (originalStatus == ProductStatus.ACTIVE) {
				product.status = ProductStatus.INACTIVE
				catalogService.flushProduct(product)
			}
			image.primary = false
			catalogService.flushProduct(product)
			product.images.first { it.id != imageId }.primary = true
		}
		product.removeImage(image)
		if (originalStatus == ProductStatus.ACTIVE && wasPrimary) {
			product.status = originalStatus
		}
		product.updatedAt = now()
		catalogService.flushProduct(product)
	}

	private fun uploadAll(files: List<Pair<ByteArray, String>>): List<StoredMedia> {
		val assets = mutableListOf<StoredMedia>()
		try {
			files.forEach { (content, type) ->
				if (content.isEmpty() || !type.startsWith("image/")) invalidMedia()
				assets += upload(content, type)
			}
			return assets
		} catch (exception: RuntimeException) {
			compensate(assets.map { it.externalId })
			throw exception
		}
	}

	private fun upload(content: ByteArray, contentType: String): StoredMedia = try {
		mediaStorage.upload(content, contentType)
	} catch (exception: RuntimeException) {
		throw ApiException(ApiError.CATALOG_005, HttpStatus.UNPROCESSABLE_ENTITY)
	}

	private fun compensate(externalIds: List<String>) {
		externalIds.forEach { id ->
			try {
				mediaStorage.delete(id)
			} catch (exception: RuntimeException) {
				logger.warn("Falha ao compensar asset de mídia {}", id)
			}
		}
	}

	private fun invalidMedia(): Nothing = throw ApiException(ApiError.CATALOG_004, HttpStatus.UNPROCESSABLE_ENTITY)
	private fun now(): Instant = Instant.now(clock)

	private companion object {
		val logger = LoggerFactory.getLogger(ProductMediaService::class.java)
	}
}
