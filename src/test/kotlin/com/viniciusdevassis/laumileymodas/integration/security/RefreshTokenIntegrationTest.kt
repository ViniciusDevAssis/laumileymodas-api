package com.viniciusdevassis.laumileymodas.integration.security

import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.beans.factory.annotation.Autowired
import com.viniciusdevassis.laumileymodas.infrastructure.security.*
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import java.time.Instant
import java.util.UUID

class RefreshTokenIntegrationTest:Phase4IntegrationTest(){
	@Autowired lateinit var encoder:JwtEncoder
	@Autowired lateinit var properties:JwtProperties
	@Test fun `endpoint csrf fornece cookie legivel sem autenticar ou emitir tokens`() { val token=csrf();assertThat(token.value).isNotBlank();assertThat(token.isHttpOnly).isFalse();assertThat(jdbc.queryForObject("select count(*) from refresh_token",Long::class.java)).isZero() }

	@Test fun `rotaciona refresh rejeita reuso e revoga familia`() {
		val email=register();val session=login(email);val xsrf=csrf()
		val rotated=mockMvc.perform(post("/auth/refresh").cookie(session.refreshCookie,xsrf).header("X-XSRF-TOKEN",xsrf.value).header("Origin","http://localhost:3000"))
			.andExpect(status().isOk).andExpect(jsonPath("$.accessToken").isString).andReturn()
		assertThat(rotated.response.getCookie("LAUMILEY_REFRESH")!!.value).isNotEqualTo(session.refreshCookie.value)
		mockMvc.perform(post("/auth/refresh").cookie(session.refreshCookie,xsrf).header("X-XSRF-TOKEN",xsrf.value).header("Origin","http://localhost:3000"))
			.andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("AUTH_004"))
		assertThat(jdbc.queryForObject("select count(*) from refresh_token where revoked_at is null and consumed_at is null",Long::class.java)).isZero()
	}

	@Test fun `logout exige csrf e revoga familia`() { val session=login(register());mockMvc.perform(post("/auth/logout").cookie(session.refreshCookie).header("Origin","http://localhost:3000")).andExpect(status().isForbidden);val xsrf=csrf();mockMvc.perform(post("/auth/logout").cookie(session.refreshCookie,xsrf).header("X-XSRF-TOKEN",xsrf.value).header("Origin","http://localhost:3000")).andExpect(status().isNoContent);assertThat(jdbc.queryForObject("select count(*) from refresh_token where revoked_at is null",Long::class.java)).isZero() }

	@Test fun `refresh expirado ou revogado e rejeitado`() { val session=login(register());jdbc.update("update refresh_token set revoked_at=created_at");val xsrf=csrf();mockMvc.perform(post("/auth/refresh").cookie(session.refreshCookie,xsrf).header("X-XSRF-TOKEN",xsrf.value).header("Origin","http://localhost:3000")).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("AUTH_003")) }
	@Test fun `refresh jwt expirado e rejeitado antes de consultar estado`() { val now=Instant.now();val claims=JwtClaimsSet.builder().issuer(properties.issuer).subject(UUID.randomUUID().toString()).audience(listOf(properties.refreshAudience)).issuedAt(now.minusSeconds(1200)).expiresAt(now.minusSeconds(600)).id(UUID.randomUUID().toString()).claim("typ","refresh").claim("family_id",UUID.randomUUID().toString()).build();val expired=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId).build(),claims)).tokenValue;val xsrf=csrf();mockMvc.perform(post("/auth/refresh").cookie(jakarta.servlet.http.Cookie("LAUMILEY_REFRESH",expired),xsrf).header("X-XSRF-TOKEN",xsrf.value).header("Origin","http://localhost:3000")).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("AUTH_003")) }
}
