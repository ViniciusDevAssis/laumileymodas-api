package com.viniciusdevassis.laumileymodas.domain.catalog

import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import java.time.Instant
import java.util.UUID

class Product(
	val id: UUID,
	val category: Category,
	val name: String,
	val description: String,
	status: ProductStatus,
	images: List<ProductImage>,
	val createdAt: Instant,
	val updatedAt: Instant,
) {
	var status: ProductStatus = status
		private set

	var images: List<ProductImage> = validateImages(images, status)
		private set

	init {
		if (name.isBlank() || name.length > 160 || name != name.trim()) {
			throw DomainException(CatalogError.INVALID_PRODUCT_NAME)
		}
		if (description.isBlank() || description.length > 5_000 || description != description.trim()) {
			throw DomainException(CatalogError.INVALID_PRODUCT_DESCRIPTION)
		}
	}

	val mainImage: ProductImage
		get() = images.single(ProductImage::primary)

	fun activate() {
		validateImages(images, ProductStatus.ACTIVE)
		status = ProductStatus.ACTIVE
	}

	fun deactivate() {
		status = ProductStatus.INACTIVE
	}

	fun replaceImages(newImages: List<ProductImage>) {
		images = validateImages(newImages, status)
	}

	private fun validateImages(
		images: List<ProductImage>,
		status: ProductStatus,
	): List<ProductImage> {
		if (status == ProductStatus.ACTIVE && images.isEmpty()) {
			throw DomainException(CatalogError.PRODUCT_REQUIRES_IMAGE)
		}
		if (images.isNotEmpty() && images.count(ProductImage::primary) != 1) {
			throw DomainException(CatalogError.PRODUCT_REQUIRES_ONE_PRIMARY)
		}
		if (images.map(ProductImage::displayOrder).distinct().size != images.size) {
			throw DomainException(CatalogError.DUPLICATE_IMAGE_ORDER)
		}
		return images.sortedBy(ProductImage::displayOrder)
	}
}
