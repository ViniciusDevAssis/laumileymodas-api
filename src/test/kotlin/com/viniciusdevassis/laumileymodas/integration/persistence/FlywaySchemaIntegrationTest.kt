package com.viniciusdevassis.laumileymodas.integration.persistence

import com.viniciusdevassis.laumileymodas.integration.support.PostgresIntegrationTest
import jakarta.persistence.EntityManagerFactory
import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate

@SpringBootTest
class FlywaySchemaIntegrationTest : PostgresIntegrationTest() {

	@Autowired
	private lateinit var flyway: Flyway

	@Autowired
	private lateinit var entityManagerFactory: EntityManagerFactory

	@Autowired
	private lateinit var jdbcTemplate: JdbcTemplate

	@Test
	fun `aplica as migrations e inicializa o Hibernate com o schema validado`() {
		assertThat(flyway.info().current()?.version?.version).isEqualTo("2")
		assertThat(entityManagerFactory.isOpen).isTrue()

		val tables = jdbcTemplate.queryForList(
			"""
			SELECT table_name
			FROM information_schema.tables
			WHERE table_schema = 'public'
			  AND table_name IN (
			    'account',
			    'account_external_identity',
			    'customer',
			    'refresh_token',
			    'oauth_handoff',
			    'category',
			    'product',
			    'product_image'
			  )
			""".trimIndent(),
			String::class.java,
		)

		assertThat(tables).containsExactlyInAnyOrder(
			"account",
			"account_external_identity",
			"customer",
			"refresh_token",
			"oauth_handoff",
			"category",
			"product",
			"product_image",
		)
	}
}
