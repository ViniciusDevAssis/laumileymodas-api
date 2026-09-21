package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.domain.entities.Account
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
	private val accountRepository: AccountRepository,
) : UserDetailsService {
	override fun loadUserByUsername(username: String): UserDetails {
		val email = Account.normalizeEmail(username)
		val account = accountRepository.findByEmail(email)
			?: throw UsernameNotFoundException("Credenciais inválidas.")
		val passwordHash = account.passwordHash
			?: throw UsernameNotFoundException("Credenciais inválidas.")

		return User.withUsername(account.email)
			.password(passwordHash)
			.authorities("ROLE_${account.role.name}")
			.build()
	}
}
