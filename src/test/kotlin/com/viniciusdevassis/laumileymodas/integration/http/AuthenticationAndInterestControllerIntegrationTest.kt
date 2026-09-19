package com.viniciusdevassis.laumileymodas.integration.http

import com.viniciusdevassis.laumileymodas.application.auth.CompleteGoogleLoginUseCase
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.VerifiedGoogleIdentity
import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

class AuthenticationAndInterestControllerIntegrationTest:Phase4IntegrationTest(){
	@Autowired lateinit var google:CompleteGoogleLoginUseCase
	@Test fun `cliente autenticado confirma interesse idempotente e recupera link sem duplicar acompanhamento`() { val session=login(register());val product=seedActiveProduct();val first=mockMvc.post("/products/$product/interests"){header(HttpHeaders.AUTHORIZATION,"Bearer ${session.accessToken}");header("Idempotency-Key","http-key")}.andExpect{status{isCreated()};jsonPath("$.whatsappUrl"){value(org.hamcrest.Matchers.startsWith("https://wa.me/"))}}.andReturn();val interest=objectMapper.readTree(first.response.contentAsString).get("interestId").asText();mockMvc.post("/products/$product/interests"){header(HttpHeaders.AUTHORIZATION,"Bearer ${session.accessToken}");header("Idempotency-Key","http-key")}.andExpect{status{isOk()};jsonPath("$.interestId"){value(interest)}};mockMvc.get("/customers/me/interests/$interest/whatsapp-link"){header(HttpHeaders.AUTHORIZATION,"Bearer ${session.accessToken}")}.andExpect{status{isOk()};jsonPath("$.interestId"){value(interest)}};assertThat(jdbc.queryForObject("select count(*) from reminder",Long::class.java)).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from contact_record",Long::class.java)).isEqualTo(1) }

	@Test fun `handoff Google exige bootstrap csrf e e consumido uma vez sem token na URL`() { val handoff=google.execute(VerifiedGoogleIdentity("test-admin-sub","admin@example.test",null,null),true);val xsrf=csrf();val cookie=Cookie("LAUMILEY_OAUTH_HANDOFF",handoff.rawHandle);mockMvc.post("/auth/google/exchange"){cookie(cookie,xsrf);header("X-XSRF-TOKEN",xsrf.value);header("Origin","http://localhost:3000")}.andExpect{status{isOk()};jsonPath("$.accessToken"){isString()}};mockMvc.post("/auth/google/exchange"){cookie(cookie,xsrf);header("X-XSRF-TOKEN",xsrf.value);header("Origin","http://localhost:3000")}.andExpect{status{isUnauthorized()};jsonPath("$.code"){value("AUTH_005")}} }
	@Test fun `conclusao de cadastro Google consome handoff protegido por csrf`() { val handoff=google.execute(VerifiedGoogleIdentity("new-http-sub","new-http@test.com","Ana","Silva"),false);val xsrf=csrf();val cookie=Cookie("LAUMILEY_OAUTH_HANDOFF",handoff.rawHandle);val body="""{"firstName":"Ana","lastName":"Silva","whatsappPhone":"+5511999999999"}""";mockMvc.post("/auth/google/customers"){cookie(cookie,xsrf);header("X-XSRF-TOKEN",xsrf.value);header("Origin","http://localhost:3000");contentType=MediaType.APPLICATION_JSON;content=body}.andExpect{status{isCreated()};jsonPath("$.account.role"){value("CLIENT")}};mockMvc.post("/auth/google/customers"){cookie(cookie,xsrf);header("X-XSRF-TOKEN",xsrf.value);header("Origin","http://localhost:3000");contentType=MediaType.APPLICATION_JSON;content=body}.andExpect{status{isUnauthorized()}} }
}
