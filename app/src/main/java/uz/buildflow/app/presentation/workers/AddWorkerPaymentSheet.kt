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
import uz.buildflow.app.domain.model.PaymentType
import uz.buildflow.app.domain.model.WorkerPayment
import uz.buildflow.app.presentation.common.AmountInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWorkerPaymentSheet(
    existingPayment: WorkerPayment? = null,
    defaultDate: String = DateUtil.today(),
    onDismiss: () -> Unit,
    onDelete: ((WorkerPayment) -> Unit)? = null,
    onSave: (amount: Double, date: String, type: PaymentType, description: String?, isPaid: Boolean) -> Unit
) {
    val targetDate = remember { existingPayment?.date ?: defaultDate }
    var amountStr by remember {
        mutableStateOf(if (existingPayment != null && existingPayment.amount > 0) existingPayment.amount.toLong().toString() else "")
    }
    var selectedType by remember {
        mutableStateOf(if (existingPayment?.type == PaymentType.SALARY) PaymentType.SALARY else PaymentType.ADVANCE)
    }
    var description by remember {
        mutableStateOf(existingPayment?.description ?: "")
    }

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
                        text = DateUtil.formatToFullDisplay(targetDate),
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

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Izoh (masalan: Naqd berildi, kartaga o'tkazildi)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // ASOSIY TUGMALAR: Xuddi Bonus kabi [Pul berildi] va [Qarzga yozish]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        onSave(amt, targetDate, selectedType, description.trim().ifBlank { null }, true)
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
                ) {
                    Text("Pul berildi", color = Color.White, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        onSave(amt, targetDate, selectedType, description.trim().ifBlank { null }, false)
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepBluePrimary)
                ) {
                    Text("Qarzga yozish", color = DeepBluePrimary, fontSize = 13.sp)
                }
            }

            if (isEditMode && onDelete != null && existingPayment != null) {
                OutlinedButton(
                    onClick = { onDelete(existingPayment) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoseExpense
                    )
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = RoseExpense)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("To'lovni o'chirish", color = RoseExpense, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
