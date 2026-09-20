package com.viniciusdevassis.laumileymodas.infrastructure.repositories

import com.viniciusdevassis.laumileymodas.domain.entities.Category
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CategoryRepository : JpaRepository<Category, UUID>
