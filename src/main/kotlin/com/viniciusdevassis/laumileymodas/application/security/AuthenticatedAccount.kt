package com.viniciusdevassis.laumileymodas.application.security

import com.viniciusdevassis.laumileymodas.domain.account.AccountRole
import java.util.UUID

data class AuthenticatedAccount(val accountId: UUID, val role: AccountRole)
