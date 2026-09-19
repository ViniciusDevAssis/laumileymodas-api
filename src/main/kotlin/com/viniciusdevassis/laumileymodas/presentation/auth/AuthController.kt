package com.viniciusdevassis.laumileymodas.presentation.auth

import com.viniciusdevassis.laumileymodas.application.auth.*
import com.viniciusdevassis.laumileymodas.application.port.ClockProvider
import com.viniciusdevassis.laumileymodas.application.port.account.AccountRepository
import com.viniciusdevassis.laumileymodas.application.port.customer.CustomerRepository
import jakarta.servlet.http.*
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.web.util.UriComponentsBuilder
import java.util.UUID

@RestController @RequestMapping("/auth")
class AuthController(private val register: RegisterCustomerWithPasswordUseCase, private val login: LoginWithPasswordUseCase, private val refresh: RefreshAccessTokenUseCase, private val logout: LogoutUseCase, private val exchange: ExchangeGoogleHandoffUseCase, private val googleRegistration: CompleteGoogleCustomerRegistrationUseCase, private val accounts: AccountRepository, private val customers: CustomerRepository, private val clock: ClockProvider, @Value("\${laumiley.security.refresh-cookie.name}") private val refreshCookieName: String, @Value("\${laumiley.security.refresh-cookie.path}") private val refreshPath: String, @Value("\${laumiley.security.refresh-cookie.secure:false}") private val cookieSecure: Boolean, @Value("\${laumiley.security.refresh-cookie.same-site:Lax}") private val sameSite: String, @Value("\${laumiley.security.oauth-handoff.cookie-name}") private val handoffCookieName: String) {
	@PostMapping("/customers") fun register(@Valid @RequestBody request: RegisterCustomerRequest): ResponseEntity<CurrentAccountResponse> {
		val result = register.execute(RegisterCustomerCommand(request.firstName, request.lastName, request.email, request.password, request.whatsappPhone))
		return ResponseEntity.created(UriComponentsBuilder.fromPath("/auth/me").build().toUri()).body(result.account.response(result.customer))
	}
	@PostMapping("/login") fun login(@Valid @RequestBody request: LoginRequest, servlet: HttpServletRequest, response: HttpServletResponse): AuthTokenResponse {
		val pair = login.execute(request.email, request.password, servlet.getHeader("Origin") ?: servlet.remoteAddr); setRefresh(response, pair)
		return tokenResponse(pair)
	}
	@PostMapping("/refresh") fun refresh(request: HttpServletRequest, response: HttpServletResponse): AuthTokenResponse { val pair = refresh.execute(cookieValue(request, refreshCookieName)); setRefresh(response, pair); return tokenResponse(pair) }
	@PostMapping("/logout") fun logout(request: HttpServletRequest, response: HttpServletResponse): ResponseEntity<Void> { logout.execute(cookieValue(request, refreshCookieName)); response.addHeader(HttpHeaders.SET_COOKIE, cookie(refreshCookieName, "", 0).toString()); return ResponseEntity.noContent().build() }
	@PostMapping("/google/exchange") fun exchange(request: HttpServletRequest, response: HttpServletResponse): AuthTokenResponse { val pair=exchange.execute(cookieValue(request, handoffCookieName)); setRefresh(response,pair); clearHandoff(response); return tokenResponse(pair) }
	@PostMapping("/google/customers") fun completeGoogle(servlet: HttpServletRequest, @Valid @RequestBody request: GoogleCustomerRegistrationRequest, response: HttpServletResponse): ResponseEntity<AuthTokenResponse> { val pair=googleRegistration.execute(cookieValue(servlet, handoffCookieName),request.firstName,request.lastName,request.whatsappPhone); setRefresh(response,pair);clearHandoff(response);return ResponseEntity.status(HttpStatus.CREATED).body(tokenResponse(pair)) }
	@GetMapping("/me") fun me(@org.springframework.security.core.annotation.AuthenticationPrincipal jwt: Jwt): CurrentAccountResponse { val account=accounts.findById(UUID.fromString(jwt.subject))!!; return account.response(customers.findByAccountId(account.id)) }
	private fun tokenResponse(pair: TokenPair): AuthTokenResponse { val account=accounts.findById(pair.accountId)!!; return pair.response(account, customers.findByAccountId(account.id), clock.now()) }
	private fun setRefresh(response: HttpServletResponse, pair: TokenPair) { response.addHeader(HttpHeaders.SET_COOKIE, cookie(refreshCookieName,pair.refreshToken,java.time.Duration.between(clock.now(),pair.refreshExpiresAt).seconds).toString()) }
	private fun clearHandoff(response: HttpServletResponse) { response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(handoffCookieName,"").httpOnly(true).secure(cookieSecure).sameSite(sameSite).path("/api/v1/auth/google").maxAge(0).build().toString()) }
	private fun cookie(name:String,value:String,maxAge:Long)=ResponseCookie.from(name,value).httpOnly(true).secure(cookieSecure).sameSite(sameSite).path(refreshPath).maxAge(maxAge).build()
	private fun cookieValue(request: HttpServletRequest, name: String) = request.cookies?.firstOrNull { it.name == name }?.value ?: ""
}
