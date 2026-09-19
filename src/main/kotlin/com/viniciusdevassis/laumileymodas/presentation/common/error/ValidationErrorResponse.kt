package com.viniciusdevassis.laumileymodas.presentation.common.error

data class ValidationErrorResponse(
	val field: String,
	val code: String = PresentationError.VALIDATION.code,
	val message: String,
)
