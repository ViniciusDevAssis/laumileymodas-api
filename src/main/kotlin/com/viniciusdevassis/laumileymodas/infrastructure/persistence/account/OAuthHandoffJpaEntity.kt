package com.viniciusdevassis.laumileymodas.infrastructure.persistence.account

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity @Table(name = "oauth_handoff")
class OAuthHandoffJpaEntity(
	@Id var id: UUID,
	@Column(name = "handle_hash") var handleHash: String,
	var purpose: String,
	@Column(name = "account_id") var accountId: UUID?,
	var provider: String = "GOOGLE",
	@Column(name = "provider_subject") var providerSubject: String,
	@Column(name = "verified_email") var verifiedEmail: String,
	@Column(name = "given_name") var givenName: String?,
	@Column(name = "family_name") var familyName: String?,
	@Column(name = "expires_at") var expiresAt: Instant,
	@Column(name = "consumed_at") var consumedAt: Instant?,
	@Column(name = "created_at") var createdAt: Instant,
) { protected constructor() : this(UUID.randomUUID(), "", "CLIENT_REGISTRATION", null, "GOOGLE", "", "", null, null, Instant.MAX, null, Instant.EPOCH) }
