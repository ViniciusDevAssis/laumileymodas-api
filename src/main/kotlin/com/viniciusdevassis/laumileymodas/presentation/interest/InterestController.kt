package com.viniciusdevassis.laumileymodas.presentation.interest

import com.viniciusdevassis.laumileymodas.application.interest.*
import org.springframework.http.*
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class InterestController(private val register: RegisterProductInterestUseCase, private val getLink: GetInterestWhatsappLinkUseCase) {
	@PostMapping("/products/{productId}/interests")
	fun register(@PathVariable productId: UUID, @RequestHeader("Idempotency-Key") key: String, @org.springframework.security.core.annotation.AuthenticationPrincipal jwt: Jwt): ResponseEntity<InterestResponse> {
		val result=register.execute(UUID.fromString(jwt.subject),productId,key)
		return ResponseEntity.status(if(result.created) HttpStatus.CREATED else HttpStatus.OK).body(result.response())
	}
	@GetMapping("/customers/me/interests/{interestId}/whatsapp-link")
	fun link(@PathVariable interestId:UUID,@org.springframework.security.core.annotation.AuthenticationPrincipal jwt:Jwt)=WhatsappLinkResponse(interestId,getLink.execute(UUID.fromString(jwt.subject),interestId))
}
