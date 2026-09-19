package com.viniciusdevassis.laumileymodas.integration.security

import com.viniciusdevassis.laumileymodas.application.auth.*
import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.VerifiedGoogleIdentity
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.OAuthAuthorizationRequestCookieRepository
import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.get
import java.util.UUID
import org.springframework.mock.web.*
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest

class GoogleAuthenticationIntegrationTest:Phase4IntegrationTest(){
	@Autowired lateinit var complete:CompleteGoogleLoginUseCase
	@Autowired lateinit var registration:CompleteGoogleCustomerRegistrationUseCase
	@Autowired lateinit var exchange:ExchangeGoogleHandoffUseCase
	@Autowired lateinit var authorizationRequests:OAuthAuthorizationRequestCookieRepository

	@Test fun `novo cliente Google conclui cadastro e vinculo usa somente sub`() { val handoff=complete.execute(VerifiedGoogleIdentity("new-sub","google@test.com","Ana","Silva"),false);val pair=registration.execute(handoff.rawHandle,"Ana","Silva","+5511999999999");assertThat(pair.accessToken).isNotBlank();assertThat(jdbc.queryForObject("select subject from account_external_identity",String::class.java)).isEqualTo("new-sub");val again=complete.execute(VerifiedGoogleIdentity("new-sub","email-alterado@test.com",null,null),false);assertThat(exchange.execute(again.rawHandle).accountId).isEqualTo(pair.accountId) }

	@Test fun `nao vincula sub inedito por coincidencia de email`() { val email=register("same@test.com");assertThatThrownBy{complete.execute(VerifiedGoogleIdentity("other-sub",email,null,null),false)}.isInstanceOfSatisfying(ApplicationException::class.java){assertThat(it.error).isEqualTo(AuthError.GOOGLE_EMAIL_CONFLICT)};assertThat(jdbc.queryForObject("select count(*) from account_external_identity",Long::class.java)).isZero() }

	@Test fun `somente sub e email Google configurados criam a unica admin`() { assertThatThrownBy{complete.execute(VerifiedGoogleIdentity("wrong","admin@example.test",null,null),true)}.isInstanceOf(ApplicationException::class.java);val handoff=complete.execute(VerifiedGoogleIdentity("test-admin-sub","admin@example.test",null,null),true);val tokens=exchange.execute(handoff.rawHandle);assertThat(tokens.accessToken).isNotBlank();assertThat(jdbc.queryForObject("select role from account where id=?",String::class.java,tokens.accountId)).isEqualTo("ADMIN") }

	@Test fun `inicio Google usa redirect conhecido sem tokens na URL`() { val location=mockMvc.get("/auth/google/client").andExpect{status{is3xxRedirection()}}.andReturn().response.redirectedUrl;assertThat(location).isEqualTo("/api/v1/oauth2/authorization/google");assertThat(location).doesNotContain("token","handoff") }

	@Test fun `state nonce e authorization request permanecem cifrados em cookie temporario`() { val request=OAuth2AuthorizationRequest.authorizationCode().authorizationUri("https://accounts.google.com/o/oauth2/v2/auth").clientId("client").redirectUri("https://app.test/callback").state("state-secret").additionalParameters(mapOf("nonce" to "nonce-secret")).build();val response=MockHttpServletResponse();authorizationRequests.saveAuthorizationRequest(request,MockHttpServletRequest(),response);val cookie=response.getCookie("LAUMILEY_OAUTH_REQUEST")!!;assertThat(cookie.value).doesNotContain("state-secret","nonce-secret");val callback=MockHttpServletRequest().apply{setCookies(cookie)};assertThat(authorizationRequests.loadAuthorizationRequest(callback)!!.state).isEqualTo("state-secret") }
}
