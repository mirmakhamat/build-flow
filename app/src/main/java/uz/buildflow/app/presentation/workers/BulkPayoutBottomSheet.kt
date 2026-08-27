package uz.buildflow.app.presentation.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.presentation.common.AmountInputField
import uz.buildflow.app.presentation.common.DatePickerField

const val SOURCE_OWN_POCKET = "OWN_POCKET"

data class BulkWorkerPayoutState(
    val workerId: String,
    val workerName: String,
    val position: String?,
    val currentDebt: Double,
    val amountStr: String,
    val isSelected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkPayoutBottomSheet(
    workersWithStats: List<WorkerWithStats>,
    availableObjects: List<BuildObject>,
    currentObjectId: String,
    onDismiss: () -> Unit,
    onExecutePayout: (payouts: Map<String, Double>, paymentDate: String, payerObjectId: String?) -> Unit
) {
    // Har bir ishchi uchun boshlang'ich holat: agar qarzi bo'lsa qarzi, bo'lmasa 0
    var payoutStates by remember(workersWithStats) {
        mutableStateOf(
            workersWithStats.map { item ->
                val debt = item.stats?.remainingDebtToWorker ?: 0.0
                BulkWorkerPayoutState(
                    workerId = item.worker.id,
                    workerName = item.worker.name,
                    position = item.worker.position,
                    currentDebt = debt,
                    amountStr = if (debt > 0) debt.toLong().toString() else "0",
                    isSelected = debt > 0
                )
            }
        )
    }

    var paymentDate by remember { mutableStateOf(DateUtil.today()) }
    var selectedPayerObjectId by remember { mutableStateOf<String?>(null) }
    var isObjectMenuExpanded by remember { mutableStateOf(false) }

    val totalPayoutAmount = remember(payoutStates) {
        payoutStates.filter { it.isSelected }.sumOf { it.amountStr.toDoubleOrNull() ?: 0.0 }
    }

    val selectedCount = remember(payoutStates) {
        payoutStates.count { it.isSelected }
    }

    val allSelected = remember(payoutStates) {
        payoutStates.isNotEmpty() && payoutStates.all { it.isSelected }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceLight,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Sarlavha va Yopish
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ommaviy Ish Haqi To'lash",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "${payoutStates.size} nafar ishchi (${selectedCount} tasi tanlandi)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hammasini tanlash / Bekor qilish paneli
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantLight.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val newSelectedState = !allSelected
                        payoutStates = payoutStates.map { it.copy(isSelected = newSelectedState) }
                    }
                ) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            payoutStates = payoutStates.map { it.copy(isSelected = checked) }
                        }
                    )
                    Text(
                        text = if (allSelected) "Hammasini bekor qilish" else "Hammasini tanlash",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = DeepBluePrimary
                    )
                }

                Text(
                    text = "Tanlandi: $selectedCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Ishchilar ro'yxati va inputlar
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(payoutStates, key = { it.workerId }) { state ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (state.isSelected) DeepBluePrimary.copy(alpha = 0.04f) else SurfaceLight
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(
                                if (state.isSelected) DeepBluePrimary.copy(alpha = 0.4f) else BorderColor
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = state.isSelected,
                                onCheckedChange = { checked ->
                                    payoutStates = payoutStates.map {
                                        if (it.workerId == state.workerId) it.copy(isSelected = checked) else it
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.width(4.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.workerName,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Qarzdorlik: ${CurrencyFormatter.formatAmount(state.currentDebt)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (state.currentDebt > 0) AmberWarning else EmeraldSuccess
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // To'lanadigan summa inputi
                            Box(modifier = Modifier.width(135.dp)) {
                                AmountInputField(
                                    value = state.amountStr,
                                    onValueChange = { newVal ->
                                        payoutStates = payoutStates.map {
                                            if (it.workerId == state.workerId) it.copy(amountStr = newVal, isSelected = true) else it
                                        }
                                    },
                                    label = "To'lov",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // To'lov parametrlari (Sana va Kassa)
            DatePickerField(
                value = paymentDate,
                onDateSelected = { paymentDate = it },
                label = "Pul berilgan sana"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Kassa manbasi tanlash
            val currentObj = availableObjects.find { it.id == currentObjectId }
            val selectedObj = availableObjects.find { it.id == selectedPayerObjectId }
            val sourceDisplayName = when (selectedPayerObjectId) {
                SOURCE_OWN_POCKET -> "👤 O'zimning hisobimdan (Shaxsiy cho'ntak)"
                null -> "${currentObj?.name ?: "Ushbu obyekt"} (O'z kassasidan)"
                else -> "${selectedObj?.name} kassasidan"
            }

            ExposedDropdownMenuBox(
                expanded = isObjectMenuExpanded,
                onExpandedChange = { isObjectMenuExpanded = !isObjectMenuExpanded }
            ) {
                OutlinedTextField(
                    value = sourceDisplayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("To'lov manbasi (Kassa)") },
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
                    DropdownMenuItem(
                        text = { Text("👤 O'zimning hisobimdan (Shaxsiy cho'ntak)") },
                        onClick = {
                            selectedPayerObjectId = SOURCE_OWN_POCKET
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

            Spacer(modifier = Modifier.height(14.dp))

            // Jami summa xulosasi va Tasdiqlash tugmasi
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Jami to'lanayotgan summa:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = CurrencyFormatter.formatAmount(totalPayoutAmount),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess
                        )
                    }

                    Button(
                        onClick = {
                            val payouts = payoutStates
                                .filter { it.isSelected }
                                .associate { it.workerId to (it.amountStr.toDoubleOrNull() ?: 0.0) }
                                .filter { it.value > 0 }

                            onExecutePayout(payouts, paymentDate, selectedPayerObjectId)
                            onDismiss()
                        },
                        enabled = totalPayoutAmount > 0 && selectedCount > 0,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("To'lash", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}
