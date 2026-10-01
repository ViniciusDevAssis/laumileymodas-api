package com.viniciusdevassis.laumileymodas.presentation.controllers

import com.viniciusdevassis.laumileymodas.application.CatalogService
import com.viniciusdevassis.laumileymodas.application.ProductMediaService
import com.viniciusdevassis.laumileymodas.domain.entities.Category
import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.entities.ProductImage
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.presentation.dtos.AdminProductCreateRequest
import com.viniciusdevassis.laumileymodas.presentation.dtos.AdminProductReplaceRequest
import com.viniciusdevassis.laumileymodas.presentation.dtos.CategoryRepresentation
import com.viniciusdevassis.laumileymodas.presentation.dtos.CategoryRequest
import com.viniciusdevassis.laumileymodas.presentation.dtos.CategorySummary
import com.viniciusdevassis.laumileymodas.presentation.dtos.ProductImagePatchRequest
import com.viniciusdevassis.laumileymodas.presentation.dtos.ProductImageRepresentation
import com.viniciusdevassis.laumileymodas.presentation.dtos.ProductRepresentation
import com.viniciusdevassis.laumileymodas.presentation.dtos.ProductStatusPatchRequest
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.EntityModel
import org.springframework.hateoas.IanaLinkRelations
import org.springframework.hateoas.MediaTypes
import org.springframework.hateoas.PagedModel
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
class AdminCatalogController(
	private val catalogService: CatalogService,
	private val productMediaService: ProductMediaService,
	private val pagedResourcesAssembler: PagedResourcesAssembler<Product>,
) {
	@GetMapping("/admin/categories", produces = [MediaTypes.HAL_JSON_VALUE])
	fun listCategories(): CollectionModel<EntityModel<CategoryRepresentation>> {
		val categories = catalogService.listCategories().map { it.toModel() }
		return CollectionModel.of(categories, linkTo(methodOn(AdminCatalogController::class.java).listCategories()).withSelfRel())
	}

	@PostMapping("/admin/categories", produces = [MediaTypes.HAL_JSON_VALUE])
	fun createCategory(@Valid @RequestBody request: CategoryRequest): ResponseEntity<EntityModel<CategoryRepresentation>> {
		val model = catalogService.createCategory(request.name).toModel()
		return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model)
	}

	@PutMapping("/admin/categories/{categoryId}", produces = [MediaTypes.HAL_JSON_VALUE])
	fun replaceCategory(@PathVariable categoryId: UUID, @Valid @RequestBody request: CategoryRequest): EntityModel<CategoryRepresentation> =
		catalogService.replaceCategory(categoryId, request.name).toModel()

	@GetMapping("/admin/products", produces = [MediaTypes.HAL_JSON_VALUE])
	fun listProducts(@RequestParam(required = false) status: ProductStatus?, pageable: Pageable): PagedModel<EntityModel<ProductRepresentation>> =
		pagedResourcesAssembler.toModel(catalogService.listAdminProducts(status, pageable)) { it.toProductModel() }

	@PostMapping("/admin/products", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaTypes.HAL_JSON_VALUE])
	fun createProduct(
		@Valid @RequestPart("metadata") metadata: AdminProductCreateRequest,
		@RequestPart("images") images: List<MultipartFile>,
		request: HttpServletRequest,
	): ResponseEntity<EntityModel<ProductRepresentation>> {
		val product = productMediaService.createProduct(
			metadata.name,
			metadata.description,
			metadata.categoryId,
			metadata.status,
			metadata.primaryImageIndex,
			images.map { it.bytes to (it.contentType ?: "") },
		)
		val model = product.toProductModel()
		val location = ServletUriComponentsBuilder.fromRequest(request).path("/{productId}").buildAndExpand(product.id).toUri()
		return ResponseEntity.created(location).body(model)
	}

	@GetMapping("/admin/products/{productId}", produces = [MediaTypes.HAL_JSON_VALUE])
	fun getProduct(@PathVariable productId: UUID): EntityModel<ProductRepresentation> = catalogService.getAdminProduct(productId).toProductModel()

	@PutMapping("/admin/products/{productId}", produces = [MediaTypes.HAL_JSON_VALUE])
	fun replaceProduct(@PathVariable productId: UUID, @Valid @RequestBody request: AdminProductReplaceRequest): EntityModel<ProductRepresentation> =
		catalogService.replaceProduct(productId, request.name, request.description, request.categoryId).toProductModel()

	@PatchMapping("/admin/products/{productId}", consumes = ["application/merge-patch+json"], produces = [MediaTypes.HAL_JSON_VALUE])
	fun patchProduct(@PathVariable productId: UUID, @Valid @RequestBody request: ProductStatusPatchRequest): EntityModel<ProductRepresentation> =
		catalogService.changeProductStatus(productId, request.status).toProductModel()

	@PostMapping("/admin/products/{productId}/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE], produces = [MediaTypes.HAL_JSON_VALUE])
	fun addImage(
		@PathVariable productId: UUID,
		@RequestPart("file") file: MultipartFile,
		@RequestParam(required = false) primary: Boolean?,
		@RequestParam(required = false) displayOrder: Int?,
	): ResponseEntity<EntityModel<ProductImageRepresentation>> {
		val image = productMediaService.addImage(productId, file.bytes, file.contentType ?: "", primary, displayOrder)
		val model = image.toImageModel(productId)
		return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model)
	}

	@PatchMapping("/admin/products/{productId}/images/{imageId}", consumes = ["application/merge-patch+json"], produces = [MediaTypes.HAL_JSON_VALUE])
	fun patchImage(
		@PathVariable productId: UUID,
		@PathVariable imageId: UUID,
		@Valid @RequestBody request: ProductImagePatchRequest,
	): EntityModel<ProductImageRepresentation> = productMediaService.updateImage(productId, imageId, request.primary, request.displayOrder).toImageModel(productId)

	@DeleteMapping("/admin/products/{productId}/images/{imageId}")
	fun deleteImage(@PathVariable productId: UUID, @PathVariable imageId: UUID): ResponseEntity<Void> {
		productMediaService.deleteImage(productId, imageId)
		return ResponseEntity.noContent().build()
	}

	private fun Category.toModel(): EntityModel<CategoryRepresentation> {
		val id = requireNotNull(id)
		return EntityModel.of(CategoryRepresentation(id, name))
			.add(linkTo(methodOn(AdminCatalogController::class.java).replaceCategory(id, CategoryRequest(name))).withSelfRel())
	}

	private fun Product.toProductModel(): EntityModel<ProductRepresentation> {
		val id = requireNotNull(id)
		return EntityModel.of(
			ProductRepresentation(id, name, description, CategorySummary(requireNotNull(category.id), category.name), status, orderedImages().map { it.toRepresentation(id) }),
		).apply {
			add(linkTo(methodOn(AdminCatalogController::class.java).getProduct(id)).withSelfRel())
			add(linkTo(methodOn(AdminCatalogController::class.java).listProducts(null, Pageable.unpaged())).withRel(IanaLinkRelations.COLLECTION))
			add(linkTo(methodOn(AdminCatalogController::class.java).replaceProduct(id, AdminProductReplaceRequest(name, description, requireNotNull(category.id)))).withRel("replace"))
			add(linkTo(methodOn(AdminCatalogController::class.java).patchProduct(id, ProductStatusPatchRequest(status))).withRel("status"))
		}
	}

	private fun ProductImage.toRepresentation(productId: UUID) = ProductImageRepresentation(requireNotNull(id), url, primary, displayOrder)
		.add(org.springframework.hateoas.Link.of(url, org.springframework.hateoas.IanaLinkRelations.SELF))
		.add(linkTo(methodOn(AdminCatalogController::class.java).getProduct(productId)).withRel("product"))

	private fun ProductImage.toImageModel(productId: UUID): EntityModel<ProductImageRepresentation> {
		val representation = toRepresentation(productId)
		return EntityModel.of(representation).add(representation.getRequiredLink(IanaLinkRelations.SELF))
	}
}
