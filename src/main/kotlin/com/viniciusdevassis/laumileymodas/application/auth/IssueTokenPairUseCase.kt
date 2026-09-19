package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.port.*
import com.viniciusdevassis.laumileymodas.application.port.security.*
import com.viniciusdevassis.laumileymodas.domain.account.*
import com.viniciusdevassis.laumileymodas.infrastructure.security.secureHash
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class TokenPair(val accountId: java.util.UUID, val accessToken: String, val accessExpiresAt: java.time.Instant, val refreshToken: String, val refreshExpiresAt: java.time.Instant)

@Service
class IssueTokenPairUseCase(private val issuer: TokenIssuer, private val refreshTokens: RefreshTokenRepository, private val ids: IdGenerator, private val clock: ClockProvider) {
	@Transactional fun issue(account: Account): TokenPair = issue(account, ids.newId())

	@Transactional fun issue(account: Account, familyId: java.util.UUID): TokenPair {
		val access = issuer.issueAccess(account.id, account.role); val refresh = issuer.issueRefresh(account.id, familyId)
		refreshTokens.save(RefreshToken(ids.newId(), account.id, familyId, secureHash(refresh.jti), refresh.expiresAt, createdAt = clock.now()))
		return TokenPair(account.id, access.value, access.expiresAt, refresh.value, refresh.expiresAt)
	}
}

