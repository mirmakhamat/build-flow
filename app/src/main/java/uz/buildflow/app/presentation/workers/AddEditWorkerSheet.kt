package uz.buildflow.app.presentation.workers

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
import uz.buildflow.app.domain.model.Worker
import uz.buildflow.app.presentation.common.AmountInputField
import uz.buildflow.app.presentation.common.PhoneInputField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWorkerSheet(
    existingWorker: Worker?,
    availableObjects: List<BuildObject>,
    defaultSelectedObjectId: String?,
    onDismiss: () -> Unit,
    onSave: (objectId: String, name: String, phone: String?, position: String?, defaultRate: Double, startDate: String) -> Unit
) {
    var selectedObjectId by remember {
        mutableStateOf(
            existingWorker?.objectId ?: defaultSelectedObjectId ?: availableObjects.firstOrNull()?.id ?: ""
        )
    }
    var name by remember { mutableStateOf(existingWorker?.name ?: "") }
    var phone by remember { mutableStateOf(existingWorker?.phone ?: "") }
    var position by remember { mutableStateOf(existingWorker?.position ?: "") }
    var defaultRateStr by remember { mutableStateOf(existingWorker?.defaultRate?.toLong()?.toString() ?: "250000") }
    var startDate by remember { mutableStateOf(existingWorker?.startDate ?: DateUtil.today()) }

    val isValid = name.isNotBlank() && selectedObjectId.isNotBlank()

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
                    text = if (existingWorker == null) "Yangi Ishchi Qo'shish" else "Ishchini Tahrirlash",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            // Obyekt tanlash (agar 2 yoki undan ortiq bo'lsa)
            if (availableObjects.size > 1) {
                Text(text = "Obyektni tanlang", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableObjects.forEach { obj ->
                        FilterChip(
                            selected = selectedObjectId == obj.id,
                            onClick = { selectedObjectId = obj.id },
                            label = { Text(obj.name) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Ishchining ismi (F.I.Sh)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = position,
                onValueChange = { position = it },
                label = { Text("Kasbi / Lavozimi (Usta, Santexnik, Malyar)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            AmountInputField(
                value = defaultRateStr,
                onValueChange = { defaultRateStr = it },
                label = "Standart kunlik stavka (tavsiya)"
            )

            PhoneInputField(
                value = phone,
                onValueChange = { phone = it },
                label = "Telefon raqami (masalan: 901234567)"
            )

            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Ish boshlagan sana (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    val rate = defaultRateStr.toDoubleOrNull() ?: 0.0
                    onSave(selectedObjectId, name.trim(), phone.trim().ifBlank { null }, position.trim().ifBlank { null }, rate, startDate)
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
                    text = if (existingWorker == null) "Ishchini Saqlash" else "O'zgarishlarni Saqlash",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
