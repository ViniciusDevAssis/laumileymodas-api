package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.*
import com.viniciusdevassis.laumileymodas.application.port.account.*
import com.viniciusdevassis.laumileymodas.application.port.security.OAuthHandoffRepository
import com.viniciusdevassis.laumileymodas.domain.account.*
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.VerifiedGoogleIdentity
import com.viniciusdevassis.laumileymodas.infrastructure.security.secureHash
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.Duration
import java.util.Base64

data class CreatedGoogleHandoff(val rawHandle: String, val purpose: OAuthHandoffPurpose)

@Service
class CompleteGoogleLoginUseCase(private val accounts: AccountRepository, private val identities: ExternalIdentityRepository, private val handoffs: OAuthHandoffRepository, private val ids: IdGenerator, private val clock: ClockProvider, @Value("\${laumiley.security.admin.google-sub}") private val adminSub: String, @Value("\${laumiley.security.oauth-handoff.ttl:10m}") private val ttl: Duration) {
	@Transactional fun execute(identity: VerifiedGoogleIdentity): CreatedGoogleHandoff {
		val now = clock.now(); val linked = identities.findGoogleBySubject(identity.subject)
		val purpose: OAuthHandoffPurpose; val accountId: java.util.UUID?
		if (identity.subject == adminSub) {
			val account = if (linked != null) accounts.findById(linked.accountId)!! else {
				if (accounts.findByNormalizedEmail(identity.email) != null) throw ApplicationException(AuthError.GOOGLE_EMAIL_CONFLICT)
				val admin = accounts.save(Account(ids.newId(), identity.email, null, AccountRole.ADMIN, true, now, now))
				identities.save(AccountExternalIdentity(ids.newId(), admin.id, ExternalIdentityProvider.GOOGLE, identity.subject, identity.email, now, now)); admin
			}
			if (account.role != AccountRole.ADMIN) throw ApplicationException(AuthError.GOOGLE_ADMIN_NOT_ALLOWED)
			purpose = OAuthHandoffPurpose.EXISTING_ACCOUNT_LOGIN; accountId = account.id
		} else if (linked != null) {
			val account = accounts.findById(linked.accountId) ?: throw ApplicationException(AuthError.INVALID_HANDOFF)
			if (account.role == AccountRole.ADMIN) throw ApplicationException(AuthError.GOOGLE_ADMIN_NOT_ALLOWED)
			purpose = OAuthHandoffPurpose.EXISTING_ACCOUNT_LOGIN; accountId = account.id
		} else {
			if (accounts.findByNormalizedEmail(identity.email) != null) throw ApplicationException(AuthError.GOOGLE_EMAIL_CONFLICT)
			purpose = OAuthHandoffPurpose.CLIENT_REGISTRATION; accountId = null
		}
		val raw = ByteArray(32).also(SecureRandom()::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
		handoffs.save(OAuthHandoff(ids.newId(), secureHash(raw), purpose, accountId, identity.subject, identity.email, identity.givenName, identity.familyName, now.plus(ttl), createdAt = now))
		return CreatedGoogleHandoff(raw, purpose)
	}
}
