package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {
	fun findByTokenHash(tokenHash: String): RefreshToken?
	fun findByFamilyId(familyId: UUID): List<RefreshToken>
}
