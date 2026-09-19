package com.viniciusdevassis.laumileymodas.domain.common

interface DomainError {
	val code: String
	val message: String
	val type: Type

	enum class Type {
		INVALID,
		NOT_FOUND,
		CONFLICT,
	}
}
