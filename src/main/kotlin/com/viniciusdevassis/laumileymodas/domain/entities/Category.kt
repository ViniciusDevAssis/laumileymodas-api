package com.viniciusdevassis.laumileymodas.domain.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "category")
class Category(
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	var id: UUID? = null,

	@Column(nullable = false, length = 120)
	var name: String,

	@Column(nullable = false, unique = true, length = 120)
	var normalizedName: String = normalize(name),

	@Column(nullable = false)
	val createdAt: Instant = Instant.now(),

	@Column(nullable = false)
	var updatedAt: Instant = createdAt,
) {
	init {
		require(name.isNotBlank()) { "Categoria deve possuir nome." }
	}

	fun rename(newName: String, updatedAt: Instant = Instant.now()) {
		require(newName.isNotBlank()) { "Categoria deve possuir nome." }
		name = newName
		normalizedName = normalize(newName)
		this.updatedAt = updatedAt
	}

	companion object {
		fun normalize(value: String): String = value.trim().lowercase()
	}
}
