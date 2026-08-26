package uz.buildflow.app.presentation.workers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import uz.buildflow.app.domain.model.BuildObject
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
                    if (uiState.isSelectionMode) {
                        Text(
                            text = "Tanlandi: ${uiState.selectedWorkerIds.size} nafar",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepBluePrimary
                        )
                    } else {
                        Text(
                            text = "Ishchilar va Davomat",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Bekor qilish",
                                tint = TextPrimary
                            )
                        }
                    } else if (onBackToObjects != null) {
                        IconButton(onClick = onBackToObjects) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Barcha Obyektlar"
                            )
                        }
                    }
                },
                actions = {
                    if (uiState.isSelectionMode) {
                        // Barchasini tanlash
                        IconButton(onClick = { viewModel.selectAllWorkers() }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Barchasini tanlash",
                                tint = DeepBluePrimary
                            )
                        }
                    } else {
                        // Ommaviy ko'chirish rejimini yoqish
                        if (uiState.workers.isNotEmpty()) {
                            IconButton(onClick = { viewModel.toggleSelectionMode() }) {
                                Icon(
                                    imageVector = Icons.Default.MoveToInbox,
                                    contentDescription = "Obyektga ko'chirish",
                                    tint = DeepBluePrimary
                                )
                            }
                        }
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                FloatingActionButton(
                    onClick = { viewModel.openAddWorker() },
                    containerColor = DeepBluePrimary,
                    contentColor = SurfaceLight,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Ishchi qo'shish")
                }
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
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 16.dp,
                            bottom = if (uiState.isSelectionMode && uiState.selectedWorkerIds.isNotEmpty()) 90.dp else 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.workers, key = { it.worker.id }) { item ->
                            val isSelected = uiState.selectedWorkerIds.contains(item.worker.id)
                            SelectableWorkerCard(
                                worker = item.worker,
                                stats = item.stats,
                                isSelectionMode = uiState.isSelectionMode,
                                isSelected = isSelected,
                                onSelectToggle = { viewModel.toggleWorkerSelection(item.worker.id) },
                                onClick = {
                                    if (uiState.isSelectionMode) {
                                        viewModel.toggleWorkerSelection(item.worker.id)
                                    } else {
                                        onWorkerClick(item.worker.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // PASTKI OMMAVIY KO'CHIRISH FLOATING TUGMASI (Selection Mode)
            AnimatedVisibility(
                visible = uiState.isSelectionMode && uiState.selectedWorkerIds.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepBluePrimary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${uiState.selectedWorkerIds.size} nafar ishchi tanlandi",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Boshqa obyektga o'tkazish",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Button(
                            onClick = { viewModel.openTransferSheet() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ko'chirish", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Ishchi qo'shish / tahrirlash Sheet
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

    // Ishchilarni Ommaviy Ko'chirish Sheet
    if (uiState.isTransferSheetOpen) {
        TransferWorkersBottomSheet(
            selectedWorkerCount = uiState.selectedWorkerIds.size,
            currentObjectId = uiState.selectedObjectId ?: "",
            availableObjects = uiState.availableObjects,
            onDismiss = { viewModel.closeTransferSheet() },
            onConfirmTransfer = { targetObjectId ->
                viewModel.transferSelectedWorkers(targetObjectId)
            }
        )
    }
}

@Composable
fun SelectableWorkerCard(
    worker: Worker,
    stats: uz.buildflow.app.domain.model.WorkerStats?,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DeepBluePrimary.copy(alpha = 0.08f) else SurfaceLight
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DeepBluePrimary)
        ) else CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(BorderColor.copy(alpha = 0.5f))
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ko'p tanlash rejimida Checkbox
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = DeepBluePrimary)
                )
            } else {
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

            if (!isSelectionMode) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextMuted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferWorkersBottomSheet(
    selectedWorkerCount: Int,
    currentObjectId: String,
    availableObjects: List<BuildObject>,
    onDismiss: () -> Unit,
    onConfirmTransfer: (targetObjectId: String) -> Unit
) {
    val eligibleObjects = remember(availableObjects, currentObjectId) {
        availableObjects.filter { it.id != currentObjectId }
    }

    var selectedTargetObjectId by remember(eligibleObjects) {
        mutableStateOf(eligibleObjects.firstOrNull()?.id ?: "")
    }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ishchilarni Ko'chirish",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "$selectedWorkerCount nafar ishchi boshqa obyektga o'tkazilmoqda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            if (eligibleObjects.isEmpty()) {
                Text(
                    text = "Ko'chirish uchun boshqa obyekt mavjud emas. Avval yangi obyekt yarating.",
                    color = RoseExpense,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                val currentTargetObj = eligibleObjects.find { it.id == selectedTargetObjectId }

                ExposedDropdownMenuBox(
                    expanded = isDropdownExpanded,
                    onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = currentTargetObj?.name ?: "Obyektni tanlang",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Qaysi obyektga ko'chiriladi?") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        eligibleObjects.forEach { objItem ->
                            DropdownMenuItem(
                                text = { Text(objItem.name) },
                                onClick = {
                                    selectedTargetObjectId = objItem.id
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Axborot xabari
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepBluePrimary.copy(alpha = 0.07f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = DeepBluePrimary)
                        Text(
                            text = "Eslatma: Ishchilarning o'tgan kungi barcha davomatlari va berilgan to'lovlari avvalgi obyekt balansida to'liq saqlanadi. Yangi davomatlar esa yangi obyektga yoziladi.",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepBluePrimary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Bekor qilish")
                    }

                    Button(
                        onClick = {
                            if (selectedTargetObjectId.isNotBlank()) {
                                onConfirmTransfer(selectedTargetObjectId)
                            }
                        },
                        enabled = selectedTargetObjectId.isNotBlank(),
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary, contentColor = Color.White)
                    ) {
                        Text("Ko'chirishni tasdiqlash", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
