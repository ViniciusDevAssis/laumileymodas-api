package com.viniciusdevassis.laumileymodas.presentation.controllers

import com.viniciusdevassis.laumileymodas.application.InterestService
import com.viniciusdevassis.laumileymodas.domain.entities.Interest
import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.entities.ProductImage
import com.viniciusdevassis.laumileymodas.presentation.dtos.CategorySummary
import com.viniciusdevassis.laumileymodas.presentation.dtos.InterestRepresentation
import com.viniciusdevassis.laumileymodas.presentation.dtos.ProductImageRepresentation
import com.viniciusdevassis.laumileymodas.presentation.dtos.ProductRepresentation
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.MediaTypes
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
class InterestController(
	private val interestService: InterestService,
) {
	@PostMapping("/products/{productId}/interests", produces = [MediaTypes.HAL_JSON_VALUE])
	fun createInterest(
		@PathVariable productId: UUID,
		@RequestHeader("Idempotency-Key") idempotencyKey: String,
		authentication: Authentication,
	): ResponseEntity<EntityModel<InterestRepresentation>> {
		val result = interestService.confirm(UUID.fromString(authentication.name), productId, idempotencyKey)
		val model = result.interest.toModel(result.whatsappUrl)
		val location = ServletUriComponentsBuilder.fromCurrentContextPath()
			.path("/products/{productId}/interests")
			.buildAndExpand(productId)
			.toUri()
		val response = if (result.created) ResponseEntity.created(location) else ResponseEntity.ok()
		return response.body(model)
	}

	private fun Interest.toModel(whatsappUrl: String): EntityModel<InterestRepresentation> {
		val productModel = product.toRepresentation()
		return EntityModel.of(
			InterestRepresentation(
				id = requireNotNull(id),
				customerId = requireNotNull(customer.id),
				product = productModel,
				createdAt = createdAt,
				whatsappUrl = whatsappUrl,
			),
			Link.of("/products/${product.id}/interests", "self"),
			Link.of("/products/${product.id}", "product"),
			Link.of(whatsappUrl, "whatsapp"),
		)
	}

	private fun Product.toRepresentation() = ProductRepresentation(
		id = requireNotNull(id),
		name = name,
		description = description,
		category = CategorySummary(requireNotNull(category.id), category.name),
		status = status,
		images = orderedImages().map { it.toRepresentation() },
	)

	private fun ProductImage.toRepresentation() = ProductImageRepresentation(
		id = requireNotNull(id),
		url = url,
		primary = primary,
		displayOrder = displayOrder,
	)
}
