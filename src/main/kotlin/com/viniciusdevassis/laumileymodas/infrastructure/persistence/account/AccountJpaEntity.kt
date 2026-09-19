package com.viniciusdevassis.laumileymodas.infrastructure.persistence.account

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name = "account")
class AccountJpaEntity(
	@Id var id: UUID,
	var email: String,
	@Column(name = "normalized_email") var normalizedEmail: String,
	@Column(name = "password_hash") var passwordHash: String?,
	var role: String,
	var enabled: Boolean,
	@Column(name = "created_at") var createdAt: Instant,
	@Column(name = "updated_at") var updatedAt: Instant,
) { protected constructor() : this(UUID.randomUUID(), "", "", null, "CLIENT", true, Instant.EPOCH, Instant.EPOCH) }
