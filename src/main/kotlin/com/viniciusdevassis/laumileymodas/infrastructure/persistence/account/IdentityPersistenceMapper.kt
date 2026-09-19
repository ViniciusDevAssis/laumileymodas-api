package com.viniciusdevassis.laumileymodas.infrastructure.persistence.account

import com.viniciusdevassis.laumileymodas.domain.account.*
import com.viniciusdevassis.laumileymodas.domain.customer.Customer
import com.viniciusdevassis.laumileymodas.infrastructure.persistence.customer.CustomerJpaEntity

fun Account.toJpa() = AccountJpaEntity(id, email, normalizedEmail, passwordHash, role.name, enabled, createdAt, updatedAt)
fun AccountJpaEntity.toDomain() = Account(id, email, passwordHash, AccountRole.valueOf(role), enabled, createdAt, updatedAt)
fun AccountExternalIdentity.toJpa() = ExternalIdentityJpaEntity(id, accountId, provider.name, subject, emailAtLink, createdAt, lastLoginAt)
fun ExternalIdentityJpaEntity.toDomain() = AccountExternalIdentity(id, accountId, ExternalIdentityProvider.valueOf(provider), subject, emailAtLink, createdAt, lastLoginAt)
fun Customer.toJpa() = CustomerJpaEntity(id, accountId, firstName, lastName, whatsappPhone, proactiveContactAuthorized, contactConsentChangedAt, createdAt, updatedAt)
fun CustomerJpaEntity.toDomain() = Customer(id, accountId, firstName, lastName, whatsappPhone, proactiveContactAuthorized, contactConsentChangedAt, createdAt, updatedAt)
