package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.presentation.advice.ApiErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class ApiAuthenticationEntryPoint(
	private val objectMapper: ObjectMapper,
	private val clock: Clock,
) : AuthenticationEntryPoint {
	override fun commence(
		request: HttpServletRequest,
		response: HttpServletResponse,
		authException: AuthenticationException,
	) {
		val status = HttpStatus.UNAUTHORIZED
		response.status = status.value()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		objectMapper.writeValue(
			response.outputStream,
			ApiErrorResponse(
				timestamp = Instant.now(clock),
				status = status.value(),
				code = ApiError.AUTH_001.code,
				message = ApiError.AUTH_001.message,
				path = request.requestURI,
			),
		)
	}
}
