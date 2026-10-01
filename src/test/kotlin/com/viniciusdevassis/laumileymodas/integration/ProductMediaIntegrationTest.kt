package com.viniciusdevassis.laumileymodas.integration

import com.viniciusdevassis.laumileymodas.application.CatalogService
import com.viniciusdevassis.laumileymodas.application.ProductMediaService
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CategoryRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ProductRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@AutoConfigureMockMvc
@Import(FakeMediaStorageConfiguration::class)
class ProductMediaIntegrationTest(
	@Autowired private val catalogService: CatalogService,
	@Autowired private val productMediaService: ProductMediaService,
	@Autowired private val mediaStorage: FakeMediaStorage,
	@Autowired private val productRepository: ProductRepository,
	@Autowired private val categoryRepository: CategoryRepository,
) : PostgresIntegrationTest() {
	@BeforeEach
	fun clear() {
		mediaStorage.reset()
		productRepository.deleteAll()
		categoryRepository.deleteAll()
	}

	@Test
	fun `falha de upload compensa asset anterior e nao persiste produto ou imagem`() {
		val category = catalogService.createCategory("Blusas")
		mediaStorage.failOnUploadNumber = 2

		assertFailsWith<ApiException> {
			productMediaService.createProduct(
				"Blusa", "Descrição", requireNotNull(category.id), ProductStatus.ACTIVE, 0,
				listOf(byteArrayOf(1) to "image/jpeg", byteArrayOf(2) to "image/jpeg"),
			)
		}

		assertEquals(1, mediaStorage.uploaded.size)
		assertEquals(listOf(mediaStorage.uploaded.single().externalId), mediaStorage.deleted.toList())
		assertEquals(0, productRepository.count())
	}

	@Test
	fun `falha de persistencia apos upload remove asset externo`() {
		val missingCategory = UUID.randomUUID()
		assertFailsWith<ApiException> {
			productMediaService.createProduct(
				"Vestido", "Descrição", missingCategory, ProductStatus.INACTIVE, 0,
				listOf(byteArrayOf(1) to "image/jpeg"),
			)
		}

		assertEquals(1, mediaStorage.uploaded.size)
		assertEquals(1, mediaStorage.deleted.size)
		assertEquals(0, productRepository.count())
	}

	@Test
	fun `falha de exclusao externa preserva imagem e referencia local`() {
		val category = catalogService.createCategory("Calças")
		val product = productMediaService.createProduct(
			"Calça", "Descrição", requireNotNull(category.id), ProductStatus.INACTIVE, 0,
			listOf(byteArrayOf(1) to "image/jpeg"),
		)
		val image = product.images.single()
		mediaStorage.failDelete = true

		assertFailsWith<ApiException> { productMediaService.deleteImage(requireNotNull(product.id), requireNotNull(image.id)) }

		assertEquals(1, productRepository.findWithDetailsById(requireNotNull(product.id))?.images?.size)
		assertEquals(image.externalId, productRepository.findWithDetailsById(requireNotNull(product.id))?.images?.single()?.externalId)
	}

	@Test
	fun `produto inativo nao pode excluir sua unica imagem`() {
		val category = catalogService.createCategory("Conjuntos")
		val product = productMediaService.createProduct(
			"Conjunto", "Descrição", requireNotNull(category.id), ProductStatus.INACTIVE, 0,
			listOf(byteArrayOf(1) to "image/jpeg"),
		)
		val image = product.images.single()

		assertFailsWith<ApiException> { productMediaService.deleteImage(requireNotNull(product.id), requireNotNull(image.id)) }

		val persisted = requireNotNull(productRepository.findWithDetailsById(requireNotNull(product.id)))
		assertEquals(1, persisted.images.size)
		assertEquals(1, persisted.images.count { it.primary })
		assertEquals(emptyList(), mediaStorage.deleted)
	}

	@Test
	fun `excluir imagem principal de produto inativo promove outra imagem`() {
		val category = catalogService.createCategory("Jaquetas")
		val product = productMediaService.createProduct(
			"Jaqueta", "Descrição", requireNotNull(category.id), ProductStatus.INACTIVE, 0,
			listOf(byteArrayOf(1) to "image/jpeg", byteArrayOf(2) to "image/jpeg"),
		)
		val deletedPrimary = product.images.single { it.primary }

		productMediaService.deleteImage(requireNotNull(product.id), requireNotNull(deletedPrimary.id))

		val persisted = requireNotNull(productRepository.findWithDetailsById(requireNotNull(product.id)))
		assertEquals(1, persisted.images.size)
		assertEquals(1, persisted.images.count { it.primary })
		assertEquals(true, persisted.images.single().primary)
		assertEquals(listOf(deletedPrimary.externalId), mediaStorage.deleted.toList())
	}
}
