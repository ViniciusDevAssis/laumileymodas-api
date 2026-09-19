package com.viniciusdevassis.laumileymodas.integration.support

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.*
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.UUID

@SpringBootTest @AutoConfigureMockMvc
abstract class Phase4IntegrationTest: PostgresIntegrationTest() {
	@Autowired protected lateinit var mockMvc:MockMvc
	@Autowired protected lateinit var objectMapper:ObjectMapper
	@Autowired protected lateinit var jdbc:JdbcTemplate

	@BeforeEach fun cleanPhase4Database(){ jdbc.execute("TRUNCATE TABLE contact_record, reminder, interest, product_image, product, category, oauth_handoff, refresh_token, account_external_identity, customer, account CASCADE") }

	protected fun csrf():Cookie { val result=mockMvc.get("/auth/csrf").andExpect{status{isNoContent()}}.andReturn(); return requireNotNull(result.response.getCookie("XSRF-TOKEN")) }
	protected fun register(email:String="client-${UUID.randomUUID()}@test.com",phone:String="+5511999999999"):String { val body="""{"firstName":"Ana","lastName":"Silva","email":"$email","password":"Senha123!","whatsappPhone":"$phone"}""";mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/customers").contentType("application/json").content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated);return email }
	protected fun login(email:String,origin:String="http://localhost:3000"):AuthSession { val body="""{"email":"$email","password":"Senha123!"}""";val result=mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/auth/login").contentType("application/json").header("Origin",origin).content(body)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk).andReturn();return AuthSession(objectMapper.readTree(result.response.contentAsString).get("accessToken").asText(),requireNotNull(result.response.getCookie("LAUMILEY_REFRESH"))) }
	protected fun seedActiveProduct():UUID { val category=UUID.randomUUID();val product=UUID.randomUUID();val image=UUID.randomUUID();val now=java.sql.Timestamp.from(Instant.now());jdbc.update("insert into category(id,name,normalized_name,created_at,updated_at) values (?,?,?,?,?)",category,"Vestidos","vestidos",now,now);jdbc.update("insert into product(id,category_id,name,description,status,created_at,updated_at) values (?,?,?,?,?,?,?)",product,category,"Vestido Azul","Descrição","ACTIVE",now,now);jdbc.update("insert into product_image(id,product_id,external_id,secure_url,is_primary,display_order,created_at) values (?,?,?,?,?,?,?)",image,product,"asset-$image","https://img.test/$image.jpg",true,0,now);return product }
}
data class AuthSession(val accessToken:String,val refreshCookie:Cookie)
