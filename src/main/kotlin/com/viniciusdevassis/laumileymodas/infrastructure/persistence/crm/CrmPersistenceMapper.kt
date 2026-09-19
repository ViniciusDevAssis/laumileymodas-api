package com.viniciusdevassis.laumileymodas.infrastructure.persistence.crm

import com.viniciusdevassis.laumileymodas.domain.crm.*

fun Reminder.toJpa() = ReminderJpaEntity(id, customerId, interestId, description, dueAt, status.name, completedAt, createdAt, updatedAt)
fun ContactRecord.toJpa() = ContactRecordJpaEntity(id, customerId, interestId, reminderId, status.name, occurredAt, channel, description, completedAt, createdAt, updatedAt)
