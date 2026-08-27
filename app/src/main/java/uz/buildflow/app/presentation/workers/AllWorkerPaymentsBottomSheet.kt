package uz.buildflow.app.presentation.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import uz.buildflow.app.domain.model.PaymentType
import uz.buildflow.app.domain.model.Worker
import uz.buildflow.app.domain.model.WorkerPayment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllWorkerPaymentsBottomSheet(
    payments: List<WorkerPayment>,
    workers: List<Worker>,
    availableObjects: List<BuildObject>,
    currentObjectId: String,
    onDismiss: () -> Unit,
    onPaymentClick: (WorkerPayment) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val workersMap = remember(workers) { workers.associateBy { it.id } }
    val objectsMap = remember(availableObjects) { availableObjects.associateBy { it.id } }

    val filteredPayments = remember(payments, searchQuery, workersMap) {
        if (searchQuery.isBlank()) {
            payments.sortedByDescending { it.paymentDate ?: it.date }
        } else {
            val q = searchQuery.trim().lowercase()
            payments.filter { p ->
                val wName = workersMap[p.workerId]?.name?.lowercase() ?: ""
                val desc = p.description?.lowercase() ?: ""
                wName.contains(q) || desc.contains(q)
            }.sortedByDescending { it.paymentDate ?: it.date }
        }
    }

    val totalAmount = remember(filteredPayments) {
        filteredPayments.sumOf { it.amount }
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
            // Sarlavha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ishchilar To'lovlar Tarixi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Jami ${filteredPayments.size} ta to'lov yozuvi",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Qidiruv inputi
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ishchi ismi yoki izoh bo'yicha qidirish...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Tozalash", tint = TextMuted)
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepBluePrimary,
                    unfocusedBorderColor = BorderColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            val isPrivacyMode = uz.buildflow.app.core.preferences.LocalPrivacyMode.current
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldSuccess.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Jami berilgan summa:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(
                        text = CurrencyFormatter.formatAmount(totalAmount, isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // To'lovlar ro'yxati
            if (filteredPayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "Mos keluvchi to'lovlar topilmadi" else "Hali to'lovlar amalga oshirilmagan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredPayments, key = { it.id }) { payment ->
                        val worker = workersMap[payment.workerId]
                        val workerName = worker?.name ?: "Noma'lum ishchi"

                        val isPayerOtherObj = payment.payerObjectId != null && payment.payerObjectId != currentObjectId && payment.payerObjectId != SOURCE_OWN_POCKET
                        val isOwnPocket = payment.payerObjectId == SOURCE_OWN_POCKET
                        val payerObjName = if (isPayerOtherObj) objectsMap[payment.payerObjectId]?.name ?: "Boshqa obyekt" else null

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPaymentClick(payment) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = workerName,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )

                                    val typeLabel = when (payment.type) {
                                        PaymentType.SALARY -> "Ish haqi"
                                        PaymentType.ADVANCE -> "Avans"
                                        PaymentType.BONUS_PAYOUT -> "Bonus to'lovi"
                                        PaymentType.OTHER -> "To'lov"
                                    }

                                    Text(
                                        text = if (!payment.description.isNullOrBlank()) "$typeLabel · ${payment.description}" else typeLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val displayDate = payment.paymentDate ?: payment.date
                                        Text(
                                            text = "Sana: ${DateUtil.formatToDisplay(displayDate)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )

                                        if (isOwnPocket) {
                                            Surface(
                                                color = Color(0xFFFEF3C7),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "O'z hisobidan",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = Color(0xFFD97706),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        } else if (isPayerOtherObj && payerObjName != null) {
                                            Surface(
                                                color = DeepBluePrimary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "$payerObjName kassasidan",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = DeepBluePrimary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = CurrencyFormatter.formatAmount(payment.amount, isPrivacyMode),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
