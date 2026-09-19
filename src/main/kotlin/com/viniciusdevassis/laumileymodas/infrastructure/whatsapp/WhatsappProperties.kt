package com.viniciusdevassis.laumileymodas.infrastructure.whatsapp

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("laumiley.whatsapp")
data class WhatsappProperties(val number: String = "", val messageTemplate: String = "Olá! Tenho interesse no produto {productName} (código {productId}).")
