package com.viniciusdevassis.laumileymodas.application.auth

import com.viniciusdevassis.laumileymodas.application.common.ApplicationException
import com.viniciusdevassis.laumileymodas.application.port.*
import com.viniciusdevassis.laumileymodas.application.port.account.AccountRepository
import com.viniciusdevassis.laumileymodas.application.port.customer.CustomerRepository
import com.viniciusdevassis.laumileymodas.domain.account.Account
import com.viniciusdevassis.laumileymodas.domain.customer.Customer
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class RegisterCustomerCommand(val firstName: String, val lastName: String, val email: String, val password: String, val whatsappPhone: String)
data class RegisteredCustomer(val account: Account, val customer: Customer)

@Service
class RegisterCustomerWithPasswordUseCase(private val accounts: AccountRepository, private val customers: CustomerRepository, private val encoder: PasswordEncoder, private val ids: IdGenerator, private val clock: ClockProvider) {
	@Transactional fun execute(command: RegisterCustomerCommand): RegisteredCustomer {
		if (command.password.toByteArray(Charsets.UTF_8).size > 72) throw ApplicationException(AuthError.INVALID_PASSWORD)
		if (accounts.findByNormalizedEmail(command.email) != null) throw ApplicationException(AuthError.EMAIL_ALREADY_EXISTS)
		val now = clock.now()
		val account = accounts.save(Account.publicClient(ids.newId(), command.email, encoder.encode(command.password), now))
		val customer = customers.save(Customer(ids.newId(), account.id, command.firstName.trim(), command.lastName.trim(), Customer.normalizePhone(command.whatsappPhone), false, null, now, now))
		return RegisteredCustomer(account, customer)
	}
}

