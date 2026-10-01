package com.viniciusdevassis.laumileymodas.infrastructure.cloudinary

import com.cloudinary.Cloudinary
import com.viniciusdevassis.laumileymodas.application.MediaStorage
import com.viniciusdevassis.laumileymodas.application.StoredMedia
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CloudinaryMediaStorage(
	@Value("\${laumiley.cloudinary.cloud-name}") cloudName: String,
	@Value("\${laumiley.cloudinary.api-key}") apiKey: String,
	@Value("\${laumiley.cloudinary.api-secret}") apiSecret: String,
) : MediaStorage {
	private val cloudinary = Cloudinary(mapOf("cloud_name" to cloudName, "api_key" to apiKey, "api_secret" to apiSecret))

	override fun upload(content: ByteArray, contentType: String): StoredMedia {
		val result = cloudinary.uploader().upload(
			content,
			mapOf("folder" to "laumiley/products", "resource_type" to "image"),
		)
		val url = result["secure_url"] as? String ?: error("Cloudinary não retornou a URL segura do asset.")
		val externalId = result["public_id"] as? String ?: error("Cloudinary não retornou o identificador do asset.")
		return StoredMedia(url, externalId)
	}

	override fun delete(externalId: String) {
		val result = cloudinary.uploader().destroy(externalId, emptyMap<String, Any>())
		if (result["result"] !in setOf("ok", "not found")) error("Cloudinary não confirmou a exclusão do asset.")
	}
}
