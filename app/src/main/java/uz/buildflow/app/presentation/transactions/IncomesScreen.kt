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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.MoneyTransaction
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.MetricCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomesScreen(
    viewModel: IncomesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
                            Text(text = CurrencyFormatter.formatAmount(uiState.totalPrice), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Jami olingan avans/pul:", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(text = CurrencyFormatter.formatAmount(uiState.totalIncome), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                        }

                        HorizontalDivider(color = BorderColor)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Qolib ketgan summa (Qoldiq):", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                            Text(
                                text = CurrencyFormatter.formatAmount(uiState.remainingReceivable),
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
            onDismiss = { viewModel.closeAddIncome() },
            onDelete = { tx -> viewModel.deleteTransaction(tx) },
            onSave = { amt, date, desc ->
                viewModel.saveIncome(amt, date, desc)
            }
        )
    }
}

@Composable
fun IncomeItemCard(
    tx: MoneyTransaction,
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
                    text = tx.description ?: "Mijoz to'lovi / Avans",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = DateUtil.formatToDisplay(tx.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "+${CurrencyFormatter.formatAmount(tx.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldSuccess
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
