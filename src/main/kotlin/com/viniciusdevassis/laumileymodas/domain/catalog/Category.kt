package com.viniciusdevassis.laumileymodas.domain.catalog

import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import java.text.Normalizer
import java.time.Instant
import java.util.Locale
import java.util.UUID

data class Category(
	val id: UUID,
	val name: String,
	val createdAt: Instant,
	val updatedAt: Instant,
) {
	init {
		if (name.isBlank() || name.length > 120 || name != name.trim()) {
			throw DomainException(CatalogError.INVALID_CATEGORY_NAME)
		}
	}

	val normalizedName: String
		get() = Normalizer.normalize(name, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
}
