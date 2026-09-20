package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtIssuerValidator
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

@Service
class TokenService(
	@Value("\${laumiley.security.jwt.secret}") jwtSecret: String,
	@Value("\${laumiley.security.jwt.issuer}") private val issuer: String,
	@Value("\${laumiley.security.jwt.audience}") private val audience: String,
	@Value("\${laumiley.security.jwt.access-token-ttl}") private val accessTokenTtl: Duration,
	private val clock: Clock,
) {
	private val secretBytes = jwtSecret.toByteArray(Charsets.UTF_8)
	private val secretKey = SecretKeySpec(secretBytes, HMAC_ALGORITHM)
	private val decoder: JwtDecoder = NimbusJwtDecoder
		.withSecretKey(secretKey)
		.macAlgorithm(MacAlgorithm.HS256)
		.build()
		.also { it.setJwtValidator(jwtValidator()) }

	init {
		require(secretBytes.size >= MIN_SECRET_BYTES) {
			"O segredo JWT deve possuir pelo menos 256 bits."
		}
		require(issuer.isNotBlank()) { "Issuer JWT deve ser configurado." }
		require(audience.isNotBlank()) { "Audience JWT deve ser configurado." }
		require(!accessTokenTtl.isZero && !accessTokenTtl.isNegative) { "TTL do access token deve ser positivo." }
	}

	fun createAccessToken(accountId: UUID, role: Role): String {
		val now = Instant.now(clock)
		val claims = JWTClaimsSet.Builder()
			.issuer(issuer)
			.audience(audience)
			.issueTime(Date.from(now))
			.expirationTime(Date.from(now.plus(accessTokenTtl)))
			.subject(accountId.toString())
			.claim(ROLE_CLAIM, role.name)
			.build()
		val jwt = SignedJWT(JWSHeader(JWSAlgorithm.HS256), claims)
		jwt.sign(MACSigner(secretBytes))

		return jwt.serialize()
	}

	fun validate(accessToken: String): Jwt {
		val jwt = decoder.decode(accessToken)
		val now = Instant.now(clock)

		if (jwt.getClaimAsString("iss") != issuer) {
			throw BadJwtException("Issuer inválido.")
		}
		if (!jwt.audience.contains(audience)) {
			throw BadJwtException("Audience inválida.")
		}
		if (jwt.expiresAt == null || !jwt.expiresAt!!.isAfter(now)) {
			throw BadJwtException("Token expirado.")
		}
		if (jwt.subject.isNullOrBlank()) {
			throw BadJwtException("Subject ausente.")
		}
		if (jwt.getClaimAsString(ROLE_CLAIM).isNullOrBlank()) {
			throw BadJwtException("Role ausente.")
		}

		return jwt
	}

	fun accountId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)

	fun role(jwt: Jwt): Role = Role.valueOf(jwt.getClaimAsString(ROLE_CLAIM))

	private fun jwtValidator(): DelegatingOAuth2TokenValidator<Jwt> {
		val timestampValidator = JwtTimestampValidator(Duration.ZERO).also { it.setClock(clock) }
		val audienceValidator = OAuth2TokenValidator<Jwt> { jwt ->
			if (jwt.audience.contains(audience)) {
				OAuth2TokenValidatorResult.success()
			} else {
				OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "Audience inválida.", null))
			}
		}
				val issuerValidator = OAuth2TokenValidator<Jwt> { jwt ->
			if (jwt.getClaimAsString("iss") == issuer) {
				OAuth2TokenValidatorResult.success()
			} else {
				OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token", "Issuer inválido.", null))
			}
		}
		return DelegatingOAuth2TokenValidator(issuerValidator, timestampValidator, audienceValidator)
	}

	companion object {
		private const val HMAC_ALGORITHM = "HmacSHA256"
		private const val MIN_SECRET_BYTES = 32
		private const val ROLE_CLAIM = "role"
	}
}
