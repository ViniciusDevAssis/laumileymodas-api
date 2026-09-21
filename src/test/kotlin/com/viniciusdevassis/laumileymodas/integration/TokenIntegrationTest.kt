package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.RefreshTokenRepository
import com.viniciusdevassis.laumileymodas.infrastructure.security.TokenService
import com.viniciusdevassis.laumileymodas.infrastructure.security.RefreshTokenCookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@AutoConfigureMockMvc
class TokenIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val authService: AuthService,
	@Autowired private val tokenService: TokenService,
	@Autowired private val refreshTokenCookie: RefreshTokenCookie,
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
	fun `JWT valido autentica e expirado ou invalido retorna 401`() {
		val customer = authService.registerCustomer("Ana", "Silva", "jwt@example.com", "senha-forte-123", "+5511999999999")
		val tokens = authService.login("jwt@example.com", "senha-forte-123", "127.0.0.1")
		mockMvc.get("/customers/me") { header("Authorization", "Bearer ${tokens.accessToken}") }
			.andExpect { status { isOk() } }

		val expiredService = TokenService(
			jwtSecret = "test-secret-with-at-least-256-bits-for-hmac-validation",
			issuer = "laumiley-modas-api-test",
			audience = "laumiley-api-test",
			accessTokenTtl = Duration.ofSeconds(1),
			clock = Clock.fixed(Instant.now().minusSeconds(120), ZoneOffset.UTC),
		)
		val expiredToken = expiredService.createAccessToken(requireNotNull(customer.account.id), Role.CLIENT)
		mockMvc.get("/customers/me") { header("Authorization", "Bearer $expiredToken") }
			.andExpect { status { isUnauthorized() } }
		mockMvc.get("/customers/me") { header("Authorization", "Bearer not.a.jwt") }
			.andExpect { status { isUnauthorized() } }
	}

	@Test
	fun `refresh rotaciona revoga e detecta reuso`() {
		authService.registerCustomer("Ana", "Silva", "rotate@example.com", "senha-forte-123", "+5511999999999")
		val first = authService.login("rotate@example.com", "senha-forte-123", "127.0.0.1")
		val firstPersisted = refreshTokenRepository.findAll().single()
		assertEquals(refreshTokenCookie.hash(first.refreshToken), firstPersisted.tokenHash)

		val second = authService.refresh(first.refreshToken)
		val records = refreshTokenRepository.findAll()
		assertEquals(2, records.size)
		assertEquals(refreshTokenCookie.hash(second.refreshToken), records.first { it.tokenHash != firstPersisted.tokenHash }.tokenHash)
		assertNotNull(records.first { it.tokenHash == firstPersisted.tokenHash }.consumedAt)
		assertTrue(tokenService.validate(second.accessToken).subject.isNotBlank())

		org.junit.jupiter.api.assertThrows<com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException> {
			authService.refresh(first.refreshToken)
		}
		assertTrue(refreshTokenRepository.findAll().all { it.revokedAt != null })

		val other = authService.login("rotate@example.com", "senha-forte-123", "127.0.0.1")
		authService.logout(other.refreshToken)
		val logoutRecord = refreshTokenRepository.findAll().first { it.tokenHash == refreshTokenCookie.hash(other.refreshToken) }
		assertNotNull(logoutRecord.revokedAt)
	}
}
