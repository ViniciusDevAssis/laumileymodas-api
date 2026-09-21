package com.viniciusdevassis.laumileymodas.application

import com.viniciusdevassis.laumileymodas.domain.entities.Account
import com.viniciusdevassis.laumileymodas.domain.entities.Customer
import com.viniciusdevassis.laumileymodas.domain.entities.RefreshToken
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiError
import com.viniciusdevassis.laumileymodas.domain.exceptions.ApiException
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.CustomerRepository
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.RefreshTokenRepository
import com.viniciusdevassis.laumileymodas.infrastructure.security.AuthenticationRateLimiter
import com.viniciusdevassis.laumileymodas.infrastructure.security.RefreshTokenCookie
import com.viniciusdevassis.laumileymodas.infrastructure.security.TokenService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class AuthService(
	private val accountRepository: AccountRepository,
	private val customerRepository: CustomerRepository,
	private val refreshTokenRepository: RefreshTokenRepository,
	private val tokenService: TokenService,
	private val refreshTokenCookie: RefreshTokenCookie,
	private val rateLimiter: AuthenticationRateLimiter,
	private val authenticationManager: AuthenticationManager,
	private val passwordEncoder: PasswordEncoder,
	private val clock: Clock,
	@Value("\${laumiley.security.jwt.refresh-token-ttl}") private val refreshTokenTtl: Duration,
	@Value("\${laumiley.security.jwt.access-token-ttl}") private val accessTokenTtl: Duration,
) {
	@Transactional
	fun registerCustomer(
		firstName: String,
		lastName: String,
		email: String,
		password: String,
		whatsappPhone: String,
	): Customer {
		val normalizedEmail = Account.normalizeEmail(email)
		if (accountRepository.findByEmail(normalizedEmail) != null) {
			throw ApiException(ApiError.AUTH_003, HttpStatus.CONFLICT)
		}

		val account = accountRepository.save(
			Account(
				email = normalizedEmail,
				passwordHash = passwordEncoder.encode(password),
				role = Role.CLIENT,
				createdAt = now(),
			),
		)
		return customerRepository.save(
			Customer(
				account = account,
				firstName = firstName.trim(),
				lastName = lastName.trim(),
				whatsappPhone = whatsappPhone.trim(),
				createdAt = now(),
			),
		)
	}

	@Transactional(readOnly = true)
	fun getCustomer(accountId: UUID): Customer {
		val customer = customerRepository.findByAccountId(accountId)
			?: throw ApiException(ApiError.CUSTOMER_001, HttpStatus.NOT_FOUND)
		customer.account.email
		return customer
	}

	@Transactional
	fun updateCustomerWhatsApp(accountId: UUID, whatsappPhone: String): Customer {
		val customer = customerRepository.findByAccountId(accountId)
			?: throw ApiException(ApiError.CUSTOMER_001, HttpStatus.NOT_FOUND)
		customer.whatsappPhone = whatsappPhone.trim()
		customer.updatedAt = now()
		customer.account.email
		return customer
	}

	@Transactional
	fun login(email: String, password: String, origin: String): AuthTokens {
		val normalizedEmail = Account.normalizeEmail(email)
		val rateLimitKey = rateLimitKey(origin, normalizedEmail)
		if (rateLimiter.isBlocked(rateLimitKey)) {
			throw ApiException(ApiError.AUTH_004, HttpStatus.TOO_MANY_REQUESTS)
		}

		val account = try {
			authenticationManager.authenticate(UsernamePasswordAuthenticationToken(normalizedEmail, password))
				.principal.let { principal ->
					accountRepository.findByEmail((principal as org.springframework.security.core.userdetails.UserDetails).username)
				}
		} catch (exception: AuthenticationException) {
			null
		}
		if (account == null) {
			rateLimiter.recordFailure(rateLimitKey)
			throw ApiException(ApiError.AUTH_002, HttpStatus.UNAUTHORIZED)
		}

		rateLimiter.recordSuccess(rateLimitKey)
		return issueTokens(account, UUID.randomUUID())
	}

	@Transactional
	fun createRefreshTokenFor(account: Account): String = createRefreshToken(account, UUID.randomUUID())

	@Transactional(noRollbackFor = [ApiException::class])
	fun refresh(rawRefreshToken: String): AuthTokens {
		val token = findRefreshToken(rawRefreshToken)
		val now = now()

		if (token.consumedAt != null) {
			revokeFamily(token.familyId, now)
			throw ApiException(ApiError.AUTH_001, HttpStatus.UNAUTHORIZED)
		}
		if (!token.isActive(now)) {
			throw ApiException(ApiError.AUTH_001, HttpStatus.UNAUTHORIZED)
		}

		token.consumedAt = now
		return issueTokens(token.account, token.familyId)
	}

	@Transactional
	fun logout(rawRefreshToken: String) {
		val token = refreshTokenRepository.findByTokenHash(refreshTokenCookie.hash(rawRefreshToken)) ?: return
		if (token.revokedAt == null) {
			token.revokedAt = now()
		}
	}

	private fun issueTokens(account: Account, familyId: UUID): AuthTokens {
		val rawRefreshToken = createRefreshToken(account, familyId)

		return AuthTokens(
			accessToken = tokenService.createAccessToken(requireNotNull(account.id), account.role),
			refreshToken = rawRefreshToken,
			role = account.role,
			expiresIn = accessTokenTtl.seconds,
		)
	}

	private fun createRefreshToken(account: Account, familyId: UUID): String {
		val rawRefreshToken = refreshTokenCookie.generateToken()
		refreshTokenRepository.save(
			RefreshToken(
				account = account,
				tokenHash = refreshTokenCookie.hash(rawRefreshToken),
				familyId = familyId,
				expiresAt = now().plus(refreshTokenTtl),
				createdAt = now(),
			),
		)
		return rawRefreshToken
	}

	private fun rateLimitKey(origin: String, normalizedEmail: String): String =
		"${origin.trim().lowercase()}|$normalizedEmail"

	private fun findRefreshToken(rawRefreshToken: String): RefreshToken =
		refreshTokenRepository.findByTokenHash(refreshTokenCookie.hash(rawRefreshToken))
			?: throw ApiException(ApiError.AUTH_001, HttpStatus.UNAUTHORIZED)

	private fun revokeFamily(familyId: UUID, revokedAt: Instant) {
		refreshTokenRepository.findByFamilyId(familyId).forEach { token ->
			if (token.revokedAt == null) {
				token.revokedAt = revokedAt
			}
		}
	}

	private fun now(): Instant = Instant.now(clock)

	data class AuthTokens(
		val accessToken: String,
		val refreshToken: String,
		val role: Role,
		val expiresIn: Long,
	)

}
