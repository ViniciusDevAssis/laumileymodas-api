package com.viniciusdevassis.laumileymodas.presentation.auth

import jakarta.validation.constraints.*

data class RegisterCustomerRequest(@field:NotBlank @field:Size(max=100) val firstName: String, @field:NotBlank @field:Size(max=100) val lastName: String, @field:Email @field:Size(max=320) val email: String, @field:Size(min=8,max=64) val password: String, @field:NotBlank @field:Size(max=20) val whatsappPhone: String)
data class LoginRequest(@field:Email @field:Size(max=320) val email: String, @field:NotBlank @field:Size(max=64) val password: String)
data class GoogleCustomerRegistrationRequest(@field:NotBlank @field:Size(max=100) val firstName: String, @field:NotBlank @field:Size(max=100) val lastName: String, @field:NotBlank @field:Size(max=20) val whatsappPhone: String)
