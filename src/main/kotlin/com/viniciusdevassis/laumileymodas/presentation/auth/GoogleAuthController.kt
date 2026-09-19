package com.viniciusdevassis.laumileymodas.presentation.auth

import com.viniciusdevassis.laumileymodas.infrastructure.security.oauth.OAuthAuthorizationRequestCookieRepository
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestController @RequestMapping("/auth/google")
class GoogleAuthController(private val state: OAuthAuthorizationRequestCookieRepository) {
	@GetMapping("/client") fun client(response:HttpServletResponse)=start("CLIENT",response)
	@GetMapping("/admin") fun admin(response:HttpServletResponse)=start("ADMIN",response)
	private fun start(intent:String,response:HttpServletResponse):ResponseEntity<Void>{ response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("LAUMILEY_OAUTH_INTENT",state.seal(intent)).httpOnly(true).sameSite("Lax").path("/api/v1/auth/google").maxAge(java.time.Duration.ofMinutes(10)).build().toString()); return ResponseEntity.status(HttpStatus.FOUND).location(java.net.URI.create("/api/v1/oauth2/authorization/google")).build() }
}
