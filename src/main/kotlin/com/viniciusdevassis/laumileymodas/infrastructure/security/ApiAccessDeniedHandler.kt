package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.presentation.advice.ApiErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class ApiAccessDeniedHandler(
	private val objectMapper: ObjectMapper,
	private val clock: Clock,
) : AccessDeniedHandler {
	override fun handle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		accessDeniedException: AccessDeniedException,
	) {
		val status = HttpStatus.FORBIDDEN
		response.status = status.value()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		objectMapper.writeValue(
			response.outputStream,
			ApiErrorResponse(
				timestamp = Instant.now(clock),
				status = status.value(),
				code = ApiError.SEC_001.code,
				message = ApiError.SEC_001.message,
				path = request.requestURI,
			),
		)
	}
}
