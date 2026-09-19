package com.viniciusdevassis.laumileymodas.unit.domain.catalog

import com.viniciusdevassis.laumileymodas.domain.catalog.CatalogError
import com.viniciusdevassis.laumileymodas.domain.catalog.Category
import com.viniciusdevassis.laumileymodas.domain.catalog.Product
import com.viniciusdevassis.laumileymodas.domain.catalog.ProductImage
import com.viniciusdevassis.laumileymodas.domain.catalog.ProductStatus
import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant
import java.util.UUID

class ProductTest {

	@Test
	fun `produto ativo exige ao menos uma imagem`() {
		assertThatThrownBy {
			product(status = ProductStatus.ACTIVE, images = emptyList())
		}
			.isInstanceOfSatisfying(DomainException::class.java) {
				assertThat(it.error).isEqualTo(CatalogError.PRODUCT_REQUIRES_IMAGE)
			}
	}

	@Test
	fun `produto com imagens exige exatamente uma principal`() {
		assertThatThrownBy {
			product(images = listOf(image(primary = false, order = 0), image(primary = false, order = 1)))
		}
			.isInstanceOfSatisfying(DomainException::class.java) {
				assertThat(it.error).isEqualTo(CatalogError.PRODUCT_REQUIRES_ONE_PRIMARY)
			}

		assertThatThrownBy {
			product(images = listOf(image(primary = true, order = 0), image(primary = true, order = 1)))
		}
			.isInstanceOfSatisfying(DomainException::class.java) {
				assertThat(it.error).isEqualTo(CatalogError.PRODUCT_REQUIRES_ONE_PRIMARY)
			}
	}

	@Test
	fun `produto inativo sem imagem so pode ser ativado depois de receber midia valida`() {
		val product = product(images = emptyList())

		assertThatThrownBy(product::activate)
			.isInstanceOfSatisfying(DomainException::class.java) {
				assertThat(it.error).isEqualTo(CatalogError.PRODUCT_REQUIRES_IMAGE)
			}

		product.replaceImages(listOf(image(primary = true, order = 0)))
		product.activate()

		assertThat(product.status).isEqualTo(ProductStatus.ACTIVE)
	}

	@Test
	fun `imagens permanecem ordenadas e ordens duplicadas sao rejeitadas`() {
		val product = product(
			images = listOf(
				image(primary = false, order = 2),
				image(primary = true, order = 0),
			),
		)

		assertThat(product.images.map(ProductImage::displayOrder)).containsExactly(0, 2)

		assertThatThrownBy {
			product.replaceImages(listOf(image(primary = true, order = 0), image(primary = false, order = 0)))
		}
			.isInstanceOfSatisfying(DomainException::class.java) {
				assertThat(it.error).isEqualTo(CatalogError.DUPLICATE_IMAGE_ORDER)
			}
	}

	private fun product(
		status: ProductStatus = ProductStatus.INACTIVE,
		images: List<ProductImage>,
	): Product = Product(
		id = UUID.randomUUID(),
		category = Category(UUID.randomUUID(), "Vestidos", NOW, NOW),
		name = "Vestido floral",
		description = "Vestido leve para dias quentes.",
		status = status,
		images = images,
		createdAt = NOW,
		updatedAt = NOW,
	)

	private fun image(primary: Boolean, order: Int): ProductImage = ProductImage(
		id = UUID.randomUUID(),
		externalId = "catalog/${UUID.randomUUID()}",
		url = URI("https://res.cloudinary.com/demo/image/upload/sample-$order.jpg"),
		primary = primary,
		displayOrder = order,
		createdAt = NOW,
	)

	companion object {
		private val NOW: Instant = Instant.parse("2026-09-19T12:00:00Z")
	}
}
