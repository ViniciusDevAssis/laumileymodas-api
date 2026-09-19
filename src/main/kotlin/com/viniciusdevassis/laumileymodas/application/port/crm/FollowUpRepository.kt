package com.viniciusdevassis.laumileymodas.application.port.crm

import com.viniciusdevassis.laumileymodas.domain.crm.*
import com.viniciusdevassis.laumileymodas.domain.interest.Interest

interface FollowUpRepository {
	fun save(interest: Interest, reminder: Reminder, contact: ContactRecord): Interest
}
