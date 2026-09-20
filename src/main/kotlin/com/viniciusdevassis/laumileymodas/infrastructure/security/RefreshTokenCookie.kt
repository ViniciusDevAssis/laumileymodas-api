package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

@Component
class RefreshTokenCookie(
	@Value("\${laumiley.security.jwt.refresh-token-ttl}") private val refreshTokenTtl: Duration,
	@Value("\${laumiley.security.cookies.secure}") private val secure: Boolean,
	@Value("\${laumiley.security.cookies.same-site}") private val sameSite: String,
) {
	private val secureRandom = SecureRandom()

	init {
		require(!refreshTokenTtl.isZero && !refreshTokenTtl.isNegative) { "TTL do refresh token deve ser positivo." }
		require(sameSite.isNotBlank()) { "SameSite do cookie deve ser configurado." }
	}

	fun generateToken(): String {
		val bytes = ByteArray(TOKEN_BYTES)
		secureRandom.nextBytes(bytes)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
	}

	fun hash(rawToken: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(Charsets.UTF_8))
		return digest.joinToString(separator = "") { "%02x".format(it) }
	}

	fun create(rawToken: String): ResponseCookie = baseCookie(rawToken)
		.maxAge(refreshTokenTtl)
		.build()

	fun expire(): ResponseCookie = baseCookie("")
		.maxAge(Duration.ZERO)
		.build()

	private fun baseCookie(value: String): ResponseCookie.ResponseCookieBuilder =
		ResponseCookie.from(COOKIE_NAME, value)
			.httpOnly(true)
			.secure(secure)
			.sameSite(sameSite)
			.path(COOKIE_PATH)

	companion object {
		const val COOKIE_NAME = "LAUMILEY_REFRESH"
		private const val COOKIE_PATH = "/api/v1/auth"
		private const val TOKEN_BYTES = 32
	}
}
