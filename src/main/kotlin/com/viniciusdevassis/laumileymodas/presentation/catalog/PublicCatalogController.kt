package com.viniciusdevassis.laumileymodas.presentation.catalog

import com.viniciusdevassis.laumileymodas.application.catalog.GetPublicProductUseCase
import com.viniciusdevassis.laumileymodas.application.catalog.ListPublicCatalogUseCase
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
@RequestMapping("/catalog/products")
class PublicCatalogController(
	private val listPublicCatalog: ListPublicCatalogUseCase,
	private val getPublicProduct: GetPublicProductUseCase,
	private val mapper: CatalogResponseMapper,
) {

	@GetMapping
	fun list(
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
	): ProductPageResponse = mapper.toResponse(listPublicCatalog(page, size))

	@GetMapping("/{productId}")
	fun detail(@PathVariable productId: UUID): ProductResponse =
		mapper.toResponse(getPublicProduct(productId))
}
