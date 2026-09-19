package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtKeyConfiguration {
	@Bean
	fun jwtKeyPair(properties: JwtProperties): JwtKeyPair {
		return runCatching {
			JwtKeyPair(parsePublic(properties.publicKey), parsePrivate(properties.privateKey))
		}.getOrElse {
			check(properties.privateKey.startsWith("test-")) { "Chaves JWT PEM inválidas." }
			val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
			JwtKeyPair(pair.public as RSAPublicKey, pair.private as RSAPrivateKey)
		}
	}

	@Bean fun jwtEncoder(keys: JwtKeyPair, properties: JwtProperties): JwtEncoder {
		val jwk = RSAKey.Builder(keys.publicKey).privateKey(keys.privateKey).keyID(properties.keyId).build()
		return NimbusJwtEncoder(ImmutableJWKSet<SecurityContext>(JWKSet(jwk)))
	}

	@Bean("refreshJwtDecoder") fun refreshJwtDecoder(keys: JwtKeyPair): JwtDecoder = NimbusJwtDecoder.withPublicKey(keys.publicKey).build()

	private fun parsePublic(value: String) = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decodePem(value))) as RSAPublicKey
	private fun parsePrivate(value: String) = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(decodePem(value))) as RSAPrivateKey
	private fun decodePem(value: String) = Base64.getDecoder().decode(value.replace("\\n", "\n").replace(Regex("-----[^-]+-----|\\s"), ""))
}

data class JwtKeyPair(val publicKey: RSAPublicKey, val privateKey: RSAPrivateKey)
