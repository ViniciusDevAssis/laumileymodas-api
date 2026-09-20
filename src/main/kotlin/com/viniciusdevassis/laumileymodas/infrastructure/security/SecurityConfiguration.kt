package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(
	private val authenticationEntryPoint: ApiAuthenticationEntryPoint,
	private val accessDeniedHandler: ApiAccessDeniedHandler,
	@Value("\${laumiley.frontend.allowed-origins}") private val allowedOrigins: String,
) {
	@Bean
	fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
		http
			.csrf { it.disable() }
			.cors { }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.exceptionHandling {
				it.authenticationEntryPoint(authenticationEntryPoint)
				it.accessDeniedHandler(accessDeniedHandler)
			}
			.authorizeHttpRequests {
				it
					.requestMatchers(
						"/oauth2/**",
						"/login/oauth2/**",
						"/error",
					).permitAll()
					.requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()
					.anyRequest().authenticated()
			}
			.httpBasic { it.disable() }
			.formLogin { it.disable() }
			.logout { it.disable() }
			.build()

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration = CorsConfiguration()
		configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotBlank() }
		configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
		configuration.allowedHeaders = listOf("Authorization", "Content-Type", "X-XSRF-TOKEN", "Idempotency-Key")
		configuration.exposedHeaders = listOf("Location")
		configuration.allowCredentials = true

		return UrlBasedCorsConfigurationSource().also {
			it.registerCorsConfiguration("/**", configuration)
		}
	}
}
