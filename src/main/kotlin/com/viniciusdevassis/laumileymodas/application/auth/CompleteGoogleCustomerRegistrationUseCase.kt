package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.*
import com.viniciusdevassis.laumileymodas.application.port.account.*
import com.viniciusdevassis.laumileymodas.application.port.customer.CustomerRepository
import com.viniciusdevassis.laumileymodas.application.port.security.OAuthHandoffRepository
import com.viniciusdevassis.laumileymodas.domain.account.*
import com.viniciusdevassis.laumileymodas.domain.customer.Customer
import com.viniciusdevassis.laumileymodas.infrastructure.security.secureHash
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service class CompleteGoogleCustomerRegistrationUseCase(private val handoffs: OAuthHandoffRepository, private val accounts: AccountRepository, private val identities: ExternalIdentityRepository, private val customers: CustomerRepository, private val tokens: IssueTokenPairUseCase, private val ids: IdGenerator, private val clock: ClockProvider) {
	@Transactional fun execute(raw: String, firstName: String, lastName: String, phone: String): TokenPair {
		val now = clock.now(); val handoff = handoffs.findByHandleHashForUpdate(secureHash(raw))
		if (handoff == null || handoff.purpose != OAuthHandoffPurpose.CLIENT_REGISTRATION || !handoff.isUsable(now)) throw ApplicationException(AuthError.INVALID_HANDOFF)
		if (accounts.findByNormalizedEmail(handoff.verifiedEmail) != null) throw ApplicationException(AuthError.GOOGLE_EMAIL_CONFLICT)
		val account = accounts.save(Account(ids.newId(), handoff.verifiedEmail.trim(), null, AccountRole.CLIENT, true, now, now))
		identities.save(AccountExternalIdentity(ids.newId(), account.id, ExternalIdentityProvider.GOOGLE, handoff.providerSubject, handoff.verifiedEmail, now, now))
		customers.save(Customer(ids.newId(), account.id, firstName.trim(), lastName.trim(), Customer.normalizePhone(phone), false, null, now, now))
		handoffs.save(handoff.consume(now)); return tokens.issue(account)
	}
}
