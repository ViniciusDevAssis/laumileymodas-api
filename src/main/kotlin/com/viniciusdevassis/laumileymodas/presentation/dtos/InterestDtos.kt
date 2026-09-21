package com.viniciusdevassis.laumileymodas.presentation.dtos

import org.springframework.hateoas.server.core.Relation
import java.time.Instant
import java.util.UUID

@Relation(itemRelation = "interest")
data class InterestRepresentation(
	val id: UUID,
	val customerId: UUID,
	val product: ProductRepresentation,
	val createdAt: Instant,
	val whatsappUrl: String,
)
