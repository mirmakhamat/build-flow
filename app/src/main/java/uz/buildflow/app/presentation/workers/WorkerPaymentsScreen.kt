package uz.buildflow.app.presentation.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.PaymentType
import uz.buildflow.app.domain.model.WorkerPayment
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.MetricCard
import uz.buildflow.app.presentation.common.PrivacyToggleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerPaymentsScreen(
    viewModel: WorkerPaymentsViewModel,
    workerName: String,
    onBack: () -> Unit,
    onTogglePrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState.stats
    val isPrivacyMode = LocalPrivacyMode.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "$workerName — To'lovlar Tarixi",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    PrivacyToggleButton(onToggle = onTogglePrivacy)
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Yangilash (Refresh)",
                            tint = DeepBluePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Balans va qarz kartochkalari
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Berilgan Pul",
                        amount = CurrencyFormatter.formatAmount(stats?.totalPaid ?: 0.0),
                        accentColor = EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Qarz kunlar",
                        amount = CurrencyFormatter.formatAmount(stats?.totalUnpaidAccrued ?: 0.0),
                        accentColor = AmberWarning,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Sof Qarz",
                        amount = CurrencyFormatter.formatAmount(stats?.remainingDebtToWorker ?: 0.0),
                        accentColor = if ((stats?.remainingDebtToWorker ?: 0.0) > 0) RoseExpense else EmeraldSuccess,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.payments.isEmpty() && !uiState.isLoading) {
                    EmptyStateView(
                        title = "To'lovlar tarixi bo'sh",
                        description = "To'lovlar, avanslar va bonuslar faqat kalendar orqali sanani tanlab kiritiladi.",
                        icon = Icons.Default.Payments,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.payments, key = { it.id }) { p ->
                            PaymentItemCard(
                                payment = p,
                                onClick = { viewModel.openEditPayment(p) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddSheetOpen) {
        AddWorkerPaymentSheet(
            existingPayment = uiState.selectedPayment,
            onDismiss = { viewModel.closeAddPayment() },
            onDelete = { p -> viewModel.deletePayment(p) },
            onSave = { amt, date, type, desc, isPaid, payerObjId, paymentDate ->
                if (isPaid) {
                    viewModel.savePayment(amt, date, type, desc, payerObjId, paymentDate)
                } else if (uiState.selectedPayment != null) {
                    viewModel.deletePayment(uiState.selectedPayment!!)
                }
            }
        )
    }
}

@Composable
fun PaymentItemCard(
    payment: WorkerPayment,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (payment.type) {
                        PaymentType.SALARY -> "Ish haqi to'lovi"
                        PaymentType.ADVANCE -> "Avans"
                        PaymentType.BONUS_PAYOUT -> "Bonus to'lovi"
                        PaymentType.OTHER -> "To'lov"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                if (!payment.description.isNullOrBlank()) {
                    Text(
                        text = payment.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                val dateLabel = if (!payment.paymentDate.isNullOrBlank() && payment.paymentDate != payment.date) {
                    "Berilgan sana: ${DateUtil.formatToDisplay(payment.paymentDate)} (${DateUtil.formatToDisplay(payment.date)} uchun)"
                } else {
                    DateUtil.formatToDisplay(payment.date)
                }

                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }

            val isPrivacyMode = LocalPrivacyMode.current
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = CurrencyFormatter.formatAmount(payment.amount, isPrivacyMode),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = DeepBluePrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Tahrirlash",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
