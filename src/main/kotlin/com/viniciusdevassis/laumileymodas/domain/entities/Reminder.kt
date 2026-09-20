package com.viniciusdevassis.laumileymodas.domain.entities

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
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reminder")
class Reminder(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "interest_id", nullable = false, unique = true)
	val interest: Interest,

	@Column(nullable = false, length = 1000)
	val description: String,

	@Column(nullable = false)
	val dueAt: Instant,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	var status: FollowUpStatus = FollowUpStatus.PENDING,

	@Column
	var completedAt: Instant? = null,

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),

	@Column(nullable = false)
	var updatedAt: Instant = createdAt,
) {
	init {
		require(description.isNotBlank()) { "Lembrete deve possuir descrição." }
		validateCompletion()
	}

	fun complete(completedAt: Instant) {
		status = FollowUpStatus.COMPLETED
		this.completedAt = completedAt
		updatedAt = completedAt
	}

	private fun validateCompletion() {
		require(status != FollowUpStatus.COMPLETED || completedAt != null) {
			"Lembrete concluído deve possuir data de conclusão."
		}
	}
}
