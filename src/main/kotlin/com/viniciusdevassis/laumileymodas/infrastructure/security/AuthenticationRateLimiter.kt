package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class AuthenticationRateLimiter(
	@Value("\${laumiley.security.rate-limit.max-failures}") private val maxFailures: Int,
	@Value("\${laumiley.security.rate-limit.window}") private val window: Duration,
	private val clock: Clock,
) {
	private val attempts = ConcurrentHashMap<String, AttemptCounter>()

	init {
		require(maxFailures > 0) { "Limite de falhas de autenticação deve ser positivo." }
		require(!window.isZero && !window.isNegative) { "Janela do rate limit deve ser positiva." }
	}

	fun isBlocked(key: String): Boolean {
		val normalizedKey = normalize(key)
		val current = attempts[normalizedKey] ?: return false
		if (current.isExpired(now())) {
			attempts.remove(normalizedKey, current)
			return false
		}
		return current.failures >= maxFailures
	}

	fun recordFailure(key: String) {
		val normalizedKey = normalize(key)
		val now = now()
		attempts.compute(normalizedKey) { _, current ->
			if (current == null || current.isExpired(now)) {
				AttemptCounter(failures = 1, expiresAt = now.plus(window))
			} else {
				current.copy(failures = current.failures + 1)
			}
		}
	}

	fun recordSuccess(key: String) {
		attempts.remove(normalize(key))
	}

	private fun now(): Instant = Instant.now(clock)

	private fun normalize(key: String): String = key.trim().lowercase()

	private data class AttemptCounter(
		val failures: Int,
		val expiresAt: Instant,
	) {
		fun isExpired(now: Instant): Boolean = !expiresAt.isAfter(now)
	}
}
