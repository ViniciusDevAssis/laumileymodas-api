package com.viniciusdevassis.laumileymodas.infrastructure.security

import com.viniciusdevassis.laumileymodas.infrastructure.repositories.AccountRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class SecurityFilter(
	private val tokenService: TokenService,
	private val accountRepository: AccountRepository,
) : OncePerRequestFilter() {
	override fun doFilterInternal(
		request: HttpServletRequest,
		response: HttpServletResponse,
		filterChain: FilterChain,
	) {
		val bearer = request.getHeader("Authorization")
			?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
			?.substringAfter(' ')
			?.takeIf(String::isNotBlank)

		if (bearer != null && SecurityContextHolder.getContext().authentication == null) {
			try {
				val jwt = tokenService.validate(bearer)
				val accountId = tokenService.accountId(jwt)
				val account = accountRepository.findById(accountId).orElse(null)
				if (account != null) {
					val authentication = UsernamePasswordAuthenticationToken(
						accountId.toString(),
						null,
						listOf(SimpleGrantedAuthority("ROLE_${account.role.name}")),
					)
					SecurityContextHolder.getContext().authentication = authentication
				}
			} catch (_: JwtException) {
				// O entry point responde com o contrato uniforme para tokens inválidos.
			} catch (_: IllegalArgumentException) {
				// Subject ou papel inválido resulta em autenticação ausente.
			}
		}

		filterChain.doFilter(request, response)
	}
}
