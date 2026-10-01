package com.viniciusdevassis.laumileymodas.presentation.dtos

import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.springframework.hateoas.server.core.Relation
import java.util.UUID

data class CategoryRequest(@field:NotBlank @field:Size(max = 120) val name: String)

@Relation(collectionRelation = "categories", itemRelation = "category")
data class CategoryRepresentation(val id: UUID, val name: String)

data class AdminProductCreateRequest(
	@field:NotBlank @field:Size(max = 200) val name: String,
	@field:NotBlank @field:Size(max = 2000) val description: String,
	@field:NotNull val categoryId: UUID,
	@field:NotNull val status: ProductStatus,
	@field:PositiveOrZero val primaryImageIndex: Int,
)

data class AdminProductReplaceRequest(
	@field:NotBlank @field:Size(max = 200) val name: String,
	@field:NotBlank @field:Size(max = 2000) val description: String,
	@field:NotNull val categoryId: UUID,
)

data class ProductStatusPatchRequest(@field:NotNull val status: ProductStatus)

data class ProductImagePatchRequest(
	val primary: Boolean? = null,
	@field:PositiveOrZero val displayOrder: Int? = null,
)
