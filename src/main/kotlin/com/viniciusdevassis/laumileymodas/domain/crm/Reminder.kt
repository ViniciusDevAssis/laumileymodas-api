package com.viniciusdevassis.laumileymodas.domain.crm

import java.time.Instant
import java.util.UUID

enum class ReminderStatus { PENDING, COMPLETED }

data class Reminder(
	val id: UUID,
	val customerId: UUID,
	val interestId: UUID,
	val description: String,
	val dueAt: Instant,
	val status: ReminderStatus,
	val completedAt: Instant?,
	val createdAt: Instant,
	val updatedAt: Instant,
) {
	val actionable get() = status == ReminderStatus.PENDING
	fun isOverdue(now: Instant) = actionable && dueAt.isBefore(now)

	companion object {
		fun pending(id: UUID, customerId: UUID, interestId: UUID, productName: String, now: Instant) = Reminder(
			id, customerId, interestId, "Completar atendimento do interesse em $productName pelo WhatsApp.",
			now, ReminderStatus.PENDING, null, now, now,
		)
	}
}
