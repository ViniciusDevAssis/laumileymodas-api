package com.viniciusdevassis.laumileymodas.application.port.security

import com.viniciusdevassis.laumileymodas.domain.account.RefreshToken
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository {
	fun findByJtiHashForUpdate(hash: String): RefreshToken?
	fun save(token: RefreshToken): RefreshToken
	fun revokeFamily(familyId: UUID, at: Instant)
}
