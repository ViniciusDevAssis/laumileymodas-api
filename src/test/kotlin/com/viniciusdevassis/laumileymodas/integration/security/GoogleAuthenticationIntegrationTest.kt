package com.viniciusdevassis.laumileymodas.integration.security

import com.viniciusdevassis.laumileymodas.application.auth.*
import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.VerifiedGoogleIdentity
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.OAuthAuthorizationRequestCookieRepository
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.GoogleIntentAuthorizationRequestResolver
import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.get
import java.util.UUID
import org.springframework.mock.web.*
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class GoogleAuthenticationIntegrationTest:Phase4IntegrationTest(){
	@Autowired lateinit var complete:CompleteGoogleLoginUseCase
	@Autowired lateinit var registration:CompleteGoogleCustomerRegistrationUseCase
	@Autowired lateinit var exchange:ExchangeGoogleHandoffUseCase
	@Autowired lateinit var authorizationRequests:OAuthAuthorizationRequestCookieRepository
	@Autowired lateinit var clientRegistrations:ClientRegistrationRepository
	@Autowired lateinit var googleAuthorizationResolver:GoogleIntentAuthorizationRequestResolver

	@Test fun `novo cliente Google conclui cadastro e vinculo usa somente sub`() { val handoff=complete.execute(VerifiedGoogleIdentity("new-sub","google@test.com","Ana","Silva"),false);val pair=registration.execute(handoff.rawHandle,"Ana","Silva","+5511999999999");assertThat(pair.accessToken).isNotBlank();assertThat(jdbc.queryForObject("select subject from account_external_identity",String::class.java)).isEqualTo("new-sub");val again=complete.execute(VerifiedGoogleIdentity("new-sub","email-alterado@test.com",null,null),false);assertThat(exchange.execute(again.rawHandle).accountId).isEqualTo(pair.accountId) }

	@Test fun `nao vincula sub inedito por coincidencia de email`() { val email=register("same@test.com");assertThatThrownBy{complete.execute(VerifiedGoogleIdentity("other-sub",email,null,null),false)}.isInstanceOfSatisfying(ApplicationException::class.java){assertThat(it.error).isEqualTo(AuthError.GOOGLE_EMAIL_CONFLICT)};assertThat(jdbc.queryForObject("select count(*) from account_external_identity",Long::class.java)).isZero() }

	@Test fun `somente sub e email Google configurados criam a unica admin`() { assertThatThrownBy{complete.execute(VerifiedGoogleIdentity("wrong","admin@example.test",null,null),true)}.isInstanceOf(ApplicationException::class.java);val handoff=complete.execute(VerifiedGoogleIdentity("test-admin-sub","admin@example.test",null,null),true);val tokens=exchange.execute(handoff.rawHandle);assertThat(tokens.accessToken).isNotBlank();assertThat(jdbc.queryForObject("select role from account where id=?",String::class.java,tokens.accountId)).isEqualTo("ADMIN") }

	@Test fun `pontos de inicio Google convergem para registration sem cookie de intencao separado`() { val client=mockMvc.get("/auth/google/client").andExpect{status{is3xxRedirection()}}.andReturn().response;val admin=mockMvc.get("/auth/google/admin").andExpect{status{is3xxRedirection()}}.andReturn().response;assertThat(client.redirectedUrl).startsWith("https://accounts.google.com/");assertThat(admin.redirectedUrl).startsWith("https://accounts.google.com/");assertThat(client.getHeaders("Set-Cookie")).noneMatch{it.contains("LAUMILEY_OAUTH_INTENT")};assertThat(client.redirectedUrl).doesNotContain("token","handoff") }

	@Test fun `intencao fica vinculada ao state no authorization request cifrado`() { val response=mockMvc.get("/auth/google/admin").andExpect{status{is3xxRedirection()}}.andReturn().response;val cookie=response.getCookie("LAUMILEY_OAUTH_REQUEST")!!;assertThat(cookie.value).doesNotContain("ADMIN");val callback=MockHttpServletRequest().apply{setCookies(cookie)};val request=authorizationRequests.loadAuthorizationRequest(callback)!!;assertThat(request.state).isNotBlank();assertThat(request.attributes[OAuthAuthorizationRequestCookieRepository.INTENT_ATTRIBUTE]).isEqualTo("ADMIN");assertThat(callback.getAttribute(OAuthAuthorizationRequestCookieRepository.INTENT_ATTRIBUTE)).isEqualTo("ADMIN") }

	@Test fun `resolver Google cria authorization request para os dois pontos de inicio`() { val client=MockHttpServletRequest().apply{requestURI="/auth/google/client";servletPath="/auth/google/client"};val admin=MockHttpServletRequest().apply{requestURI="/auth/google/admin";servletPath="/auth/google/admin"};val clientAuthorization=googleAuthorizationResolver.resolve(client)!!;authorizationRequests.saveAuthorizationRequest(clientAuthorization,client,MockHttpServletResponse());assertThat(clientAuthorization.attributes[OAuthAuthorizationRequestCookieRepository.INTENT_ATTRIBUTE]).isEqualTo("CLIENT");assertThat(googleAuthorizationResolver.resolve(admin)?.attributes?.get(OAuthAuthorizationRequestCookieRepository.INTENT_ATTRIBUTE)).isEqualTo("ADMIN") }

	@Test fun `callback configurado considera context path uma unica vez`() { assertThat(clientRegistrations.findByRegistrationId("google").redirectUri).isEqualTo("{baseUrl}/auth/google/callback");val response=mockMvc.get("/api/v1/auth/google/client") { contextPath = "/api/v1" }.andExpect{status{is3xxRedirection()}}.andReturn().response;assertThat(URLDecoder.decode(response.redirectedUrl,StandardCharsets.UTF_8)).contains("redirect_uri=http://localhost/api/v1/auth/google/callback").doesNotContain("/api/v1/api/v1/") }

	@Test fun `state nonce e authorization request permanecem cifrados em cookie temporario`() { val request=OAuth2AuthorizationRequest.authorizationCode().authorizationUri("https://accounts.google.com/o/oauth2/v2/auth").clientId("client").redirectUri("https://app.test/callback").state("state-secret").additionalParameters(mapOf("nonce" to "nonce-secret")).build();val response=MockHttpServletResponse();authorizationRequests.saveAuthorizationRequest(request,MockHttpServletRequest(),response);val cookie=response.getCookie("LAUMILEY_OAUTH_REQUEST")!!;assertThat(cookie.value).doesNotContain("state-secret","nonce-secret");val callback=MockHttpServletRequest().apply{setCookies(cookie)};assertThat(authorizationRequests.loadAuthorizationRequest(callback)!!.state).isEqualTo("state-secret") }
}
