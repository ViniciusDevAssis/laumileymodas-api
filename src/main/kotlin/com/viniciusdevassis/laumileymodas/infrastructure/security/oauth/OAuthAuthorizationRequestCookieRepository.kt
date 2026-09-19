package com.viniciusdevassis.laumileymodas.infrastructure.security.oauth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
import jakarta.servlet.http.*
import org.springframework.http.ResponseCookie
import org.springframework.http.HttpHeaders
import java.io.*
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
class OAuthAuthorizationRequestCookieRepository(@Value("\${laumiley.security.oauth-handoff.encryption-key}") secret: String) : AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
	private val key = SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.toByteArray()), "AES")
	private val cookieName = "LAUMILEY_OAUTH_REQUEST"
	fun seal(value: String): String {
		val iv = ByteArray(12).also(SecureRandom()::nextBytes); val cipher = Cipher.getInstance("AES/GCM/NoPadding")
		cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv)); return Base64.getUrlEncoder().withoutPadding().encodeToString(iv + cipher.doFinal(value.toByteArray()))
	}
	fun open(value: String): String {
		val bytes = Base64.getUrlDecoder().decode(value); val cipher = Cipher.getInstance("AES/GCM/NoPadding")
		cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(0, 12))); return String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)))
	}

	override fun loadAuthorizationRequest(request: HttpServletRequest): OAuth2AuthorizationRequest? = request.cookies?.firstOrNull { it.name == cookieName }?.value?.let { deserialize(open(it)) }

	override fun saveAuthorizationRequest(authorizationRequest: OAuth2AuthorizationRequest?, request: HttpServletRequest, response: HttpServletResponse) {
		val cookie = if (authorizationRequest == null) ResponseCookie.from(cookieName, "").maxAge(0) else ResponseCookie.from(cookieName, seal(serialize(authorizationRequest))).maxAge(java.time.Duration.ofMinutes(10))
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.httpOnly(true).secure(request.isSecure).sameSite("Lax").path("/api/v1/auth/google").build().toString())
	}

	override fun removeAuthorizationRequest(request: HttpServletRequest, response: HttpServletResponse): OAuth2AuthorizationRequest? = loadAuthorizationRequest(request).also { saveAuthorizationRequest(null, request, response) }

	private fun serialize(value: OAuth2AuthorizationRequest): String {
		val output = ByteArrayOutputStream(); ObjectOutputStream(output).use { it.writeObject(value) }
		return Base64.getUrlEncoder().withoutPadding().encodeToString(output.toByteArray())
	}

	private fun deserialize(value: String): OAuth2AuthorizationRequest {
		val bytes = Base64.getUrlDecoder().decode(value)
		return ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() as OAuth2AuthorizationRequest }
	}
}
