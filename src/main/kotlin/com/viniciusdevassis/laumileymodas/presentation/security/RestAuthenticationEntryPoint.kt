package com.viniciusdevassis.laumileymodas.presentation.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.viniciusdevassis.laumileymodas.presentation.common.error.ApiErrorResponse
import com.viniciusdevassis.laumileymodas.presentation.common.error.PresentationError
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.OffsetDateTime

@Component
class RestAuthenticationEntryPoint(
	private val objectMapper: ObjectMapper,
	private val clock: Clock,
) : AuthenticationEntryPoint {

	override fun commence(
		request: HttpServletRequest,
		response: HttpServletResponse,
		authException: AuthenticationException,
	) {
		response.status = HttpServletResponse.SC_UNAUTHORIZED
		response.characterEncoding = StandardCharsets.UTF_8.name()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		objectMapper.writeValue(
			response.outputStream,
			ApiErrorResponse(
				timestamp = OffsetDateTime.now(clock),
				status = HttpServletResponse.SC_UNAUTHORIZED,
				code = PresentationError.AUTHENTICATION_REQUIRED.code,
				message = PresentationError.AUTHENTICATION_REQUIRED.message,
				path = request.requestURI,
				traceId = MDC.get("traceId"),
			),
		)
	}
}
