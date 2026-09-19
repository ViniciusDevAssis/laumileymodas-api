package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("laumiley.security.authentication-rate-limit")
data class AuthenticationRateLimitProperties(val maxFailedAttempts: Int = 5, val window: Duration = Duration.ofMinutes(15))
