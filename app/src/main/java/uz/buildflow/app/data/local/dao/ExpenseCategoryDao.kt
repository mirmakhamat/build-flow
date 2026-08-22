package uz.buildflow.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uz.buildflow.app.data.local.entity.ExpenseCategoryEntity

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY is_default DESC, name ASC")
    fun getAllCategories(): Flow<List<ExpenseCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<ExpenseCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: ExpenseCategoryEntity)
}
