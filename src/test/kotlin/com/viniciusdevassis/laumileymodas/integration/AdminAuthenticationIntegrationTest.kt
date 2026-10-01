package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.application.GoogleAuthService
import com.viniciusdevassis.laumileymodas.domain.entities.Account
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountExternalIdentityRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.RefreshTokenRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant
import kotlin.test.assertEquals

@AutoConfigureMockMvc
class AdminAuthenticationIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val googleAuthService: GoogleAuthService,
	@Autowired private val authService: AuthService,
	@Autowired private val accountRepository: AccountRepository,
	@Autowired private val customerRepository: CustomerRepository,
	@Autowired private val identityRepository: AccountExternalIdentityRepository,
	@Autowired private val refreshTokenRepository: RefreshTokenRepository,
) : PostgresIntegrationTest() {
	@BeforeEach
	fun clear() {
		refreshTokenRepository.deleteAll()
		customerRepository.deleteAll()
		identityRepository.deleteAll()
		accountRepository.deleteAll()
	}

	@Test
	fun `somente sub Google configurado acessa administracao`() {
		mockMvc.get("/admin/categories").andExpect { status { isUnauthorized() } }
		val client = bearerFor("other-google-sub", "client-admin-test@example.com")
		mockMvc.get("/admin/categories") { header("Authorization", client) }
			.andExpect { status { isForbidden() } }
		assertEquals(Role.CLIENT, accountRepository.findByEmail("client-admin-test@example.com")?.role)

		val admin = bearerFor("test-admin-sub", "admin-sub-test@example.com")
		mockMvc.get("/admin/categories") { header("Authorization", admin) }
			.andExpect { status { isOk() } }
		assertEquals(Role.ADMIN, accountRepository.findByEmail("admin-sub-test@example.com")?.role)
		assertEquals(1, accountRepository.findAll().count { it.role == Role.ADMIN })
	}

	private fun bearerFor(subject: String, email: String): String {
		val now = Instant.now()
		val claims = mapOf<String, Any>(
			"iss" to "https://accounts.google.com", "sub" to subject,
			"aud" to listOf("test-google-client"), "iat" to now, "exp" to now.plusSeconds(3600),
			"email" to email, "email_verified" to true, "given_name" to "Loja", "family_name" to "Laumiley",
		)
		val idToken = OidcIdToken("id-$subject", now, now.plusSeconds(3600), claims)
		val user = DefaultOidcUser(listOf(SimpleGrantedAuthority("ROLE_USER")), idToken)
		val rawRefresh = googleAuthService.authenticate(user)
		return "Bearer ${authService.refresh(rawRefresh).accessToken}"
	}
}
