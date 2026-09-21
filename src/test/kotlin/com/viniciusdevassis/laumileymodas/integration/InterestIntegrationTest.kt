package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.application.InterestService
import com.viniciusdevassis.laumileymodas.domain.entities.Category
import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.entities.ProductImage
import com.viniciusdevassis.laumileymodas.domain.enums.FollowUpStatus
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CategoryRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ContactRecordRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.InterestRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ProductRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ReminderRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.RefreshTokenRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@AutoConfigureMockMvc
class InterestIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val authService: AuthService,
	@Autowired private val interestService: InterestService,
	@Autowired private val accountRepository: AccountRepository,
	@Autowired private val customerRepository: CustomerRepository,
	@Autowired private val refreshTokenRepository: RefreshTokenRepository,
	@Autowired private val categoryRepository: CategoryRepository,
	@Autowired private val productRepository: ProductRepository,
	@Autowired private val interestRepository: InterestRepository,
	@Autowired private val reminderRepository: ReminderRepository,
	@Autowired private val contactRecordRepository: ContactRecordRepository,
) : PostgresIntegrationTest() {
	@BeforeEach
	fun cleanDatabase() {
		contactRecordRepository.deleteAll()
		reminderRepository.deleteAll()
		interestRepository.deleteAll()
		refreshTokenRepository.deleteAll()
		productRepository.deleteAll()
		categoryRepository.deleteAll()
		customerRepository.deleteAll()
		accountRepository.deleteAll()
	}

	@Test
	fun `interesse cria um acompanhamento unico e repeticao retorna o existente`() {
		val customer = authService.registerCustomer("Ana", "Silva", "interest@example.com", "senha-forte-123", "+5511999999999")
		val token = authService.login("interest@example.com", "senha-forte-123", "127.0.0.1").accessToken
		val product = activeProduct("Vestido azul")
		val key = UUID.randomUUID().toString()

		mockMvc.post("/products/${product.id}/interests") {
			header("Authorization", "Bearer $token")
			header("Idempotency-Key", key)
			accept = MediaType.parseMediaType("application/hal+json")
		}.andExpect {
			status { isCreated() }
			content { contentTypeCompatibleWith("application/hal+json") }
			jsonPath("$.customerId") { value(customer.id.toString()) }
			jsonPath("$.product.id") { value(product.id.toString()) }
			jsonPath("$.whatsappUrl") { value(org.hamcrest.Matchers.containsString("https://wa.me/5511999999999?text=")) }
			jsonPath("$._links.whatsapp.href") { value(org.hamcrest.Matchers.containsString("https://wa.me/")) }
		}

		mockMvc.post("/products/${product.id}/interests") {
			header("Authorization", "Bearer $token")
			header("Idempotency-Key", key)
		}.andExpect {
			status { isOk() }
		}
		assertEquals(1L, interestRepository.count())
		assertEquals(1L, reminderRepository.count())
		assertEquals(1L, contactRecordRepository.count())
		assertEquals(FollowUpStatus.PENDING, reminderRepository.findAll().single().status)
		assertEquals(FollowUpStatus.PENDING, contactRecordRepository.findAll().single().status)
		assertEquals("WHATSAPP", contactRecordRepository.findAll().single().channel)
	}

	@Test
	fun `produto inativo e reutilizacao da chave para outro produto nao criam acompanhamento`() {
		val customer = authService.registerCustomer("Ana", "Silva", "interest-errors@example.com", "senha-forte-123", "+5511999999999")
		val token = authService.login("interest-errors@example.com", "senha-forte-123", "127.0.0.1").accessToken
		val active = activeProduct("Saia preta")
		val inactive = productRepository.save(product("Saia reservada", ProductStatus.INACTIVE, active.category))
		val key = UUID.randomUUID().toString()

		mockMvc.post("/products/${inactive.id}/interests") {
			header("Authorization", "Bearer $token")
			header("Idempotency-Key", UUID.randomUUID().toString())
		}.andExpect { status { isNotFound() } }

		mockMvc.post("/products/${active.id}/interests") {
			header("Authorization", "Bearer $token")
			header("Idempotency-Key", key)
		}.andExpect { status { isCreated() } }

		val other = activeProduct("Blusa branca")
		mockMvc.post("/products/${other.id}/interests") {
			header("Authorization", "Bearer $token")
			header("Idempotency-Key", key)
		}.andExpect {
			status { isConflict() }
			jsonPath("$.code") { value(ApiError.INTEREST_002.code) }
		}
		assertTrue(interestRepository.count() == 1L)
		assertEquals(1L, reminderRepository.count())
		assertEquals(1L, contactRecordRepository.count())
	}

	@Test
	fun `confirmacoes concorrentes com a mesma chave criam um unico conjunto`() {
		val customer = authService.registerCustomer("Ana", "Silva", "parallel-interest@example.com", "senha-forte-123", "+5511999999999")
		val product = activeProduct("Vestido amarelo")
		val ready = CountDownLatch(2)
		val start = CountDownLatch(1)
		val executor = Executors.newFixedThreadPool(2)
		try {
			val results = (1..2).map {
				executor.submit<InterestService.InterestResult> {
					ready.countDown()
					check(start.await(5, TimeUnit.SECONDS))
					interestService.confirm(requireNotNull(customer.account.id), requireNotNull(product.id), "same-concurrent-key")
				}
			}
			assertTrue(ready.await(5, TimeUnit.SECONDS))
			start.countDown()
			val outcomes = results.map { it.get(10, TimeUnit.SECONDS) }
			assertEquals(1, outcomes.count { it.created })
		} finally {
			executor.shutdownNow()
		}
		assertEquals(1L, interestRepository.count())
		assertEquals(1L, reminderRepository.count())
		assertEquals(1L, contactRecordRepository.count())
	}

	private fun activeProduct(name: String): Product {
		val category = categoryRepository.findAll().firstOrNull() ?: categoryRepository.save(Category(name = "Vestidos"))
		return productRepository.save(product(name, ProductStatus.ACTIVE, category))
	}

	private fun product(name: String, status: ProductStatus, category: Category) =
		Product(category = category, name = name, description = "Descrição da peça", status = status).also {
			it.addImage(ProductImage(url = "https://example.com/${name.hashCode()}.jpg", externalId = "media-${UUID.randomUUID()}", primary = true, displayOrder = 0))
		}
}
