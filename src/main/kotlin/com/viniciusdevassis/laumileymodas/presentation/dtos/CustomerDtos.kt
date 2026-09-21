package com.viniciusdevassis.laumileymodas.presentation.dtos

import org.springframework.hateoas.server.core.Relation
import jakarta.validation.constraints.Pattern
import java.util.UUID

@Relation(collectionRelation = "customers", itemRelation = "customer")
data class CustomerRepresentation(
	val id: UUID,
	val firstName: String,
	val lastName: String,
	val email: String,
	val whatsappPhone: String?,
	val proactiveContactAuthorized: Boolean,
)

data class WhatsAppUpdateRequest(
	@field:Pattern(regexp = "^\\+[1-9][0-9]{7,14}$") val whatsappPhone: String,
)
