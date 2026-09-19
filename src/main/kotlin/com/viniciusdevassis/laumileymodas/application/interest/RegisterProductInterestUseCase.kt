package com.viniciusdevassis.laumileymodas.application.interest

import com.viniciusdevassis.laumileymodas.application.port.*
import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogQueryRepository
import com.viniciusdevassis.laumileymodas.application.port.crm.FollowUpRepository
import com.viniciusdevassis.laumileymodas.application.port.customer.CustomerRepository
import com.viniciusdevassis.laumileymodas.application.port.interest.InterestRepository
import com.viniciusdevassis.laumileymodas.application.port.whatsapp.WhatsappLinkGenerator
import com.viniciusdevassis.laumileymodas.domain.catalog.CatalogError
import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import com.viniciusdevassis.laumileymodas.domain.crm.*
import com.viniciusdevassis.laumileymodas.domain.interest.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

data class InterestResult(val interest: Interest, val productName: String, val whatsappUrl: String, val created: Boolean)

@Service
class RegisterProductInterestUseCase(private val catalog: CatalogQueryRepository, private val customers: CustomerRepository, private val interests: InterestRepository, private val followUps: FollowUpRepository, private val links: WhatsappLinkGenerator, private val ids: IdGenerator, private val clock: ClockProvider) {
	@Transactional fun execute(accountId: UUID, productId: UUID, idempotencyKey: String): InterestResult {
		val product = catalog.findActiveById(productId) ?: throw DomainException(CatalogError.PRODUCT_NOT_FOUND)
		val customer = customers.findByAccountId(accountId) ?: throw DomainException(InterestError.NOT_FOUND)
		interests.lockIdempotency(customer.id, idempotencyKey)
		val existing = interests.findByCustomerAndKey(customer.id, idempotencyKey)
		if (existing != null) {
			if (existing.productId != productId) throw DomainException(InterestError.IDEMPOTENCY_CONFLICT)
			return InterestResult(existing, product.name, links.generate(product.id, product.name), false)
		}
		val now = clock.now(); val interest = Interest(ids.newId(), customer.id, product.id, idempotencyKey, now)
		val reminder = Reminder.pending(ids.newId(), customer.id, interest.id, product.name, now)
		val contact = ContactRecord.pending(ids.newId(), customer.id, interest.id, reminder.id, now)
		followUps.save(interest, reminder, contact)
		return InterestResult(interest, product.name, links.generate(product.id, product.name), true)
	}
}
