package uz.buildflow.app.domain.model

import java.util.UUID

data class ExpenseCategoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CategoryExpenseBreakdown(
    val categoryName: String,
    val totalAmount: Double
)
