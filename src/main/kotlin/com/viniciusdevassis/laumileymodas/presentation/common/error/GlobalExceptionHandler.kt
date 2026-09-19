package com.viniciusdevassis.laumileymodas.presentation.common.error

import com.viniciusdevassis.laumileymodas.application.common.ApplicationError
import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.domain.common.DomainError
import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Clock
import java.time.OffsetDateTime

@RestControllerAdvice
class GlobalExceptionHandler(
	private val clock: Clock,
) {

	private val logger = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(MethodArgumentNotValidException::class)
	fun handleMethodArgumentNotValid(
		exception: MethodArgumentNotValidException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		val errors = exception.bindingResult.fieldErrors
			.map {
				ValidationErrorResponse(
					field = it.field,
					message = it.defaultMessage ?: "Valor inválido.",
				)
			}
			.sortedBy(ValidationErrorResponse::field)

		return validationResponse(request, errors)
	}

	@ExceptionHandler(BindException::class)
	fun handleBindException(
		exception: BindException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		val errors = exception.bindingResult.fieldErrors
			.map {
				ValidationErrorResponse(
					field = it.field,
					message = it.defaultMessage ?: "Valor inválido.",
				)
			}
			.sortedBy(ValidationErrorResponse::field)

		return validationResponse(request, errors)
	}

	@ExceptionHandler(ConstraintViolationException::class)
	fun handleConstraintViolation(
		exception: ConstraintViolationException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		val errors = exception.constraintViolations
			.map {
				ValidationErrorResponse(
					field = it.propertyPath.toString(),
					message = it.message,
				)
			}
			.sortedBy(ValidationErrorResponse::field)

		return validationResponse(request, errors)
	}

	@ExceptionHandler(
		HttpMessageNotReadableException::class,
		MissingServletRequestParameterException::class,
	)
	fun handleInvalidRequest(
		exception: Exception,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		val field = (exception as? MissingServletRequestParameterException)?.parameterName ?: "request"
		return validationResponse(
			request,
			listOf(ValidationErrorResponse(field = field, message = "Valor ausente ou inválido.")),
		)
	}

	@ExceptionHandler(DomainException::class)
	fun handleDomainException(
		exception: DomainException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> = errorResponse(
		status = exception.error.type.toHttpStatus(),
		code = exception.error.code,
		message = exception.error.message,
		request = request,
	)

	@ExceptionHandler(ApplicationException::class)
	fun handleApplicationException(
		exception: ApplicationException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> = errorResponse(
		status = exception.error.type.toHttpStatus(),
		code = exception.error.code,
		message = exception.error.message,
		request = request,
	)

	@ExceptionHandler(AuthenticationException::class)
	fun handleAuthenticationException(
		exception: AuthenticationException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> = errorResponse(
		status = HttpStatus.UNAUTHORIZED,
		code = PresentationError.AUTHENTICATION_REQUIRED.code,
		message = PresentationError.AUTHENTICATION_REQUIRED.message,
		request = request,
	)

	@ExceptionHandler(AccessDeniedException::class)
	fun handleAccessDeniedException(
		exception: AccessDeniedException,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> = errorResponse(
		status = HttpStatus.FORBIDDEN,
		code = PresentationError.ACCESS_DENIED.code,
		message = PresentationError.ACCESS_DENIED.message,
		request = request,
	)

	@ExceptionHandler(Exception::class)
	fun handleUnexpectedException(
		exception: Exception,
		request: HttpServletRequest,
	): ResponseEntity<ApiErrorResponse> {
		logger.error(
			"Erro inesperado ao processar {}: tipo={}, traceId={}",
			request.requestURI,
			exception.javaClass.simpleName,
			MDC.get("traceId"),
		)
		return errorResponse(
			status = HttpStatus.INTERNAL_SERVER_ERROR,
			code = PresentationError.INTERNAL.code,
			message = PresentationError.INTERNAL.message,
			request = request,
		)
	}

	private fun validationResponse(
		request: HttpServletRequest,
		errors: List<ValidationErrorResponse>,
	): ResponseEntity<ApiErrorResponse> = errorResponse(
		status = HttpStatus.BAD_REQUEST,
		code = PresentationError.VALIDATION.code,
		message = PresentationError.VALIDATION.message,
		request = request,
		errors = errors,
	)

	private fun errorResponse(
		status: HttpStatus,
		code: String,
		message: String,
		request: HttpServletRequest,
		errors: List<ValidationErrorResponse>? = null,
	): ResponseEntity<ApiErrorResponse> = ResponseEntity.status(status).body(
		ApiErrorResponse(
			timestamp = OffsetDateTime.now(clock),
			status = status.value(),
			code = code,
			message = message,
			path = request.requestURI,
			traceId = MDC.get("traceId"),
			errors = errors,
		),
	)

	private fun DomainError.Type.toHttpStatus(): HttpStatus = when (this) {
		DomainError.Type.INVALID -> HttpStatus.UNPROCESSABLE_ENTITY
		DomainError.Type.NOT_FOUND -> HttpStatus.NOT_FOUND
		DomainError.Type.CONFLICT -> HttpStatus.CONFLICT
	}

	private fun ApplicationError.Type.toHttpStatus(): HttpStatus = when (this) {
		ApplicationError.Type.INVALID -> HttpStatus.BAD_REQUEST
		ApplicationError.Type.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
		ApplicationError.Type.FORBIDDEN -> HttpStatus.FORBIDDEN
		ApplicationError.Type.NOT_FOUND -> HttpStatus.NOT_FOUND
		ApplicationError.Type.CONFLICT -> HttpStatus.CONFLICT
		ApplicationError.Type.TOO_MANY_REQUESTS -> HttpStatus.TOO_MANY_REQUESTS
		ApplicationError.Type.EXTERNAL_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE
	}
}
