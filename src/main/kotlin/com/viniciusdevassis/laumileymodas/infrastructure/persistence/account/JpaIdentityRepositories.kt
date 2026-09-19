package com.viniciusdevassis.laumileymodas.infrastructure.persistence.account

import com.viniciusdevassis.laumileymodas.application.port.account.*
import com.viniciusdevassis.laumileymodas.application.port.customer.CustomerRepository
import com.viniciusdevassis.laumileymodas.domain.account.*
import com.viniciusdevassis.laumileymodas.domain.customer.Customer
import com.viniciusdevassis.laumileymodas.infrastructure.persistence.customer.CustomerJpaEntity
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class JpaIdentityRepositories(private val entityManager: EntityManager) : AccountRepository, ExternalIdentityRepository, CustomerRepository {
	override fun findById(id: UUID) = entityManager.find(AccountJpaEntity::class.java, id)?.toDomain()
	override fun findByNormalizedEmail(email: String) = entityManager.createQuery("select a from AccountJpaEntity a where a.normalizedEmail = :email", AccountJpaEntity::class.java).setParameter("email", Account.normalizeEmail(email)).resultList.firstOrNull()?.toDomain()
	@Transactional override fun save(account: Account): Account = entityManager.merge(account.toJpa()).toDomain()
	override fun findGoogleBySubject(subject: String) = entityManager.createQuery("select e from ExternalIdentityJpaEntity e where e.provider = 'GOOGLE' and e.subject = :subject", ExternalIdentityJpaEntity::class.java).setParameter("subject", subject).resultList.firstOrNull()?.toDomain()
	@Transactional override fun save(identity: AccountExternalIdentity): AccountExternalIdentity = entityManager.merge(identity.toJpa()).toDomain()
	override fun findByAccountId(accountId: UUID) = entityManager.createQuery("select c from CustomerJpaEntity c where c.accountId = :id", CustomerJpaEntity::class.java).setParameter("id", accountId).resultList.firstOrNull()?.toDomain()
	@Transactional override fun save(customer: Customer): Customer = entityManager.merge(customer.toJpa()).toDomain()
}
