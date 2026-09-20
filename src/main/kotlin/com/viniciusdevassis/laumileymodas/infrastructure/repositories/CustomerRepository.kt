package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CustomerRepository : JpaRepository<Customer, UUID> {
	fun findByAccountId(accountId: UUID): Customer?
}
