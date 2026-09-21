package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.Customer
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CustomerRepository : JpaRepository<Customer, UUID> {
	fun findByAccountId(accountId: UUID): Customer?

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select c from Customer c where c.account.id = :accountId")
	fun findByAccountIdForInterest(@Param("accountId") accountId: UUID): Customer?
}
