package com.viniciusdevassis.laumileymodas.application

data class StoredMedia(val url: String, val externalId: String)

interface MediaStorage {
	fun upload(content: ByteArray, contentType: String): StoredMedia
	fun delete(externalId: String)
}
