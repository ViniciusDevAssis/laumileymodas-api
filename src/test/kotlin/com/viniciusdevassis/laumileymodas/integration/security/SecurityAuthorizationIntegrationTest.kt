package com.viniciusdevassis.laumileymodas.integration.security

import com.viniciusdevassis.laumileymodas.infrastructure.security.JwtProperties
import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.*
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.Instant
import java.util.UUID
import com.viniciusdevassis.laumileymodas.application.auth.*
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.VerifiedGoogleIdentity

class SecurityAuthorizationIntegrationTest:Phase4IntegrationTest(){
	@Autowired lateinit var encoder:JwtEncoder
	@Autowired lateinit var properties:JwtProperties
	@Autowired lateinit var google:CompleteGoogleLoginUseCase
	@Autowired lateinit var googleExchange:ExchangeGoogleHandoffUseCase

	@Test fun `anonimo recebe 401 e client recebe 403 no contrato proprio`() { mockMvc.get("/auth/me").andExpect{status{isUnauthorized()};jsonPath("$.code"){value("SECURITY_001")}};val session=login(register());mockMvc.get("/admin/categories"){header(HttpHeaders.AUTHORIZATION,"Bearer ${session.accessToken}")}.andExpect{status{isForbidden()};jsonPath("$.code"){value("SECURITY_002")}} }

	@Test fun `access valido autentica e expirado ou adulterado recebe 401`() { val session=login(register());mockMvc.get("/auth/me"){header(HttpHeaders.AUTHORIZATION,"Bearer ${session.accessToken}")}.andExpect{status{isOk()};jsonPath("$.role"){value("CLIENT")}};val expired=expiredAccess();mockMvc.get("/auth/me"){header(HttpHeaders.AUTHORIZATION,"Bearer $expired")}.andExpect{status{isUnauthorized()};jsonPath("$.code"){value("SECURITY_001")}};val parts=session.accessToken.split('.');val tampered=parts[0]+"."+parts[1]+"."+(if(parts[2][0]=='A')'B' else 'A')+parts[2].drop(1);mockMvc.get("/auth/me"){header(HttpHeaders.AUTHORIZATION,"Bearer $tampered")}.andExpect{status{isUnauthorized()}} }

	@Test fun `cors aceita somente origem configurada com credenciais`() { mockMvc.perform(options("/auth/refresh").header("Origin","http://localhost:3000").header("Access-Control-Request-Method","POST")).andExpect(status().isOk).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:3000")).andExpect(header().string("Access-Control-Allow-Credentials","true"));mockMvc.perform(options("/auth/refresh").header("Origin","https://evil.test").header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden); }

	@Test fun `retirada de consentimento nao muda permissoes`() { val email=register();val session=login(email);jdbc.update("update customer set proactive_contact_authorized=true");jdbc.update("update customer set proactive_contact_authorized=false");mockMvc.get("/auth/me"){header(HttpHeaders.AUTHORIZATION,"Bearer ${session.accessToken}")}.andExpect{status{isOk()}} }
	@Test fun `admin Google autorizado recebe papel administrativo`() { val handoff=google.execute(VerifiedGoogleIdentity("test-admin-sub","admin@example.test",null,null),true);val token=googleExchange.execute(handoff.rawHandle).accessToken;mockMvc.get("/auth/me"){header(HttpHeaders.AUTHORIZATION,"Bearer $token")}.andExpect{status{isOk()};jsonPath("$.role"){value("ADMIN")}} }

	private fun expiredAccess():String { val now=Instant.now();val claims=JwtClaimsSet.builder().issuer(properties.issuer).subject(UUID.randomUUID().toString()).audience(listOf(properties.accessAudience)).issuedAt(now.minusSeconds(1200)).expiresAt(now.minusSeconds(600)).id(UUID.randomUUID().toString()).claim("typ","access").claim("role","CLIENT").build();return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId).build(),claims)).tokenValue }
}
