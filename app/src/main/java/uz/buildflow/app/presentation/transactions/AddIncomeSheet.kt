package uz.buildflow.app.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.theme.DeepBluePrimary
import uz.buildflow.app.core.theme.EmeraldSuccess
import uz.buildflow.app.core.theme.RoseExpense
import uz.buildflow.app.core.theme.SurfaceVariantLight
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.MoneyTransaction
import uz.buildflow.app.presentation.common.AmountInputField
import uz.buildflow.app.presentation.common.DatePickerField

enum class IncomeSourceType {
    CLIENT,
    INTER_OBJECT_TRANSFER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeSheet(
    existingTransaction: MoneyTransaction? = null,
    availableObjects: List<BuildObject> = emptyList(),
    currentObjectId: String = "",
    onDismiss: () -> Unit,
    onDelete: ((MoneyTransaction) -> Unit)? = null,
    onSave: (amount: Double, date: String, description: String?, sourceObjectId: String?) -> Unit
) {
    var amountStr by remember {
        mutableStateOf(existingTransaction?.amount?.toLong()?.toString() ?: "")
    }
    var date by remember {
        mutableStateOf(existingTransaction?.date ?: DateUtil.today())
    }
    var description by remember {
        mutableStateOf(
            existingTransaction?.description?.replace(Regex("^\\[.*?\\]\\s*"), "") ?: ""
        )
    }

    val isExistingTransfer = remember {
        existingTransaction?.description?.startsWith("[") == true &&
        existingTransaction.description.contains("kassasidan o'tkazma")
    }

    var sourceType by remember {
        mutableStateOf(if (isExistingTransfer) IncomeSourceType.INTER_OBJECT_TRANSFER else IncomeSourceType.CLIENT)
    }

    val otherObjects = remember(availableObjects, currentObjectId) {
        availableObjects.filter { it.id != currentObjectId }
    }

    var selectedSourceObjectId by remember(otherObjects) {
        mutableStateOf<String?>(otherObjects.firstOrNull()?.id)
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val isEditMode = existingTransaction != null
    val isValid = amountStr.isNotBlank() && (sourceType == IncomeSourceType.CLIENT || selectedSourceObjectId != null)

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
                    text = if (isEditMode) "Kirimni Tahrirlash" else "Pul Kirimi (Tushum)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            // MANBA TURI: MIJOZDAN TO'LOV YOKI BOSHQA OBYEKT KASSASIDAN O'TKAZMA
            if (!isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = sourceType == IncomeSourceType.CLIENT,
                        onClick = { sourceType = IncomeSourceType.CLIENT },
                        label = { Text("Mijozdan to'lov") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = sourceType == IncomeSourceType.INTER_OBJECT_TRANSFER,
                        onClick = { sourceType = IncomeSourceType.INTER_OBJECT_TRANSFER },
                        label = { Text("Kassalararo o'tkazma") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // AGAR KASSALARARO O'TKAZMA BO'LSA - QAYSI OBYEKT KASSASIDAN PUL OLINMOQDA?
            if (sourceType == IncomeSourceType.INTER_OBJECT_TRANSFER && otherObjects.isNotEmpty()) {
                val selectedSourceObj = otherObjects.find { it.id == selectedSourceObjectId }

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedSourceObj?.name ?: "Chiqim qilinadigan obyektni tanlang",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pul qaysi obyekt kassasidan olinmoqda?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        otherObjects.forEach { objItem ->
                            DropdownMenuItem(
                                text = { Text(objItem.name) },
                                onClick = {
                                    selectedSourceObjectId = objItem.id
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepBluePrimary.copy(alpha = 0.08f))
                ) {
                    Text(
                        text = "💡 Eslatma: Ushbu summa tanlangan obyekt kassasidan Chiqim (Xarajat), ushbu joriy obyektga esa Kirim bo'lib yoziladi. Ikkala tomon o'zaro bog'lanadi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepBluePrimary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Tushgan summa (so'm)"
            )

            DatePickerField(
                value = date,
                onDateSelected = { date = it },
                label = "Tushum sanasi"
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Izoh (ixtiyoriy)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    val finalSourceObjId = if (sourceType == IncomeSourceType.INTER_OBJECT_TRANSFER) selectedSourceObjectId else null
                    onSave(amt, date, description.trim().ifBlank { null }, finalSourceObjId)
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
                    color = Color.White,
                    fontWeight = FontWeight.Bold
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
