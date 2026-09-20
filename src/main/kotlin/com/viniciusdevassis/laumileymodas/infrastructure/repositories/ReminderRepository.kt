package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.Reminder
import com.viniciusdevassis.laumileymodas.domain.enums.FollowUpStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReminderRepository : JpaRepository<Reminder, UUID> {
	fun findByInterestId(interestId: UUID): Reminder?
	fun findByStatus(status: FollowUpStatus, pageable: Pageable): Page<Reminder>
}
