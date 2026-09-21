package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options
import org.springframework.test.web.servlet.post
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
class CsrfAndCorsIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
) : PostgresIntegrationTest() {
	@Test
	fun `csrf disponibiliza cookie frontend legivel e protege refresh logout`() {
		val initialized = mockMvc.get("/auth/csrf").andExpect { status { isNoContent() } }.andReturn()
		val csrfCookie = initialized.response.getCookie("XSRF-TOKEN")
		assertNotNull(csrfCookie)

		mockMvc.post("/auth/refresh") {
			cookie(csrfCookie)
		}.andExpect {
			status { isForbidden() }
			jsonPath("$.code") { value(ApiError.SEC_001.code) }
		}

		mockMvc.post("/auth/refresh") {
			cookie(csrfCookie)
			header("X-XSRF-TOKEN", csrfCookie.value)
		}.andExpect {
			status { isUnauthorized() }
			jsonPath("$.code") { value(ApiError.AUTH_001.code) }
		}

		mockMvc.post("/auth/logout") {
			cookie(csrfCookie)
			header("X-XSRF-TOKEN", csrfCookie.value)
		}.andExpect {
			status { isNoContent() }
		}
	}

	@Test
	fun `cors aceita apenas origem configurada e credenciais`() {
		mockMvc.options("/products") {
			header("Origin", "http://localhost:3000")
			header("Access-Control-Request-Method", "GET")
		}.andExpect {
			status { isOk() }
			header { string("Access-Control-Allow-Origin", "http://localhost:3000") }
			header { string("Access-Control-Allow-Credentials", "true") }
		}

		mockMvc.options("/products") {
			header("Origin", "https://evil.example")
			header("Access-Control-Request-Method", "GET")
		}.andExpect {
			status { isForbidden() }
			header { doesNotExist("Access-Control-Allow-Origin") }
		}
	}
}
