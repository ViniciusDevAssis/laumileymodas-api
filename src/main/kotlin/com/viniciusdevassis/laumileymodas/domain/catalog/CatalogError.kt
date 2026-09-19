package com.viniciusdevassis.laumileymodas.domain.catalog

import com.viniciusdevassis.laumileymodas.domain.common.DomainError

enum class CatalogError(
	override val code: String,
	override val message: String,
	override val type: DomainError.Type,
) : DomainError {
	INVALID_CATEGORY_NAME("CATALOG_001", "O nome da categoria é inválido.", DomainError.Type.INVALID),
	INVALID_PRODUCT_NAME("CATALOG_002", "O nome do produto é inválido.", DomainError.Type.INVALID),
	INVALID_PRODUCT_DESCRIPTION("CATALOG_003", "A descrição do produto é inválida.", DomainError.Type.INVALID),
	INVALID_PRODUCT_IMAGE("CATALOG_004", "A imagem do produto é inválida.", DomainError.Type.INVALID),
	PRODUCT_REQUIRES_IMAGE("CATALOG_005", "Um produto ativo deve possuir ao menos uma imagem.", DomainError.Type.INVALID),
	PRODUCT_REQUIRES_ONE_PRIMARY("CATALOG_006", "O produto deve possuir exatamente uma imagem principal.", DomainError.Type.INVALID),
	DUPLICATE_IMAGE_ORDER("CATALOG_007", "As imagens do produto devem possuir ordens distintas.", DomainError.Type.INVALID),
	PRODUCT_NOT_FOUND("CATALOG_008", "Produto não encontrado.", DomainError.Type.NOT_FOUND),
}
