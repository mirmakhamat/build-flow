package uz.buildflow.app.presentation.objects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.presentation.common.MetricCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectDetailScreen(
    viewModel: ObjectDetailViewModel,
    onBack: () -> Unit,
    onNavigateToWorkers: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIncomes: () -> Unit,
    onNavigateToDailyAttendance: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val obj = uiState.obj
    val summary = uiState.summary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = obj?.name ?: "Obyekt Dashboard",
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
        }
    ) { paddingValues ->
        if (uiState.isLoading || obj == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepBluePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BackgroundLight)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Asosiy Moliyaviy Kartochka
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepBlueDark)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Obyekt Umumiy Qiymati",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                        Text(
                            text = CurrencyFormatter.formatAmount(obj.totalPrice),
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = SurfaceLight
                        )

                        HorizontalDivider(color = DeepBluePrimary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Tushgan pul", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(
                                    text = CurrencyFormatter.formatAmount(summary?.totalReceivedIncome ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Kutilmoqda", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                                Text(
                                    text = CurrencyFormatter.formatAmount(summary?.remainingReceivable ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AmberWarning
                                )
                            }
                        }
                    }
                }

                // 2. Kassa va Xarajat Metriklari
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Qo'ldagi pul (Kassa)",
                        amount = CurrencyFormatter.formatAmountShort(summary?.cashBalance ?: 0.0) + " so'm",
                        accentColor = if ((summary?.cashBalance ?: 0.0) >= 0) EmeraldSuccess else RoseExpense,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Jami Xarajat (Hisoblangan)",
                        amount = CurrencyFormatter.formatAmountShort(summary?.totalExpenses ?: 0.0) + " so'm",
                        accentColor = RoseExpense,
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Ishchilar va Ish kunlari
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Faol Ishchilar",
                        amount = "${summary?.totalWorkerCount ?: 0} nafar",
                        accentColor = DeepBlueLight,
                        icon = Icons.Default.Group,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Ishlangan kunlar",
                        amount = "${summary?.totalWorkDaysCount ?: 0} kun",
                        accentColor = DeepBluePrimary,
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 4. Tezkor Harakatlar Tugmalari
                Text(
                    text = "Tezkor Amallar",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onNavigateToDailyAttendance,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary, contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Davomat", color = Color.White)
                    }

                    Button(
                        onClick = onNavigateToIncomes,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Kirimlar", color = Color.White)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToWorkers,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepBluePrimary)
                    ) {
                        Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = DeepBluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ishchilar", color = DeepBluePrimary)
                    }

                    OutlinedButton(
                        onClick = onNavigateToExpenses,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepBluePrimary)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp), tint = DeepBluePrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Xarajatlar", color = DeepBluePrimary)
                    }
                }

                // 5. Xarajatlar va Ishchilar Taqsimoti (Breakdown)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Batafsil Hisob-kitob",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        HorizontalDivider(color = BorderColor)

                        BreakdownRow(title = "Hisoblangan ish haqi", amount = summary?.totalWorkerSalary ?: 0.0)
                        BreakdownRow(title = "Hisoblangan bonuslar", amount = summary?.totalBonuses ?: 0.0)
                        BreakdownRow(title = "Ishchilarga to'langan (Real)", amount = summary?.totalPaidToWorkers ?: 0.0, customColor = EmeraldSuccess)
                        BreakdownRow(
                            title = "Ishchilarga qolgan qarz",
                            amount = summary?.totalWorkerDebt ?: 0.0,
                            customColor = if ((summary?.totalWorkerDebt ?: 0.0) > 0) RoseExpense else EmeraldSuccess
                        )

                        HorizontalDivider(color = BorderColor)
                        Text(
                            text = "Qo'shimcha Xarajatlar",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextSecondary
                        )

                        if (summary?.categoryBreakdowns.isNullOrEmpty()) {
                            BreakdownRow(title = "Boshqa xarajatlar", amount = summary?.totalOtherExpenses ?: 0.0)
                        } else {
                            summary?.categoryBreakdowns?.forEach { item ->
                                BreakdownRow(title = item.categoryName, amount = item.totalAmount)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreakdownRow(
    title: String,
    amount: Double,
    customColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = CurrencyFormatter.formatAmount(amount),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = customColor ?: TextPrimary
        )
    }
}
