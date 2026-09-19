package com.viniciusdevassis.laumileymodas.domain.common

open class DomainException(
	val error: DomainError,
	cause: Throwable? = null,
) : RuntimeException(error.message, cause)
