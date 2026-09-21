package com.viniciusdevassis.laumileymodas.infrastructure.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfiguration(
	@Value("\${laumiley.frontend.allowed-origins}") private val allowedOrigins: String,
	@Value("\${laumiley.frontend.auth-error-url}") private val authErrorUrl: String,
	@Value("\${laumiley.security.cookies.secure}") private val secureCookies: Boolean,
) {
	@Bean
	@Order(1)
	fun oauth2SecurityFilterChain(http: HttpSecurity, oauth2SuccessHandler: OAuth2SuccessHandler): SecurityFilterChain =
		http
			.securityMatcher("/oauth2/**", "/login/oauth2/**")
			.cors { }
			.csrf { it.disable() }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
			.authorizeHttpRequests { it.anyRequest().permitAll() }
			.oauth2Login {
				it.successHandler(oauth2SuccessHandler)
				it.failureHandler { _, response, _ -> response.sendRedirect(authErrorUrl) }
			}
			.build()

	@Bean
	@Order(2)
	fun apiSecurityFilterChain(
		http: HttpSecurity,
		securityFilter: SecurityFilter,
		authenticationEntryPoint: ApiAuthenticationEntryPoint,
		accessDeniedHandler: ApiAccessDeniedHandler,
	): SecurityFilterChain {
		val csrfRepository = CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
			setCookiePath("/api/v1")
			setSecure(secureCookies)
		}
		return http
			.securityMatcher("/**")
			.csrf {
				it.csrfTokenRepository(csrfRepository)
				it.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
				it.ignoringRequestMatchers("/auth/login", "/customers", "/customers/me/whatsapp", "/products/*/interests")
			}
			.cors { }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.exceptionHandling {
				it.authenticationEntryPoint(authenticationEntryPoint)
				it.accessDeniedHandler(accessDeniedHandler)
			}
			.authorizeHttpRequests {
				it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				it.requestMatchers(HttpMethod.GET, "/products", "/products/**", "/auth/csrf").permitAll()
				it.requestMatchers(HttpMethod.POST, "/customers", "/auth/login", "/auth/refresh", "/auth/logout").permitAll()
				it.requestMatchers(HttpMethod.GET, "/customers/me").hasRole("CLIENT")
				it.requestMatchers(HttpMethod.PUT, "/customers/me/whatsapp").hasRole("CLIENT")
				it.requestMatchers("/error").permitAll()
				it.requestMatchers("/admin/**").hasRole("ADMIN")
				it.requestMatchers(HttpMethod.POST, "/products/*/interests").hasRole("CLIENT")
				it.anyRequest().authenticated()
			}
			.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter::class.java)
			.httpBasic { it.disable() }
			.formLogin { it.disable() }
			.logout { it.disable() }
			.build()
	}

	@Bean
	fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

	@Bean
	fun securityFilterRegistration(securityFilter: SecurityFilter): FilterRegistrationBean<SecurityFilter> =
		FilterRegistrationBean(securityFilter).apply { isEnabled = false }

	@Bean
	fun authenticationManager(configuration: AuthenticationConfiguration): AuthenticationManager =
		configuration.authenticationManager

	@Bean
	fun corsConfigurationSource(): CorsConfigurationSource {
		val configuration = CorsConfiguration()
		configuration.allowedOrigins = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotBlank() }
		configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
		configuration.allowedHeaders = listOf("Authorization", "Content-Type", "X-XSRF-TOKEN", "Idempotency-Key")
		configuration.exposedHeaders = listOf("Location")
		configuration.allowCredentials = true
		return UrlBasedCorsConfigurationSource().also { it.registerCorsConfiguration("/**", configuration) }
	}
}
