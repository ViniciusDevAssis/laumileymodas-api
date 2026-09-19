package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.*
import com.viniciusdevassis.laumileymodas.application.port.account.AccountRepository
import com.viniciusdevassis.laumileymodas.application.port.security.*
import com.viniciusdevassis.laumileymodas.domain.account.RefreshToken
import com.viniciusdevassis.laumileymodas.infrastructure.security.secureHash
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RefreshAccessTokenUseCase(private val issuer: TokenIssuer, private val tokens: RefreshTokenRepository, private val accounts: AccountRepository, private val ids: IdGenerator, private val clock: ClockProvider) {
	@Transactional(noRollbackFor = [ApplicationException::class]) fun execute(raw: String): TokenPair {
		val parsed = issuer.parseRefresh(raw); val now = clock.now()
		val current = tokens.findByJtiHashForUpdate(secureHash(parsed.jti)) ?: throw ApplicationException(AuthError.INVALID_REFRESH_TOKEN)
		if (current.consumedAt != null) { tokens.revokeFamily(current.familyId, now); throw ApplicationException(AuthError.REFRESH_REUSE) }
		if (!current.isActive(now)) throw ApplicationException(AuthError.INVALID_REFRESH_TOKEN)
		val account = accounts.findById(current.accountId)?.takeIf { it.enabled } ?: throw ApplicationException(AuthError.ACCOUNT_DISABLED)
		val access = issuer.issueAccess(account.id, account.role); val refresh = issuer.issueRefresh(account.id, current.familyId)
		val successor = RefreshToken(ids.newId(), account.id, current.familyId, secureHash(refresh.jti), refresh.expiresAt, createdAt = now)
		tokens.save(successor); tokens.save(current.consume(now, successor.id))
		return TokenPair(account.id, access.value, access.expiresAt, refresh.value, refresh.expiresAt)
	}
}

