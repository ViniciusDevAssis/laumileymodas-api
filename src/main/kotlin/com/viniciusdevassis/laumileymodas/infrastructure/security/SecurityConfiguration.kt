package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.application.auth.CompleteGoogleLoginUseCase
import com.viniciusdevassis.laumileymodas.domain.account.OAuthHandoffPurpose
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.*
import com.viniciusdevassis.laumileymodas.presentation.security.*
import jakarta.servlet.FilterChain
import jakarta.servlet.http.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.*
import org.springframework.core.convert.converter.Converter
import org.springframework.http.*
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.server.resource.authentication.*
import org.springframework.security.web.*
import org.springframework.security.web.csrf.*
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.stereotype.Component
import org.springframework.web.cors.*
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Configuration
@EnableMethodSecurity
class SecurityConfiguration(
    private val entryPoint: RestAuthenticationEntryPoint,
    private val denied: RestAccessDeniedHandler,
) {

	val matcher = PathPatternRequestMatcher.withDefaults()
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        decoder: JwtDecoder,
        oauthSuccess: GoogleOAuthSuccessHandler,
        oauthFailure: GoogleOAuthFailureHandler,
        originFilter: CookieEndpointOriginFilter,
        googleAuthorizationRequestResolver: GoogleIntentAuthorizationRequestResolver
    ): SecurityFilterChain {
        val csrf = CookieCsrfTokenRepository.withHttpOnlyFalse()
            .apply { setCookieName("XSRF-TOKEN"); setHeaderName("X-XSRF-TOKEN"); setCookiePath("/api/v1") }
        http.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        http.cors { }
        http.csrf {
            it.csrfTokenRepository(csrf)
				.csrfTokenRequestHandler(CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(
                    matcher.matcher(HttpMethod.POST, "/auth/customers"),
                    matcher.matcher(HttpMethod.POST, "/auth/login"),
                    matcher.matcher("/catalog/**"),
                    matcher.matcher("/oauth2/**"),
                    matcher.matcher("/auth/google/callback")
                )
        }
        http.authorizeHttpRequests {
            it.requestMatchers(
                "/catalog/**",
                "/auth/customers",
                "/auth/login",
                "/auth/csrf",
                "/auth/google/client",
                "/auth/google/admin",
                "/auth/google/callback",
                "/oauth2/**"
            ).permitAll()
                .requestMatchers("/products/*/interests", "/customers/me/**").hasRole("CLIENT")
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/auth/refresh", "/auth/logout", "/auth/google/exchange", "/auth/google/customers")
                .permitAll()
                .requestMatchers("/auth/me").authenticated().anyRequest().denyAll()
        }
        http.oauth2ResourceServer {
            it.jwt { jwt -> jwt.decoder(decoder).jwtAuthenticationConverter(jwtConverter()) }
                .authenticationEntryPoint(entryPoint).accessDeniedHandler(denied)
        }
        http.oauth2Login {
            it.authorizationEndpoint { endpoint ->
                endpoint.authorizationRequestRepository(oauthSuccess.authorizationRequestRepository)
                    .authorizationRequestResolver(googleAuthorizationRequestResolver)
            }.redirectionEndpoint { endpoint -> endpoint.baseUri("/auth/google/callback") }.successHandler(oauthSuccess)
                .failureHandler(oauthFailure)
        }
        http.exceptionHandling { it.authenticationEntryPoint(entryPoint).accessDeniedHandler(denied) }
        http.addFilterBefore(originFilter, CsrfFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(@Value("\${laumiley.frontend.allowed-origins}") origins: String): CorsConfigurationSource =
        UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration(
                "/**",
                CorsConfiguration().apply {
                    allowedOrigins = origins.split(',').map(String::trim); allowedMethods =
                    listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"); allowedHeaders =
                    listOf("Authorization", "Content-Type", "Idempotency-Key", "X-XSRF-TOKEN"); allowCredentials = true
                })
        }

    @Bean
    @Primary
    fun accessJwtDecoder(keys: JwtKeyPair, properties: JwtProperties): JwtDecoder {
        val decoder = NimbusJwtDecoder.withPublicKey(keys.publicKey).build()
        val access = OAuth2TokenValidator<Jwt> { jwt ->
            if (jwt.getClaimAsString("iss") == properties.issuer && jwt.audience.contains(properties.accessAudience) && jwt.getClaimAsString(
                    "typ"
                ) == "access"
            ) OAuth2TokenValidatorResult.success() else OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token"))
        }
        decoder.setJwtValidator(DelegatingOAuth2TokenValidator(JwtTimestampValidator(), access)); return decoder
    }

    private fun jwtConverter(): Converter<Jwt, out org.springframework.security.authentication.AbstractAuthenticationToken> =
        Converter { jwt ->
            JwtAuthenticationToken(
                jwt,
                listOf(SimpleGrantedAuthority("ROLE_${jwt.getClaimAsString("role")}")),
                jwt.subject
            )
        }
}

@Component
class CookieEndpointOriginFilter(
    @Value("\${laumiley.frontend.allowed-origins}") origins: String,
    private val denied: RestAccessDeniedHandler
) : OncePerRequestFilter() {
    private val allowed = origins.split(',').map(String::trim).toSet();
    private val paths = setOf("/auth/refresh", "/auth/logout", "/auth/google/exchange", "/auth/google/customers")
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        if (request.method == "POST" && paths.contains(request.servletPath)) {
            val origin = request.getHeader("Origin"); if (origin == null || !allowed.contains(origin)) {
                denied.handle(
                    request,
                    response,
                    org.springframework.security.access.AccessDeniedException("Origem não autorizada")
                ); return
            }
        }
        chain.doFilter(request, response)
    }
}

@Component
class GoogleOAuthSuccessHandler(
    private val verifier: GoogleIdentityVerifier,
    private val complete: CompleteGoogleLoginUseCase,
    val authorizationRequestRepository: OAuthAuthorizationRequestCookieRepository,
    @Value("\${laumiley.frontend.auth-success-url}") private val successUrl: String,
    @Value("\${laumiley.frontend.registration-url}") private val registrationUrl: String,
    @Value("\${laumiley.security.oauth-handoff.cookie-name}") private val handoffName: String,
    @Value("\${laumiley.security.oauth-handoff.cookie-path}") private val handoffPath: String,
    @Value("\${laumiley.security.oauth-handoff.secure:false}") private val secure: Boolean,
    @Value("\${laumiley.security.oauth-handoff.same-site:Lax}") private val sameSite: String
) : org.springframework.security.web.authentication.AuthenticationSuccessHandler {
    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: org.springframework.security.core.Authentication
    ) {
        val intent = request.getAttribute(OAuthAuthorizationRequestCookieRepository.INTENT_ATTRIBUTE) as? String
            ?: throw org.springframework.security.authentication.BadCredentialsException("Intenção OAuth ausente")
        val result = complete.execute(
            verifier.verify(authentication.principal as org.springframework.security.oauth2.core.oidc.user.OidcUser),
            intent == "ADMIN"
        )
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            ResponseCookie.from(handoffName, result.rawHandle).httpOnly(true).secure(secure).sameSite(sameSite)
                .path(handoffPath).maxAge(Duration.ofMinutes(10)).build().toString()
        )
        response.sendRedirect(if (result.purpose == OAuthHandoffPurpose.EXISTING_ACCOUNT_LOGIN) successUrl else registrationUrl)
    }
}

@Component
class GoogleOAuthFailureHandler(@Value("\${laumiley.frontend.auth-error-url}") private val errorUrl: String) :
    org.springframework.security.web.authentication.AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: org.springframework.security.core.AuthenticationException
    ) {
        response.sendRedirect(errorUrl)
    }
}
