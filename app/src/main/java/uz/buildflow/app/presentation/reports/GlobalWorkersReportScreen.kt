package uz.buildflow.app.presentation.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.PrivacyToggleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalWorkersReportScreen(
    viewModel: GlobalWorkersReportViewModel,
    onNavigateBack: () -> Unit,
    onTogglePrivacy: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isPrivacyMode = LocalPrivacyMode.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ishchilar Umumiy Hisoboti",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = DeepBluePrimary
                            )
                        )
                        Text(
                            text = "Barcha obyektlar va to'lovlar tahlili",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Orqaga",
                            tint = DeepBluePrimary
                        )
                    }
                },
                actions = {
                    PrivacyToggleButton(onToggle = onTogglePrivacy)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = DeepBluePrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Yig'ma Banner
                item {
                    val summary = uiState.summary
                    if (summary != null) {
                        GlobalWorkersExecutiveBanner(summary = summary, isPrivacyMode = isPrivacyMode)
                    }
                }

                // 2. Qidiruv va Filtrlar
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Ishchi ismi, kasbi, telefoni yoki obyekti...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = DeepBluePrimary)
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Tozalash")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DeepBluePrimary,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = SurfaceLight,
                                unfocusedContainerColor = SurfaceLight
                            )
                        )

                        // Filtr Chiplari
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = uiState.activeFilter == WorkerReportFilter.ALL,
                                onClick = { viewModel.onFilterChanged(WorkerReportFilter.ALL) },
                                label = { Text("Barchasi (${uiState.summary?.workers?.size ?: 0})") }
                            )
                            FilterChip(
                                selected = uiState.activeFilter == WorkerReportFilter.HAS_DEBT,
                                onClick = { viewModel.onFilterChanged(WorkerReportFilter.HAS_DEBT) },
                                label = { Text("Qarzdorlik bor (${uiState.summary?.workers?.count { it.balance > 0 } ?: 0})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AmberWarning.copy(alpha = 0.15f),
                                    selectedLabelColor = AmberWarning
                                )
                            )
                            FilterChip(
                                selected = uiState.activeFilter == WorkerReportFilter.FULLY_PAID,
                                onClick = { viewModel.onFilterChanged(WorkerReportFilter.FULLY_PAID) },
                                label = { Text("To'liq to'langan (${uiState.summary?.workers?.count { it.balance == 0.0 && it.totalEarned > 0 } ?: 0})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSuccess.copy(alpha = 0.15f),
                                    selectedLabelColor = EmeraldSuccess
                                )
                            )
                            FilterChip(
                                selected = uiState.activeFilter == WorkerReportFilter.HAS_ADVANCE,
                                onClick = { viewModel.onFilterChanged(WorkerReportFilter.HAS_ADVANCE) },
                                label = { Text("Avans (${uiState.summary?.workers?.count { it.balance < 0 } ?: 0})") }
                            )
                        }
                    }
                }

                // 3. Ishchilar Ro'yxati
                if (uiState.filteredWorkers.isEmpty()) {
                    item {
                        EmptyStateView(
                            title = "Ishchilar topilmadi",
                            description = "Qidiruv yoki filtr parametrlarini o'zgartirib ko'ring.",
                            icon = Icons.Default.SearchOff
                        )
                    }
                } else {
                    items(uiState.filteredWorkers, key = { it.workerId }) { workerItem ->
                        WorkerGlobalCard(
                            item = workerItem,
                            isPrivacyMode = isPrivacyMode,
                            onClick = { viewModel.onSelectWorker(workerItem) }
                        )
                    }
                }
            }
        }
    }

    // Ishchining To'liq Dosye & Statement Modali
    uiState.selectedWorkerForDetails?.let { workerDetail ->
        WorkerFullStatementSheet(
            item = workerDetail,
            isPrivacyMode = isPrivacyMode,
            onDismiss = { viewModel.onSelectWorker(null) }
        )
    }
}

@Composable
fun GlobalWorkersExecutiveBanner(
    summary: GlobalWorkersReportSummary,
    isPrivacyMode: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBluePrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "UMUMIY ISHCHI KONTINGENTI",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = DeepBlueLight
                    )
                    Text(
                        text = "${summary.totalWorkersCount} nafar faol ishchi",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jami Mehnat Haqi", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = CurrencyFormatter.formatAmount(summary.totalEarnedAll, isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jami To'langan", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = CurrencyFormatter.formatAmount(summary.totalPaidAll, isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jami Qoldiq Qarz", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    Text(
                        text = CurrencyFormatter.formatAmount(summary.totalDebtAll, isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = AmberWarning
                    )
                }
                if (summary.totalAdvanceAll > 0) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ortiqcha Avans", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text(
                            text = CurrencyFormatter.formatAmount(summary.totalAdvanceAll, isPrivacyMode),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF64B5F6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerGlobalCard(
    item: WorkerGlobalReportItem,
    isPrivacyMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(DeepBlueLight.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.workerName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepBluePrimary
                        )
                    }
                    Column {
                        Text(
                            text = item.workerName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = item.position.ifBlank { "Kasbi belgilanmagan" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Balance Status Badge
                Surface(
                    color = when {
                        item.balance > 0 -> AmberWarning.copy(alpha = 0.15f)
                        item.balance < 0 -> Color(0xFFE3F2FD)
                        else -> EmeraldSuccess.copy(alpha = 0.15f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when {
                            item.balance > 0 -> "Qarz: ${CurrencyFormatter.formatAmount(item.balance, isPrivacyMode)}"
                            item.balance < 0 -> "Avans: ${CurrencyFormatter.formatAmount(-item.balance, isPrivacyMode)}"
                            else -> "To'liq to'langan"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            item.balance > 0 -> AmberWarning
                            item.balance < 0 -> Color(0xFF1976D2)
                            else -> EmeraldSuccess
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Ishlagan Obyektlari
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item.workedObjects.forEach { objName ->
                    Surface(
                        color = DeepBlueLight.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Apartment, contentDescription = null, tint = DeepBluePrimary, modifier = Modifier.size(12.dp))
                            Text(text = objName, style = MaterialTheme.typography.labelSmall, color = DeepBluePrimary)
                        }
                    }
                }
            }

            HorizontalDivider(color = BorderColor)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📅 ${item.workedDaysCount} kun ishlagan",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = TextSecondary
                )

                Text(
                    text = "Hisob: ${CurrencyFormatter.formatAmountShort(item.totalEarned, isPrivacyMode)} | To'landi: ${CurrencyFormatter.formatAmountShort(item.totalPaid, isPrivacyMode)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerFullStatementSheet(
    item: WorkerGlobalReportItem,
    isPrivacyMode: Boolean,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("📅 Davomat (${item.daysHistory.size})", "💸 To'lovlar (${item.paymentsHistory.size})", "🏢 Obyektlar (${item.objectsBreakdown.size})")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(bottom = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.workerName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    Text(
                        text = "${item.position.ifBlank { "Ishchi" }} • ${item.phone ?: "Telefon yo'q"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            // Summary row in modal
            Surface(
                color = DeepBlueLight.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Hisoblangan", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            CurrencyFormatter.formatAmount(item.totalEarned, isPrivacyMode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    Column {
                        Text("To'langan", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            CurrencyFormatter.formatAmount(item.totalPaid, isPrivacyMode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldSuccess
                        )
                    }
                    Column {
                        Text("Qoldiq Qarz", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            CurrencyFormatter.formatAmount(item.balance, isPrivacyMode),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (item.balance > 0) AmberWarning else EmeraldSuccess
                        )
                    }
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceLight,
                contentColor = DeepBluePrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Davomat Tarixi
                    if (item.daysHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Ishlagan kunlar tarixi mavjud emas", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(item.daysHistory) { day ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = BackgroundLight)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = day.date,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary
                                            )
                                            Surface(
                                                color = if (day.isFullyCovered) EmeraldSuccess.copy(alpha = 0.15f) else AmberWarning.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (day.isFullyCovered) "Yopilgan" else "Qarz: ${CurrencyFormatter.formatAmount(day.remainingDebt, isPrivacyMode)}",
                                                    color = if (day.isFullyCovered) EmeraldSuccess else AmberWarning,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "🏢 ${day.objectName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = DeepBluePrimary
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Stavka: ${CurrencyFormatter.formatAmount(day.dailyRate, isPrivacyMode)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary
                                            )
                                            if (day.bonusAmount > 0) {
                                                Text(
                                                    text = "+Bonus: ${CurrencyFormatter.formatAmount(day.bonusAmount, isPrivacyMode)}",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = EmeraldSuccess
                                                )
                                            }
                                        }

                                        if (day.bonusReason != null) {
                                            Text(
                                                text = "💡 Izoh: ${day.bonusReason}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // To'lovlar Tarixi
                    if (item.paymentsHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("To'lovlar tarixi mavjud emas", color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(item.paymentsHistory) { payment ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = BackgroundLight)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = payment.date,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = CurrencyFormatter.formatAmount(payment.amount, isPrivacyMode),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldSuccess
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("🏢 ${payment.objectName}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            Text(payment.paymentSource, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = DeepBluePrimary)
                                        }

                                        if (!payment.note.isNullOrBlank()) {
                                            Text("💡 ${payment.note}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Obyektlar Kesimi
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(item.objectsBreakdown) { obj ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = BackgroundLight)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "🏢 ${obj.objectName}",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DeepBluePrimary
                                    )
                                    Text(
                                        text = "Ishlagan kunlar: ${obj.daysCount} kun",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                    HorizontalDivider(color = BorderColor)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Hisoblangan: ${CurrencyFormatter.formatAmount(obj.earnedAmount, isPrivacyMode)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "To'langan: ${CurrencyFormatter.formatAmount(obj.paidAmount, isPrivacyMode)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
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
    }
}
