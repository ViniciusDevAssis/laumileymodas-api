package com.viniciusdevassis.laumileymodas.integration.support

import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@ActiveProfiles("test")
abstract class PostgresIntegrationTest {

	companion object {
		@JvmStatic
		val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17-alpine")
			.withDatabaseName("laumiley_test")
			.withUsername("test")
			.withPassword("test")
			.apply { start() }

		@DynamicPropertySource
		@JvmStatic
		fun postgresProperties(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
			registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
		}
	}
}
