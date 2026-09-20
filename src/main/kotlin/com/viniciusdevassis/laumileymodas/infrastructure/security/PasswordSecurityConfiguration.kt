package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class PasswordSecurityConfiguration {
	@Bean
	fun passwordEncoder(): PasswordEncoder = DelegatingPasswordEncoder(
		"bcrypt",
		mapOf("bcrypt" to BCryptPasswordEncoder(12)),
	)
}
