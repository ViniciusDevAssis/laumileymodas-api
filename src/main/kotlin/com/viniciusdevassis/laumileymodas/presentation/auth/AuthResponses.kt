package com.viniciusdevassis.laumileymodas.presentation.auth

import com.viniciusdevassis.laumileymodas.application.auth.TokenPair
import com.viniciusdevassis.laumileymodas.domain.account.Account
import com.viniciusdevassis.laumileymodas.domain.customer.Customer
import java.time.Duration
import java.time.Instant
import java.util.UUID

data class CurrentAccountResponse(val id: UUID, val email: String, val role: String, val customerId: UUID?)
data class AuthTokenResponse(val accessToken: String, val tokenType: String = "Bearer", val expiresIn: Long, val account: CurrentAccountResponse)
fun Account.response(customer: Customer?) = CurrentAccountResponse(id, email, role.name, customer?.id)
fun TokenPair.response(account: Account, customer: Customer?, now: Instant) = AuthTokenResponse(accessToken, expiresIn = Duration.between(now, accessExpiresAt).seconds.coerceAtLeast(0), account = account.response(customer))
