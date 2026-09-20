package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.AccountExternalIdentity
import com.viniciusdevassis.laumileymodas.domain.enums.Provider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AccountExternalIdentityRepository : JpaRepository<AccountExternalIdentity, UUID> {
	fun findByProviderAndSubject(provider: Provider, subject: String): AccountExternalIdentity?
}
