package uz.buildflow.app.presentation.objects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.DeepBluePrimary
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.ObjectStatus
import uz.buildflow.app.presentation.common.AmountInputField
import uz.buildflow.app.presentation.common.DatePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditObjectSheet(
    existingObject: BuildObject?,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String?, totalPrice: Double, startDate: String, status: ObjectStatus) -> Unit
) {
    var name by remember { mutableStateOf(existingObject?.name ?: "") }
    var description by remember { mutableStateOf(existingObject?.description ?: "") }
    var totalPriceStr by remember {
        mutableStateOf(
            if (existingObject != null && existingObject.totalPrice > 0)
                existingObject.totalPrice.toLong().toString()
            else ""
        )
    }
    var startDate by remember { mutableStateOf(existingObject?.startDate ?: DateUtil.today()) }
    var status by remember { mutableStateOf(existingObject?.status ?: ObjectStatus.ACTIVE) }

    val isValid = name.isNotBlank()

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
                    text = if (existingObject == null) "Yangi Obyekt Yaratish" else "Obyektni Tahrirlash",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Obyekt nomi * (masalan: Chilonzor 12-uy)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AmountInputField(
                    value = totalPriceStr,
                    onValueChange = { totalPriceStr = it },
                    label = "Obyekt umumiy shartnoma summasi",
                    isOptional = true
                )
                if (totalPriceStr.isBlank()) {
                    Text(
                        text = "💡 Eslatma: Summa kiritilmasa, kutilayotgan foyda va mijoz qarzdorligi hisoblanmaydi.",
                        style = MaterialTheme.typography.labelSmall,
                        color = DeepBluePrimary
                    )
                }
            }

            DatePickerField(
                value = startDate,
                onDateSelected = { startDate = it },
                label = "Boshlanish sanasi"
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Izoh / Tavsif (ixtiyoriy)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Button(
                onClick = {
                    val price = totalPriceStr.toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), description.trim().ifBlank { null }, price, startDate, status)
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
                    text = if (existingObject == null) "Obyektni Saqlash" else "O'zgarishlarni Saqlash",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
