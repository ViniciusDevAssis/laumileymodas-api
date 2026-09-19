package com.viniciusdevassis.laumileymodas.infrastructure.whatsapp

import com.viniciusdevassis.laumileymodas.application.port.whatsapp.WhatsappLinkGenerator
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

@Component
@EnableConfigurationProperties(WhatsappProperties::class)
class WaMeLinkGenerator(private val properties: WhatsappProperties): WhatsappLinkGenerator {
	override fun generate(productId: UUID, productName: String): String {
		val number = properties.number.filter(Char::isDigit)
		require(number.isNotBlank()) { "Número do WhatsApp não configurado." }
		val text = properties.messageTemplate.replace("{productName}", productName).replace("{productId}", productId.toString())
		return "https://wa.me/$number?text=${URLEncoder.encode(text, StandardCharsets.UTF_8).replace("+", "%20")}" 
	}
}
