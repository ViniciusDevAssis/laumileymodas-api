package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.domain.entities.Account
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.RefreshTokenRepository
import com.viniciusdevassis.laumileymodas.infrastructure.security.RefreshTokenCookie
import com.viniciusdevassis.laumileymodas.infrastructure.security.TokenService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceIntegrationTest(
	@Autowired private val authService: AuthService,
	@Autowired private val accountRepository: AccountRepository,
	@Autowired private val customerRepository: CustomerRepository,
	@Autowired private val refreshTokenRepository: RefreshTokenRepository,
	@Autowired private val refreshTokenCookie: RefreshTokenCookie,
	@Autowired private val tokenService: TokenService,
) : PostgresIntegrationTest() {
	@BeforeEach
	fun cleanDatabase() {
		refreshTokenRepository.deleteAll()
		customerRepository.deleteAll()
		accountRepository.deleteAll()
	}

	@Test
	fun `cadastro publico cria apenas cliente com email normalizado e senha BCrypt`() {
		val customer = authService.registerCustomer(
			firstName = "Maria",
			lastName = "Silva",
			email = "  MARIA@EXAMPLE.COM ",
			password = "senha-segura",
			whatsappPhone = "+5511999999999",
		)

		val account = accountRepository.findByEmail("maria@example.com")

		assertNotNull(account)
		assertEquals(Role.CLIENT, account.role)
		assertEquals(customer.account.id, account.id)
		assertTrue(requireNotNull(account.passwordHash).startsWith("$2a$12$"))
		assertTrue(BCryptPasswordEncoder().matches("senha-segura", account.passwordHash))
		assertFalse(accountRepository.existsByRole(Role.ADMIN))
	}

	@Test
	fun `login local emite access token e persiste somente hash do refresh token`() {
		val account = registeredAccount("login@example.com", "senha-correta")

		val tokens = authService.login(" LOGIN@example.com ", "senha-correta", "127.0.0.1")
		val persisted = refreshTokenRepository.findAll().single()
		val jwt = tokenService.validate(tokens.accessToken)

		assertEquals(account.id, tokenService.accountId(jwt))
		assertEquals(Role.CLIENT, tokenService.role(jwt))
		assertEquals(refreshTokenCookie.hash(tokens.refreshToken), persisted.tokenHash)
		assertNotEquals(tokens.refreshToken, persisted.tokenHash)
		assertTrue(persisted.isActive(java.time.Instant.now().minusSeconds(1)))
	}

	@Test
	fun `login invalido registra falha e rejeita credenciais`() {
		registeredAccount("falha@example.com", "senha-correta")

		assertThrows<ApiException> {
			authService.login("falha@example.com", "senha-errada", "127.0.0.1")
		}

		assertTrue(refreshTokenRepository.findAll().isEmpty())
	}

	@Test
	fun `refresh consome token anterior e emite novo par de tokens na mesma familia`() {
		registeredAccount("refresh@example.com", "senha-correta")
		val first = authService.login("refresh@example.com", "senha-correta", "127.0.0.1")

		val second = authService.refresh(first.refreshToken)
		val persisted = refreshTokenRepository.findAll().sortedBy { it.createdAt }

		assertEquals(2, persisted.size)
		assertNotNull(persisted.first().consumedAt)
		assertNull(persisted.last().consumedAt)
		assertEquals(persisted.first().familyId, persisted.last().familyId)
		assertEquals(refreshTokenCookie.hash(second.refreshToken), persisted.last().tokenHash)
	}

	@Test
	fun `reuso de refresh token consumido revoga familia`() {
		registeredAccount("reuso@example.com", "senha-correta")
		val first = authService.login("reuso@example.com", "senha-correta", "127.0.0.1")
		authService.refresh(first.refreshToken)

		assertThrows<ApiException> {
			authService.refresh(first.refreshToken)
		}

		assertTrue(refreshTokenRepository.findAll().all { it.revokedAt != null })
	}

	@Test
	fun `logout revoga refresh token informado`() {
		registeredAccount("logout@example.com", "senha-correta")
		val tokens = authService.login("logout@example.com", "senha-correta", "127.0.0.1")

		authService.logout(tokens.refreshToken)

		assertNotNull(refreshTokenRepository.findAll().single().revokedAt)
	}

	private fun registeredAccount(email: String, password: String) =
		authService.registerCustomer(
			firstName = "Cliente",
			lastName = "Teste",
			email = email,
			password = password,
			whatsappPhone = "+5511999999999",
		).account
}
