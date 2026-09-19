package com.viniciusdevassis.laumileymodas.infrastructure.persistence.account

import com.viniciusdevassis.laumileymodas.application.port.security.OAuthHandoffRepository
import com.viniciusdevassis.laumileymodas.domain.account.*
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaOAuthHandoffRepository(private val em: EntityManager) : OAuthHandoffRepository {
	override fun findByHandleHashForUpdate(hash: String): OAuthHandoff? = em.createQuery("select h from OAuthHandoffJpaEntity h where h.handleHash=:hash", OAuthHandoffJpaEntity::class.java).setParameter("hash", hash).setLockMode(LockModeType.PESSIMISTIC_WRITE).resultList.firstOrNull()?.domain()
	@Transactional override fun save(handoff: OAuthHandoff): OAuthHandoff = em.merge(handoff.jpa()).domain()
}

private fun OAuthHandoffJpaEntity.domain() = OAuthHandoff(id, handleHash, OAuthHandoffPurpose.valueOf(purpose), accountId, providerSubject, verifiedEmail, givenName, familyName, expiresAt, consumedAt, createdAt)
private fun OAuthHandoff.jpa() = OAuthHandoffJpaEntity(id, handleHash, purpose.name, accountId, "GOOGLE", providerSubject, verifiedEmail, givenName, familyName, expiresAt, consumedAt, createdAt)
