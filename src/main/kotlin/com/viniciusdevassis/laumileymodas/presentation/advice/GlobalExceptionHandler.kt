package com.viniciusdevassis.laumileymodas.presentation.advice

import com.fasterxml.jackson.annotation.JsonInclude
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Clock
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ApiErrorResponse(
	val timestamp: Instant,
	val status: Int,
	val code: String,
	val message: String,
	val path: String,
	val fieldErrors: Map<String, String>? = null,
)

@RestControllerAdvice
class GlobalExceptionHandler(
	private val clock: Clock,
) {
	@ExceptionHandler(ApiException::class)
	fun handleApiException(
		exception: ApiException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		errorResponse(exception.status, exception.error, request.requestURI, exception.message)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleValidationException(
		exception: MethodArgumentNotValidException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		val fieldErrors = exception.bindingResult.fieldErrors
			.associate { it.field to (it.defaultMessage ?: ApiError.VALIDATION_001.message) }

		return errorResponse(
			status = HttpStatus.BAD_REQUEST,
			error = ApiError.VALIDATION_001,
			path = request.requestURI,
			message = ApiError.VALIDATION_001.message,
			fieldErrors = fieldErrors,
		)
	}

	@ExceptionHandler(AccessDeniedException::class, AuthorizationDeniedException::class)
	fun handleAccessDeniedException(
		exception: RuntimeException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		errorResponse(HttpStatus.FORBIDDEN, ApiError.SEC_001, request.requestURI)

	@ExceptionHandler(Exception::class)
	fun handleUnexpectedException(
		exception: Exception,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> =
		errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ApiError.INTERNAL_001, request.requestURI)

	private fun errorResponse(
		status: HttpStatus,
		error: ApiError,
		path: String,
		message: String = error.message,
		fieldErrors: Map<String, String>? = null,
	): ResponseEntity<ApiErrorResponse> =
		ResponseEntity.status(status).body(
			ApiErrorResponse(
				timestamp = Instant.now(clock),
				status = status.value(),
				code = error.code,
				message = message,
				path = path,
				fieldErrors = fieldErrors,
			),
		)
}
