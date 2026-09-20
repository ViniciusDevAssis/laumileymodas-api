package com.viniciusdevassis.laumileymodas.domain.exceptions

import org.springframework.http.HttpStatus

class ApiException(
	val error: ApiError,
	val status: HttpStatus,
	override val message: String = error.message,
) : RuntimeException(message)
