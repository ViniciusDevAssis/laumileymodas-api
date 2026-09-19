package com.viniciusdevassis.laumileymodas.presentation.interest

import com.viniciusdevassis.laumileymodas.application.interest.InterestResult
import java.time.Instant
import java.util.UUID

data class InterestProductResponse(val id: UUID, val name: String)
data class InterestResponse(val interestId: UUID, val product: InterestProductResponse, val createdAt: Instant, val whatsappUrl: String)
data class WhatsappLinkResponse(val interestId: UUID, val whatsappUrl: String)
fun InterestResult.response() = InterestResponse(interest.id, InterestProductResponse(interest.productId, productName), interest.createdAt, whatsappUrl)
