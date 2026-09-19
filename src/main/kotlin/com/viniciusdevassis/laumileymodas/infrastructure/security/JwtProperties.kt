package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("laumiley.security.jwt")
data class JwtProperties(
	val issuer: String = "laumiley-modas-api",
	val accessAudience: String = "laumiley-api",
	val refreshAudience: String = "laumiley-refresh",
	val accessTokenTtl: Duration = Duration.ofMinutes(15),
	val refreshTokenTtl: Duration = Duration.ofDays(30),
	val privateKey: String = "",
	val publicKey: String = "",
	val keyId: String = "laumiley-v1",
)
