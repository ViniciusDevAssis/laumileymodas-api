package com.viniciusdevassis.laumileymodas.application.port.interest

import com.viniciusdevassis.laumileymodas.domain.interest.Interest
import java.util.UUID

interface InterestRepository {
	fun lockIdempotency(customerId: UUID, key: String)
	fun findByCustomerAndKey(customerId: UUID, key: String): Interest?
	fun findOwned(interestId: UUID, customerId: UUID): Interest?
}
