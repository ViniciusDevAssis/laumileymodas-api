package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.application.auth.AuthError
import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.ClockProvider
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
@EnableConfigurationProperties(AuthenticationRateLimitProperties::class)
class AuthenticationRateLimiter(private val properties: AuthenticationRateLimitProperties, private val clock: ClockProvider) {
	private data class Attempts(val count: Int, val startedAt: Instant)
	private val failures = ConcurrentHashMap<String, Attempts>()
	fun check(key: String) {
		val value = failures[key] ?: return
		if (!clock.now().isBefore(value.startedAt.plus(properties.window))) failures.remove(key, value)
		else if (value.count >= properties.maxFailedAttempts) throw ApplicationException(AuthError.RATE_LIMITED)
	}
	fun failed(key: String) { val now = clock.now(); failures.compute(key) { _, old -> if (old == null || !now.isBefore(old.startedAt.plus(properties.window))) Attempts(1, now) else old.copy(count = old.count + 1) } }
	fun succeeded(key: String) { failures.remove(key) }
}
