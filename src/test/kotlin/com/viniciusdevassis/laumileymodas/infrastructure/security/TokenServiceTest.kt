package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.domain.enums.Role
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.oauth2.jwt.BadJwtException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals

class TokenServiceTest {
	private val clock = MutableClock(Instant.parse("2026-09-20T12:00:00Z"))
	private val service = TokenService(
		jwtSecret = "test-secret-with-at-least-256-bits-for-hmac-validation",
		issuer = "laumiley-test",
		audience = "laumiley-api-test",
		accessTokenTtl = Duration.ofMinutes(15),
		clock = clock,
	)

	@Test
	fun `cria e valida access token HMAC com issuer audience subject role e expiracao`() {
		val accountId = UUID.randomUUID()

		val token = service.createAccessToken(accountId, Role.CLIENT)
		val jwt = service.validate(token)

		assertEquals(accountId, service.accountId(jwt))
		assertEquals(Role.CLIENT, service.role(jwt))
		assertEquals("laumiley-test", jwt.getClaimAsString("iss"))
		assertEquals(listOf("laumiley-api-test"), jwt.audience)
		assertEquals(clock.instant().plus(Duration.ofMinutes(15)), jwt.expiresAt)
	}

	@Test
	fun `rejeita token expirado`() {
		val token = service.createAccessToken(UUID.randomUUID(), Role.ADMIN)

		clock.instant = clock.instant().plus(Duration.ofMinutes(16))

		assertThrows<BadJwtException> {
			service.validate(token)
		}
	}

	@Test
	fun `rejeita token com audience diferente`() {
		val token = service.createAccessToken(UUID.randomUUID(), Role.CLIENT)
		val otherAudienceService = TokenService(
			jwtSecret = "test-secret-with-at-least-256-bits-for-hmac-validation",
			issuer = "laumiley-test",
			audience = "other-audience",
			accessTokenTtl = Duration.ofMinutes(15),
			clock = clock,
		)

		assertThrows<BadJwtException> {
			otherAudienceService.validate(token)
		}
	}

	@Test
	fun `exige segredo com pelo menos 256 bits`() {
		assertThrows<IllegalArgumentException> {
			TokenService(
				jwtSecret = "short-secret",
				issuer = "laumiley-test",
				audience = "laumiley-api-test",
				accessTokenTtl = Duration.ofMinutes(15),
				clock = clock,
			)
		}
	}

	private class MutableClock(var instant: Instant) : Clock() {
		override fun getZone(): ZoneId = ZoneId.of("UTC")
		override fun withZone(zone: ZoneId): Clock = this
		override fun instant(): Instant = instant
	}
}

