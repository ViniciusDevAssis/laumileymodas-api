package com.viniciusdevassis.laumileymodas.application.catalog

import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogProductPage
import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogQueryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ListPublicCatalogUseCase(
	private val catalogQueryRepository: CatalogQueryRepository,
) {

	@Transactional(readOnly = true)
	operator fun invoke(page: Int, size: Int): CatalogProductPage =
		catalogQueryRepository.findActive(page, size)
}
