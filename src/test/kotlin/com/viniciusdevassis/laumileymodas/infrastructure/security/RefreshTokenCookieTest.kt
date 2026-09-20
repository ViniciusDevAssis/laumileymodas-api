package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RefreshTokenCookieTest {
	private val refreshTokenCookie = RefreshTokenCookie(
		refreshTokenTtl = Duration.ofDays(30),
		secure = true,
		sameSite = "Lax",
	)

	@Test
	fun `gera refresh token opaco aleatorio e hash sha256`() {
		val token = refreshTokenCookie.generateToken()
		val otherToken = refreshTokenCookie.generateToken()

		assertNotEquals(token, otherToken)
		assertTrue(token.length >= 40)
		assertEquals(64, refreshTokenCookie.hash(token).length)
		assertTrue(refreshTokenCookie.hash(token).matches(Regex("^[0-9a-f]{64}$")))
	}

	@Test
	fun `cria cookie HttpOnly com escopo do auth`() {
		val token = refreshTokenCookie.generateToken()

		val cookie = refreshTokenCookie.create(token)

		assertEquals(RefreshTokenCookie.COOKIE_NAME, cookie.name)
		assertEquals(token, cookie.value)
		assertTrue(cookie.isHttpOnly)
		assertTrue(cookie.isSecure)
		assertEquals("Lax", cookie.sameSite)
		assertEquals("/api/v1/auth", cookie.path)
		assertEquals(Duration.ofDays(30), cookie.maxAge)
	}

	@Test
	fun `expira cookie de refresh`() {
		val cookie = refreshTokenCookie.expire()

		assertEquals(RefreshTokenCookie.COOKIE_NAME, cookie.name)
		assertEquals("", cookie.value)
		assertEquals(Duration.ZERO, cookie.maxAge)
	}
}
