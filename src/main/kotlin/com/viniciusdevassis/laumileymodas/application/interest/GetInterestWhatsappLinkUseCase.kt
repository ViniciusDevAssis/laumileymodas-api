package com.viniciusdevassis.laumileymodas.application.interest

import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogQueryRepository
import com.viniciusdevassis.laumileymodas.application.port.customer.CustomerRepository
import com.viniciusdevassis.laumileymodas.application.port.interest.InterestRepository
import com.viniciusdevassis.laumileymodas.application.port.whatsapp.WhatsappLinkGenerator
import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import com.viniciusdevassis.laumileymodas.domain.interest.InterestError
import org.springframework.stereotype.Service
import java.util.UUID

@Service class GetInterestWhatsappLinkUseCase(private val customers: CustomerRepository, private val interests: InterestRepository, private val catalog: CatalogQueryRepository, private val links: WhatsappLinkGenerator) {
	fun execute(accountId: UUID, interestId: UUID): String {
		val customer = customers.findByAccountId(accountId) ?: throw DomainException(InterestError.NOT_FOUND)
		val interest = interests.findOwned(interestId, customer.id) ?: throw DomainException(InterestError.NOT_FOUND)
		val product = catalog.findActiveById(interest.productId) ?: throw DomainException(InterestError.NOT_FOUND)
		return links.generate(product.id, product.name)
	}
}
