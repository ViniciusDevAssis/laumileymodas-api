package com.viniciusdevassis.laumileymodas.presentation.auth

import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.GoogleIntentAuthorizationRequestResolver
import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.OAuthAuthorizationRequestCookieRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("/auth/google")
class GoogleAuthController(
	private val resolver: GoogleIntentAuthorizationRequestResolver,
	private val authorizationRequests: OAuthAuthorizationRequestCookieRepository,
) {
	@GetMapping("/client")
	fun client(request: HttpServletRequest, response: HttpServletResponse) = start(request, response)

	@GetMapping("/admin")
	fun admin(request: HttpServletRequest, response: HttpServletResponse) = start(request, response)

	private fun start(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Void> {
		val authorizationRequest = requireNotNull(resolver.resolve(request))
		authorizationRequests.saveAuthorizationRequest(authorizationRequest, request, response)
		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(authorizationRequest.authorizationRequestUri))
			.build()
	}
}
