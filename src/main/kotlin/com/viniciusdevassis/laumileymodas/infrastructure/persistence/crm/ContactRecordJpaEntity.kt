package com.viniciusdevassis.laumileymodas.infrastructure.persistence.crm

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name="contact_record")
class ContactRecordJpaEntity(@Id var id: UUID, @Column(name="customer_id") var customerId: UUID, @Column(name="interest_id") var interestId: UUID, @Column(name="reminder_id") var reminderId: UUID, var status: String, @Column(name="occurred_at") var occurredAt: Instant?, var channel: String, var description: String?, @Column(name="completed_at") var completedAt: Instant?, @Column(name="created_at") var createdAt: Instant, @Column(name="updated_at") var updatedAt: Instant) {
	protected constructor(): this(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "PENDING", null, "WHATSAPP", null, null, Instant.EPOCH, Instant.EPOCH)
}
