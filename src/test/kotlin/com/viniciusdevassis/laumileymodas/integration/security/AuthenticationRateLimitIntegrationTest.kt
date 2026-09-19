package com.viniciusdevassis.laumileymodas.integration.security

import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import com.viniciusdevassis.laumileymodas.infrastructure.security.*
import com.viniciusdevassis.laumileymodas.application.port.ClockProvider
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import java.time.*

class AuthenticationRateLimitIntegrationTest:Phase4IntegrationTest(){
	@Test fun `excesso de falhas retorna 429 mas tentativas validas normais nao sao bloqueadas`() { val email=register();val body="""{"email":"$email","password":"errada"}""";repeat(5){mockMvc.perform(post("/auth/login").contentType("application/json").header("Origin","http://localhost:3000").content(body)).andExpect(status().isUnauthorized)};mockMvc.perform(post("/auth/login").contentType("application/json").header("Origin","http://localhost:3000").content(body)).andExpect(status().isTooManyRequests).andExpect(jsonPath("$.code").value("AUTH_008"));login(register(),"http://localhost:3000") }
	@Test fun `janela expirada libera novas tentativas`() { var now=Instant.parse("2026-01-01T00:00:00Z");val limiter=AuthenticationRateLimiter(AuthenticationRateLimitProperties(2,Duration.ofMinutes(1)),ClockProvider{now});limiter.failed("key");limiter.failed("key");assertThatThrownBy{limiter.check("key")};now=now.plusSeconds(61);assertThatCode{limiter.check("key")}.doesNotThrowAnyException() }
}
