package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthenticationRateLimiterTest {
	private val clock = MutableClock(Instant.parse("2026-09-20T12:00:00Z"))
	private val limiter = AuthenticationRateLimiter(
		maxFailures = 3,
		window = Duration.ofMinutes(15),
		clock = clock,
	)

	@Test
	fun `bloqueia apos limite de falhas na janela`() {
		limiter.recordFailure("cliente@example.com")
		limiter.recordFailure("cliente@example.com")
		assertFalse(limiter.isBlocked("cliente@example.com"))

		limiter.recordFailure("cliente@example.com")

		assertTrue(limiter.isBlocked("cliente@example.com"))
	}

	@Test
	fun `sucesso limpa falhas e nao bloqueia fluxo normal`() {
		limiter.recordFailure("cliente@example.com")
		limiter.recordFailure("cliente@example.com")

		limiter.recordSuccess("cliente@example.com")

		assertFalse(limiter.isBlocked("cliente@example.com"))
	}

	@Test
	fun `janela expirada libera novas tentativas`() {
		limiter.recordFailure("cliente@example.com")
		limiter.recordFailure("cliente@example.com")
		limiter.recordFailure("cliente@example.com")
		assertTrue(limiter.isBlocked("cliente@example.com"))

		clock.instant = clock.instant().plus(Duration.ofMinutes(16))

		assertFalse(limiter.isBlocked("cliente@example.com"))
	}

	private class MutableClock(var instant: Instant) : Clock() {
		override fun getZone(): ZoneId = ZoneId.of("UTC")
		override fun withZone(zone: ZoneId): Clock = this
		override fun instant(): Instant = instant
	}
}
