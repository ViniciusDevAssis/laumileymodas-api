package com.viniciusdevassis.laumileymodas.application.common

open class ApplicationException(
	val error: ApplicationError,
	cause: Throwable? = null,
) : RuntimeException(error.message, cause)
