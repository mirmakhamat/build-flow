package uz.buildflow.app.presentation.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.PrivacyToggleButton
import uz.buildflow.app.presentation.common.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalReportsScreen(
    viewModel: GlobalReportsViewModel,
    onBack: () -> Unit,
    onNavigateToObject: (String) -> Unit,
    onNavigateToWorkersReport: () -> Unit = {},
    onTogglePrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.summary
    val isPrivacyMode = LocalPrivacyMode.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Kompaniya Umumiy Hisoboti",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Barcha obyektlar konsolidatsiyasi",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
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
            } else if (summary == null || summary.totalObjectsCount == 0) {
                EmptyStateView(
                    title = "Ma'lumotlar mavjud emas",
                    description = "Umumiy hisobotni ko'rish uchun avval kamida bitta obyekt qo'shing.",
                    icon = Icons.Default.Assessment,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = uiState.selectedTab.ordinal,
                        containerColor = SurfaceLight,
                        contentColor = DeepBluePrimary
                    ) {
                        GlobalReportTab.values().forEach { tab ->
                            Tab(
                                selected = uiState.selectedTab == tab,
                                onClick = { viewModel.selectTab(tab) },
                                text = {
                                    Text(
                                        text = tab.title,
                                        fontWeight = if (uiState.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                        }
                    }

                    // Tab Kontenti
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (uiState.selectedTab) {
                            GlobalReportTab.OVERVIEW -> {
                                GlobalOverviewTab(
                                    summary = summary,
                                    onIncomesClick = { viewModel.openDrillDown(GlobalDrillDownType.GLOBAL_INCOMES) },
                                    onExpensesClick = { viewModel.openDrillDown(GlobalDrillDownType.GLOBAL_EXPENSES) },
                                    onWorkerDebtsClick = { viewModel.openDrillDown(GlobalDrillDownType.GLOBAL_WORKER_DEBTS) },
                                    onCashBalancesClick = { viewModel.openDrillDown(GlobalDrillDownType.GLOBAL_CASH_BALANCES) },
                                    onOwnPocketClick = { viewModel.openDrillDown(GlobalDrillDownType.GLOBAL_OWN_POCKET) }
                                )
                            }
                            GlobalReportTab.OBJECTS_RANKING -> {
                                GlobalObjectsRankingTab(
                                    profitabilities = summary.objectProfitabilities,
                                    onNavigateToObject = onNavigateToObject
                                )
                            }
                            GlobalReportTab.EXPENSES_CATEGORIES -> {
                                GlobalExpenseCategoriesTab(
                                    categories = summary.globalCategoryBreakdowns,
                                    totalExpenses = summary.totalExpenses
                                )
                            }
                            GlobalReportTab.INTER_OBJECT_BALANCES -> {
                                GlobalInterObjectBalancesTab(
                                    summary = summary,
                                    objects = uiState.availableObjects
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.selectedDrillDownType != null && summary != null) {
        GlobalDrillDownBottomSheet(
            drillType = uiState.selectedDrillDownType!!,
            summary = summary,
            allWorkersStats = uiState.allWorkersStats,
            onDismiss = { viewModel.closeDrillDown() },
            onNavigateToObject = { objId ->
                viewModel.closeDrillDown()
                onNavigateToObject(objId)
            },
            onNavigateToWorkersReport = {
                viewModel.closeDrillDown()
                onNavigateToWorkersReport()
            }
        )
    }
}

@Composable
private fun GlobalOverviewTab(
    summary: GlobalFinancialSummary,
    onIncomesClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onWorkerDebtsClick: () -> Unit,
    onCashBalancesClick: () -> Unit,
    onOwnPocketClick: () -> Unit
) {
    val isPrivacyMode = LocalPrivacyMode.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Asosiy Moliyaviy Salomatlik Banneri
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceLight),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                .size(36.dp)
                                .background(DeepBlueLight.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = DeepBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Kompaniya Balansi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${summary.totalObjectsCount} ta obyekt (${summary.activeObjectsCount} faol)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Rentabellik nishoni
                    val margin = summary.overallProfitMargin
                    Surface(
                        color = if (margin >= 20) EmeraldSuccess.copy(alpha = 0.12f) else AmberWarning.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isPrivacyMode) "••••••" else "${String.format(java.util.Locale.US, "%.1f", margin)}% foyda",
                            color = if (margin >= 20) EmeraldSuccess else AmberWarning,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                // Kutilayotgan Sof Foyda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Kutilayotgan Sof Foyda:", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text(
                            text = CurrencyFormatter.formatAmount(summary.estimatedProfit, isPrivacyMode),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (summary.estimatedProfit >= 0) EmeraldSuccess else RoseExpense
                        )
                    }
                }

                // Tushum / Shartnoma Progress indikatori
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tushgan pul ulushi",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = if (isPrivacyMode) "••••••" else "${String.format(java.util.Locale.US, "%.1f", summary.incomeCollectionRate)}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = DeepBluePrimary
                        )
                    }
                    val progress = if (summary.totalAgreedPrice > 0) (summary.totalClientIncome / summary.totalAgreedPrice).toFloat().coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldSuccess,
                        trackColor = BorderColor.copy(alpha = 0.5f)
                    )
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                // Asosiy 3 ta tezkor quti (Kassa, Qarz, O'z hisobidan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Erkin Kassa
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCashBalancesClick() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepBlueLight.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Kassalarda", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = CurrencyFormatter.formatAmountShort(summary.totalCashBalance, isPrivacyMode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepBluePrimary
                            )
                        }
                    }

                    // Ishchilarga qarz
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onWorkerDebtsClick() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (summary.totalWorkerDebt > 0) RoseExpense.copy(alpha = 0.08f) else EmeraldSuccess.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Ishchi qarzlari", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(
                                text = CurrencyFormatter.formatAmountShort(summary.totalWorkerDebt, isPrivacyMode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (summary.totalWorkerDebt > 0) RoseExpense else EmeraldSuccess
                            )
                        }
                    }

                    // O'z hisobidan
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOwnPocketClick() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "O'z hisobidan", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309))
                            Text(
                                text = CurrencyFormatter.formatAmountShort(summary.totalPaidFromOwnPocket, isPrivacyMode),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }
        }

        // 2. Batafsil Yig'ma Ko'rsatkichlar (Bosiladigan qatorlar)
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
                    text = "Batafsil Kompaniya Ko'rsatkichlari",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "💡 Har bir qator ustiga bosib, tegishli barcha obyektlar kesimidagi ro'yxatni ko'rishingiz mumkin.",
                    style = MaterialTheme.typography.labelSmall,
                    color = DeepBluePrimary
                )

                HorizontalDivider(color = BorderColor)

                // Daromad va Shartnomalar
                Text(
                    text = "Shartnomalar & Tushumlar",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = DeepBluePrimary
                )
                InteractiveBreakdownRow(
                    title = "Barcha obyektlar umumiy narxi",
                    amount = summary.totalAgreedPrice,
                    isClickable = false
                )
                InteractiveBreakdownRow(
                    title = "Jami tushgan pul (Mijoz to'lovlari)",
                    amount = summary.totalClientIncome,
                    customColor = EmeraldSuccess,
                    onClick = onIncomesClick
                )
                InteractiveBreakdownRow(
                    title = "Mijozlardan olinishi kutilayotgan (Qoldiq)",
                    amount = summary.remainingReceivable,
                    customColor = if (summary.remainingReceivable > 0) AmberWarning else EmeraldSuccess,
                    onClick = onIncomesClick
                )

                HorizontalDivider(color = BorderColor)

                // Xarajatlar & Ish haqi
                Text(
                    text = "Xarajatlar & Ishchilarga To'lovlar",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = DeepBluePrimary
                )
                InteractiveBreakdownRow(
                    title = "Hisoblangan jami ish haqi",
                    amount = summary.totalWorkerSalaryEarned,
                    onClick = onWorkerDebtsClick
                )
                InteractiveBreakdownRow(
                    title = "Hisoblangan jami bonuslar",
                    amount = summary.totalBonusesEarned,
                    onClick = onWorkerDebtsClick
                )
                InteractiveBreakdownRow(
                    title = "Material va boshqa xarajatlar",
                    amount = summary.totalOtherExpenses,
                    onClick = onExpensesClick
                )
                InteractiveBreakdownRow(
                    title = "Jami Hisoblangan Xarajatlar",
                    amount = summary.totalExpenses,
                    customColor = RoseExpense,
                    onClick = onExpensesClick
                )
                InteractiveBreakdownRow(
                    title = "Ishchilarga haqiqatda to'langan pul",
                    amount = summary.totalPaidToWorkers,
                    customColor = EmeraldSuccess,
                    onClick = onWorkerDebtsClick
                )
                InteractiveBreakdownRow(
                    title = "Ishchilarga qolgan sof qarz",
                    amount = summary.totalWorkerDebt,
                    customColor = if (summary.totalWorkerDebt > 0) RoseExpense else EmeraldSuccess,
                    onClick = onWorkerDebtsClick
                )

                HorizontalDivider(color = BorderColor)

                // Kassa va Cho'ntak
                Text(
                    text = "Kassalar & Shaxsiy Mablag'lar",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = DeepBluePrimary
                )
                InteractiveBreakdownRow(
                    title = "Kassalardan chiqqan jami mablag'",
                    amount = summary.totalCashOutflow,
                    onClick = onCashBalancesClick
                )
                InteractiveBreakdownRow(
                    title = "Kompaniya bo'yicha erkin kassa qoldig'i",
                    amount = summary.totalCashBalance,
                    customColor = DeepBluePrimary,
                    onClick = onCashBalancesClick
                )
                InteractiveBreakdownRow(
                    title = "Shaxsiy cho'ntakdan qoplangan summa",
                    amount = summary.totalPaidFromOwnPocket,
                    customColor = Color(0xFFD97706),
                    onClick = onOwnPocketClick
                )
            }
        }
    }
}

@Composable
private fun GlobalObjectsRankingTab(
    profitabilities: List<ObjectProfitabilityItem>,
    onNavigateToObject: (String) -> Unit
) {
    val isPrivacyMode = LocalPrivacyMode.current

    if (profitabilities.isEmpty()) {
        EmptyStateView(
            title = "Obyektlar mavjud emas",
            description = "Obyektlar qo'shilgandan so'ng bu yerda ularning rentabellik reytingi chiqadi.",
            icon = Icons.Default.Apartment
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(profitabilities, key = { it.objectId }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToObject(item.objectId) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.objectName,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Shartnoma: ${CurrencyFormatter.formatAmountShort(item.totalPrice, isPrivacyMode)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            StatusBadge(
                                statusText = when (item.status) {
                                    ObjectStatus.ACTIVE -> "Faol"
                                    ObjectStatus.PLANNED -> "Rejada"
                                    ObjectStatus.COMPLETED -> "Tugagan"
                                    ObjectStatus.CANCELLED -> "Bekor"
                                },
                                color = when (item.status) {
                                    ObjectStatus.ACTIVE -> DeepBluePrimary
                                    ObjectStatus.PLANNED -> AmberWarning
                                    ObjectStatus.COMPLETED -> EmeraldSuccess
                                    ObjectStatus.CANCELLED -> RoseExpense
                                },
                                backgroundColor = when (item.status) {
                                    ObjectStatus.ACTIVE -> DeepBlueLight.copy(alpha = 0.15f)
                                    ObjectStatus.PLANNED -> AmberWarning.copy(alpha = 0.15f)
                                    ObjectStatus.COMPLETED -> EmeraldSuccess.copy(alpha = 0.15f)
                                    ObjectStatus.CANCELLED -> RoseExpense.copy(alpha = 0.15f)
                                }
                            )
                        }

                        HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                        // Tushum, Xarajat, Kassa
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Tushum:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatAmountShort(item.totalIncome, isPrivacyMode),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess
                                )
                            }
                            Column {
                                Text(text = "Xarajat:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatAmountShort(item.totalExpenses, isPrivacyMode),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = RoseExpense
                                )
                            }
                            Column {
                                Text(text = "Kassa:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatAmountShort(item.cashBalance, isPrivacyMode),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DeepBluePrimary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Sof Foyda:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatAmountShort(item.estimatedProfit, isPrivacyMode),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (item.estimatedProfit >= 0) EmeraldSuccess else RoseExpense
                                )
                            }
                        }

                        // Foyda Marjasi va Progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val progress = (item.completionPercentage / 100.0).toFloat().coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = EmeraldSuccess,
                                trackColor = BorderColor.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isPrivacyMode) "••••••" else "${String.format(java.util.Locale.US, "%.1f", item.profitMargin)}% rentabellik",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (item.profitMargin >= 20) EmeraldSuccess else AmberWarning
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalExpenseCategoriesTab(
    categories: List<CategoryExpenseBreakdown>,
    totalExpenses: Double
) {
    val isPrivacyMode = LocalPrivacyMode.current

    if (categories.isEmpty()) {
        EmptyStateView(
            title = "Xarajatlar mavjud emas",
            description = "Xarajatlar kiritilgach bu yerda barcha obyektlar bo'yicha yig'ma taqsimot ko'rinadi.",
            icon = Icons.Default.PieChart
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Yig'ma Donut Chart kartochkasi
            val colors = remember {
                listOf(
                    Color(0xFF3B82F6),
                    Color(0xFF10B981),
                    Color(0xFFF59E0B),
                    Color(0xFFEF4444),
                    Color(0xFF8B5CF6),
                    Color(0xFFEC4899),
                    Color(0xFF14B8A6),
                    Color(0xFF6366F1),
                    Color(0xFF84CC16),
                    Color(0xFFF97316)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Umumiy Xarajatlar Taqsimoti",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    val totalSum = remember(categories) { categories.sumOf { it.totalAmount } }

                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            if (totalSum > 0) {
                                var startAngle = -90f
                                categories.forEachIndexed { index, item ->
                                    val sweep = (item.totalAmount / totalSum * 360f).toFloat()
                                    val color = colors[index % colors.size]
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Jami",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatAmountShort(totalSum, isPrivacyMode),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                    // Kategoriyalar ro'yxati
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        categories.forEachIndexed { index, item ->
                            val color = colors[index % colors.size]
                            val percentage = if (totalSum > 0) String.format(java.util.Locale.US, "%.1f", (item.totalAmount / totalSum) * 100.0) else "0.0"

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
                                            .size(12.dp)
                                            .background(color, CircleShape)
                                    )
                                    Text(
                                        text = item.categoryName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
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
                                        text = CurrencyFormatter.formatAmount(item.totalAmount, isPrivacyMode),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalInterObjectBalancesTab(
    summary: GlobalFinancialSummary,
    objects: List<BuildObject>
) {
    val isPrivacyMode = LocalPrivacyMode.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Har bir obyekt kassa qoldiqlari
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Obyektlar Kassalari",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Jami: ${CurrencyFormatter.formatAmountShort(summary.totalCashBalance, isPrivacyMode)}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                }

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                summary.objectSummaries.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.objectName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = CurrencyFormatter.formatAmount(s.cashBalance, isPrivacyMode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (s.cashBalance >= 0) DeepBluePrimary else RoseExpense
                        )
                    }
                }
            }
        }

        // Obyektlararo O'zaro Qarzdorliklar
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
                    text = "Kassalararo O'zaro Yordam & Qarzlar",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Boshqa obyekt hisobidan to'lab berilgan ish haqi va material xarajatlari.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                if (summary.interObjectDebts.isEmpty()) {
                    Text(
                        text = "Obyektlararo o'zaro qarzdorliklar mavjud emas 🎉",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    summary.interObjectDebts.forEach { debt ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundLight)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${debt.sourceObjectName} kassasidan",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = debt.reason,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.formatAmount(debt.amount, isPrivacyMode),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF8B5CF6)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlobalDrillDownBottomSheet(
    drillType: GlobalDrillDownType,
    summary: GlobalFinancialSummary,
    allWorkersStats: List<WorkerStats>,
    onDismiss: () -> Unit,
    onNavigateToObject: (String) -> Unit,
    onNavigateToWorkersReport: () -> Unit = {}
) {
    val isPrivacyMode = LocalPrivacyMode.current

    val title = when (drillType) {
        GlobalDrillDownType.GLOBAL_INCOMES -> "Barcha Obyektlar Tushumlari"
        GlobalDrillDownType.GLOBAL_EXPENSES -> "Barcha Obyektlar Xarajatlari"
        GlobalDrillDownType.GLOBAL_WORKER_DEBTS -> "Barcha Qarzdor Ishchilar Ro'yxati"
        GlobalDrillDownType.GLOBAL_CASH_BALANCES -> "Barcha Obyektlar Kassalari"
        GlobalDrillDownType.GLOBAL_OWN_POCKET -> "Shaxsiy Cho'ntakdan Qoplangan Mablag'lar"
        GlobalDrillDownType.INTER_OBJECT_DEBTS -> "Obyektlararo O'zaro Qarzlar"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            HorizontalDivider(color = BorderColor)

            when (drillType) {
                GlobalDrillDownType.GLOBAL_INCOMES -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(summary.objectSummaries, key = { it.objectId }) { s ->
                            DrillDownCard(
                                date = "Shartnoma: ${CurrencyFormatter.formatAmountShort(s.totalPrice, isPrivacyMode)}",
                                mainText = s.objectName,
                                subText = "Qoldiq tushum: ${CurrencyFormatter.formatAmountShort(s.remainingReceivable, isPrivacyMode)}",
                                amount = s.totalReceivedIncome,
                                amountColor = EmeraldSuccess,
                                badgeText = "Tushum",
                                badgeColor = EmeraldSuccess,
                                onClick = { onNavigateToObject(s.objectId) }
                            )
                        }
                    }
                }

                GlobalDrillDownType.GLOBAL_EXPENSES -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(summary.objectSummaries, key = { it.objectId }) { s ->
                            DrillDownCard(
                                date = "Kassa chiqimi: ${CurrencyFormatter.formatAmountShort(s.totalCashOutflow, isPrivacyMode)}",
                                mainText = s.objectName,
                                subText = "To'g'ridan-to'g'ri xarajatlar",
                                amount = s.totalExpenses,
                                amountColor = RoseExpense,
                                badgeText = "Xarajat",
                                badgeColor = RoseExpense,
                                onClick = { onNavigateToObject(s.objectId) }
                            )
                        }
                    }
                }

                GlobalDrillDownType.GLOBAL_WORKER_DEBTS -> {
                    val debtWorkers = allWorkersStats.filter { it.remainingDebtToWorker > 0 }.sortedByDescending { it.remainingDebtToWorker }
                    
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onNavigateToWorkersReport()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Batafsil Ishchilar Hisobotiga o'tish", fontWeight = FontWeight.SemiBold)
                    }

                    if (debtWorkers.isEmpty()) {
                        Text(
                            text = "Barcha ishchilarga to'lovlar to'liq amalga oshirilgan 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(debtWorkers, key = { it.workerId }) { ws ->
                                DrillDownCard(
                                    date = "${ws.workedDaysCount} kun ishlagan",
                                    mainText = ws.workerName + if (!ws.position.isNullOrBlank()) " (${ws.position})" else "",
                                    subText = "Ishladi: ${CurrencyFormatter.formatAmountShort(ws.totalEarned, isPrivacyMode)} | To'landi: ${CurrencyFormatter.formatAmountShort(ws.totalPaid, isPrivacyMode)}",
                                    amount = ws.remainingDebtToWorker,
                                    amountColor = RoseExpense,
                                    badgeText = "Qarz",
                                    badgeColor = RoseExpense,
                                    onClick = {
                                        onDismiss()
                                        onNavigateToWorkersReport()
                                    }
                                )
                            }
                        }
                    }
                }

                GlobalDrillDownType.GLOBAL_CASH_BALANCES -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(summary.objectSummaries, key = { it.objectId }) { s ->
                            DrillDownCard(
                                date = "Tushgan: ${CurrencyFormatter.formatAmountShort(s.totalReceivedIncome, isPrivacyMode)}",
                                mainText = s.objectName,
                                subText = "Chiqim: ${CurrencyFormatter.formatAmountShort(s.totalCashOutflow, isPrivacyMode)}",
                                amount = s.cashBalance,
                                amountColor = if (s.cashBalance >= 0) DeepBluePrimary else RoseExpense,
                                badgeText = "Kassa",
                                badgeColor = DeepBluePrimary,
                                onClick = { onNavigateToObject(s.objectId) }
                            )
                        }
                    }
                }

                GlobalDrillDownType.GLOBAL_OWN_POCKET -> {
                    val ownPocketList = summary.objectSummaries.filter { it.totalPaidFromOwnPocket > 0 }
                    if (ownPocketList.isEmpty()) {
                        Text(
                            text = "Shaxsiy cho'ntakdan qoplangan xarajatlar yo'q.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ownPocketList, key = { it.objectId }) { s ->
                                DrillDownCard(
                                    date = "O'z hisobidan",
                                    mainText = s.objectName,
                                    subText = "Ish haqi: ${CurrencyFormatter.formatAmountShort(s.totalPaidFromOwnPocketForThisWorkers, isPrivacyMode)} | Xarajat: ${CurrencyFormatter.formatAmountShort(s.totalExpensesPaidFromOwnPocket, isPrivacyMode)}",
                                    amount = s.totalPaidFromOwnPocket,
                                    amountColor = Color(0xFFD97706),
                                    badgeText = "Cho'ntak",
                                    badgeColor = Color(0xFFD97706),
                                    onClick = { onNavigateToObject(s.objectId) }
                                )
                            }
                        }
                    }
                }

                GlobalDrillDownType.INTER_OBJECT_DEBTS -> {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(summary.interObjectDebts) { debt ->
                            DrillDownCard(
                                date = debt.sourceObjectName,
                                mainText = debt.reason,
                                subText = "Kassadan to'langan",
                                amount = debt.amount,
                                amountColor = Color(0xFF8B5CF6),
                                badgeText = "O'tkazma",
                                badgeColor = Color(0xFF8B5CF6)
                            )
                        }
                    }
                }
            }
        }
    }
}
