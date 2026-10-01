package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.MediaStorage
import com.viniciusdevassis.laumileymodas.application.StoredMedia
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@TestConfiguration
class FakeMediaStorageConfiguration {
	@Bean
	@Primary
	fun fakeMediaStorage() = FakeMediaStorage()
}

class FakeMediaStorage : MediaStorage {
	val uploaded = CopyOnWriteArrayList<StoredMedia>()
	val deleted = CopyOnWriteArrayList<String>()
	var failOnUploadNumber: Int? = null
	var failDelete = false
	private var uploadCount = 0

	override fun upload(content: ByteArray, contentType: String): StoredMedia {
		uploadCount++
		if (uploadCount == failOnUploadNumber) error("Falha de upload simulada")
		return StoredMedia("https://media.example/${UUID.randomUUID()}.jpg", "laumiley/${UUID.randomUUID()}").also(uploaded::add)
	}

	override fun delete(externalId: String) {
		if (failDelete) error("Falha de exclusão simulada")
		deleted += externalId
	}

	fun reset() {
		uploaded.clear()
		deleted.clear()
		failOnUploadNumber = null
		failDelete = false
		uploadCount = 0
	}
}
