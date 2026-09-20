package com.viniciusdevassis.laumileymodas.integration.security

import com.viniciusdevassis.laumileymodas.application.auth.AuthError
import com.viniciusdevassis.laumileymodas.application.auth.CompleteGoogleCustomerRegistrationUseCase
import com.viniciusdevassis.laumileymodas.application.auth.CompleteGoogleLoginUseCase
import com.viniciusdevassis.laumileymodas.application.auth.ExchangeGoogleHandoffUseCase
import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.domain.account.OAuthHandoffPurpose
import com.viniciusdevassis.laumileymodas.infrastructure.security.GoogleOAuthSuccessHandler
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.VerifiedGoogleIdentity
import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.test.web.servlet.get
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant

class GoogleAuthenticationIntegrationTest : Phase4IntegrationTest() {
    @Autowired
    lateinit var complete: CompleteGoogleLoginUseCase

    @Autowired
    lateinit var registration: CompleteGoogleCustomerRegistrationUseCase

    @Autowired
    lateinit var exchange: ExchangeGoogleHandoffUseCase

    @Autowired
    lateinit var clientRegistrations: ClientRegistrationRepository

    @Autowired
    lateinit var successHandler: GoogleOAuthSuccessHandler

    @Test
    fun `novo cliente Google conclui cadastro e vinculo usa somente sub`() {
        val handoff = complete.execute(VerifiedGoogleIdentity("new-sub", "google@test.com", "Ana", "Silva"))
        val pair = registration.execute(handoff.rawHandle, "Ana", "Silva", "+5511999999999")

        assertThat(pair.accessToken).isNotBlank()
        assertThat(jdbc.queryForObject("select subject from account_external_identity", String::class.java))
            .isEqualTo("new-sub")

        val again = complete.execute(VerifiedGoogleIdentity("new-sub", "email-alterado@test.com", null, null))
        assertThat(exchange.execute(again.rawHandle).accountId).isEqualTo(pair.accountId)
    }

    @Test
    fun `nao vincula sub inedito por coincidencia de email`() {
        val email = register("same@test.com")

        assertThatThrownBy {
            complete.execute(VerifiedGoogleIdentity("other-sub", email, null, null))
        }.isInstanceOfSatisfying(ApplicationException::class.java) {
            assertThat(it.error).isEqualTo(AuthError.GOOGLE_EMAIL_CONFLICT)
        }
        assertThat(jdbc.queryForObject("select count(*) from account_external_identity", Long::class.java)).isZero()
    }

    @Test
    fun `somente o sub configurado cria a unica admin e os demais seguem como client`() {
        val clientHandoff = complete.execute(
            VerifiedGoogleIdentity("not-admin-sub", "cliente-google@test.com", "Cliente", "Google"),
        )
        assertThat(clientHandoff.purpose).isEqualTo(OAuthHandoffPurpose.CLIENT_REGISTRATION)
        val clientTokens = registration.execute(
            clientHandoff.rawHandle,
            "Cliente",
            "Google",
            "+5511999999999",
        )
        assertThat(
            jdbc.queryForObject("select role from account where id=?", String::class.java, clientTokens.accountId),
        ).isEqualTo("CLIENT")

        val adminHandoff = complete.execute(
            VerifiedGoogleIdentity("test-admin-sub", "qualquer-email-admin@test.com", null, null),
        )
        val adminTokens = exchange.execute(adminHandoff.rawHandle)
        assertThat(adminTokens.accessToken).isNotBlank()
        assertThat(
            jdbc.queryForObject("select role from account where id=?", String::class.java, adminTokens.accountId),
        ).isEqualTo("ADMIN")
    }

    @Test
    fun `endpoint nativo usa registration google e callback padrao com context path`() {
        assertThat(clientRegistrations.findByRegistrationId("google").redirectUri)
            .isEqualTo("{baseUrl}/{action}/oauth2/code/{registrationId}")

        val result = mockMvc.get("http://localhost:8080/api/v1/oauth2/authorization/google") {
            contextPath = "/api/v1"
        }.andExpect {
            status { is3xxRedirection() }
        }.andReturn()

        val redirect = URLDecoder.decode(result.response.redirectedUrl, StandardCharsets.UTF_8)
        assertThat(redirect)
            .startsWith("https://accounts.google.com/")
            .contains("redirect_uri=http://localhost:8080/api/v1/login/oauth2/code/google")
            .contains("scope=openid profile email")
            .doesNotContain("/api/v1/api/v1/")
        assertThat(result.request.getSession(false)).isNotNull()
        assertThat(result.response.getCookie("LAUMILEY_OAUTH_REQUEST")).isNull()
    }

    @Test
    fun `success handler decide papel pelo sub cria handoff e encerra sessao OAuth`() {
        val request = MockHttpServletRequest().apply { getSession(true) }
        val response = MockHttpServletResponse()

        successHandler.onAuthenticationSuccess(
            request,
            response,
            googleAuthentication("test-admin-sub", "owner@test.com"),
        )

        assertThat(response.redirectedUrl).isEqualTo("http://localhost:3000/auth/google/success")
        assertThat(request.getSession(false)).isNull()
        val handoff = response.getCookie("LAUMILEY_OAUTH_HANDOFF")
        assertThat(handoff).isNotNull
        val tokens = exchange.execute(handoff!!.value)
        assertThat(jdbc.queryForObject("select role from account where id=?", String::class.java, tokens.accountId))
            .isEqualTo("ADMIN")
    }

    private fun googleAuthentication(subject: String, email: String): OAuth2AuthenticationToken {
        val now = Instant.now()
        val idToken = OidcIdToken(
            "google-id-token",
            now,
            now.plusSeconds(300),
            mapOf(
                "sub" to subject,
                "email" to email,
                "email_verified" to true,
            ),
        )
        val authorities = listOf(SimpleGrantedAuthority("OIDC_USER"))
        val user = DefaultOidcUser(authorities, idToken)
        return OAuth2AuthenticationToken(user, authorities, "google")
    }
}
