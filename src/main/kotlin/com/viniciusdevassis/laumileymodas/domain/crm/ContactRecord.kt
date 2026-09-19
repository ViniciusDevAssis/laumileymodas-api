package com.viniciusdevassis.laumileymodas.domain.crm

import java.time.Instant
import java.util.UUID

enum class ContactRecordStatus { PENDING, COMPLETED }

data class ContactRecord(
	val id: UUID,
	val customerId: UUID,
	val interestId: UUID,
	val reminderId: UUID,
	val status: ContactRecordStatus,
	val occurredAt: Instant?,
	val channel: String,
	val description: String?,
	val completedAt: Instant?,
	val createdAt: Instant,
	val updatedAt: Instant,
) {
	companion object {
		fun pending(id: UUID, customerId: UUID, interestId: UUID, reminderId: UUID, now: Instant) = ContactRecord(
			id, customerId, interestId, reminderId, ContactRecordStatus.PENDING, null, "WHATSAPP", null, null, now, now,
		)
	}
}
