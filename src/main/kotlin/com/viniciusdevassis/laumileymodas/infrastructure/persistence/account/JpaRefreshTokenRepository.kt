package com.viniciusdevassis.laumileymodas.infrastructure.persistence.account

import com.viniciusdevassis.laumileymodas.application.port.security.RefreshTokenRepository
import com.viniciusdevassis.laumileymodas.domain.account.RefreshToken
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Repository
class JpaRefreshTokenRepository(private val em: EntityManager) : RefreshTokenRepository {
	override fun findByJtiHashForUpdate(hash: String): RefreshToken? = em.createQuery("select r from RefreshTokenJpaEntity r where r.jtiHash=:hash", RefreshTokenJpaEntity::class.java).setParameter("hash", hash).setLockMode(LockModeType.PESSIMISTIC_WRITE).resultList.firstOrNull()?.domain()
	@Transactional override fun save(token: RefreshToken): RefreshToken = em.merge(token.jpa()).domain()
	@Transactional override fun revokeFamily(familyId: UUID, at: Instant) { em.createQuery("update RefreshTokenJpaEntity r set r.revokedAt=:at where r.familyId=:family and r.revokedAt is null").setParameter("at", at).setParameter("family", familyId).executeUpdate() }
}

private fun RefreshTokenJpaEntity.domain() = RefreshToken(id, accountId, familyId, jtiHash, expiresAt, consumedAt, revokedAt, replacedById, createdAt)
private fun RefreshToken.jpa() = RefreshTokenJpaEntity(id, accountId, familyId, jtiHash, expiresAt, consumedAt, revokedAt, replacedById, createdAt)
