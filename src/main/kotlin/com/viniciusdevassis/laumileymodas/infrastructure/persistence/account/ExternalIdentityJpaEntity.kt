package com.viniciusdevassis.laumileymodas.infrastructure.persistence.account

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name = "account_external_identity")
class ExternalIdentityJpaEntity(
	@Id var id: UUID,
	@Column(name = "account_id") var accountId: UUID,
	var provider: String,
	var subject: String,
	@Column(name = "email_at_link") var emailAtLink: String,
	@Column(name = "created_at") var createdAt: Instant,
	@Column(name = "last_login_at") var lastLoginAt: Instant,
) { protected constructor() : this(UUID.randomUUID(), UUID.randomUUID(), "GOOGLE", "", "", Instant.EPOCH, Instant.EPOCH) }
