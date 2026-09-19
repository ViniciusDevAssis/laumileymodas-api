package com.viniciusdevassis.laumileymodas.application.port.security

import com.viniciusdevassis.laumileymodas.domain.account.OAuthHandoff

interface OAuthHandoffRepository {
	fun findByHandleHashForUpdate(hash: String): OAuthHandoff?
	fun save(handoff: OAuthHandoff): OAuthHandoff
}
