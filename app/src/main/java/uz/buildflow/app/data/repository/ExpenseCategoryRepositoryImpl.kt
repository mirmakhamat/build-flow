package uz.buildflow.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.buildflow.app.data.local.dao.ExpenseCategoryDao
import uz.buildflow.app.data.local.entity.ExpenseCategoryEntity
import uz.buildflow.app.data.local.toDomain
import uz.buildflow.app.data.local.toEntity
import uz.buildflow.app.domain.model.ExpenseCategoryItem
import uz.buildflow.app.domain.repository.ExpenseCategoryRepository
import java.util.UUID

class ExpenseCategoryRepositoryImpl(
    private val categoryDao: ExpenseCategoryDao
) : ExpenseCategoryRepository {

    override fun getAllCategories(): Flow<List<ExpenseCategoryItem>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun insertCategory(name: String) {
        val entity = ExpenseCategoryEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            isDefault = false
        )
        categoryDao.insertCategory(entity)
    }

    override suspend fun deleteCategory(category: ExpenseCategoryItem) {
        if (!category.isDefault) {
            categoryDao.deleteCategory(category.toEntity())
        }
    }
}
