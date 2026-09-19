package com.viniciusdevassis.laumileymodas.integration.persistence

import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogQueryRepository
import com.viniciusdevassis.laumileymodas.integration.support.PostgresIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@SpringBootTest
class CatalogPersistenceIntegrationTest : PostgresIntegrationTest() {

	@Autowired
	private lateinit var repository: CatalogQueryRepository

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@BeforeEach
	fun cleanCatalog() {
		jdbcTemplate.update("DELETE FROM product_image")
		jdbcTemplate.update("DELETE FROM product")
		jdbcTemplate.update("DELETE FROM category")
	}

	@Test
	fun `pagina somente produtos ativos em ordem deterministica`() {
		val categoryId = insertCategory()
		val oldest = insertProduct(categoryId, "Produto antigo", "ACTIVE", 1)
		val newest = insertProduct(categoryId, "Produto novo", "ACTIVE", 3)
		insertProduct(categoryId, "Produto inativo", "INACTIVE", 4)
		insertImage(oldest, primary = true, order = 0)
		insertImage(newest, primary = true, order = 0)

		val firstPage = repository.findActive(page = 0, size = 1)
		val secondPage = repository.findActive(page = 1, size = 1)

		assertThat(firstPage.items.map { it.name }).containsExactly("Produto novo")
		assertThat(firstPage.totalElements).isEqualTo(2)
		assertThat(firstPage.totalPages).isEqualTo(2)
		assertThat(secondPage.items.map { it.name }).containsExactly("Produto antigo")
	}

	@Test
	fun `carrega imagens ordenadas e filtra detalhe pelo status ativo`() {
		val categoryId = insertCategory()
		val activeId = insertProduct(categoryId, "Produto ativo", "ACTIVE", 1)
		val inactiveId = insertProduct(categoryId, "Produto inativo", "INACTIVE", 2)
		insertImage(activeId, primary = false, order = 2)
		insertImage(activeId, primary = true, order = 0)
		insertImage(inactiveId, primary = true, order = 0)

		val active = repository.findActiveById(activeId)

		assertThat(active).isNotNull
		assertThat(active!!.images.map { it.displayOrder }).containsExactly(0, 2)
		assertThat(active.mainImage.displayOrder).isZero()
		assertThat(repository.findActiveById(inactiveId)).isNull()
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
