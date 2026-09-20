package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.application.auth.CompleteGoogleLoginUseCase
import com.viniciusdevassis.laumileymodas.domain.account.OAuthHandoffPurpose
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.GoogleIdentityVerifier
import com.viniciusdevassis.laumileymodas.presentation.security.RestAccessDeniedHandler
import com.viniciusdevassis.laumileymodas.presentation.security.RestAuthenticationEntryPoint
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.core.annotation.Order
import org.springframework.core.convert.converter.Converter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseCookie
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.csrf.CookieCsrfTokenRepository
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Configuration
@EnableMethodSecurity
class SecurityConfiguration(
    private val entryPoint: RestAuthenticationEntryPoint,
    private val denied: RestAccessDeniedHandler,
) {
    private val matcher = PathPatternRequestMatcher.withDefaults()

    @Bean
    @Order(1)
    fun oauth2SecurityFilterChain(
        http: HttpSecurity,
        oauthSuccess: GoogleOAuthSuccessHandler,
        oauthFailure: GoogleOAuthFailureHandler,
    ): SecurityFilterChain {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED) }
        http.authorizeHttpRequests { it.anyRequest().permitAll() }
        http.oauth2Login {
            it.successHandler(oauthSuccess)
                .failureHandler(oauthFailure)
        }
        http.exceptionHandling { it.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied) }
        return http.build()
    }

    @Bean
    @Order(2)
    fun apiSecurityFilterChain(
        http: HttpSecurity,
        decoder: JwtDecoder,
        originFilter: CookieEndpointOriginFilter,
    ): SecurityFilterChain {
        val csrf = CookieCsrfTokenRepository.withHttpOnlyFalse().apply {
            setCookieName("XSRF-TOKEN")
            setHeaderName("X-XSRF-TOKEN")
            setCookiePath("/api/v1")
        }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.cors { }
        http.csrf {
            it.csrfTokenRepository(csrf)
                .csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(
                    matcher.matcher(HttpMethod.POST, "/auth/customers"),
                    matcher.matcher(HttpMethod.POST, "/auth/login"),
                    matcher.matcher("/catalog/**"),
                )
        }
        http.authorizeHttpRequests {
            it.requestMatchers(
                "/catalog/**",
                "/auth/customers",
                "/auth/login",
                "/auth/csrf",
            ).permitAll()
                .requestMatchers("/products/*/interests", "/customers/me/**").hasRole("CLIENT")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/auth/refresh", "/auth/logout", "/auth/google/exchange", "/auth/google/customers")
                .permitAll()
                .requestMatchers("/auth/me").authenticated()
                .anyRequest().denyAll()
        }
        http.oauth2ResourceServer {
            it.jwt { jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(jwtConverter()) }
                .authenticationEntryPoint(entryPoint)
                .accessDeniedHandler(denied)
        }
        http.exceptionHandling { it.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied) }
        http.addFilterBefore(originFilter, CsrfFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(
        @Value("\${laumiley.frontend.allowed-origins}") origins: String,
    ): CorsConfigurationSource = UrlBasedCorsConfigurationSource().apply {
        registerCorsConfiguration(
            "/**",
            CorsConfiguration().apply {
                allowedOrigins = origins.split(',').map(String::trim)
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Authorization", "Content-Type", "Idempotency-Key", "X-XSRF-TOKEN")
                allowCredentials = true
            },
        )
    }

    @Bean
    @Primary
    fun accessJwtDecoder(keys: JwtKeyPair, properties: JwtProperties): JwtDecoder {
        val decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey).build()
        val access = OAuth2TokenValidator<Jwt> { jwt ->
            if (
                jwt.getClaimAsString("iss") == properties.issuer &&
                jwt.audience.contains(properties.accessAudience) &&
                jwt.getClaimAsString("typ") == "access"
            ) {
                OAuth2TokenValidatorResult.success()
            } else {
                OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token"))
            }
        }
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(JwtTimestampValidator(), access))
        return decoder
    }

    private fun jwtConverter(): Converter<Jwt, out AbstractAuthenticationToken> = Converter { jwt ->
        JwtAuthenticationToken(
            jwt,
            listOf(SimpleGrantedAuthority("ROLE_${jwt.getClaimAsString("role")}")),
            jwt.subject,
        )
    }
}

@Component
class CookieEndpointOriginFilter(
    @Value("\${laumiley.frontend.allowed-origins}") origins: String,
    private val denied: RestAccessDeniedHandler,
) : OncePerRequestFilter() {
    private val allowed = origins.split(',').map(String::trim).toSet()
    private val paths = setOf("/auth/refresh", "/auth/logout", "/auth/google/exchange", "/auth/google/customers")

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (request.method == "POST" && paths.contains(request.servletPath)) {
            val origin = request.getHeader("Origin")
            if (origin == null || !allowed.contains(origin)) {
                denied.handle(
                    request,
                    response,
                    org.springframework.security.access.AccessDeniedException("Origem não autorizada"),
                )
                return
            }
        }
        chain.doFilter(request, response)
    }
}

@Component
class GoogleOAuthSuccessHandler(
    private val verifier: GoogleIdentityVerifier,
    private val complete: CompleteGoogleLoginUseCase,
    private val failureHandler: GoogleOAuthFailureHandler,
    @Value("\${laumiley.frontend.auth-success-url}") private val successUrl: String,
    @Value("\${laumiley.frontend.registration-url}") private val registrationUrl: String,
    @Value("\${laumiley.security.oauth-handoff.cookie-name}") private val handoffName: String,
    @Value("\${laumiley.security.oauth-handoff.cookie-path}") private val handoffPath: String,
    @Value("\${laumiley.security.oauth-handoff.secure:false}") private val secure: Boolean,
    @Value("\${laumiley.security.oauth-handoff.same-site:Lax}") private val sameSite: String,
) : AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication,
    ) {
        try {
            val result = complete.execute(verifier.verify(authentication.principal as OidcUser))
            response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(handoffName, result.rawHandle)
                    .httpOnly(true)
                    .secure(secure)
                    .sameSite(sameSite)
                    .path(handoffPath)
                    .maxAge(Duration.ofMinutes(10))
                    .build()
                    .toString(),
            )
            request.getSession(false)?.invalidate()
            response.sendRedirect(
                if (result.purpose == OAuthHandoffPurpose.EXISTING_ACCOUNT_LOGIN) successUrl else registrationUrl,
            )
        } catch (exception: Exception) {
            failureHandler.onAuthenticationFailure(
                request,
                response,
                BadCredentialsException("Falha ao concluir autenticação Google", exception),
            )
        }
    }
}

@Component
class GoogleOAuthFailureHandler(
    @Value("\${laumiley.frontend.auth-error-url}") private val errorUrl: String,
) : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: org.springframework.security.core.AuthenticationException,
    ) {
        request.getSession(false)?.invalidate()
        response.sendRedirect(errorUrl)
    }
}
