package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.application.GoogleAuthService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.stereotype.Component

@Component
class OAuth2SuccessHandler(
	private val googleAuthService: GoogleAuthService,
	private val refreshTokenCookie: RefreshTokenCookie,
	@Value("\${laumiley.frontend.auth-success-url}") private val authSuccessUrl: String,
) : AuthenticationSuccessHandler {
	override fun onAuthenticationSuccess(
		request: HttpServletRequest,
		response: HttpServletResponse,
		authentication: Authentication,
	) {
		val oidcUser = (authentication as? OAuth2AuthenticationToken)?.principal as? OidcUser
			?: throw IllegalStateException("O login Google não retornou uma identidade OIDC.")
		val refreshToken = googleAuthService.authenticate(oidcUser)
		response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(refreshToken).toString())
		request.getSession(false)?.invalidate()
		response.sendRedirect(authSuccessUrl)
	}
}
