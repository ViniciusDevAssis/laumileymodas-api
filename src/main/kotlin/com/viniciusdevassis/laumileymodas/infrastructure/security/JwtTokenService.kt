package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.application.auth.AuthError
import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.ClockProvider
import com.viniciusdevassis.laumileymodas.application.port.security.*
import com.viniciusdevassis.laumileymodas.domain.account.AccountRole
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Qualifier
import java.util.UUID

@Service
class JwtTokenService(
	private val encoder: JwtEncoder,
	@Qualifier("refreshJwtDecoder") private val decoder: JwtDecoder,
	private val properties: JwtProperties,
	private val clock: ClockProvider,
) : TokenIssuer {
	override fun issueAccess(accountId: UUID, role: AccountRole): IssuedToken {
		val now = clock.now(); val expires = now.plus(properties.accessTokenTtl); val jti = UUID.randomUUID().toString()
		return IssuedToken(encode(accountId, jti, now, expires, properties.accessAudience, "access", mapOf("role" to role.name)), jti, expires)
	}

	override fun issueRefresh(accountId: UUID, familyId: UUID): IssuedToken {
		val now = clock.now(); val expires = now.plus(properties.refreshTokenTtl); val jti = UUID.randomUUID().toString()
		return IssuedToken(encode(accountId, jti, now, expires, properties.refreshAudience, "refresh", mapOf("family_id" to familyId.toString())), jti, expires)
	}

	override fun parseRefresh(token: String): ParsedRefreshToken = try {
		val jwt = decoder.decode(token)
		if (jwt.getClaimAsString("iss") != properties.issuer || !jwt.audience.contains(properties.refreshAudience) || jwt.getClaimAsString("typ") != "refresh" || jwt.expiresAt == null || !clock.now().isBefore(jwt.expiresAt)) throw IllegalArgumentException()
		ParsedRefreshToken(UUID.fromString(jwt.subject), UUID.fromString(jwt.getClaimAsString("family_id")), jwt.id, jwt.expiresAt!!)
	} catch (exception: Exception) { throw ApplicationException(AuthError.INVALID_REFRESH_TOKEN, exception) }

	private fun encode(accountId: UUID, jti: String, issuedAt: java.time.Instant, expiresAt: java.time.Instant, audience: String, type: String, extra: Map<String, Any>): String {
		val claims = JwtClaimsSet.builder().issuer(properties.issuer).subject(accountId.toString()).audience(listOf(audience)).issuedAt(issuedAt).expiresAt(expiresAt).id(jti).claim("typ", type).apply { extra.forEach { (k, v) -> claim(k, v) } }.build()
		val header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.keyId).build()
		return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
	}
}
