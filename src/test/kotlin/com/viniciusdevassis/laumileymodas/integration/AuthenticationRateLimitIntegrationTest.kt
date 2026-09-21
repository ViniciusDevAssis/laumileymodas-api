package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.AuthService
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

@AutoConfigureMockMvc
class AuthenticationRateLimitIntegrationTest(
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
	fun `login valido normal nao e bloqueado e excesso de falhas retorna 429`() {
		authService.registerCustomer("Ana", "Silva", "normal-rate@example.com", "senha-forte-123", "+5511999999999")
		repeat(4) {
			mockMvc.post("/auth/login") {
				contentType = MediaType.APPLICATION_JSON
				content = """{"email":"normal-rate@example.com","password":"wrong-password"}"""
			}.andExpect { status { isUnauthorized() } }
		}
		mockMvc.post("/auth/login") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"normal-rate@example.com","password":"senha-forte-123"}"""
		}.andExpect { status { isOk() } }

		repeat(5) {
			mockMvc.post("/auth/login") {
				contentType = MediaType.APPLICATION_JSON
				content = """{"email":"blocked-rate@example.com","password":"wrong-password"}"""
			}.andExpect { status { isUnauthorized() } }
		}
		mockMvc.post("/auth/login") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"blocked-rate@example.com","password":"wrong-password"}"""
		}.andExpect {
			status { isTooManyRequests() }
			jsonPath("$.code") { value(ApiError.AUTH_004.code) }
			jsonPath("$.status") { value(429) }
		}
		assertEquals(1, accountRepository.findByEmail("normal-rate@example.com")?.let { 1 })
	}

	@Test
	fun `falhas de outro endereco nao bloqueiam mesma conta`() {
		authService.registerCustomer("Ana", "Silva", "isolated-rate@example.com", "senha-forte-123", "+5511999999999")
		repeat(5) {
			mockMvc.post("/auth/login") {
				with { it.remoteAddr = "192.0.2.10"; it }
				contentType = MediaType.APPLICATION_JSON
				content = """{"email":"isolated-rate@example.com","password":"wrong-password"}"""
			}.andExpect { status { isUnauthorized() } }
		}
		mockMvc.post("/auth/login") {
			with { it.remoteAddr = "192.0.2.11"; it }
			contentType = MediaType.APPLICATION_JSON
			content = """{"email":"isolated-rate@example.com","password":"senha-forte-123"}"""
		}.andExpect { status { isOk() } }
	}
}
