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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.Worker
import uz.buildflow.app.domain.model.WorkerStatus
import uz.buildflow.app.presentation.common.EmptyStateView
import uz.buildflow.app.presentation.common.OverflowAction
import uz.buildflow.app.presentation.common.OverflowMenu

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

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSuccess()
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
                    OverflowMenu(
                        actions = listOf(
                            // Boshqa obyektdagi ishchini shu obyektga alohida yozuv sifatida qo'shish
                            OverflowAction("Boshqa obyektdan ishchi qo'shish", Icons.Default.GroupAdd) { viewModel.openImportWorkerSheet() }
                        )
                    )
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
                Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Yangi ishchi qo'shish")
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
                        description = "Yangi ishchi qo'shish uchun pastdagi '+' tugmasini bosing. Boshqa obyektdagi ishchini qo'shish: yuqoridagi ⋮ menyu.",
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

    // Yangi ishchi yaratish / tahrirlash Sheet
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

    // Boshqa obyektdan ishchi olib kelish Sheet
    if (uiState.isImportSheetOpen) {
        ImportWorkerBottomSheet(
            importableWorkers = uiState.importableWorkers,
            onDismiss = { viewModel.closeImportWorkerSheet() },
            onImportWorkers = { list -> viewModel.importWorkersToCurrentObject(list) }
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
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
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
                    color = if (worker.status == WorkerStatus.ACTIVE) TextPrimary else TextMuted
                )
                if (worker.status != WorkerStatus.ACTIVE) {
                    Text(
                        text = "Nofaol · bu obyektdagi ishi ${worker.endDate?.let { DateUtil.formatToDisplay(it) } ?: ""} gacha",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWorkerBottomSheet(
    importableWorkers: List<ImportableWorkerItem>,
    onDismiss: () -> Unit,
    onImportWorkers: (List<Worker>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedWorkerIds by remember { mutableStateOf(setOf<String>()) }

    val filteredList = remember(importableWorkers, searchQuery) {
        if (searchQuery.isBlank()) {
            importableWorkers
        } else {
            val q = searchQuery.trim().lowercase()
            importableWorkers.filter {
                it.worker.name.lowercase().contains(q) ||
                (it.worker.position?.lowercase()?.contains(q) == true) ||
                it.sourceObjectName.lowercase().contains(q)
            }
        }
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
                Column {
                    Text(
                        text = "Boshqa Obyektdan Ishchi Qo'shish",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Kerakli ishchilarni tanlang (bir yoki bir nechta)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Qidirish...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Barchasini tanlash / Tozalash
                TextButton(
                    onClick = {
                        if (selectedWorkerIds.size == filteredList.size) {
                            selectedWorkerIds = emptySet()
                        } else {
                            selectedWorkerIds = filteredList.map { it.worker.id }.toSet()
                        }
                    }
                ) {
                    Text(
                        text = if (selectedWorkerIds.size == filteredList.size && filteredList.isNotEmpty()) "Tozalash" else "Barchasi",
                        fontWeight = FontWeight.Bold,
                        color = DeepBluePrimary
                    )
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Mos ishchi topilmadi", color = TextMuted)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredList, key = { it.worker.id }) { item ->
                        val isSelected = selectedWorkerIds.contains(item.worker.id)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedWorkerIds = if (isSelected) {
                                        selectedWorkerIds - item.worker.id
                                    } else {
                                        selectedWorkerIds + item.worker.id
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) DeepBluePrimary.copy(alpha = 0.08f) else SurfaceVariantLight.copy(alpha = 0.6f)
                            ),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) DeepBluePrimary else BorderColor)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedWorkerIds = if (isSelected) {
                                                selectedWorkerIds - item.worker.id
                                            } else {
                                                selectedWorkerIds + item.worker.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = DeepBluePrimary)
                                    )

                                    Column {
                                        Text(
                                            text = item.worker.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${item.worker.position ?: "Ishchi"} · ${CurrencyFormatter.formatAmountShort(item.worker.defaultRate)} / kun",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                        Text(
                                            text = "Asosiy obyekti: ${item.sourceObjectName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepBlueLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BIRIKTIRISHNI TASDIQLASH TUGMASI
            Button(
                onClick = {
                    val chosen = importableWorkers.map { it.worker }.filter { selectedWorkerIds.contains(it.id) }
                    if (chosen.isNotEmpty()) {
                        onImportWorkers(chosen)
                    }
                },
                enabled = selectedWorkerIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
            ) {
                Icon(imageVector = Icons.Default.PersonAddAlt1, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedWorkerIds.isEmpty()) "Ishchini tanlang" else "Tanlangan (${selectedWorkerIds.size}) ishchini qo'shish",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
