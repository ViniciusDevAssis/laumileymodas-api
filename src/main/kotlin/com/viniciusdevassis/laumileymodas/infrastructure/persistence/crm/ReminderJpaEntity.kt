package com.viniciusdevassis.laumileymodas.infrastructure.persistence.crm

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name="reminder")
class ReminderJpaEntity(@Id var id: UUID, @Column(name="customer_id") var customerId: UUID, @Column(name="interest_id") var interestId: UUID, var description: String, @Column(name="due_at") var dueAt: Instant, var status: String, @Column(name="completed_at") var completedAt: Instant?, @Column(name="created_at") var createdAt: Instant, @Column(name="updated_at") var updatedAt: Instant) {
	protected constructor(): this(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "", Instant.EPOCH, "PENDING", null, Instant.EPOCH, Instant.EPOCH)
}
