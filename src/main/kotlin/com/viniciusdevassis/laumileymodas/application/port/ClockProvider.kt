package com.viniciusdevassis.laumileymodas.application.port

import java.time.Instant

fun interface ClockProvider {
	fun now(): Instant
}
