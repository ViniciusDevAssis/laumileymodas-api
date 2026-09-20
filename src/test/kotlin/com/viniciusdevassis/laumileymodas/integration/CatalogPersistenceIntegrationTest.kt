package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.domain.entities.Category
import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.entities.ProductImage
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CategoryRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogPersistenceIntegrationTest(
	@Autowired private val categoryRepository: CategoryRepository,
	@Autowired private val productRepository: ProductRepository,
) : PostgresIntegrationTest() {
	@BeforeEach
	fun cleanDatabase() {
		productRepository.deleteAll()
		categoryRepository.deleteAll()
	}

	@Test
	@Transactional
	fun `consulta publica retorna apenas produtos ativos`() {
		val category = categoryRepository.save(Category(name = "Vestidos"))
		val active = productRepository.save(product("Vestido floral", ProductStatus.ACTIVE, category))
		productRepository.save(product("Vestido reservado", ProductStatus.INACTIVE, category))

		val result = productRepository.findByStatus(ProductStatus.ACTIVE, PageRequest.of(0, 10))

		assertEquals(1, result.totalElements)
		assertEquals(active.id, result.content.single().id)
		assertTrue(result.content.single().images.single().primary)
	}

	@Test
	fun `produto ativo exige imagem principal`() {
		val category = categoryRepository.save(Category(name = "Blusas"))
		val product = Product(
			category = category,
			name = "Blusa sem imagem",
			description = "Produto ativo inválido",
			status = ProductStatus.ACTIVE,
		)

		assertThrows<RuntimeException> {
			productRepository.saveAndFlush(product)
		}
	}

	@Test
	fun `produto nao permite duas imagens principais`() {
		val category = categoryRepository.save(Category(name = "Saias"))
		val product = Product(
			category = category,
			name = "Saia midi",
			description = "Saia com duas principais inválidas",
			status = ProductStatus.ACTIVE,
		)
		product.addImage(image("saia-1", primary = true, displayOrder = 0))
		product.addImage(image("saia-2", primary = true, displayOrder = 1))

		assertThrows<RuntimeException> {
			productRepository.saveAndFlush(product)
		}
	}

	@Test
	fun `ordem de exibicao da imagem e unica por produto`() {
		val category = categoryRepository.save(Category(name = "Calças"))
		val product = Product(
			category = category,
			name = "Calça jeans",
			description = "Calça com ordem duplicada",
			status = ProductStatus.INACTIVE,
		)
		product.addImage(image("calca-1", primary = false, displayOrder = 0))
		product.addImage(image("calca-2", primary = false, displayOrder = 0))

		assertThrows<DataIntegrityViolationException> {
			productRepository.saveAndFlush(product)
		}
	}

	private fun product(name: String, status: ProductStatus, category: Category): Product =
		Product(
			category = category,
			name = name,
			description = "Descrição do produto",
			status = status,
		).also {
			it.addImage(image(name, primary = true, displayOrder = 0))
		}

	private fun image(seed: String, primary: Boolean, displayOrder: Int): ProductImage =
		ProductImage(
			url = "https://example.com/$seed.jpg",
			externalId = "external-$seed-$displayOrder",
			primary = primary,
			displayOrder = displayOrder,
		)
}
