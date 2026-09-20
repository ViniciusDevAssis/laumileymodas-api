package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.Account
import com.viniciusdevassis.laumileymodas.domain.enums.Role
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccountRepository : JpaRepository<Account, UUID> {
	fun findByEmail(email: String): Account?
	fun findByRole(role: Role): Account?
	fun existsByRole(role: Role): Boolean
}
