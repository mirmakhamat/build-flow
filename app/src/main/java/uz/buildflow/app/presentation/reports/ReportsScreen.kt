package uz.buildflow.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.presentation.common.PrivacyToggleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onBackToObjects: () -> Unit,
    onTogglePrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary
    val isPrivacyMode = LocalPrivacyMode.current

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
                actions = {
                    PrivacyToggleButton(onToggle = onTogglePrivacy)
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
            if (uiState.isLoading && summary == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DeepBluePrimary)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. OBYEKTNING UMUMIY HOLATI VA RENTABELLIGI
                    if (summary != null) {
                        FinancialHealthCard(
                            summary = summary,
                            onIncomesClick = { viewModel.openDrillDown(DrillDownType.INCOMES) },
                            onWorkerDebtsClick = { viewModel.openDrillDown(DrillDownType.WORKER_DEBTS) },
                            onCashflowClick = { viewModel.openDrillDown(DrillDownType.CASH_OUTFLOW) }
                        )
                    }

                    // 2. KATEGORIYALAR BO'YICHA XARAJATLAR TAQSIMOTI GRAFIGI
                    if (summary != null && summary.categoryBreakdowns.isNotEmpty()) {
                        ExpenseDistributionChartCard(
                            summary = summary,
                            onCategoryClick = { catName ->
                                viewModel.openDrillDown(DrillDownType.CATEGORY_EXPENSES, catName)
                            }
                        )
                    }

                    // 3. TO'LIQ BATAFSIL MOLIYAVIY XULOSA (Barcha qatorlar bosiladigan!)
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
                            Text(
                                text = "💡 Har qanday ko'rsatkich ustiga bosib, unga ta'sir qilgan barcha yozuvlarni ko'rishingiz mumkin.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepBluePrimary
                            )
                            HorizontalDivider(color = BorderColor)

                            // 1. Mijoz va Daromad qismi
                            Text(
                                text = "Mijoz To'lovlari va Qoldiq",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = DeepBluePrimary
                            )
                            InteractiveBreakdownRow(
                                title = "Obyekt umumiy narxi",
                                amount = summary?.totalPrice ?: 0.0,
                                isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                                customText = "Kiritilmagan",
                                isClickable = false
                            )
                            InteractiveBreakdownRow(
                                title = "Tushgan pul (Mijoz avanslari)",
                                amount = summary?.totalClientIncome ?: 0.0,
                                customColor = EmeraldSuccess,
                                onClick = { viewModel.openDrillDown(DrillDownType.INCOMES) }
                            )
                            if ((summary?.totalTransfersIn ?: 0.0) > 0) {
                                InteractiveBreakdownRow(
                                    title = "  ↳ Boshqa obyekt kassasidan kirgan o'tkazma",
                                    amount = summary?.totalTransfersIn ?: 0.0,
                                    customColor = Color(0xFF8B5CF6),
                                    onClick = { viewModel.openDrillDown(DrillDownType.INCOMES) }
                                )
                            }
                            InteractiveBreakdownRow(
                                title = "Mijozdan qolgan summa (Qoldiq)",
                                amount = summary?.remainingReceivable ?: 0.0,
                                customColor = if ((summary?.remainingReceivable ?: 0.0) > 0) AmberWarning else EmeraldSuccess,
                                isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                                customText = "—",
                                onClick = { viewModel.openDrillDown(DrillDownType.INCOMES) }
                            )

                            HorizontalDivider(color = BorderColor)

                            // 2. Ishchilar qismi
                            Text(
                                text = "Ishchilar Hisob-kitobi",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = DeepBluePrimary
                            )
                            InteractiveBreakdownRow(
                                title = "Hisoblangan jami ish haqi",
                                amount = summary?.totalWorkerSalary ?: 0.0,
                                isClickable = false
                            )
                            InteractiveBreakdownRow(
                                title = "Hisoblangan jami bonuslar",
                                amount = summary?.totalBonuses ?: 0.0,
                                isClickable = false
                            )
                            InteractiveBreakdownRow(
                                title = "Ishchilarga berilgan to'lovlar (Jami)",
                                amount = summary?.totalPaidToWorkers ?: 0.0,
                                customColor = EmeraldSuccess,
                                onClick = { viewModel.openDrillDown(DrillDownType.WORKER_PAYMENTS) }
                            )

                            if ((summary?.totalPaidFromOwnPocketForThisWorkers ?: 0.0) > 0) {
                                InteractiveBreakdownRow(
                                    title = "  ↳ 👤 O'z hisobidan to'langan ish haqi",
                                    amount = summary?.totalPaidFromOwnPocketForThisWorkers ?: 0.0,
                                    customColor = AmberWarning,
                                    onClick = { viewModel.openDrillDown(DrillDownType.WORKER_PAYMENTS) }
                                )
                            }
                            
                            if ((summary?.totalPaidByOtherObjectsForThisWorkers ?: 0.0) > 0) {
                                InteractiveBreakdownRow(
                                    title = "  ↳ Boshqa obyekt hisobidan qoplangan",
                                    amount = summary?.totalPaidByOtherObjectsForThisWorkers ?: 0.0,
                                    customColor = DeepBluePrimary,
                                    onClick = { viewModel.openDrillDown(DrillDownType.EXTERNAL_PAID_FOR_THIS_WORKERS) }
                                )
                            }

                            if ((summary?.totalPaidForOtherObjectsWorkers ?: 0.0) > 0) {
                                InteractiveBreakdownRow(
                                    title = "  ↳ Boshqa obyekt ishchilariga to'lab berilgan",
                                    amount = summary?.totalPaidForOtherObjectsWorkers ?: 0.0,
                                    customColor = AmberWarning,
                                    onClick = { viewModel.openDrillDown(DrillDownType.EXTERNAL_WORKERS_PAID) }
                                )
                            }

                            InteractiveBreakdownRow(
                                title = "Ishchilarga qolgan qarz",
                                amount = summary?.totalWorkerDebt ?: 0.0,
                                customColor = if ((summary?.totalWorkerDebt ?: 0.0) > 0) RoseExpense else EmeraldSuccess,
                                onClick = { viewModel.openDrillDown(DrillDownType.WORKER_DEBTS) }
                            )

                            HorizontalDivider(color = BorderColor)

                            // 3. Qo'shimcha Xarajatlar
                            Text(
                                text = "Qo'shimcha Xarajatlar Taqsimoti",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = DeepBluePrimary
                            )
                            if (summary?.categoryBreakdowns.isNullOrEmpty()) {
                                InteractiveBreakdownRow(
                                    title = "Boshqa xarajatlar",
                                    amount = summary?.totalOtherExpenses ?: 0.0,
                                    onClick = { viewModel.openDrillDown(DrillDownType.ALL_EXPENSES) }
                                )
                            } else {
                                summary?.categoryBreakdowns?.forEach { item ->
                                    InteractiveBreakdownRow(
                                        title = item.categoryName,
                                        amount = item.totalAmount,
                                        onClick = { viewModel.openDrillDown(DrillDownType.CATEGORY_EXPENSES, item.categoryName) }
                                    )
                                }
                            }

                            if ((summary?.totalExpensesPaidFromOwnPocket ?: 0.0) > 0) {
                                InteractiveBreakdownRow(
                                    title = "  ↳ 👤 O'z hisobidan to'langan xarajat",
                                    amount = summary?.totalExpensesPaidFromOwnPocket ?: 0.0,
                                    customColor = AmberWarning,
                                    onClick = { viewModel.openDrillDown(DrillDownType.ALL_EXPENSES) }
                                )
                            }

                            if ((summary?.totalExpensesPaidByOtherObjects ?: 0.0) > 0) {
                                InteractiveBreakdownRow(
                                    title = "  ↳ Boshqa obyekt hisobidan to'langan xarajat",
                                    amount = summary?.totalExpensesPaidByOtherObjects ?: 0.0,
                                    customColor = DeepBluePrimary,
                                    onClick = { viewModel.openDrillDown(DrillDownType.ALL_EXPENSES) }
                                )
                            }

                            if ((summary?.totalExpensesPaidForOtherObjects ?: 0.0) > 0) {
                                InteractiveBreakdownRow(
                                    title = "  ↳ Boshqa obyekt uchun to'lab berilgan xarajat",
                                    amount = summary?.totalExpensesPaidForOtherObjects ?: 0.0,
                                    customColor = AmberWarning,
                                    onClick = { viewModel.openDrillDown(DrillDownType.ALL_EXPENSES) }
                                )
                            }

                            HorizontalDivider(color = BorderColor)

                            // 4. Yakuniy Moliyaviy Natija va Kassa
                            InteractiveBreakdownRow(
                                title = "Jami Obyekt Xarajatlari",
                                amount = summary?.totalExpenses ?: 0.0,
                                customColor = RoseExpense,
                                isBold = true,
                                onClick = { viewModel.openDrillDown(DrillDownType.ALL_EXPENSES) }
                            )
                            if ((summary?.totalPaidFromOwnPocket ?: 0.0) > 0) {
                                InteractiveBreakdownRow(
                                    title = "👤 Jami O'z hisobidan qoplangan summa",
                                    amount = summary?.totalPaidFromOwnPocket ?: 0.0,
                                    customColor = AmberWarning,
                                    isBold = true
                                )
                            }
                            InteractiveBreakdownRow(
                                title = "Kassadan chiqqan jami pul (Chiqim)",
                                amount = summary?.totalCashOutflow ?: 0.0,
                                customColor = RoseExpense,
                                isBold = true,
                                onClick = { viewModel.openDrillDown(DrillDownType.CASH_OUTFLOW) }
                            )
                            InteractiveBreakdownRow(
                                title = "Qo'ldagi pul (Kassa balansi)",
                                amount = summary?.cashBalance ?: 0.0,
                                customColor = if ((summary?.cashBalance ?: 0.0) >= 0) EmeraldSuccess else RoseExpense,
                                isBold = true,
                                onClick = { viewModel.openDrillDown(DrillDownType.CASH_OUTFLOW) }
                            )
                            InteractiveBreakdownRow(
                                title = "Taxminiy Sof Foyda",
                                amount = summary?.estimatedProfit ?: 0.0,
                                customColor = if ((summary?.estimatedProfit ?: 0.0) >= 0) EmeraldSuccess else RoseExpense,
                                isBold = true,
                                isCustomText = (summary?.totalPrice ?: 0.0) <= 0.0,
                                customText = "Kiritilmagan",
                                isClickable = false
                            )
                        }
                    }
                }
            }
        }
    }

    // DRILL-DOWN BATAFSIL RO'YXAT MODAL SHEET
    if (uiState.selectedDrillDownType != null) {
        ReportDrillDownBottomSheet(
            uiState = uiState,
            onDismiss = { viewModel.closeDrillDown() }
        )
    }
}

@Composable
fun InteractiveBreakdownRow(
    title: String,
    amount: Double,
    customColor: Color = TextPrimary,
    isBold: Boolean = false,
    isCustomText: Boolean = false,
    customText: String = "",
    isClickable: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isClickable && onClick != null) { onClick?.invoke() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = if (isBold) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
                color = if (isBold) TextPrimary else TextSecondary
            )
            if (isClickable && onClick != null) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        val isPrivacyMode = LocalPrivacyMode.current
        Text(
            text = if (isCustomText) customText else CurrencyFormatter.formatAmount(amount, isPrivacyMode),
            style = if (isBold) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = customColor
        )
    }
}

@Composable
fun FinancialHealthCard(
    summary: ObjectFinancialSummary,
    onIncomesClick: () -> Unit,
    onWorkerDebtsClick: () -> Unit,
    onCashflowClick: () -> Unit
) {
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

            // 1. RENTABELLIK PROGRESS BARI
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

            val isPrivacyMode = LocalPrivacyMode.current

            // 2. MIJOZ AVANSLARI VA ISHCHILAR QARZI TAHLILI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onIncomesClick() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Mijoz To'lovi", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = CurrencyFormatter.formatAmountShort(summary.totalClientIncome, isPrivacyMode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess
                        )
                        Text(
                            text = "Qoldiq: " + CurrencyFormatter.formatAmountShort(summary.remainingReceivable, isPrivacyMode),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onWorkerDebtsClick() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Ishchilar Qarzi", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = CurrencyFormatter.formatAmountShort(summary.totalWorkerDebt, isPrivacyMode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (summary.totalWorkerDebt > 0) RoseExpense else EmeraldSuccess
                        )
                        Text(
                            text = "To'langan: " + CurrencyFormatter.formatAmountShort(summary.totalPaidToWorkers, isPrivacyMode),
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onCashflowClick() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Kassa Qoldig'i", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = CurrencyFormatter.formatAmountShort(summary.cashBalance, isPrivacyMode),
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
}

@Composable
fun ExpenseDistributionChartCard(
    summary: ObjectFinancialSummary,
    onCategoryClick: (String) -> Unit
) {
    val isPrivacyMode = LocalPrivacyMode.current
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
                    text = "Jami: " + CurrencyFormatter.formatAmountShort(totalExpenseSum, isPrivacyMode),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCategoryClick(item.categoryName) }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
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
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMuted.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
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
                                text = CurrencyFormatter.formatAmount(item.totalAmount, isPrivacyMode),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDrillDownBottomSheet(
    uiState: ReportsUiState,
    onDismiss: () -> Unit
) {
    val drillType = uiState.selectedDrillDownType ?: return
    val categoryName = uiState.selectedCategoryName
    val isPrivacyMode = LocalPrivacyMode.current
    val allObjsMap = remember(uiState.availableObjects) { uiState.availableObjects.associateBy { it.id } }
    val allWorkersMap = remember(uiState.allWorkers) { uiState.allWorkers.associateBy { it.id } }

    val title = when (drillType) {
        DrillDownType.INCOMES -> "Tushumlar va Mijoz To'lovlari"
        DrillDownType.WORKER_PAYMENTS -> "Ishchilarga Berilgan To'lovlar"
        DrillDownType.EXTERNAL_WORKERS_PAID -> "Boshqa Obyekt Ishchilariga To'langan"
        DrillDownType.EXTERNAL_PAID_FOR_THIS_WORKERS -> "Boshqa Obyekt Hisobidan Qoplangan Ish Haqi"
        DrillDownType.WORKER_DEBTS -> "Ishchilarga Qolgan Qarzlar"
        DrillDownType.ALL_EXPENSES -> "Barcha Obyekt Xarajatlari"
        DrillDownType.CATEGORY_EXPENSES -> "$categoryName bo'yicha Xarajatlar"
        DrillDownType.CASH_OUTFLOW -> "Kassadan Chiqqan Barcha Mablag'lar"
        DrillDownType.EXTERNAL_EXPENSES_PAID -> "Boshqa Obyekt Uchun To'lab Berilgan Xarajat"
        DrillDownType.EXTERNAL_EXPENSES_PAID_BY_OTHERS -> "Boshqa Obyekt Hisobidan Qoplangan Xarajat"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Ushbu ko'rsatkichga ta'sir qilgan barcha yozuvlar tafsiloti",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            // RO'YXATLARNI TANLASH VA KO'RSATISH
            when (drillType) {
                DrillDownType.INCOMES -> {
                    val list = uiState.incomes
                    if (list.isEmpty()) {
                        EmptyDrillDownView("Kirim yozuvlari topilmadi")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list, key = { it.id }) { tx ->
                                val isTransfer = tx.description?.startsWith("[") == true && tx.description.contains("kassasidan o'tkazma")
                                DrillDownCard(
                                    date = tx.date,
                                    mainText = tx.description ?: "Mijoz to'lovi",
                                    amount = tx.amount,
                                    amountColor = EmeraldSuccess,
                                    badgeText = if (isTransfer) "Kassalararo o'tkazma" else "Mijoz to'lovi",
                                    badgeColor = if (isTransfer) Color(0xFF8B5CF6) else EmeraldSuccess
                                )
                            }
                        }
                    }
                }

                DrillDownType.WORKER_PAYMENTS -> {
                    val list = uiState.workerPayments
                    if (list.isEmpty()) {
                        EmptyDrillDownView("Ishchilarga to'lovlar yozuvlari topilmadi")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list, key = { it.id }) { wp ->
                                val worker = allWorkersMap[wp.workerId]
                                val workerName = worker?.name ?: "Noma'lum ishchi"
                                val isOwnPocket = wp.payerObjectId == "OWN_POCKET"
                                val isOtherPayer = wp.payerObjectId != null && wp.payerObjectId != wp.objectId && !isOwnPocket
                                val payerObjName = allObjsMap[wp.payerObjectId]?.name ?: "Boshqa obyekt"

                                val dateText = if (!wp.paymentDate.isNullOrBlank() && wp.paymentDate != wp.date) {
                                    "To'langan: ${DateUtil.formatToDisplay(wp.paymentDate)} (${DateUtil.formatToDisplay(wp.date)} kungi ish uchun)"
                                } else {
                                    "${DateUtil.formatToDisplay(wp.date)} kungi to'lov"
                                }

                                DrillDownCard(
                                    date = dateText,
                                    mainText = workerName,
                                    subText = if (!wp.description.isNullOrBlank()) "${wp.type.name}: ${wp.description}" else wp.type.name,
                                    amount = wp.amount,
                                    amountColor = EmeraldSuccess,
                                    badgeText = when {
                                        isOwnPocket -> "👤 O'z hisobidan"
                                        isOtherPayer -> "$payerObjName hisobidan"
                                        else -> null
                                    },
                                    badgeColor = if (isOwnPocket) AmberWarning else DeepBluePrimary
                                )
                            }
                        }
                    }
                }

                DrillDownType.EXTERNAL_WORKERS_PAID -> {
                    val list = uiState.externalWorkerPayments
                    if (list.isEmpty()) {
                        EmptyDrillDownView("Boshqa obyekt ishchilariga to'langan to'lovlar yo'q")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list, key = { it.id }) { wp ->
                                val worker = allWorkersMap[wp.workerId]
                                val workerName = worker?.name ?: "Noma'lum ishchi"
                                val workerObjName = allObjsMap[wp.objectId]?.name ?: "Boshqa obyekt"

                                val dateText = if (!wp.paymentDate.isNullOrBlank() && wp.paymentDate != wp.date) {
                                    "To'langan: ${DateUtil.formatToDisplay(wp.paymentDate)} (${DateUtil.formatToDisplay(wp.date)} uchun)"
                                } else {
                                    "${DateUtil.formatToDisplay(wp.date)} kungi to'lov"
                                }

                                DrillDownCard(
                                    date = dateText,
                                    mainText = workerName,
                                    subText = "$workerObjName ishchisi | ${wp.type.name}: ${wp.description ?: ""}",
                                    amount = wp.amount,
                                    amountColor = AmberWarning,
                                    badgeText = "$workerObjName ishchisi uchun",
                                    badgeColor = AmberWarning
                                )
                            }
                        }
                    }
                }

                DrillDownType.EXTERNAL_PAID_FOR_THIS_WORKERS -> {
                    val list = uiState.externalPaidForThisWorkers
                    if (list.isEmpty()) {
                        EmptyDrillDownView("Boshqa obyekt hisobidan to'langan ish haqlari yo'q")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list, key = { it.id }) { wp ->
                                val worker = allWorkersMap[wp.workerId]
                                val workerName = worker?.name ?: "Noma'lum ishchi"
                                val payerObjName = allObjsMap[wp.payerObjectId]?.name ?: "Boshqa obyekt"

                                val dateText = if (!wp.paymentDate.isNullOrBlank() && wp.paymentDate != wp.date) {
                                    "To'langan: ${DateUtil.formatToDisplay(wp.paymentDate)} (${DateUtil.formatToDisplay(wp.date)} uchun)"
                                } else {
                                    "${DateUtil.formatToDisplay(wp.date)} kungi to'lov"
                                }

                                DrillDownCard(
                                    date = dateText,
                                    mainText = workerName,
                                    subText = "$payerObjName kassasidan to'langan",
                                    amount = wp.amount,
                                    amountColor = DeepBluePrimary,
                                    badgeText = "$payerObjName hisobidan",
                                    badgeColor = DeepBluePrimary
                                )
                            }
                        }
                    }
                }

                DrillDownType.WORKER_DEBTS -> {
                    val list = uiState.workerStatsList.filter { it.remainingDebtToWorker > 0 }
                    if (list.isEmpty()) {
                        EmptyDrillDownView("Ishchilarga hech qanday qarz mavjud emas 🎉")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list, key = { it.workerId }) { ws ->
                                DrillDownCard(
                                    date = "${ws.workedDaysCount} ish kuni",
                                    mainText = ws.workerName + if (!ws.position.isNullOrBlank()) " (${ws.position})" else "",
                                    subText = "Ishladi: ${CurrencyFormatter.formatAmountShort(ws.totalEarned, isPrivacyMode)} | To'landi: ${CurrencyFormatter.formatAmountShort(ws.totalPaid, isPrivacyMode)}",
                                    amount = ws.remainingDebtToWorker,
                                    amountColor = RoseExpense,
                                    badgeText = "Qarz",
                                    badgeColor = RoseExpense
                                )
                            }
                        }
                    }
                }

                DrillDownType.CATEGORY_EXPENSES -> {
                    val list = uiState.expenses.filter { it.category == categoryName }
                    if (list.isEmpty()) {
                        EmptyDrillDownView("Ushbu kategoriyada xarajatlar yo'q")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list, key = { it.id }) { exp ->
                                val isOwnPocket = exp.payerObjectId == "OWN_POCKET"
                                val isOtherPayer = exp.payerObjectId != null && exp.payerObjectId != exp.objectId && !isOwnPocket
                                val payerObjName = allObjsMap[exp.payerObjectId]?.name ?: "Boshqa obyekt"
                                DrillDownCard(
                                    date = exp.date,
                                    mainText = exp.description ?: exp.category,
                                    subText = "Kategoriya: ${exp.category}",
                                    amount = exp.amount,
                                    amountColor = RoseExpense,
                                    badgeText = when {
                                        isOwnPocket -> "👤 O'z hisobidan"
                                        isOtherPayer -> "$payerObjName hisobidan"
                                        else -> null
                                    },
                                    badgeColor = if (isOwnPocket) AmberWarning else DeepBluePrimary
                                )
                            }
                        }
                    }
                }

                DrillDownType.ALL_EXPENSES, DrillDownType.CASH_OUTFLOW -> {
                    val list = uiState.expenses
                    if (list.isEmpty()) {
                        EmptyDrillDownView("Xarajatlar topilmadi")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(list, key = { it.id }) { exp ->
                                val isOwnPocket = exp.payerObjectId == "OWN_POCKET"
                                val isOtherPayer = exp.payerObjectId != null && exp.payerObjectId != exp.objectId && !isOwnPocket
                                val isTransfer = exp.category == "Kassalararo o'tkazma"
                                val payerObjName = allObjsMap[exp.payerObjectId]?.name ?: "Boshqa obyekt"

                                DrillDownCard(
                                    date = exp.date,
                                    mainText = exp.description ?: exp.category,
                                    subText = "Kategoriya: ${exp.category}",
                                    amount = exp.amount,
                                    amountColor = if (isTransfer) Color(0xFF8B5CF6) else RoseExpense,
                                    badgeText = when {
                                        isTransfer -> "Kassalararo o'tkazma"
                                        isOwnPocket -> "👤 O'z hisobidan"
                                        isOtherPayer -> "$payerObjName hisobidan"
                                        else -> null
                                    },
                                    badgeColor = when {
                                        isTransfer -> Color(0xFF8B5CF6)
                                        isOwnPocket -> AmberWarning
                                        else -> DeepBluePrimary
                                    }
                                )
                            }
                        }
                    }
                }

                else -> {
                    EmptyDrillDownView("Tafsilotlar mavjud emas")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DrillDownCard(
    date: String,
    mainText: String,
    subText: String? = null,
    amount: Double,
    amountColor: Color,
    badgeText: String? = null,
    badgeColor: Color = DeepBluePrimary,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight.copy(alpha = 0.5f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = mainText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                if (!subText.isNullOrBlank()) {
                    Text(
                        text = subText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
                Text(
                    text = if (date.contains("-")) DateUtil.formatToFullDisplay(date) else date,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }

            val isPrivacyMode = LocalPrivacyMode.current
            Text(
                text = CurrencyFormatter.formatAmount(amount, isPrivacyMode),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = amountColor
            )
        }
    }
}

@Composable
fun EmptyDrillDownView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
    }
}
