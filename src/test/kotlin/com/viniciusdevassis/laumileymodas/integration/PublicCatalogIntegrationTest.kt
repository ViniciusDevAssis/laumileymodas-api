package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.domain.entities.Category
import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.entities.ProductImage
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CategoryRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@AutoConfigureMockMvc
class PublicCatalogIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val categoryRepository: CategoryRepository,
	@Autowired private val productRepository: ProductRepository,
) : PostgresIntegrationTest() {
	@BeforeEach
	fun cleanDatabase() {
		productRepository.deleteAll()
		categoryRepository.deleteAll()
	}

	@Test
	fun `lista produtos ativos para visitante anonimo com HAL e paginacao`() {
		val category = categoryRepository.save(Category(name = "Vestidos"))
		productRepository.save(product("Vestido ativo", ProductStatus.ACTIVE, category))
		productRepository.save(product("Vestido inativo", ProductStatus.INACTIVE, category))

		mockMvc.get("/products?page=0&size=10") {
			header(HttpHeaders.ACCEPT, "application/hal+json")
		}.andExpect {
			status { isOk() }
			content { contentTypeCompatibleWith("application/hal+json") }
			jsonPath("$._embedded.products.length()") { value(1) }
			jsonPath("$._embedded.products[0].name") { value("Vestido ativo") }
			jsonPath("$._embedded.products[0].status") { value("ACTIVE") }
			jsonPath("$._embedded.products[0].images[0].primary") { value(true) }
			jsonPath("$._embedded.products[0]._links.self.href") { exists() }
			jsonPath("$.page.size") { value(10) }
			jsonPath("$._links.self.href") { exists() }
		}
	}

	@Test
	fun `lista produtos ativos filtrados por categoria`() {
		val dresses = categoryRepository.save(Category(name = "Vestidos"))
		val shirts = categoryRepository.save(Category(name = "Blusas"))
		productRepository.save(product("Vestido azul", ProductStatus.ACTIVE, dresses))
		productRepository.save(product("Blusa branca", ProductStatus.ACTIVE, shirts))

		mockMvc.get("/products?categoryId=${shirts.id}&page=0&size=10")
			.andExpect {
				status { isOk() }
				jsonPath("$._embedded.products.length()") { value(1) }
				jsonPath("$._embedded.products[0].name") { value("Blusa branca") }
			}
	}

	@Test
	fun `consulta detalhe de produto ativo com imagens ordenadas`() {
		val category = categoryRepository.save(Category(name = "Saias"))
		val product = Product(
			category = category,
			name = "Saia midi",
			description = "Saia midi estampada",
			status = ProductStatus.ACTIVE,
		)
		product.addImage(image("secundaria", primary = false, displayOrder = 0))
		product.addImage(image("principal", primary = true, displayOrder = 1))
		val saved = productRepository.save(product)

		mockMvc.get("/products/${saved.id}")
			.andExpect {
				status { isOk() }
				jsonPath("$.id") { value(saved.id.toString()) }
				jsonPath("$.category.name") { value("Saias") }
				jsonPath("$.images.length()") { value(2) }
				jsonPath("$.images[0].primary") { value(true) }
				jsonPath("$.images[0].displayOrder") { value(1) }
				jsonPath("$._links.self.href") { exists() }
				jsonPath("$._links.collection.href") { exists() }
			}
	}

	@Test
	fun `produto inativo retorna 404 no catalogo publico`() {
		val category = categoryRepository.save(Category(name = "Calças"))
		val inactive = productRepository.save(product("Calça reservada", ProductStatus.INACTIVE, category))

		mockMvc.get("/products/${inactive.id}")
			.andExpect {
				status { isNotFound() }
				jsonPath("$.code") { value(ApiError.CATALOG_001.code) }
				jsonPath("$.path") { value("/products/${inactive.id}") }
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
