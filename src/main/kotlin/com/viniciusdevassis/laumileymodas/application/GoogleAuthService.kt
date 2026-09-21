package com.viniciusdevassis.laumileymodas.application

import com.viniciusdevassis.laumileymodas.domain.entities.Account
import com.viniciusdevassis.laumileymodas.domain.entities.AccountExternalIdentity
import com.viniciusdevassis.laumileymodas.domain.entities.Customer
import com.viniciusdevassis.laumileymodas.domain.enums.Provider
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountExternalIdentityRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class GoogleAuthService(
	private val accountRepository: AccountRepository,
	private val identityRepository: AccountExternalIdentityRepository,
	private val customerRepository: CustomerRepository,
	private val authService: AuthService,
	@Value("\${laumiley.security.admin.google-sub}") private val adminGoogleSub: String,
	private val clock: Clock,
) {
	@Transactional
	fun authenticate(user: OidcUser): String {
		val subject = user.subject.takeIf(String::isNotBlank)
			?: throw ApiException(ApiError.AUTH_005, HttpStatus.UNAUTHORIZED)
		val email = user.getClaimAsString("email")?.let(Account::normalizeEmail)?.takeIf(String::isNotBlank)
			?: throw ApiException(ApiError.AUTH_005, HttpStatus.UNAUTHORIZED)
		if (user.getClaim<Boolean>("email_verified") != true) {
			throw ApiException(ApiError.AUTH_005, HttpStatus.UNAUTHORIZED)
		}

		val requestedRole = if (adminGoogleSub.isNotBlank() && subject == adminGoogleSub) Role.ADMIN else Role.CLIENT
		val identity = identityRepository.findByProviderAndSubject(Provider.GOOGLE, subject)
		val account = if (identity != null) {
			if (identity.account.role != requestedRole) {
				throw ApiException(ApiError.AUTH_006, HttpStatus.CONFLICT)
			}
			if (requestedRole == Role.CLIENT && customerRepository.findByAccountId(requireNotNull(identity.account.id)) == null) {
				throw ApiException(ApiError.AUTH_006, HttpStatus.CONFLICT)
			}
			identity.account
		} else {
			if (accountRepository.findByEmail(email) != null) {
				throw ApiException(ApiError.AUTH_003, HttpStatus.CONFLICT)
			}
			if (requestedRole == Role.ADMIN && accountRepository.existsByRole(Role.ADMIN)) {
				throw ApiException(ApiError.AUTH_006, HttpStatus.CONFLICT)
			}
			createGoogleAccount(user, email, subject, requestedRole)
		}

		return authService.createRefreshTokenFor(account)
	}

	private fun createGoogleAccount(user: OidcUser, email: String, subject: String, role: Role): Account {
		val now = Instant.now(clock)
		val account = accountRepository.save(
			Account(email = email, passwordHash = null, role = role, createdAt = now, updatedAt = now),
		)
		identityRepository.save(
			AccountExternalIdentity(account = account, provider = Provider.GOOGLE, subject = subject, createdAt = now),
		)
		if (role == Role.CLIENT) {
			val fullName = user.getClaimAsString("name")?.trim()?.takeIf(String::isNotBlank)
			val givenName = user.getClaimAsString("given_name")?.trim()?.takeIf(String::isNotBlank)
			val familyName = user.getClaimAsString("family_name")?.trim()?.takeIf(String::isNotBlank)
				?: fullName?.split(Regex("\\s+"))?.drop(1)?.joinToString(" ")?.takeIf(String::isNotBlank)
			val firstName = givenName ?: fullName?.split(Regex("\\s+"))?.firstOrNull()?.takeIf(String::isNotBlank)
				?: throw ApiException(ApiError.AUTH_005, HttpStatus.UNAUTHORIZED)
			val lastName = familyName ?: firstName
			customerRepository.save(
				Customer(
					account = account,
					firstName = firstName,
					lastName = lastName,
					whatsappPhone = null,
					createdAt = now,
					updatedAt = now,
				),
			)
		}
		return account
	}
}
