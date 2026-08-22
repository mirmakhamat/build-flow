package uz.buildflow.app.presentation.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.EmeraldSuccess
import uz.buildflow.app.core.theme.RoseExpense
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.MoneyTransaction
import uz.buildflow.app.presentation.common.AmountInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeSheet(
    existingTransaction: MoneyTransaction? = null,
    onDismiss: () -> Unit,
    onDelete: ((MoneyTransaction) -> Unit)? = null,
    onSave: (amount: Double, date: String, description: String?) -> Unit
) {
    var amountStr by remember {
        mutableStateOf(existingTransaction?.amount?.toLong()?.toString() ?: "")
    }
    var date by remember {
        mutableStateOf(existingTransaction?.date ?: DateUtil.today())
    }
    var description by remember {
        mutableStateOf(existingTransaction?.description ?: "")
    }

    val isEditMode = existingTransaction != null
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
                    text = if (isEditMode) "Kirimni Tahrirlash" else "Mijozdan Pul Kirimi (Tushum)",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Tushgan summa (so'm)"
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
                label = { Text("Izoh (masalan: 1-bosqich avansi, naqd berildi)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(amt, date, description.trim().ifBlank { null })
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldSuccess,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isEditMode) "O'zgarishlarni Saqlash" else "Kirimni Saqlash",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }

            if (isEditMode && onDelete != null && existingTransaction != null) {
                OutlinedButton(
                    onClick = { onDelete(existingTransaction) },
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
                    Text("Ushbu Kirimni O'chirish", color = RoseExpense)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
