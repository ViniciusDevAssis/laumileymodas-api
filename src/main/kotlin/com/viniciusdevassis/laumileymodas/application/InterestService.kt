package com.viniciusdevassis.laumileymodas.application

import com.viniciusdevassis.laumileymodas.domain.entities.ContactRecord
import com.viniciusdevassis.laumileymodas.domain.entities.Interest
import com.viniciusdevassis.laumileymodas.domain.entities.Reminder
import com.viniciusdevassis.laumileymodas.domain.enums.ContactChannel
import com.viniciusdevassis.laumileymodas.domain.enums.FollowUpStatus
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ContactRecordRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.InterestRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ProductRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ReminderRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class InterestService(
	private val customerRepository: CustomerRepository,
	private val productRepository: ProductRepository,
	private val interestRepository: InterestRepository,
	private val reminderRepository: ReminderRepository,
	private val contactRecordRepository: ContactRecordRepository,
	private val clock: Clock,
	@Value("\${laumiley.whatsapp.number}") private val whatsappNumber: String,
) {
	init {
		require(whatsappNumber.matches(Regex("^[1-9][0-9]{7,14}$"))) {
			"O número WhatsApp deve estar em formato E.164 sem o sinal de mais."
		}
	}

	@Transactional
	fun confirm(accountId: UUID, productId: UUID, idempotencyKey: String): InterestResult {
		if (idempotencyKey.isBlank() || idempotencyKey.length > 100) {
			throw ApiException(ApiError.VALIDATION_001, HttpStatus.BAD_REQUEST)
		}
		val customer = customerRepository.findByAccountIdForInterest(accountId)
			?: throw ApiException(ApiError.CUSTOMER_001, HttpStatus.NOT_FOUND)
		val product = productRepository.findByIdAndStatusForInterest(productId, ProductStatus.ACTIVE)
			?: throw ApiException(ApiError.INTEREST_001, HttpStatus.NOT_FOUND)

		val existing = interestRepository.findByCustomerIdAndIdempotencyKey(requireNotNull(customer.id), idempotencyKey)
		if (existing != null) {
			if (existing.product.id != productId) {
				throw ApiException(ApiError.INTEREST_002, HttpStatus.CONFLICT)
			}
			return InterestResult(existing, whatsappUrl(existing.product), created = false)
		}

		val now = Instant.now(clock)
		val interest = interestRepository.save(
			Interest(customer = customer, product = product, idempotencyKey = idempotencyKey, createdAt = now),
		)
		val productLabel = "${product.name} (${product.id})"
		reminderRepository.save(
			Reminder(
				interest = interest,
				description = "Completar atendimento via WhatsApp: cliente ${customer.id}, produto $productLabel.",
				dueAt = now,
				status = FollowUpStatus.PENDING,
				createdAt = now,
				updatedAt = now,
			),
		)
		contactRecordRepository.save(ContactRecord.pendingWhatsApp(customer, interest, now))
		return InterestResult(interest, whatsappUrl(product), created = true)
	}

	private fun whatsappUrl(product: com.viniciusdevassis.laumileymodas.domain.entities.Product): String {
		val text = "Olá! Tenho interesse no produto ${product.name} (${product.id})."
		val encoded = URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20")
		return "https://wa.me/$whatsappNumber?text=$encoded"
	}

	data class InterestResult(
		val interest: Interest,
		val whatsappUrl: String,
		val created: Boolean,
	)
}
