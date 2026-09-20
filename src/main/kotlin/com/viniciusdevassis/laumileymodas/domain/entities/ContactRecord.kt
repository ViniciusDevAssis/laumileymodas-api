package com.viniciusdevassis.laumileymodas.domain.entities

import com.viniciusdevassis.laumileymodas.domain.enums.ContactChannel
import com.viniciusdevassis.laumileymodas.domain.enums.FollowUpStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "contact_record")
class ContactRecord(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	val customer: Customer,

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "interest_id", unique = true)
	val interest: Interest? = null,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	var status: FollowUpStatus,

	@Column
	var occurredAt: Instant? = null,

	@Column(nullable = false, length = 40)
	var channel: String,

	@Column(length = 2000)
	var description: String? = null,

	@Column
	var completedAt: Instant? = null,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),

	@Column(nullable = false)
	var updatedAt: Instant = createdAt,
) {
	init {
		require(channel.isNotBlank()) { "Registro de contato deve possuir canal." }
		validateCompletion()
	}

	fun complete(occurredAt: Instant, channel: String, description: String, completedAt: Instant) {
		require(channel.isNotBlank()) { "Canal do contato deve ser informado." }
		require(description.isNotBlank()) { "Descrição do contato deve ser informada." }

		status = FollowUpStatus.COMPLETED
		this.occurredAt = occurredAt
		this.channel = channel
		this.description = description
		this.completedAt = completedAt
		updatedAt = completedAt
	}

	private fun validateCompletion() {
		require(status != FollowUpStatus.COMPLETED || (occurredAt != null && completedAt != null && !description.isNullOrBlank())) {
			"Contato concluído deve possuir data, descrição e conclusão."
		}
	}

	companion object {
		fun pendingWhatsApp(customer: Customer, interest: Interest, createdAt: Instant = Instant.now()): ContactRecord =
			ContactRecord(
				customer = customer,
				interest = interest,
				status = FollowUpStatus.PENDING,
				channel = ContactChannel.WHATSAPP.name,
				createdAt = createdAt,
				updatedAt = createdAt,
			)
	}
}
