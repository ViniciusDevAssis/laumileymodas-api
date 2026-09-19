package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.presentation.security.RestAccessDeniedHandler
import com.viniciusdevassis.laumileymodas.presentation.security.RestAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
class PublicCatalogSecurityConfiguration {

	@Bean
	@Order(100)
	fun publicCatalogSecurityFilterChain(
		http: HttpSecurity,
		authenticationEntryPoint: RestAuthenticationEntryPoint,
		accessDeniedHandler: RestAccessDeniedHandler,
	): SecurityFilterChain {
		http.securityMatcher("/catalog/products", "/catalog/products/**")
		http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
		http.authorizeHttpRequests {
			it.anyRequest().permitAll()
		}
		http.exceptionHandling {
			it.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
		}
		return http.build()
	}

	@Bean
	@Order(200)
	fun denyByDefaultSecurityFilterChain(
		http: HttpSecurity,
		authenticationEntryPoint: RestAuthenticationEntryPoint,
		accessDeniedHandler: RestAccessDeniedHandler,
	): SecurityFilterChain {
		http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
		http.authorizeHttpRequests { it.anyRequest().denyAll() }
		http.exceptionHandling {
			it.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
		}
		return http.build()
	}
}
