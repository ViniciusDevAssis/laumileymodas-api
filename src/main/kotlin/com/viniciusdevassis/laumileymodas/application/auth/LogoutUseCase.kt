package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.port.ClockProvider
import com.viniciusdevassis.laumileymodas.application.port.security.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service class LogoutUseCase(private val issuer: TokenIssuer, private val tokens: RefreshTokenRepository, private val clock: ClockProvider) {
	@Transactional fun execute(raw: String) { val parsed = issuer.parseRefresh(raw); tokens.revokeFamily(parsed.familyId, clock.now()) }
}

