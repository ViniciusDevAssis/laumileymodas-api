package com.viniciusdevassis.laumileymodas.infrastructure.persistence.customer

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name = "customer")
class CustomerJpaEntity(
	@Id var id: UUID,
	@Column(name = "account_id") var accountId: UUID,
	@Column(name = "first_name") var firstName: String,
	@Column(name = "last_name") var lastName: String,
	@Column(name = "whatsapp_phone") var whatsappPhone: String,
	@Column(name = "proactive_contact_authorized") var proactiveContactAuthorized: Boolean,
	@Column(name = "contact_consent_changed_at") var contactConsentChangedAt: Instant?,
	@Column(name = "created_at") var createdAt: Instant,
	@Column(name = "updated_at") var updatedAt: Instant,
) { protected constructor() : this(UUID.randomUUID(), UUID.randomUUID(), "", "", "+5500000000", false, null, Instant.EPOCH, Instant.EPOCH) }
