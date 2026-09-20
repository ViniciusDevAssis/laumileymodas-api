package com.viniciusdevassis.laumileymodas.presentation.controllers

import com.viniciusdevassis.laumileymodas.application.CatalogService
import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.entities.ProductImage
import com.viniciusdevassis.laumileymodas.presentation.dtos.CategorySummary
import com.viniciusdevassis.laumileymodas.presentation.dtos.ProductImageRepresentation
import com.viniciusdevassis.laumileymodas.presentation.dtos.ProductRepresentation
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.IanaLinkRelations
import org.springframework.hateoas.MediaTypes
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CatalogController(
	private val catalogService: CatalogService,
	private val pagedResourcesAssembler: PagedResourcesAssembler<Product>,
) {
	@GetMapping("/products", produces = [MediaTypes.HAL_JSON_VALUE])
	fun listProducts(
		@RequestParam(required = false) categoryId: UUID?,
		pageable: Pageable,
	): PagedModel<EntityModel<ProductRepresentation>> =
		pagedResourcesAssembler.toModel(catalogService.listActiveProducts(categoryId, pageable)) { product ->
			product.toModel()
		}

	@GetMapping("/products/{productId}", produces = [MediaTypes.HAL_JSON_VALUE])
	fun getProduct(@PathVariable productId: UUID): EntityModel<ProductRepresentation> =
		catalogService.getActiveProduct(productId).toModel()

	private fun Product.toModel(): EntityModel<ProductRepresentation> =
		EntityModel.of(
			ProductRepresentation(
				id = requireNotNull(id),
				name = name,
				description = description,
				category = CategorySummary(requireNotNull(category.id), category.name),
				status = status,
				images = orderedImages().map { it.toRepresentation() },
			),
		).apply {
			add(linkTo(methodOn(CatalogController::class.java).getProduct(requireNotNull(id))).withSelfRel())
			add(linkTo(methodOn(CatalogController::class.java).listProducts(null, Pageable.unpaged())).withRel(IanaLinkRelations.COLLECTION))
		}

	private fun ProductImage.toRepresentation(): ProductImageRepresentation =
		ProductImageRepresentation(
			id = requireNotNull(id),
			url = url,
			primary = primary,
			displayOrder = displayOrder,
		)
}
