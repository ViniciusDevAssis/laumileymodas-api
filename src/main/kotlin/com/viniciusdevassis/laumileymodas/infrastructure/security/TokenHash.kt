package com.viniciusdevassis.laumileymodas.infrastructure.security

import java.security.MessageDigest
import java.util.Base64

fun secureHash(value: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
	MessageDigest.getInstance("SHA-256").digest(value.toByteArray()),
)
