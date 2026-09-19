package com.viniciusdevassis.laumileymodas.integration.persistence

import com.viniciusdevassis.laumileymodas.application.interest.RegisterProductInterestUseCase
import com.viniciusdevassis.laumileymodas.integration.support.Phase4IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.*

class InterestFollowUpIntegrationTest:Phase4IntegrationTest(){
	@Autowired lateinit var useCase:RegisterProductInterestUseCase
	@Test fun `retry concorrente cria um unico conjunto de acompanhamento`() { val email=register();val account=jdbc.queryForObject("select id from account where normalized_email=?",UUID::class.java,email)!!;val product=seedActiveProduct();val executor=Executors.newFixedThreadPool(2);val gate=CountDownLatch(1);val futures=(1..2).map{executor.submit<com.viniciusdevassis.laumileymodas.application.interest.InterestResult>{gate.await();useCase.execute(account,product,"concurrent-key")}};gate.countDown();val results=futures.map{it.get(20,TimeUnit.SECONDS)};executor.shutdown();assertThat(results.map{it.interest.id}.distinct()).hasSize(1);assertThat(jdbc.queryForObject("select count(*) from interest",Long::class.java)).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from reminder where status='PENDING'",Long::class.java)).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from contact_record where status='PENDING'",Long::class.java)).isEqualTo(1) }
}
