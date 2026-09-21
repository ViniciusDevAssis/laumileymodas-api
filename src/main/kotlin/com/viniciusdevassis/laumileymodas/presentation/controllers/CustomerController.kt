package com.viniciusdevassis.laumileymodas.presentation.controllers

import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.domain.entities.Customer
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.presentation.dtos.CustomerRegistrationRequest
import com.viniciusdevassis.laumileymodas.presentation.dtos.CustomerRepresentation
import com.viniciusdevassis.laumileymodas.presentation.dtos.WhatsAppUpdateRequest
import jakarta.validation.Valid
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.Link
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CustomerController(
	private val authService: AuthService,
) {
	@PostMapping("/customers", produces = ["application/hal+json"])
	fun register(@Valid @RequestBody request: CustomerRegistrationRequest): ResponseEntity<EntityModel<CustomerRepresentation>> {
		val customer = authService.registerCustomer(
			request.firstName,
			request.lastName,
			request.email,
			request.password,
			request.whatsappPhone,
		)
		val location = linkTo(methodOn(CustomerController::class.java).current(null)).toUri()
		return ResponseEntity.created(location).body(customer.toModel())
	}

	@GetMapping("/customers/me", produces = ["application/hal+json"])
	fun current(authentication: Authentication?): EntityModel<CustomerRepresentation> {
		val accountId = authentication?.name?.let(UUID::fromString)
			?: throw ApiException(ApiError.AUTH_001, org.springframework.http.HttpStatus.UNAUTHORIZED)
		return authService.getCustomer(accountId).toModel()
	}

	@PutMapping("/customers/me/whatsapp", consumes = ["application/json"], produces = ["application/hal+json"])
	fun updateWhatsApp(
		@Valid @RequestBody request: WhatsAppUpdateRequest,
		authentication: Authentication,
	): EntityModel<CustomerRepresentation> =
		authService.updateCustomerWhatsApp(UUID.fromString(authentication.name), request.whatsappPhone).toModel()

	private fun Customer.toModel(): EntityModel<CustomerRepresentation> =
		EntityModel.of(
			CustomerRepresentation(
				id = requireNotNull(id),
				firstName = firstName,
				lastName = lastName,
				email = account.email,
				whatsappPhone = whatsappPhone,
				proactiveContactAuthorized = proactiveContactAuthorized,
			),
		).add(linkTo(methodOn(CustomerController::class.java).current(null)).withSelfRel())
			.add(Link.of("/customers/me/whatsapp", "whatsapp"))
}
