package com.viniciusdevassis.laumileymodas.unit.infrastructure.security

import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.ClockProvider
import com.viniciusdevassis.laumileymodas.domain.account.AccountRole
import com.viniciusdevassis.laumileymodas.infrastructure.security.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.*
import java.util.UUID

class JwtTokenServiceTest {
	private var now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
	private val properties=JwtProperties(accessTokenTtl=Duration.ofMinutes(5),refreshTokenTtl=Duration.ofDays(1),privateKey="test-private-key",publicKey="test-public-key")
	private val keys=JwtKeyConfiguration().jwtKeyPair(properties)
	private val encoder=JwtKeyConfiguration().jwtEncoder(keys,properties)
	private val decoder=JwtKeyConfiguration().refreshJwtDecoder(keys)
	private val service=JwtTokenService(encoder,decoder,properties,ClockProvider{now})

	@Test fun `emite access com claims minimos assinatura audience tipo e expiracao`() {
		val id=UUID.randomUUID(); val token=service.issueAccess(id,AccountRole.CLIENT); val jwt=decoder.decode(token.value)
		assertThat(jwt.subject).isEqualTo(id.toString()); assertThat(jwt.audience).containsExactly(properties.accessAudience)
		assertThat(jwt.getClaimAsString("typ")).isEqualTo("access");assertThat(jwt.getClaimAsString("role")).isEqualTo("CLIENT")
		assertThat(jwt.headers["kid"]).isEqualTo(properties.keyId);assertThat(jwt.headers["alg"]).isEqualTo("RS256")
		assertThat(jwt.claims).doesNotContainKeys("email","name"); assertThat(jwt.expiresAt).isEqualTo(now.plus(Duration.ofMinutes(5)))
	}

	@Test fun `refresh possui familia e nao aceita access nem token adulterado`() {
		val id=UUID.randomUUID(); val family=UUID.randomUUID(); val refresh=service.issueRefresh(id,family)
		assertThat(service.parseRefresh(refresh.value).familyId).isEqualTo(family)
		assertThatThrownBy{service.parseRefresh(service.issueAccess(id,AccountRole.CLIENT).value)}.isInstanceOf(ApplicationException::class.java)
		val parts=refresh.value.split('.');val tampered=parts[0]+"."+parts[1]+"."+(if(parts[2][0]=='A')'B' else 'A')+parts[2].drop(1)
		assertThatThrownBy{service.parseRefresh(tampered)}.isInstanceOf(ApplicationException::class.java)
	}

	@Test fun `refresh expirado e rejeitado`() { val token=service.issueRefresh(UUID.randomUUID(),UUID.randomUUID());now=now.plus(Duration.ofDays(2));assertThatThrownBy{service.parseRefresh(token.value)}.isInstanceOf(ApplicationException::class.java) }
}
