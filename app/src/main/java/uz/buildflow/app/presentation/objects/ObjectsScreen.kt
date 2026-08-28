package uz.buildflow.app.presentation.objects

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DatabaseBackupHelper
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.ObjectStatus
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.PrivacyToggleButton
import uz.buildflow.app.presentation.common.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectsScreen(
    viewModel: ObjectsViewModel,
    onObjectClick: (String) -> Unit,
    onNavigateToGlobalReports: () -> Unit,
    onExportDatabase: () -> Unit,
    onTogglePrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isPrivacyMode = LocalPrivacyMode.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            DatabaseBackupHelper.importDatabase(context, uri) {
                viewModel.refresh()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "BuildFlow",
                            style = MaterialTheme.typography.headlineMedium.copy(color = DeepBluePrimary)
                        )
                        Text(
                            text = "Barcha faol obyektlar",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToGlobalReports) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = "Kompaniya Umumiy Hisoboti",
                            tint = DeepBluePrimary
                        )
                    }
                    PrivacyToggleButton(onToggle = onTogglePrivacy)
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Zaxira Nusxadan Tiklash (Import DB)",
                            tint = DeepBluePrimary
                        )
                    }
                    IconButton(onClick = onExportDatabase) {
                        Icon(
                            imageVector = Icons.Default.SaveAlt,
                            contentDescription = "Baza Nusxasini Yuklab Olish (Backup / Eksport)",
                            tint = DeepBluePrimary
                        )
                    }
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
                onClick = { viewModel.openAddObject() },
                containerColor = DeepBluePrimary,
                contentColor = SurfaceLight,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Obyekt qo'shish")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            if (uiState.objects.isEmpty() && !uiState.isLoading) {
                EmptyStateView(
                    title = "Obyektlar mavjud emas",
                    description = "Qurilish yoki ta'mirlash obyektini qo'shish uchun pastdagi '+' tugmasini bosing.",
                    icon = Icons.Default.Apartment,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (uiState.objects.isNotEmpty()) {
                        item {
                            GlobalSummaryBanner(
                                objects = uiState.objects,
                                onOpenGlobalReports = onNavigateToGlobalReports
                            )
                        }
                    }

                    items(uiState.objects, key = { it.obj.id }) { item ->
                        ObjectCard(
                            obj = item.obj,
                            summary = item.summary,
                            onClick = { onObjectClick(item.obj.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.isAddEditSheetOpen) {
        AddEditObjectSheet(
            existingObject = uiState.selectedObject,
            onDismiss = { viewModel.closeAddEditSheet() },
            onSave = { name, desc, price, date, status ->
                viewModel.saveObject(name, desc, price, date, status)
            }
        )
    }
}

@Composable
fun ObjectCard(
    obj: BuildObject,
    summary: uz.buildflow.app.domain.model.ObjectFinancialSummary?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = obj.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Boshlangan: ${DateUtil.formatToDisplay(obj.startDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                StatusBadge(
                    statusText = when (obj.status) {
                        ObjectStatus.ACTIVE -> "Faol"
                        ObjectStatus.PLANNED -> "Rejada"
                        ObjectStatus.COMPLETED -> "Tugagan"
                        ObjectStatus.CANCELLED -> "Bekor"
                    },
                    color = when (obj.status) {
                        ObjectStatus.ACTIVE -> EmeraldSuccess
                        ObjectStatus.COMPLETED -> DeepBluePrimary
                        else -> TextSecondary
                    },
                    backgroundColor = when (obj.status) {
                        ObjectStatus.ACTIVE -> EmeraldLight
                        ObjectStatus.COMPLETED -> SurfaceVariantLight
                        else -> BorderColor
                    }
                )
            }

            HorizontalDivider(color = BorderColor)

            val isPrivacyMode = LocalPrivacyMode.current

            if (obj.totalPrice > 0) {
                Text(
                    text = CurrencyFormatter.formatAmount(obj.totalPrice, isPrivacyMode),
                    style = MaterialTheme.typography.headlineMedium.copy(color = DeepBluePrimary),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "Summa kiritilmagan",
                    style = MaterialTheme.typography.titleMedium.copy(color = TextMuted),
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Tushgan pul", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = if (isPrivacyMode) CurrencyFormatter.MASKED_AMOUNT_SHORT else CurrencyFormatter.formatAmountShort(summary?.totalReceivedIncome ?: 0.0) + " so'm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldSuccess
                    )
                }
                Column {
                    Text(text = "Jami Xarajat", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = if (isPrivacyMode) CurrencyFormatter.MASKED_AMOUNT_SHORT else CurrencyFormatter.formatAmountShort(summary?.totalExpenses ?: 0.0) + " so'm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = RoseExpense
                    )
                }
                Column {
                    Text(text = "Qo'ldagi pul", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = if (isPrivacyMode) CurrencyFormatter.MASKED_AMOUNT_SHORT else CurrencyFormatter.formatAmountShort(summary?.cashBalance ?: 0.0) + " so'm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if ((summary?.cashBalance ?: 0.0) >= 0) DeepBlueLight else RoseExpense
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalSummaryBanner(
    objects: List<ObjectWithSummary>,
    onOpenGlobalReports: () -> Unit
) {
    val isPrivacyMode = LocalPrivacyMode.current
    val totalIncome = objects.sumOf { it.summary?.totalClientIncome ?: 0.0 }
    val totalCashBalance = objects.sumOf { it.summary?.cashBalance ?: 0.0 }
    val totalWorkerDebt = objects.sumOf { it.summary?.totalWorkerDebt ?: 0.0 }
    val totalProfit = objects.sumOf { it.summary?.estimatedProfit ?: 0.0 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenGlobalReports() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .size(34.dp)
                            .background(DeepBlueLight.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = DeepBluePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Kompaniya Umumiy Balansi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${objects.size} ta obyekt bo'yicha yig'ma",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Hisobot",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = DeepBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Erkin Kassa:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(totalCashBalance, isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                }
                Column {
                    Text(text = "Tushum:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(totalIncome, isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldSuccess
                    )
                }
                Column {
                    Text(text = "Ishchi qarzi:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(totalWorkerDebt, isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (totalWorkerDebt > 0) RoseExpense else EmeraldSuccess
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Sof Foyda:", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(totalProfit, isPrivacyMode),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (totalProfit >= 0) EmeraldSuccess else RoseExpense
                    )
                }
            }
        }
    }
}
