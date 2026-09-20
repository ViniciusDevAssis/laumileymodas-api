package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@AutoConfigureMockMvc(addFilters = false)
@Import(ApiErrorIntegrationTest.ErrorTestController::class)
class ApiErrorIntegrationTest(
	@Autowired
	private val mockMvc: MockMvc,
) : PostgresIntegrationTest() {
	@Test
	fun `retorna contrato de erro para validacao`() {
		mockMvc.post("/test/errors/validation") {
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":""}"""
		}.andExpect {
			status { isBadRequest() }
			jsonPath("$.status") { value(400) }
			jsonPath("$.code") { value(ApiError.VALIDATION_001.code) }
			jsonPath("$.message") { value(ApiError.VALIDATION_001.message) }
			jsonPath("$.path") { value("/test/errors/validation") }
			jsonPath("$.timestamp") { exists() }
			jsonPath("$.fieldErrors.name") { exists() }
		}
	}

	@Test
	fun `retorna contrato de erro para excecao conhecida`() {
		mockMvc.get("/test/errors/api-exception")
			.andExpect {
				status { isConflict() }
				jsonPath("$.status") { value(409) }
				jsonPath("$.code") { value(ApiError.VALIDATION_001.code) }
				jsonPath("$.message") { value("Conflito de teste.") }
				jsonPath("$.path") { value("/test/errors/api-exception") }
				jsonPath("$.fieldErrors") { doesNotExist() }
			}
	}

	@Test
	fun `retorna fallback seguro para erro inesperado`() {
		mockMvc.get("/test/errors/unexpected")
			.andExpect {
				status { isInternalServerError() }
				jsonPath("$.status") { value(500) }
				jsonPath("$.code") { value(ApiError.INTERNAL_001.code) }
				jsonPath("$.message") { value(ApiError.INTERNAL_001.message) }
				jsonPath("$.message") { value(not(containsString("detalhe interno"))) }
				jsonPath("$.path") { value("/test/errors/unexpected") }
			}
	}

	@RestController
	class ErrorTestController {
		@PostMapping("/test/errors/validation")
		fun validation(@Valid @RequestBody request: TestRequest): Map<String, String> = mapOf("name" to request.name)

		@GetMapping("/test/errors/api-exception")
		fun apiException() {
			throw ApiException(ApiError.VALIDATION_001, HttpStatus.CONFLICT, "Conflito de teste.")
		}

		@GetMapping("/test/errors/unexpected")
		fun unexpected() {
			throw IllegalStateException("detalhe interno que nao deve vazar")
		}
	}

	data class TestRequest(
		@field:NotBlank
		val name: String,
	)
}
