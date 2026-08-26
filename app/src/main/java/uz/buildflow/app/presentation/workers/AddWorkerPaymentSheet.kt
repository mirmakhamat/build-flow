package uz.buildflow.app.presentation.workers

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
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.PaymentType
import uz.buildflow.app.domain.model.WorkerPayment
import uz.buildflow.app.presentation.common.AmountInputField
import uz.buildflow.app.presentation.common.DatePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerPaymentSheet(
    existingPayment: WorkerPayment? = null,
    defaultDate: String = DateUtil.today(),
    availableObjects: List<BuildObject> = emptyList(),
    currentObjectId: String = "",
    onDismiss: () -> Unit,
    onDelete: ((WorkerPayment) -> Unit)? = null,
    onSave: (amount: Double, date: String, type: PaymentType, description: String?, isPaid: Boolean, payerObjectId: String?, paymentDate: String?) -> Unit
) {
    val targetDate = remember { existingPayment?.date ?: defaultDate }
    var actualPaymentDate by remember {
        mutableStateOf(existingPayment?.paymentDate ?: DateUtil.today())
    }
    var amountStr by remember {
        mutableStateOf(if (existingPayment != null && existingPayment.amount > 0) existingPayment.amount.toLong().toString() else "")
    }
    var selectedType by remember {
        mutableStateOf(if (existingPayment?.type == PaymentType.SALARY) PaymentType.SALARY else PaymentType.ADVANCE)
    }
    var selectedPayerObjectId by remember {
        mutableStateOf(existingPayment?.payerObjectId)
    }
    var description by remember {
        mutableStateOf(existingPayment?.description ?: "")
    }
    var isObjectMenuExpanded by remember { mutableStateOf(false) }

    val isEditMode = existingPayment != null && existingPayment.amount > 0
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
                Column {
                    Text(
                        text = if (isEditMode) "To'lovni Tahrirlash" else "To'lov / Avans Berish",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Qaysi kun uchun: ${DateUtil.formatToFullDisplay(targetDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepBluePrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            Text(text = "To'lov turi", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    PaymentType.ADVANCE to "Avans",
                    PaymentType.SALARY to "Ish haqi to'lovi"
                ).forEach { (t, label) ->
                    FilterChip(
                        selected = selectedType == t,
                        onClick = { selectedType = t },
                        label = { Text(label) }
                    )
                }
            }

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "To'lov summasi (so'm)"
            )

            DatePickerField(
                value = actualPaymentDate,
                onDateSelected = { actualPaymentDate = it },
                label = "Pul berilgan sana"
            )

            // KROSS-OBYEKT: Qaysi obyekt kassasidan to'lanadi?
            if (availableObjects.isNotEmpty()) {
                val selectedObj = availableObjects.find { it.id == selectedPayerObjectId }
                val currentObj = availableObjects.find { it.id == currentObjectId }
                val displayName = selectedObj?.name ?: "${currentObj?.name ?: "Ushbu obyekt"} (O'z kassasidan)"

                ExposedDropdownMenuBox(
                    expanded = isObjectMenuExpanded,
                    onExpandedChange = { isObjectMenuExpanded = !isObjectMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To'lov manbasi (Obyekt kassasi)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isObjectMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isObjectMenuExpanded,
                        onDismissRequest = { isObjectMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("${currentObj?.name ?: "Ushbu obyekt"} (O'z kassasidan)") },
                            onClick = {
                                selectedPayerObjectId = null
                                isObjectMenuExpanded = false
                            }
                        )
                        availableObjects.filter { it.id != currentObjectId }.forEach { objItem ->
                            DropdownMenuItem(
                                text = { Text("${objItem.name} kassasidan") },
                                onClick = {
                                    selectedPayerObjectId = objItem.id
                                    isObjectMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Izoh (ixtiyoriy)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(amt, targetDate, selectedType, description.trim().ifBlank { null }, true, selectedPayerObjectId, actualPaymentDate.trim().ifBlank { null })
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
            ) {
                Text("Pul berildi (Kassadan chiqim)", color = Color.White)
            }

            if (isEditMode && onDelete != null && existingPayment != null) {
                OutlinedButton(
                    onClick = { onDelete(existingPayment) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseExpense)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseExpense)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("To'lovni o'chirish", color = RoseExpense, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
