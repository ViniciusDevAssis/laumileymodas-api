package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.Product
import com.viniciusdevassis.laumileymodas.domain.enums.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import jakarta.persistence.LockModeType
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProductRepository : JpaRepository<Product, UUID> {
	@EntityGraph(attributePaths = ["category"])
	fun findByStatus(status: ProductStatus, pageable: Pageable): Page<Product>

	@EntityGraph(attributePaths = ["category"])
	fun findByStatusAndCategoryId(status: ProductStatus, categoryId: UUID, pageable: Pageable): Page<Product>

	@EntityGraph(attributePaths = ["category", "imageList"])
	fun findByIdAndStatus(id: UUID, status: ProductStatus): Product?

	@EntityGraph(attributePaths = ["category", "imageList"])
	@Lock(LockModeType.PESSIMISTIC_READ)
	@Query("select p from Product p where p.id = :id and p.status = :status")
	fun findByIdAndStatusForInterest(@Param("id") id: UUID, @Param("status") status: ProductStatus): Product?
}
