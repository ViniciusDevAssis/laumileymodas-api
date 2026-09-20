package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@AutoConfigureMockMvc
@Import(SecurityErrorIntegrationTest.SecurityTestController::class)
class SecurityErrorIntegrationTest(
	@Autowired
	private val mockMvc: MockMvc,
) : PostgresIntegrationTest() {
	@Test
	fun `retorna contrato proprio para requisicao sem autenticacao`() {
		mockMvc.get("/test/security/protected")
			.andExpect {
				status { isUnauthorized() }
				jsonPath("$.status") { value(401) }
				jsonPath("$.code") { value(ApiError.AUTH_001.code) }
				jsonPath("$.message") { value(ApiError.AUTH_001.message) }
				jsonPath("$.path") { value("/test/security/protected") }
				jsonPath("$.timestamp") { exists() }
			}
	}

	@Test
	@WithMockUser(roles = ["CLIENT"])
	fun `retorna contrato proprio para usuario sem permissao`() {
		mockMvc.get("/test/security/admin")
			.andExpect {
				status { isForbidden() }
				jsonPath("$.status") { value(403) }
				jsonPath("$.code") { value(ApiError.SEC_001.code) }
				jsonPath("$.message") { value(ApiError.SEC_001.message) }
				jsonPath("$.path") { value("/test/security/admin") }
				jsonPath("$.timestamp") { exists() }
			}
	}

	@RestController
	class SecurityTestController {
		@GetMapping("/test/security/protected")
		fun protectedResource(): Map<String, String> = mapOf("status" to "ok")

		@PreAuthorize("hasRole('ADMIN')")
		@GetMapping("/test/security/admin")
		fun adminResource(): Map<String, String> = mapOf("status" to "ok")
	}
}
