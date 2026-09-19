package com.viniciusdevassis.laumileymodas.domain.catalog

import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import java.net.URI
import java.time.Instant
import java.util.UUID

data class ProductImage(
	val id: UUID,
	val externalId: String,
	val url: URI,
	val primary: Boolean,
	val displayOrder: Int,
	val createdAt: Instant,
) {
	init {
		if (
			externalId.isBlank() || externalId.length > 255 ||
			url.scheme?.lowercase() != "https" || url.host.isNullOrBlank() ||
			displayOrder < 0
		) {
			throw DomainException(CatalogError.INVALID_PRODUCT_IMAGE)
		}
	}
}
