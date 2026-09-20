package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.Interest
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface InterestRepository : JpaRepository<Interest, UUID> {
	fun findByCustomerIdAndIdempotencyKey(customerId: UUID, idempotencyKey: String): Interest?
	fun findByCustomerId(customerId: UUID, pageable: Pageable): Page<Interest>
}
