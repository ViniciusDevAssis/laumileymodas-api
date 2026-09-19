package com.viniciusdevassis.laumileymodas.integration.http

import com.viniciusdevassis.laumileymodas.integration.support.PostgresIntegrationTest
import com.viniciusdevassis.laumileymodas.presentation.security.RestAccessDeniedHandler
import com.viniciusdevassis.laumileymodas.presentation.security.RestAuthenticationEntryPoint
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiErrorResponseTestConfiguration::class)
class ApiErrorResponseIntegrationTest : PostgresIntegrationTest() {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Test
	fun `retorna erros de validacao no contrato unico`() {
		mockMvc.post("/test/validation") {
			contentType = MediaType.APPLICATION_JSON
			content = "{}"
		}
			.andExpect {
				status { isBadRequest() }
				content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
				jsonPath("$.timestamp") { exists() }
				jsonPath("$.status") { value(400) }
				jsonPath("$.code") { value("VALIDATION_001") }
				jsonPath("$.message") { value("Um ou mais campos são inválidos.") }
				jsonPath("$.path") { value("/test/validation") }
				jsonPath("$.errors[0].field") { value("name") }
				jsonPath("$.errors[0].code") { value("VALIDATION_001") }
			}
	}

	@Test
	fun `retorna 401 no contrato unico para requisicao nao autenticada`() {
		mockMvc.get("/test/authenticated")
			.andExpect {
				status { isUnauthorized() }
				content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
				jsonPath("$.status") { value(401) }
				jsonPath("$.code") { value("SECURITY_001") }
				jsonPath("$.message") { value("Autenticação necessária ou inválida.") }
				jsonPath("$.path") { value("/test/authenticated") }
				jsonPath("$.errors") { doesNotExist() }
			}
	}

	@Test
	@WithMockUser(roles = ["CLIENT"])
	fun `retorna 403 no contrato unico para papel sem permissao`() {
		mockMvc.get("/test/admin")
			.andExpect {
				status { isForbidden() }
				content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
				jsonPath("$.status") { value(403) }
				jsonPath("$.code") { value("SECURITY_002") }
				jsonPath("$.message") { value("Você não possui permissão para realizar esta operação.") }
				jsonPath("$.path") { value("/test/admin") }
			}
	}

	@Test
	fun `retorna fallback 500 sem expor detalhes internos`() {
		mockMvc.get("/test/fallback")
			.andExpect {
				status { isInternalServerError() }
				content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
				jsonPath("$.status") { value(500) }
				jsonPath("$.code") { value("INTERNAL_001") }
				jsonPath("$.message") { value("Ocorreu um erro interno inesperado.") }
				jsonPath("$.message") { value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo"))) }
				jsonPath("$.path") { value("/test/fallback") }
			}
	}
}

@TestConfiguration(proxyBeanMethods = false)
class ApiErrorResponseTestConfiguration {

	@Bean
	fun apiErrorTestController(): ApiErrorTestController = ApiErrorTestController()

	@Bean
	fun testSecurityFilterChain(
		http: HttpSecurity,
		authenticationEntryPoint: RestAuthenticationEntryPoint,
		accessDeniedHandler: RestAccessDeniedHandler,
	): SecurityFilterChain {
		http.csrf { it.disable() }
		http.authorizeHttpRequests {
			it.requestMatchers("/test/validation", "/test/fallback").permitAll()
				.requestMatchers("/test/admin").hasRole("ADMIN")
				.requestMatchers("/test/authenticated").authenticated()
				.anyRequest().denyAll()
		}
		http.exceptionHandling {
			it.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
		}
		return http.build()
	}
}

@RestController
class ApiErrorTestController {

	@PostMapping("/test/validation")
	fun validate(@Valid @RequestBody request: ApiErrorValidationRequest): Map<String, String> =
		mapOf("name" to request.name)

	@GetMapping("/test/authenticated")
	fun authenticated(): Map<String, String> = mapOf("status" to "authenticated")

	@GetMapping("/test/admin")
	fun admin(): Map<String, String> = mapOf("status" to "admin")

	@GetMapping("/test/fallback")
	fun fallback(): Nothing = throw IllegalStateException("segredo interno que não pode vazar")
}

data class ApiErrorValidationRequest(
	@field:NotBlank
	val name: String = "",
)
