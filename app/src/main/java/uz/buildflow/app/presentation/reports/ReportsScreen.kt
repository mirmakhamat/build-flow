package uz.buildflow.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.domain.model.ObjectFinancialSummary
import uz.buildflow.app.presentation.common.MetricCard
import uz.buildflow.app.presentation.objects.BreakdownRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    summary: ObjectFinancialSummary?,
    onBackToObjects: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Obyekt Moliyaviy Hisoboti",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    if (onBackToObjects != null) {
                        IconButton(onClick = onBackToObjects) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Barcha Obyektlar"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onRefresh?.invoke() }) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Umumiy Xulosa Kartochkasi
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
                        text = summary?.objectName ?: "Obyekt Xulosasi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    HorizontalDivider(color = BorderColor)

                    // 1. Mijoz va Daromad qismi
                    Text(
                        text = "Mijoz To'lovlari va Qoldiq",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    BreakdownRow(title = "Obyekt umumiy narxi", amount = summary?.totalPrice ?: 0.0)
                    BreakdownRow(title = "Tushgan pul (Olingan avanslar)", amount = summary?.totalReceivedIncome ?: 0.0, customColor = EmeraldSuccess)
                    BreakdownRow(
                        title = "Mijozdan qolgan summa (Qoldiq)",
                        amount = summary?.remainingReceivable ?: 0.0,
                        customColor = if ((summary?.remainingReceivable ?: 0.0) > 0) AmberWarning else EmeraldSuccess
                    )

                    HorizontalDivider(color = BorderColor)

                    // 2. Ishchilar qismi
                    Text(
                        text = "Ishchilar Hisob-kitobi",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    BreakdownRow(title = "Hisoblangan jami ish haqi", amount = summary?.totalWorkerSalary ?: 0.0)
                    BreakdownRow(title = "Hisoblangan jami bonuslar", amount = summary?.totalBonuses ?: 0.0)
                    BreakdownRow(title = "Ishchilarga to'langan pul (Real)", amount = summary?.totalPaidToWorkers ?: 0.0, customColor = EmeraldSuccess)
                    BreakdownRow(
                        title = "Ishchilarga qolgan qarz",
                        amount = summary?.totalWorkerDebt ?: 0.0,
                        customColor = if ((summary?.totalWorkerDebt ?: 0.0) > 0) RoseExpense else EmeraldSuccess
                    )

                    HorizontalDivider(color = BorderColor)

                    // 3. Qo'shimcha Xarajatlar
                    Text(
                        text = "Qo'shimcha Xarajatlar Taqsimoti",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    if (summary?.categoryBreakdowns.isNullOrEmpty()) {
                        BreakdownRow(title = "Boshqa xarajatlar", amount = summary?.totalOtherExpenses ?: 0.0)
                    } else {
                        summary?.categoryBreakdowns?.forEach { item ->
                            BreakdownRow(title = item.categoryName, amount = item.totalAmount)
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    // 4. Yakuniy Moliyaviy Natija
                    BreakdownRow(
                        title = "Jami Xarajatlar (Hisoblangan)",
                        amount = summary?.totalExpenses ?: 0.0,
                        customColor = RoseExpense
                    )
                    BreakdownRow(
                        title = "Taxminiy Sof Foyda",
                        amount = summary?.estimatedProfit ?: 0.0,
                        customColor = DeepBluePrimary
                    )
                }
            }

            // Kassa va Foyda Kartochkalari
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Kassa Qoldig'i (Naqd pul)",
                    amount = CurrencyFormatter.formatAmountShort(summary?.cashBalance ?: 0.0) + " so'm",
                    accentColor = if ((summary?.cashBalance ?: 0.0) >= 0) EmeraldSuccess else RoseExpense,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Taxminiy Sof Foyda",
                    amount = CurrencyFormatter.formatAmountShort(summary?.estimatedProfit ?: 0.0) + " so'm",
                    accentColor = DeepBluePrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
