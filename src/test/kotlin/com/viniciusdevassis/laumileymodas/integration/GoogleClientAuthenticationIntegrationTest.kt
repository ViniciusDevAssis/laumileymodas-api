package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.application.GoogleAuthService
import com.viniciusdevassis.laumileymodas.domain.enums.Provider
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountExternalIdentityRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.RefreshTokenRepository
import com.viniciusdevassis.laumileymodas.infrastructure.security.OAuth2SuccessHandler
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@AutoConfigureMockMvc
class GoogleClientAuthenticationIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val googleAuthService: GoogleAuthService,
	@Autowired private val authService: AuthService,
	@Autowired private val oauth2SuccessHandler: OAuth2SuccessHandler,
	@Autowired private val accountRepository: AccountRepository,
	@Autowired private val identityRepository: AccountExternalIdentityRepository,
	@Autowired private val customerRepository: CustomerRepository,
	@Autowired private val refreshTokenRepository: RefreshTokenRepository,
) : PostgresIntegrationTest() {
	@BeforeEach
	fun cleanDatabase() {
		refreshTokenRepository.deleteAll()
		customerRepository.deleteAll()
		identityRepository.deleteAll()
		accountRepository.deleteAll()
	}

	@Test
	fun `endpoint nativo do Spring inicia OAuth com callback padrao`() {
		mockMvc.get("/oauth2/authorization/google").andExpect {
			status { isFound() }
			header { string("Location", org.hamcrest.Matchers.containsString("accounts.google.com")) }
			header { string("Location", org.hamcrest.Matchers.containsString("login/oauth2/code/google")) }
		}
	}

	@Test
	fun `sub novo cria CLIENT e sub vinculado faz login sem linking por email`() {
		val firstRefreshToken = googleAuthService.authenticate(oidcUser("google-client-1", "google@example.com"))
		val account = accountRepository.findByEmail("google@example.com")
		assertEquals(Role.CLIENT, account?.role)
		assertNull(customerRepository.findByAccountId(requireNotNull(account?.id))?.whatsappPhone)
		assertEquals(1L, identityRepository.count())
		assertNotNull(firstRefreshToken)
		assertEquals(1L, refreshTokenRepository.count())
		val first = authService.refresh(firstRefreshToken)
		mockMvc.put("/customers/me/whatsapp") {
			header("Authorization", "Bearer ${first.accessToken}")
			contentType = MediaType.APPLICATION_JSON
			content = """{"whatsappPhone":"+5511888777666"}"""
		}.andExpect {
			status { isOk() }
			jsonPath("$.whatsappPhone") { value("+5511888777666") }
			jsonPath("$._links.whatsapp.href") { exists() }
		}
		assertEquals("+5511888777666", customerRepository.findByAccountId(requireNotNull(account?.id))?.whatsappPhone)

		val second = googleAuthService.authenticate(oidcUser("google-client-1", "google@example.com"))
		assertNotNull(second)
		assertEquals(3L, refreshTokenRepository.count())
	}

	@Test
	fun `sub administrativo configurado e unica forma de obter ADMIN`() {
		val adminToken = googleAuthService.authenticate(oidcUser("test-admin-sub", "admin@example.com"))
		assertNotNull(adminToken)
		assertEquals(Role.ADMIN, accountRepository.findByEmail("admin@example.com")?.role)
		assertNull(customerRepository.findByAccountId(requireNotNull(accountRepository.findByEmail("admin@example.com")?.id)))

		val clientToken = googleAuthService.authenticate(oidcUser("other-google-sub", "other@example.com"))
		assertNotNull(clientToken)
		assertEquals(Role.CLIENT, accountRepository.findByEmail("other@example.com")?.role)
		assertEquals(1, accountRepository.findAll().count { it.role == Role.ADMIN })
	}

	@Test
	fun `email existente sem vinculo Google e rejeitado`() {
		authService.registerCustomer("Ana", "Silva", "conflict@example.com", "senha-forte-123", "+5511999999999")
		org.junit.jupiter.api.assertThrows<ApiException> {
			googleAuthService.authenticate(oidcUser("unlinked-sub", "conflict@example.com"))
		}
		assertNull(identityRepository.findByProviderAndSubject(Provider.GOOGLE, "unlinked-sub"))
		assertEquals(1L, accountRepository.count())
	}

	@Test
	fun `identidade Google com email nao verificado e rejeitada`() {
		org.junit.jupiter.api.assertThrows<ApiException> {
			googleAuthService.authenticate(oidcUser("unverified-sub", "unverified@example.com", emailVerified = false))
		}
		assertEquals(0L, accountRepository.count())
	}

	@Test
	fun `success handler define refresh HttpOnly e redireciona a URL configurada sem tokens na URL`() {
		val user = oidcUser("handler-sub", "handler@example.com")
		val authentication = OAuth2AuthenticationToken(user, user.authorities, "google")
		val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
		val response = MockHttpServletResponse()

		oauth2SuccessHandler.onAuthenticationSuccess(request, response, authentication)

		assertEquals("http://localhost:3000/auth/google/success", response.redirectedUrl)
		assertTrue(requireNotNull(response.getHeader("Set-Cookie")).contains("LAUMILEY_REFRESH="))
		assertTrue(requireNotNull(response.getHeader("Set-Cookie")).contains("HttpOnly"))
		assertNull(request.getSession(false))
		assertTrue(!requireNotNull(response.redirectedUrl).contains("accessToken"))
	}

	private fun oidcUser(subject: String, email: String, emailVerified: Boolean = true): DefaultOidcUser {
		val now = Instant.now()
		val claims = mapOf<String, Any>(
			"iss" to "https://accounts.google.com",
			"sub" to subject,
			"aud" to listOf("test-google-client"),
			"iat" to now,
			"exp" to now.plusSeconds(3600),
			"email" to email,
			"email_verified" to emailVerified,
			"given_name" to "Google",
			"family_name" to "Client",
			"name" to "Google Client",
		)
		val idToken = OidcIdToken("id-token-$subject", now, now.plusSeconds(3600), claims)
		return DefaultOidcUser(listOf(SimpleGrantedAuthority("ROLE_USER")), idToken)
	}
}
