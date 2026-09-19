package com.viniciusdevassis.laumileymodas.infrastructure.persistence.account

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name = "refresh_token")
class RefreshTokenJpaEntity(
	@Id var id: UUID,
	@Column(name = "account_id") var accountId: UUID,
	@Column(name = "family_id") var familyId: UUID,
	@Column(name = "jti_hash") var jtiHash: String,
	@Column(name = "expires_at") var expiresAt: Instant,
	@Column(name = "consumed_at") var consumedAt: Instant?,
	@Column(name = "revoked_at") var revokedAt: Instant?,
	@Column(name = "replaced_by_id") var replacedById: UUID?,
	@Column(name = "created_at") var createdAt: Instant,
) { protected constructor() : this(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "", Instant.MAX, null, null, null, Instant.EPOCH) }
