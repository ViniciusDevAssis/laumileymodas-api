package com.viniciusdevassis.laumileymodas.integration.http

import com.viniciusdevassis.laumileymodas.integration.support.PostgresIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class PublicCatalogControllerIntegrationTest : PostgresIntegrationTest() {

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@BeforeEach
	fun cleanCatalog() {
		jdbcTemplate.update("DELETE FROM product_image")
		jdbcTemplate.update("DELETE FROM product")
		jdbcTemplate.update("DELETE FROM category")
	}

	@Test
	fun `catalogo vazio e uma resposta publica valida`() {
		mockMvc.get("/catalog/products")
			.andExpect {
				status { isOk() }
				content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
				jsonPath("$.items.length()") { value(0) }
				jsonPath("$.page") { value(0) }
				jsonPath("$.size") { value(20) }
				jsonPath("$.totalElements") { value(0) }
				jsonPath("$.totalPages") { value(0) }
			}
	}

	@Test
	fun `lista anonimamente somente produtos ativos com imagens ordenadas`() {
		val categoryId = insertCategory()
		val activeId = insertProduct(categoryId, "Vestido floral", "ACTIVE", 2)
		val inactiveId = insertProduct(categoryId, "Vestido indisponível", "INACTIVE", 3)
		insertImage(activeId, primary = false, order = 2)
		insertImage(activeId, primary = true, order = 0)
		insertImage(inactiveId, primary = true, order = 0)

		mockMvc.get("/catalog/products")
			.andExpect {
				status { isOk() }
				jsonPath("$.items.length()") { value(1) }
				jsonPath("$.items[0].id") { value(activeId.toString()) }
				jsonPath("$.items[0].name") { value("Vestido floral") }
				jsonPath("$.items[0].category.name") { value("Vestidos") }
				jsonPath("$.items[0].images[0].displayOrder") { value(0) }
				jsonPath("$.items[0].images[1].displayOrder") { value(2) }
				jsonPath("$.items[0].mainImage.displayOrder") { value(0) }
				jsonPath("$.items[0].images[0].externalId") { doesNotExist() }
				jsonPath("$.totalElements") { value(1) }
			}
	}

	@Test
	fun `consulta anonimamente o detalhe de produto ativo`() {
		val categoryId = insertCategory()
		val productId = insertProduct(categoryId, "Camisa de linho", "ACTIVE", 1)
		insertImage(productId, primary = true, order = 0)

		mockMvc.get("/catalog/products/$productId")
			.andExpect {
				status { isOk() }
				jsonPath("$.id") { value(productId.toString()) }
				jsonPath("$.description") { value("Descrição de Camisa de linho") }
				jsonPath("$.images.length()") { value(1) }
			}
	}

	@Test
	fun `produto inativo nao e revelado no detalhe publico`() {
		val categoryId = insertCategory()
		val productId = insertProduct(categoryId, "Produto inativo", "INACTIVE", 1)
		insertImage(productId, primary = true, order = 0)

		assertNotFound(productId)
	}

	@Test
	fun `produto inexistente retorna erro estavel`() {
		assertNotFound(UUID.randomUUID())
	}

	private fun assertNotFound(productId: UUID) {
		mockMvc.get("/catalog/products/$productId")
			.andExpect {
				status { isNotFound() }
				content { contentTypeCompatibleWith(MediaType.APPLICATION_JSON) }
				jsonPath("$.status") { value(404) }
				jsonPath("$.code") { value("CATALOG_008") }
				jsonPath("$.message") { value("Produto não encontrado.") }
				jsonPath("$.path") { value("/catalog/products/$productId") }
			}
	}

	private fun insertCategory(): UUID {
		val id = UUID.randomUUID()
		jdbcTemplate.update(
			"INSERT INTO category (id, name, normalized_name, created_at, updated_at) VALUES (?, ?, ?, ?, ?)",
			id,
			"Vestidos",
			"vestidos",
			NOW,
			NOW,
		)
		return id
	}

	private fun insertProduct(categoryId: UUID, name: String, status: String, minute: Long): UUID {
		val id = UUID.randomUUID()
		val createdAt = NOW.plusMinutes(minute)
		jdbcTemplate.update(
			"""
			INSERT INTO product (id, category_id, name, description, status, created_at, updated_at)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""".trimIndent(),
			id,
			categoryId,
			name,
			"Descrição de $name",
			status,
			createdAt,
			createdAt,
		)
		return id
	}

	private fun insertImage(productId: UUID, primary: Boolean, order: Int) {
		val id = UUID.randomUUID()
		jdbcTemplate.update(
			"""
			INSERT INTO product_image
			    (id, product_id, external_id, secure_url, is_primary, display_order, created_at)
			VALUES (?, ?, ?, ?, ?, ?, ?)
			""".trimIndent(),
			id,
			productId,
			"catalog/$id",
			"https://res.cloudinary.com/demo/image/upload/$id.jpg",
			primary,
			order,
			NOW,
		)
	}

	companion object {
		private val NOW: OffsetDateTime = OffsetDateTime.of(2026, 9, 19, 12, 0, 0, 0, ZoneOffset.UTC)
	}
}
