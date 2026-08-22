package uz.buildflow.app.presentation.objects

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DatabaseBackupHelper
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.ObjectStatus
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.StatusBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectsScreen(
    viewModel: ObjectsViewModel,
    onObjectClick: (String) -> Unit,
    onExportDatabase: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

            Text(
                text = CurrencyFormatter.formatAmount(obj.totalPrice),
                style = MaterialTheme.typography.headlineMedium.copy(color = DeepBluePrimary),
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Tushgan pul", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(summary?.totalReceivedIncome ?: 0.0) + " so'm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldSuccess
                    )
                }
                Column {
                    Text(text = "Jami Xarajat", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(summary?.totalExpenses ?: 0.0) + " so'm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = RoseExpense
                    )
                }
                Column {
                    Text(text = "Qo'ldagi pul", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = CurrencyFormatter.formatAmountShort(summary?.cashBalance ?: 0.0) + " so'm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if ((summary?.cashBalance ?: 0.0) >= 0) DeepBlueLight else RoseExpense
                    )
                }
            }
        }
    }
}
