package uz.buildflow.app.presentation.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.domain.model.Worker
import uz.buildflow.app.presentation.common.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkersScreen(
    viewModel: WorkersViewModel,
    onBackToObjects: (() -> Unit)? = null,
    onWorkerClick: (String) -> Unit,
    onBatchAttendanceClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ishchilar va Davomat",
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
                    if (!uiState.selectedObjectId.isNullOrBlank()) {
                        IconButton(onClick = onBatchAttendanceClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.FactCheck,
                                contentDescription = "Guruhli davomat",
                                tint = DeepBluePrimary
                            )
                        }
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
                onClick = { viewModel.openAddWorker() },
                containerColor = DeepBluePrimary,
                contentColor = SurfaceLight,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Ishchi qo'shish")
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
                if (uiState.workers.isEmpty() && !uiState.isLoading) {
                    EmptyStateView(
                        title = "Ishchilar mavjud emas",
                        description = "Ushbu obyektga yangi ishchi biriktirish uchun pastdagi '+' tugmasini bosing.",
                        icon = Icons.Default.Engineering,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.workers, key = { it.worker.id }) { item ->
                            WorkerCard(
                                worker = item.worker,
                                stats = item.stats,
                                onClick = { onWorkerClick(item.worker.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.isAddEditSheetOpen) {
        AddEditWorkerSheet(
            existingWorker = uiState.selectedWorker,
            availableObjects = uiState.availableObjects,
            defaultSelectedObjectId = uiState.selectedObjectId,
            onDismiss = { viewModel.closeAddEditSheet() },
            onSave = { objId, name, phone, pos, rate, date ->
                viewModel.saveWorker(objId, name, phone, pos, rate, date)
            }
        )
    }
}

@Composable
fun WorkerCard(
    worker: Worker,
    stats: uz.buildflow.app.domain.model.WorkerStats?,
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
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DeepBluePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = worker.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = DeepBluePrimary
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = worker.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "${worker.position ?: "Ishchi"} · ${CurrencyFormatter.formatAmountShort(worker.defaultRate)} / kun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "To'langan: ${CurrencyFormatter.formatAmountShort(stats?.totalPaid ?: 0.0)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = EmeraldSuccess
                    )
                    Text(
                        text = "Qarz: ${CurrencyFormatter.formatAmountShort(stats?.remainingDebtToWorker ?: 0.0)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if ((stats?.remainingDebtToWorker ?: 0.0) > 0) RoseExpense else TextMuted
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}
