package com.viniciusdevassis.laumileymodas.application.port.customer

import com.viniciusdevassis.laumileymodas.domain.customer.Customer
import java.util.UUID

interface CustomerRepository {
	fun findByAccountId(accountId: UUID): Customer?
	fun save(customer: Customer): Customer
}
