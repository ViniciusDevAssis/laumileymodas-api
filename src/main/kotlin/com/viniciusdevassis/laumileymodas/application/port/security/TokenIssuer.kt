package com.viniciusdevassis.laumileymodas.application.port.security

import com.viniciusdevassis.laumileymodas.domain.account.AccountRole
import java.time.Instant
import java.util.UUID

data class IssuedToken(val value: String, val jti: String, val expiresAt: Instant)
data class ParsedRefreshToken(val accountId: UUID, val familyId: UUID, val jti: String, val expiresAt: Instant)

interface TokenIssuer {
	fun issueAccess(accountId: UUID, role: AccountRole): IssuedToken
	fun issueRefresh(accountId: UUID, familyId: UUID): IssuedToken
	fun parseRefresh(token: String): ParsedRefreshToken
}
