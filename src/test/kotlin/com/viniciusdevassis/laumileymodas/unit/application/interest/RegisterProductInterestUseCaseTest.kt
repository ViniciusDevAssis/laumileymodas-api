package com.viniciusdevassis.laumileymodas.unit.application.interest

import com.viniciusdevassis.laumileymodas.application.interest.RegisterProductInterestUseCase
import com.viniciusdevassis.laumileymodas.application.port.*
import com.viniciusdevassis.laumileymodas.application.port.catalog.*
import com.viniciusdevassis.laumileymodas.application.port.crm.FollowUpRepository
import com.viniciusdevassis.laumileymodas.application.port.customer.CustomerRepository
import com.viniciusdevassis.laumileymodas.application.port.interest.InterestRepository
import com.viniciusdevassis.laumileymodas.application.port.whatsapp.WhatsappLinkGenerator
import com.viniciusdevassis.laumileymodas.domain.catalog.*
import com.viniciusdevassis.laumileymodas.domain.common.DomainException
import com.viniciusdevassis.laumileymodas.domain.crm.*
import com.viniciusdevassis.laumileymodas.domain.customer.Customer
import com.viniciusdevassis.laumileymodas.domain.interest.*
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Instant
import java.util.UUID

class RegisterProductInterestUseCaseTest {
	private val now=Instant.parse("2026-01-01T12:00:00Z"); private val accountId=UUID.randomUUID(); private val customer=Customer(UUID.randomUUID(),accountId,"Ana","Silva","+5511999999999",createdAt=now,updatedAt=now)
	private val category=Category(UUID.randomUUID(),"Vestidos",now,now); private val product=Product(UUID.randomUUID(),category,"Vestido Azul","Descrição",ProductStatus.ACTIVE,listOf(ProductImage(UUID.randomUUID(),"asset",URI("https://img.test/a.jpg"),true,0,now)),now,now)
	private val interests=FakeInterests(); private val follow=FakeFollowUp(interests); private var sequence=0
	private fun useCase(active:Boolean=true)=RegisterProductInterestUseCase(object:CatalogQueryRepository{override fun findActive(page:Int,size:Int)=CatalogProductPage(emptyList(),0,size,0,0);override fun findActiveById(productId:UUID)=if(active&&productId==product.id)product else null},object:CustomerRepository{override fun findByAccountId(accountId:UUID)=customer.takeIf{accountId==this@RegisterProductInterestUseCaseTest.accountId};override fun save(customer:Customer)=customer},interests,follow,WhatsappLinkGenerator{_,_->"https://wa.me/5511"},IdGenerator{UUID.nameUUIDFromBytes((sequence++).toString().toByteArray())},ClockProvider{now})

	@Test fun `cria exatamente um interesse lembrete e contato pendente`() { val result=useCase().execute(accountId,product.id,"key-1");assertThat(result.created).isTrue();assertThat(follow.reminder!!.status).isEqualTo(ReminderStatus.PENDING);assertThat(follow.contact!!.status).isEqualTo(ContactRecordStatus.PENDING);assertThat(follow.contact!!.reminderId).isEqualTo(follow.reminder!!.id) }
	@Test fun `retry da mesma chave devolve interesse sem novo acompanhamento`() { val service=useCase();val first=service.execute(accountId,product.id,"key-2");val second=service.execute(accountId,product.id,"key-2");assertThat(second.interest.id).isEqualTo(first.interest.id);assertThat(second.created).isFalse();assertThat(follow.saves).isEqualTo(1) }
	@Test fun `produto inativo nao cria interesse`() { assertThatThrownBy{useCase(false).execute(accountId,product.id,"key-3")}.isInstanceOf(DomainException::class.java);assertThat(follow.saves).isZero() }

	private class FakeInterests:InterestRepository{val values=mutableListOf<Interest>();override fun lockIdempotency(customerId:UUID,key:String){};override fun findByCustomerAndKey(customerId:UUID,key:String)=values.find{it.customerId==customerId&&it.idempotencyKey==key};override fun findOwned(interestId:UUID,customerId:UUID)=values.find{it.id==interestId&&it.customerId==customerId}}
	private class FakeFollowUp(private val repo:FakeInterests):FollowUpRepository{var reminder:Reminder?=null;var contact:ContactRecord?=null;var saves=0;override fun save(interest:Interest,reminder:Reminder,contact:ContactRecord):Interest{saves++;repo.values+=interest;this.reminder=reminder;this.contact=contact;return interest}}
}
