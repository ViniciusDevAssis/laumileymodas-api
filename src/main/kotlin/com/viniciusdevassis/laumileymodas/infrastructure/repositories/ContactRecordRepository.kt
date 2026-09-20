package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.ContactRecord
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ContactRecordRepository : JpaRepository<ContactRecord, UUID> {
	fun findByInterestId(interestId: UUID): ContactRecord?
	fun findByCustomerId(customerId: UUID, pageable: Pageable): Page<ContactRecord>
}
