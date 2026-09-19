package com.viniciusdevassis.laumileymodas.presentation.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.viniciusdevassis.laumileymodas.presentation.common.error.ApiErrorResponse
import com.viniciusdevassis.laumileymodas.presentation.common.error.PresentationError
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.OffsetDateTime

@Component
class RestAccessDeniedHandler(
	private val objectMapper: ObjectMapper,
	private val clock: Clock,
) : AccessDeniedHandler {

	override fun handle(
		request: HttpServletRequest,
		response: HttpServletResponse,
		accessDeniedException: AccessDeniedException,
	) {
		response.status = HttpServletResponse.SC_FORBIDDEN
		response.characterEncoding = StandardCharsets.UTF_8.name()
		response.contentType = MediaType.APPLICATION_JSON_VALUE
		objectMapper.writeValue(
			response.outputStream,
			ApiErrorResponse(
				timestamp = OffsetDateTime.now(clock),
				status = HttpServletResponse.SC_FORBIDDEN,
				code = PresentationError.ACCESS_DENIED.code,
				message = PresentationError.ACCESS_DENIED.message,
				path = request.requestURI,
				traceId = MDC.get("traceId"),
			),
		)
	}
}
