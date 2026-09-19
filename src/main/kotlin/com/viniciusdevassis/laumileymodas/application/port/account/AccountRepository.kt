package com.viniciusdevassis.laumileymodas.application.port.account

import com.viniciusdevassis.laumileymodas.domain.account.Account
import java.util.UUID

interface AccountRepository {
	fun findById(id: UUID): Account?
	fun findByNormalizedEmail(email: String): Account?
	fun save(account: Account): Account
}
