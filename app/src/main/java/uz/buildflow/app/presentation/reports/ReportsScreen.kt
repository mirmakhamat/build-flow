package uz.buildflow.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import uz.buildflow.app.domain.model.ObjectFinancialSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    summary: ObjectFinancialSummary?,
    onBackToObjects: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Moliyaviy Tahlil va Hisobot",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToObjects) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Obyektlar")
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
            // 1. OBYEKTNING UMUMIY HOLATI VA RENTABELLIGI (Vizual grafiklar bilan)
            if (summary != null) {
                FinancialHealthCard(summary = summary)
            }

            // 2. KATEGORIYALAR BO'YICHA XARAJATLAR TAQSIMOTI GRAFIGI
            if (summary != null && summary.categoryBreakdowns.isNotEmpty()) {
                ExpenseDistributionChartCard(summary = summary)
            }

            // 3. TO'LIQ BATAFSIL MOLIYAVIY XULOSA
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
                    BreakdownRow(
                        title = "Obyekt umumiy narxi",
                        amount = summary?.totalPrice ?: 0.0,
                        isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                        customText = "Kiritilmagan"
                    )
                    BreakdownRow(title = "Tushgan pul (Olingan avanslar)", amount = summary?.totalReceivedIncome ?: 0.0, customColor = EmeraldSuccess)
                    BreakdownRow(
                        title = "Mijozdan qolgan summa (Qoldiq)",
                        amount = summary?.remainingReceivable ?: 0.0,
                        customColor = if ((summary?.remainingReceivable ?: 0.0) > 0) AmberWarning else EmeraldSuccess,
                        isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                        customText = "—"
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
                    BreakdownRow(title = "Ishchilarga berilgan to'lovlar (Jami)", amount = summary?.totalPaidToWorkers ?: 0.0, customColor = EmeraldSuccess)
                    
                    if ((summary?.totalPaidByOtherObjectsForThisWorkers ?: 0.0) > 0) {
                        BreakdownRow(
                            title = "  ↳ Boshqa obyekt hisobidan qoplangan",
                            amount = summary?.totalPaidByOtherObjectsForThisWorkers ?: 0.0,
                            customColor = DeepBluePrimary
                        )
                    }

                    if ((summary?.totalPaidForOtherObjectsWorkers ?: 0.0) > 0) {
                        BreakdownRow(
                            title = "  ↳ Boshqa obyekt ishchilariga to'lab berilgan",
                            amount = summary?.totalPaidForOtherObjectsWorkers ?: 0.0,
                            customColor = AmberWarning
                        )
                    }

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

                    if ((summary?.totalExpensesPaidByOtherObjects ?: 0.0) > 0) {
                        BreakdownRow(
                            title = "  ↳ Boshqa obyekt hisobidan to'langan xarajat",
                            amount = summary?.totalExpensesPaidByOtherObjects ?: 0.0,
                            customColor = DeepBluePrimary
                        )
                    }

                    if ((summary?.totalExpensesPaidForOtherObjects ?: 0.0) > 0) {
                        BreakdownRow(
                            title = "  ↳ Boshqa obyekt uchun to'lab berilgan xarajat",
                            amount = summary?.totalExpensesPaidForOtherObjects ?: 0.0,
                            customColor = AmberWarning
                        )
                    }

                    HorizontalDivider(color = BorderColor)

                    // 4. Yakuniy Moliyaviy Natija va Kassa
                    BreakdownRow(
                        title = "Jami Obyekt Xarajatlari",
                        amount = summary?.totalExpenses ?: 0.0,
                        customColor = RoseExpense,
                        isBold = true
                    )
                    BreakdownRow(
                        title = "Kassadan chiqqan jami pul (Chiqim)",
                        amount = summary?.totalCashOutflow ?: 0.0,
                        customColor = RoseExpense,
                        isBold = true
                    )
                    BreakdownRow(
                        title = "Qo'ldagi pul (Kassa balansi)",
                        amount = summary?.cashBalance ?: 0.0,
                        customColor = if ((summary?.cashBalance ?: 0.0) >= 0) EmeraldSuccess else RoseExpense,
                        isBold = true
                    )
                    BreakdownRow(
                        title = "Taxminiy Sof Foyda",
                        amount = summary?.estimatedProfit ?: 0.0,
                        customColor = if ((summary?.estimatedProfit ?: 0.0) >= 0) EmeraldSuccess else RoseExpense,
                        isBold = true,
                        isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                        customText = "Kiritilmagan"
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialHealthCard(summary: ObjectFinancialSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DeepBluePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = DeepBluePrimary)
                    }
                    Text(
                        text = "Moliyaviy Rentabellik",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (summary.totalPrice > 0) {
                    val profitMargin = ((summary.estimatedProfit / summary.totalPrice) * 100).toInt()
                    Surface(
                        color = if (profitMargin >= 0) EmeraldLight else RoseLight,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Marja: $profitMargin%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (profitMargin >= 0) EmeraldSuccess else RoseExpense,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 1. RENTABELLIK PROGRESS BARI (Obyekt Narxi vs Xarajatlar vs Foyda)
            if (summary.totalPrice > 0) {
                val expenseRatio = (summary.totalExpenses / summary.totalPrice).toFloat().coerceIn(0f, 1f)
                val profitRatio = (1f - expenseRatio).coerceAtLeast(0f)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Xarajat yuki: ${(expenseRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = RoseExpense
                        )
                        Text(
                            text = "Foyda ulushi: ${(profitRatio * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess
                        )
                    }

                    // Segmented Visual Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(SurfaceVariantLight)
                    ) {
                        if (expenseRatio > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(expenseRatio.coerceAtLeast(0.01f))
                                    .background(RoseExpense)
                            )
                        }
                        if (profitRatio > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(profitRatio.coerceAtLeast(0.01f))
                                    .background(EmeraldSuccess)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

            // 2. MIJOZ AVANSLARI VA ISHCHILAR QARZI TAHLILI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Mijoz To'lovi", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(summary.totalReceivedIncome),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "Qoldiq: " + CurrencyFormatter.formatAmountShort(summary.remainingReceivable),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Ishchilar Qarzi", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(summary.totalWorkerDebt),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.totalWorkerDebt > 0) RoseExpense else EmeraldSuccess
                    )
                    Text(
                        text = "To'langan: " + CurrencyFormatter.formatAmountShort(summary.totalPaidToWorkers),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Kassa Qoldig'i", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(summary.cashBalance),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.cashBalance >= 0) EmeraldSuccess else RoseExpense
                    )
                    Text(
                        text = "Sof Kassa",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseDistributionChartCard(summary: ObjectFinancialSummary) {
    val totalExpenseSum = remember(summary.categoryBreakdowns) {
        summary.categoryBreakdowns.sumOf { it.totalAmount }.coerceAtLeast(1.0)
    }

    val palette = listOf(
        DeepBluePrimary,
        Color(0xFF0D9488), // Teal
        AmberWarning,
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFF3B82F6), // Blue
        RoseExpense
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(RoseExpense.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.PieChart, contentDescription = null, tint = RoseExpense)
                    }
                    Text(
                        text = "Xarajatlar Taqsimoti",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Text(
                    text = "Jami: " + CurrencyFormatter.formatAmountShort(totalExpenseSum),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            // Segmented Distribution Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceVariantLight)
            ) {
                summary.categoryBreakdowns.forEachIndexed { index, item ->
                    val ratio = (item.totalAmount / totalExpenseSum).toFloat()
                    if (ratio > 0f) {
                        val barColor = palette[index % palette.size]
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(ratio.coerceAtLeast(0.01f))
                                .background(barColor)
                        )
                    }
                }
            }

            // Kategoriya qatorlari
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                summary.categoryBreakdowns.forEachIndexed { index, item ->
                    val percentage = ((item.totalAmount / totalExpenseSum) * 100).toInt()
                    val itemColor = palette[index % palette.size]

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(itemColor)
                            )
                            Text(
                                text = item.categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatAmount(item.totalAmount),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
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
    customColor: Color = TextPrimary,
    isBold: Boolean = false,
    isCustomText: Boolean = false,
    customText: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = if (isBold) TextPrimary else TextSecondary
        )
        Text(
            text = if (isCustomText) customText else CurrencyFormatter.formatAmount(amount),
            style = if (isBold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = customColor
        )
    }
}
