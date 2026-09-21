package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.RefreshTokenRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@AutoConfigureMockMvc
class LocalAuthenticationIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val authService: AuthService,
	@Autowired private val accountRepository: AccountRepository,
	@Autowired private val customerRepository: CustomerRepository,
	@Autowired private val refreshTokenRepository: RefreshTokenRepository,
) : PostgresIntegrationTest() {
	@BeforeEach
	fun cleanDatabase() {
		refreshTokenRepository.deleteAll()
		customerRepository.deleteAll()
		accountRepository.deleteAll()
	}

	@Test
	fun `cadastro publico sempre cria CLIENT e salva senha BCrypt custo doze`() {
		mockMvc.post("/customers") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"firstName":"Ana","lastName":"Silva","email":"ANA@EXAMPLE.COM","password":"senha-forte-123","whatsappPhone":"+5511999999999"}"""
		}.andExpect {
			status { isCreated() }
			content { contentTypeCompatibleWith("application/hal+json") }
			jsonPath("$.email") { value("ana@example.com") }
		}

		val account = accountRepository.findByEmail("ana@example.com")
		assertEquals(Role.CLIENT, account?.role)
		assertTrue(requireNotNull(account?.passwordHash).startsWith("\$2a\$12\$"))
		assertEquals(1L, customerRepository.count())
		assertEquals(null, accountRepository.findByRole(Role.ADMIN))
	}

	@Test
	fun `campo de papel enviado pelo cliente nunca promove a conta`() {
		mockMvc.post("/customers") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"firstName":"Ana","lastName":"Silva","email":"ana-role@example.com","password":"senha-forte-123","whatsappPhone":"+5511999999999","role":"ADMIN"}"""
		}.andExpect { status { isCreated() } }
		assertEquals(Role.CLIENT, accountRepository.findByEmail("ana-role@example.com")?.role)
		assertEquals(null, accountRepository.findByRole(Role.ADMIN))
	}

	@Test
	fun `login valido emite access JWT e refresh somente em cookie HttpOnly`() {
		register()
		mockMvc.post("/auth/login") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"ana@example.com","password":"senha-forte-123"}"""
		}.andExpect {
			status { isOk() }
			jsonPath("$.accessToken") { exists() }
			jsonPath("$.tokenType") { value("Bearer") }
			jsonPath("$.role") { value("CLIENT") }
		}.andReturn().response.let { response ->
			assertTrue(requireNotNull(response.getHeader("Set-Cookie")).contains("LAUMILEY_REFRESH="))
			assertTrue(requireNotNull(response.getHeader("Set-Cookie")).contains("HttpOnly"))
		}
	}

	@Test
	fun `login invalido retorna 401 no contrato proprio`() {
		register()
		mockMvc.post("/auth/login") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"ana@example.com","password":"senha-errada"}"""
		}.andExpect {
			status { isUnauthorized() }
			jsonPath("$.code") { value(ApiError.AUTH_002.code) }
			jsonPath("$.message") { value(ApiError.AUTH_002.message) }
		}
	}

	private fun register() {
		authService.registerCustomer("Ana", "Silva", "ana@example.com", "senha-forte-123", "+5511999999999")
	}
}
