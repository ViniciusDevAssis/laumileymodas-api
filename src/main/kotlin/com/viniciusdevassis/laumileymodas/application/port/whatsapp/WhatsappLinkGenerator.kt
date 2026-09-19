package com.viniciusdevassis.laumileymodas.application.port.whatsapp

import java.util.UUID

fun interface WhatsappLinkGenerator { fun generate(productId: UUID, productName: String): String }
