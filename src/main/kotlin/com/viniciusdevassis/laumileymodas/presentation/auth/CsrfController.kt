package com.viniciusdevassis.laumileymodas.presentation.auth

import org.springframework.http.ResponseEntity
import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController class CsrfController {
	@GetMapping("/auth/csrf") fun csrf(token: CsrfToken): ResponseEntity<Void> { token.token; return ResponseEntity.noContent().build() }
}
