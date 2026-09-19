package com.viniciusdevassis.laumileymodas.application.catalog

import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogQueryRepository
import com.viniciusdevassis.laumileymodas.domain.catalog.CatalogError
import com.viniciusdevassis.laumileymodas.domain.catalog.Product
import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class GetPublicProductUseCase(
	private val catalogQueryRepository: CatalogQueryRepository,
) {

	@Transactional(readOnly = true)
	operator fun invoke(productId: UUID): Product =
		catalogQueryRepository.findActiveById(productId)
			?: throw DomainException(CatalogError.PRODUCT_NOT_FOUND)
}
