package com.viniciusdevassis.laumileymodas.presentation.dtos

import com.viniciusdevassis.laumileymodas.domain.enums.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class CustomerRegistrationRequest(
	@field:NotBlank @field:Size(max = 120) val firstName: String,
	@field:NotBlank @field:Size(max = 120) val lastName: String,
	@field:NotBlank @field:Email @field:Size(max = 320) val email: String,
	@field:NotBlank @field:Size(min = 8, max = 200) val password: String,
	@field:NotBlank @field:Pattern(regexp = "^\\+[1-9][0-9]{7,14}$") val whatsappPhone: String,
)

data class LoginRequest(
	@field:NotBlank @field:Email val email: String,
	@field:NotBlank val password: String,
)

data class TokenResponse(
	val accessToken: String,
	val tokenType: String = "Bearer",
	val expiresIn: Long,
	val role: Role,
)
