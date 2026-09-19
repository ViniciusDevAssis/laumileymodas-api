package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.account.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.security.AuthenticationRateLimiter
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class LoginWithPasswordUseCase(private val accounts: AccountRepository, private val encoder: PasswordEncoder, private val limiter: AuthenticationRateLimiter, private val tokens: IssueTokenPairUseCase) {
	fun execute(email: String, password: String, origin: String): TokenPair {
		val key = "$origin|${email.trim().lowercase()}"; limiter.check(key)
		val account = accounts.findByNormalizedEmail(email)
		if (account == null || account.passwordHash == null || !account.enabled || !encoder.matches(password, account.passwordHash)) {
			limiter.failed(key); throw ApplicationException(AuthError.INVALID_CREDENTIALS)
		}
		limiter.succeeded(key); return tokens.issue(account)
	}
}

