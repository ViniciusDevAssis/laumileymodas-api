package com.viniciusdevassis.laumileymodas.infrastructure.persistence.interest

import com.viniciusdevassis.laumileymodas.application.port.crm.FollowUpRepository
import com.viniciusdevassis.laumileymodas.application.port.interest.InterestRepository
import com.viniciusdevassis.laumileymodas.domain.crm.*
import com.viniciusdevassis.laumileymodas.domain.interest.Interest
import com.viniciusdevassis.laumileymodas.infrastructure.persistence.crm.*
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class JpaInterestRepository(private val em: EntityManager): InterestRepository, FollowUpRepository {
	override fun lockIdempotency(customerId: UUID, key: String) { em.createNativeQuery("select pg_advisory_xact_lock(hashtextextended(:key, 0))").setParameter("key", "$customerId:$key").singleResult }
	override fun findByCustomerAndKey(customerId: UUID, key: String) = em.createQuery("select i from InterestJpaEntity i where i.customerId=:customer and i.idempotencyKey=:key", InterestJpaEntity::class.java).setParameter("customer", customerId).setParameter("key", key).resultList.firstOrNull()?.domain()
	override fun findOwned(interestId: UUID, customerId: UUID) = em.createQuery("select i from InterestJpaEntity i where i.id=:id and i.customerId=:customer", InterestJpaEntity::class.java).setParameter("id", interestId).setParameter("customer", customerId).resultList.firstOrNull()?.domain()
	@Transactional override fun save(interest: Interest, reminder: Reminder, contact: ContactRecord): Interest {
		em.persist(InterestJpaEntity(interest.id, interest.customerId, interest.productId, interest.idempotencyKey, interest.createdAt))
		em.persist(reminder.toJpa()); em.persist(contact.toJpa()); em.flush(); return interest
	}
}
private fun InterestJpaEntity.domain() = Interest(id, customerId, productId, idempotencyKey, createdAt)
