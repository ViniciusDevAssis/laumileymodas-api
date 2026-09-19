package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.ClockProvider
import com.viniciusdevassis.laumileymodas.application.port.account.AccountRepository
import com.viniciusdevassis.laumileymodas.application.port.security.OAuthHandoffRepository
import com.viniciusdevassis.laumileymodas.domain.account.OAuthHandoffPurpose
import com.viniciusdevassis.laumileymodas.infrastructure.security.secureHash
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service class ExchangeGoogleHandoffUseCase(private val handoffs: OAuthHandoffRepository, private val accounts: AccountRepository, private val tokenPairs: IssueTokenPairUseCase, private val clock: ClockProvider) {
	@Transactional fun execute(raw: String): TokenPair {
		val now = clock.now(); val handoff = handoffs.findByHandleHashForUpdate(secureHash(raw))
		if (handoff == null || handoff.purpose != OAuthHandoffPurpose.EXISTING_ACCOUNT_LOGIN || !handoff.isUsable(now)) throw ApplicationException(AuthError.INVALID_HANDOFF)
		val account = accounts.findById(handoff.accountId!!) ?: throw ApplicationException(AuthError.INVALID_HANDOFF)
		handoffs.save(handoff.consume(now)); return tokenPairs.issue(account)
	}
}
