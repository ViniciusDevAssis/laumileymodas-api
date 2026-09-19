package com.viniciusdevassis.laumileymodas.infrastructure.persistence.catalog

import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogProductPage
import com.viniciusdevassis.laumileymodas.application.port.catalog.CatalogQueryRepository
import com.viniciusdevassis.laumileymodas.domain.catalog.Product
import com.viniciusdevassis.laumileymodas.domain.catalog.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

interface SpringDataProductRepository : JpaRepository<ProductJpaEntity, UUID> {

	@Query("select p.id from ProductJpaEntity p where p.status = :status")
	fun findIdsByStatus(
		@Param("status") status: String,
		pageable: PageRequest,
	): Page<UUID>

	@Query(
		"""
		select distinct p
		from ProductJpaEntity p
		join fetch p.category
		left join fetch p.images
		where p.id in :ids
		""",
	)
	fun findDetailedByIds(@Param("ids") ids: Collection<UUID>): List<ProductJpaEntity>

	@Query(
		"""
		select distinct p
		from ProductJpaEntity p
		join fetch p.category
		left join fetch p.images
		where p.id = :id and p.status = :status
		""",
	)
	fun findDetailedByIdAndStatus(
		@Param("id") id: UUID,
		@Param("status") status: String,
	): ProductJpaEntity?
}

@Repository
class JpaCatalogQueryRepository(
	private val repository: SpringDataProductRepository,
	private val mapper: CatalogPersistenceMapper,
) : CatalogQueryRepository {

	override fun findActive(page: Int, size: Int): CatalogProductPage {
		val pageable = PageRequest.of(
			page,
			size,
			Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")),
		)
		val idPage = repository.findIdsByStatus(ProductStatus.ACTIVE.name, pageable)
		if (idPage.isEmpty) {
			return CatalogProductPage(emptyList(), page, size, idPage.totalElements, idPage.totalPages)
		}

		val productsById = repository.findDetailedByIds(idPage.content)
			.associateBy(ProductJpaEntity::id)
		val products = idPage.content.mapNotNull(productsById::get).map(mapper::toDomain)

		return CatalogProductPage(products, page, size, idPage.totalElements, idPage.totalPages)
	}

	override fun findActiveById(productId: UUID): Product? =
		repository.findDetailedByIdAndStatus(productId, ProductStatus.ACTIVE.name)?.let(mapper::toDomain)
}
