package uz.buildflow.app.presentation.expenses

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.DeepBluePrimary
import uz.buildflow.app.core.theme.RoseExpense
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.Expense
import uz.buildflow.app.domain.model.ExpenseCategoryItem
import uz.buildflow.app.presentation.common.AmountInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    existingExpense: Expense? = null,
    categories: List<ExpenseCategoryItem>,
    onDismiss: () -> Unit,
    onDelete: ((Expense) -> Unit)? = null,
    onAddNewCategory: (String) -> Unit,
    onSave: (category: String, amount: Double, date: String, description: String?, workerId: String?) -> Unit
) {
    var selectedCategoryName by remember {
        mutableStateOf(existingExpense?.category ?: categories.firstOrNull()?.name ?: "Boshqa xarajat")
    }
    var amountStr by remember {
        mutableStateOf(existingExpense?.amount?.toLong()?.toString() ?: "")
    }
    var date by remember {
        mutableStateOf(existingExpense?.date ?: DateUtil.today())
    }
    var description by remember {
        mutableStateOf(existingExpense?.description ?: "")
    }
    var isNewCategoryDialogOpen by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val isEditMode = existingExpense != null
    val isValid = amountStr.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Xarajatni Tahrirlash" else "Yangi Xarajat Kiritish",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            Text(text = "Kategoriya", style = MaterialTheme.typography.labelMedium)
            
            OptInFlowRow(
                categories = categories,
                selectedCategory = selectedCategoryName,
                onCategorySelect = { selectedCategoryName = it },
                onAddCategoryClick = { isNewCategoryDialogOpen = true }
            )

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Xarajat summasi (so'm)"
            )

            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Sana (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Izoh (masalan: 10 qop sement, taksi)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(selectedCategoryName, amt, date, description.trim().ifBlank { null }, null)
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepBluePrimary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isEditMode) "O'zgarishlarni Saqlash" else "Xarajatni Saqlash",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            if (isEditMode && onDelete != null && existingExpense != null) {
                OutlinedButton(
                    onClick = { onDelete(existingExpense) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoseExpense
                    )
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = RoseExpense)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ushbu Xarajatni O'chirish", color = RoseExpense)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (isNewCategoryDialogOpen) {
        AlertDialog(
            onDismissRequest = { isNewCategoryDialogOpen = false },
            title = { Text("Yangi Kategoriya Yaratish") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Kategoriya nomi (masalan: Santexnika)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddNewCategory(newCategoryName.trim())
                            selectedCategoryName = newCategoryName.trim()
                            newCategoryName = ""
                            isNewCategoryDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepBluePrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Qo'shish", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewCategoryDialogOpen = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptInFlowRow(
    categories: List<ExpenseCategoryItem>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    onAddCategoryClick: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { cat ->
            FilterChip(
                selected = selectedCategory == cat.name,
                onClick = { onCategorySelect(cat.name) },
                label = { Text(cat.name) }
            )
        }

        AssistChip(
            onClick = onAddCategoryClick,
            label = { Text("Yangi") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        )
    }
}
