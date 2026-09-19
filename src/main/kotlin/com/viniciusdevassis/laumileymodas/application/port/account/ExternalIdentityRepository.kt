package com.viniciusdevassis.laumileymodas.application.port.account

import com.viniciusdevassis.laumileymodas.domain.account.AccountExternalIdentity

interface ExternalIdentityRepository {
	fun findGoogleBySubject(subject: String): AccountExternalIdentity?
	fun save(identity: AccountExternalIdentity): AccountExternalIdentity
}
