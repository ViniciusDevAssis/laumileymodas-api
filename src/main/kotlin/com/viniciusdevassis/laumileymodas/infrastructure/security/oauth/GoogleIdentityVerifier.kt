package com.viniciusdevassis.laumileymodas.infrastructure.security.oauth

import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Component

data class VerifiedGoogleIdentity(val subject: String, val email: String, val givenName: String?, val familyName: String?)

@Component class GoogleIdentityVerifier {
	fun verify(user: OidcUser): VerifiedGoogleIdentity {
		require(user.subject.isNotBlank() && user.emailVerified == true && !user.email.isNullOrBlank()) { "Identidade Google inválida." }
		return VerifiedGoogleIdentity(user.subject, user.email, user.givenName, user.familyName)
	}
}
