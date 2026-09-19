package com.viniciusdevassis.laumileymodas.application.common

interface ApplicationError {
	val code: String
	val message: String
	val type: Type

	enum class Type {
		INVALID,
		UNAUTHORIZED,
		FORBIDDEN,
		NOT_FOUND,
		CONFLICT,
		TOO_MANY_REQUESTS,
		EXTERNAL_UNAVAILABLE,
	}
}
