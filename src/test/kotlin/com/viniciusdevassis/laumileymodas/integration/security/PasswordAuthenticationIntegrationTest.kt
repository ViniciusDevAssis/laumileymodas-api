package com.viniciusdevassis.laumileymodas.integration.security

import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

class PasswordAuthenticationIntegrationTest:Phase4IntegrationTest(){
	@Test fun `cadastro publico sempre cria client com bcrypt custo 12 e consentimento negado`() { val email=register();val row=jdbc.queryForMap("select a.role,a.password_hash,c.proactive_contact_authorized from account a join customer c on c.account_id=a.id where a.normalized_email=?",email);assertThat(row["role"]).isEqualTo("CLIENT");assertThat(row["password_hash"].toString()).startsWith("{bcrypt}").contains("\$2a\$12\$");assertThat(row["proactive_contact_authorized"]).isEqualTo(false) }
	@Test fun `login valido emite access e cookie refresh e invalido retorna contrato proprio`() { val email=register();val valid=login(email);assertThat(valid.accessToken.split('.')).hasSize(3);assertThat(valid.refreshCookie.isHttpOnly).isTrue();mockMvc.perform(post("/auth/login").contentType("application/json").content("""{"email":"$email","password":"errada"}""")).andExpect(status().isUnauthorized).andExpect(jsonPath("$.code").value("AUTH_001")) }
}
