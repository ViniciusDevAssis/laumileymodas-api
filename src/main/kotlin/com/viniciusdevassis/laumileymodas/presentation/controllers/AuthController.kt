package com.viniciusdevassis.laumileymodas.presentation.controllers

import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.security.RefreshTokenCookie
import com.viniciusdevassis.laumileymodas.presentation.dtos.LoginRequest
import com.viniciusdevassis.laumileymodas.presentation.dtos.TokenResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
	private val authService: AuthService,
	private val refreshTokenCookie: RefreshTokenCookie,
) {
	@GetMapping("/auth/csrf")
	fun csrf(csrfToken: CsrfToken): ResponseEntity<Void> {
		csrfToken.token
		return ResponseEntity.noContent().build()
	}

	@PostMapping("/auth/login")
	fun login(@Valid @RequestBody request: LoginRequest, httpRequest: HttpServletRequest): ResponseEntity<TokenResponse> =
		withRefreshCookie(authService.login(request.email, request.password, httpRequest.remoteAddr ?: "unknown"))

	@PostMapping("/auth/refresh")
	fun refresh(@CookieValue(name = RefreshTokenCookie.COOKIE_NAME, required = false) refreshToken: String?): ResponseEntity<TokenResponse> {
		val raw = refreshToken ?: throw ApiException(ApiError.AUTH_001, HttpStatus.UNAUTHORIZED)
		return withRefreshCookie(authService.refresh(raw))
	}

	@PostMapping("/auth/logout")
	fun logout(@CookieValue(name = RefreshTokenCookie.COOKIE_NAME, required = false) refreshToken: String?): ResponseEntity<Void> {
		if (refreshToken != null) authService.logout(refreshToken)
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie.expire().toString())
			.build()
	}

	private fun withRefreshCookie(tokens: AuthService.AuthTokens): ResponseEntity<TokenResponse> =
		ResponseEntity.ok()
			.header(HttpHeaders.SET_COOKIE, refreshTokenCookie.create(tokens.refreshToken).toString())
			.body(TokenResponse(tokens.accessToken, expiresIn = tokens.expiresIn, role = tokens.role))
}
