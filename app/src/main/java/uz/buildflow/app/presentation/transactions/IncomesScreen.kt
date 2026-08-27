package uz.buildflow.app.presentation.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.MoneyTransaction
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.MetricCard
import uz.buildflow.app.presentation.common.PrivacyToggleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomesScreen(
    viewModel: IncomesViewModel,
    onBack: () -> Unit,
    onTogglePrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPrivacyMode = LocalPrivacyMode.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pul Kirimlari & Avanslar",
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddIncome() },
                containerColor = EmeraldSuccess,
                contentColor = SurfaceLight,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Kirim qo'shish")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 3 ta Asosiy Ko'rsatkichlar Kartochkasi
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Obyekt umumiy narxi:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(text = if (uiState.totalPrice > 0) CurrencyFormatter.formatAmount(uiState.totalPrice, isPrivacyMode) else "Kiritilmagan", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Mijoz to'lagan summa (Avans):", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(text = CurrencyFormatter.formatAmount(uiState.totalClientIncome, isPrivacyMode), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                        }

                        if (uiState.totalTransfersIn > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "  ↳ Boshqa obyekt kassasidan o'tkazma:", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8B5CF6))
                                Text(text = CurrencyFormatter.formatAmount(uiState.totalTransfersIn, isPrivacyMode), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF8B5CF6))
                            }
                        }

                        HorizontalDivider(color = BorderColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Mijozdan qolgan summa (Qoldiq):", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(
                                text = if (uiState.totalPrice > 0) CurrencyFormatter.formatAmount(uiState.remainingReceivable, isPrivacyMode) else "—",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (uiState.remainingReceivable > 0) AmberWarning else EmeraldSuccess
                            )
                        }
                    }
                }

                if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                    EmptyStateView(
                        title = "Kirimlar yo'q",
                        description = "Mijozdan olingan pullarni qayd qilish uchun '+' tugmasini bosing.",
                        icon = Icons.Default.Paid,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.transactions, key = { it.id }) { tx ->
                            IncomeItemCard(
                                tx = tx,
                                onClick = { viewModel.openEditIncome(tx) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddSheetOpen) {
        AddIncomeSheet(
            existingTransaction = uiState.selectedTransaction,
            availableObjects = uiState.availableObjects,
            currentObjectId = uiState.selectedObjectId ?: "",
            onDismiss = { viewModel.closeAddIncome() },
            onDelete = { tx -> viewModel.deleteTransaction(tx) },
            onSave = { amt, date, desc, sourceObjId ->
                viewModel.saveIncome(amt, date, desc, sourceObjId)
            }
        )
    }
}

@Composable
fun IncomeItemCard(
    tx: MoneyTransaction,
    onClick: () -> Unit
) {
    val isTransfer = tx.description?.startsWith("[") == true && tx.description.contains("kassasidan o'tkazma")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = if (isTransfer) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF8B5CF6).copy(alpha = 0.5f))
        ) else CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderColor)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (isTransfer) {
                    Surface(
                        color = Color(0xFFEDE9FE),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = "Kassalararo o'tkazma",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = tx.description ?: "Mijoz to'lovi / Avans",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = DateUtil.formatToFullDisplay(tx.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            val isPrivacyMode = LocalPrivacyMode.current
            Text(
                text = "+ " + CurrencyFormatter.formatAmount(tx.amount, isPrivacyMode),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = EmeraldSuccess
            )
        }
    }
}
