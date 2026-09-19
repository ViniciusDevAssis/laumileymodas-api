package com.viniciusdevassis.laumileymodas.application.port

import java.util.UUID

fun interface IdGenerator {
	fun newId(): UUID
}
