package com.viniciusdevassis.laumileymodas.integration

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest
@ActiveProfiles("test")
abstract class PostgresIntegrationTest {
	companion object {
		private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17-alpine").also { it.start() }

		@DynamicPropertySource
		@JvmStatic
		fun configurePostgres(registry: DynamicPropertyRegistry) {
			registry.add("spring.datasource.url", postgres::getJdbcUrl)
			registry.add("spring.datasource.username", postgres::getUsername)
			registry.add("spring.datasource.password", postgres::getPassword)
			registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
		}
	}
}
