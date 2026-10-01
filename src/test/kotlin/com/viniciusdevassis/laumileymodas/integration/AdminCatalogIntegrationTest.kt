package com.viniciusdevassis.laumileymodas.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.viniciusdevassis.laumileymodas.application.AuthService
import com.viniciusdevassis.laumileymodas.application.GoogleAuthService
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountExternalIdentityRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CategoryRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.ProductRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.RefreshTokenRepository
import com.viniciusdevassis.laumileymodas.application.CatalogService
import com.viniciusdevassis.laumileymodas.application.ProductMediaService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.delete
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

@AutoConfigureMockMvc
@Import(FakeMediaStorageConfiguration::class)
class AdminCatalogIntegrationTest(
	@Autowired private val mockMvc: MockMvc,
	@Autowired private val objectMapper: ObjectMapper,
	@Autowired private val googleAuthService: GoogleAuthService,
	@Autowired private val authService: AuthService,
	@Autowired private val catalogService: CatalogService,
	@Autowired private val productMediaService: ProductMediaService,
	@Autowired private val mediaStorage: FakeMediaStorage,
	@Autowired private val accountRepository: AccountRepository,
	@Autowired private val identityRepository: AccountExternalIdentityRepository,
	@Autowired private val customerRepository: CustomerRepository,
	@Autowired private val refreshTokenRepository: RefreshTokenRepository,
	@Autowired private val productRepository: ProductRepository,
	@Autowired private val categoryRepository: CategoryRepository,
) : PostgresIntegrationTest() {
	private lateinit var admin: String

	@BeforeEach
	fun clear() {
		mediaStorage.reset()
		refreshTokenRepository.deleteAll()
		productRepository.deleteAll()
		categoryRepository.deleteAll()
		customerRepository.deleteAll()
		identityRepository.deleteAll()
		accountRepository.deleteAll()
		admin = adminBearer()
	}

	@Test
	fun `admin cria e edita categoria e produto e publica imagens ordenadas`() {
		val categoryResult = mockMvc.post("/admin/categories") {
			header("Authorization", admin)
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":"Vestidos"}"""
		}.andExpect { status { isCreated() }; jsonPath("name") { value("Vestidos") }; jsonPath("_links.self.href") { exists() } }
		val categoryId = UUID.fromString(objectMapper.readTree(categoryResult.andReturn().response.contentAsString).get("id").asText())
		mockMvc.put("/admin/categories/$categoryId") {
			header("Authorization", admin)
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":"Roupas"}"""
		}.andExpect { status { isOk() }; jsonPath("name") { value("Roupas") } }

		val metadata = MockMultipartFile("metadata", "", "application/json", """{"name":"Vestido azul","description":"Peça leve","categoryId":"$categoryId","status":"ACTIVE","primaryImageIndex":1}""".toByteArray())
		val firstImage = MockMultipartFile("images", "frente.jpg", "image/jpeg", byteArrayOf(1, 2, 3))
		val secondImage = MockMultipartFile("images", "costas.jpg", "image/jpeg", byteArrayOf(4, 5, 6))
		val created = mockMvc.perform(multipart("/admin/products").file(metadata).file(firstImage).file(secondImage).header("Authorization", admin))
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.images.length()").value(2))
			.andExpect(jsonPath("$.images[0].primary").value(true))
			.andReturn()
		val productId = objectMapper.readTree(created.response.contentAsString).get("id").asText()
		mockMvc.get("/products/$productId").andExpect {
			status { isOk() }
			jsonPath("$.images[0].primary") { value(true) }
			jsonPath("$.images[0].url") { exists() }
			jsonPath("$.images[0]._links.self.href") { exists() }
			jsonPath("$.images[0]._links.product.href") { exists() }
		}
		mockMvc.get("/admin/products") { header("Authorization", admin) }.andExpect {
			status { isOk() }
			jsonPath("$._embedded.products.length()") { value(1) }
		}

		mockMvc.put("/admin/products/$productId") {
			header("Authorization", admin)
			contentType = MediaType.APPLICATION_JSON
			content = """{"name":"Vestido novo","description":"Atualizado","categoryId":"$categoryId"}"""
		}.andExpect { status { isOk() }; jsonPath("name") { value("Vestido novo") } }
		mockMvc.patch("/admin/products/$productId") {
			header("Authorization", admin)
			contentType = MediaType.valueOf("application/merge-patch+json")
			content = """{"status":"INACTIVE"}"""
		}.andExpect { status { isOk() }; jsonPath("status") { value("INACTIVE") } }
		mockMvc.get("/products/$productId").andExpect { status { isNotFound() } }
		mockMvc.patch("/admin/products/$productId") {
			header("Authorization", admin)
			contentType = MediaType.valueOf("application/merge-patch+json")
			content = """{"status":"ACTIVE"}"""
		}.andExpect { status { isOk() } }
	}

	@Test
	fun `admin troca imagem principal reordena adiciona e remove sem violar invariantes`() {
		val category = catalogService.createCategory("Saias")
		val product = createActiveProduct(requireNotNull(category.id))
		val oldPrimary = product.orderedImages().first { it.primary }
		val other = product.images.first { !it.primary }
		mockMvc.patch("/admin/products/${product.id}/images/${other.id}") {
			header("Authorization", admin)
			contentType = MediaType.valueOf("application/merge-patch+json")
			content = """{"primary":true,"displayOrder":0}"""
		}.andExpect { status { isOk() }; jsonPath("$.primary") { value(true) }; jsonPath("$.displayOrder") { value(0) } }
		assertEquals(1, productRepository.findWithDetailsById(requireNotNull(product.id))?.images?.count { it.primary })

		val extra = MockMultipartFile("file", "detalhe.jpg", "image/jpeg", byteArrayOf(9))
		mockMvc.perform(multipart("/admin/products/${product.id}/images").file(extra).header("Authorization", admin))
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.displayOrder").value(2))
		mockMvc.delete("/admin/products/${product.id}/images/${oldPrimary.id}") { header("Authorization", admin) }
			.andExpect { status { isNoContent() } }
		assertEquals(1, productRepository.findWithDetailsById(requireNotNull(product.id))?.images?.count { it.primary })
	}

	private fun createActiveProduct(categoryId: UUID) = productMediaService.createProduct(
		"Saia", "Descrição", categoryId, com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus.ACTIVE, 0,
		listOf(byteArrayOf(1) to "image/jpeg", byteArrayOf(2) to "image/jpeg"),
	)

	private fun adminBearer(): String {
		val now = Instant.now()
		val claims = mapOf<String, Any>("iss" to "https://accounts.google.com", "sub" to "test-admin-sub", "aud" to listOf("test-google-client"), "iat" to now, "exp" to now.plusSeconds(3600), "email" to "admin-catalog@example.com", "email_verified" to true, "given_name" to "Admin", "family_name" to "Laumiley")
		val token = OidcIdToken("catalog-admin", now, now.plusSeconds(3600), claims)
		val user = DefaultOidcUser(listOf(SimpleGrantedAuthority("ROLE_USER")), token)
		return "Bearer ${authService.refresh(googleAuthService.authenticate(user)).accessToken}"
	}
}
