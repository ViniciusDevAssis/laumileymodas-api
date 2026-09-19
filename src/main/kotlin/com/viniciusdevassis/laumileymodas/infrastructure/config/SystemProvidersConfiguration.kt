package com.viniciusdevassis.laumileymodas.infrastructure.config

import com.viniciusdevassis.laumileymodas.application.port.ClockProvider
import com.viniciusdevassis.laumileymodas.application.port.IdGenerator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.util.UUID

@Configuration
class SystemProvidersConfiguration {

	@Bean
	fun systemClock(): Clock = Clock.systemUTC()

	@Bean
	fun clockProvider(clock: Clock): ClockProvider = ClockProvider(clock::instant)

	@Bean
	fun idGenerator(): IdGenerator = IdGenerator(UUID::randomUUID)
}
