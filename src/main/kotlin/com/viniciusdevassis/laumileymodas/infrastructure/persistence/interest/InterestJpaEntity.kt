package com.viniciusdevassis.laumileymodas.infrastructure.persistence.interest

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name="interest")
class InterestJpaEntity(@Id var id: UUID, @Column(name="customer_id") var customerId: UUID, @Column(name="product_id") var productId: UUID, @Column(name="idempotency_key") var idempotencyKey: String, @Column(name="created_at") var createdAt: Instant) {
	protected constructor(): this(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "", Instant.EPOCH)
}
