package uz.buildflow.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.buildflow.app.domain.model.ExpenseCategoryItem

interface ExpenseCategoryRepository {
    fun getAllCategories(): Flow<List<ExpenseCategoryItem>>
    suspend fun insertCategory(name: String)
    suspend fun deleteCategory(category: ExpenseCategoryItem)
}
