package uz.buildflow.app.presentation.workers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.domain.model.DuplicateWorkerGroup
import uz.buildflow.app.domain.model.WorkerCandidateInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergeWorkersBottomSheet(
    viewModel: MergeWorkersViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var confirmGroupToMerge by remember { mutableStateOf<DuplicateWorkerGroup?>(null) }
    var confirmMergeAll by remember { mutableStateOf(false) }
    var confirmManualMerge by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceLight,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                containerColor = BackgroundLight,
                topBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceLight)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dublikat Ishchilarni Birlashtirish",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = DeepBluePrimary
                                    )
                                )
                                Text(
                                    text = "Ortiqcha profillarni 1 taga jamlash (barcha kunlar va to'lovlar saqlanadi)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tab selector
                        TabRow(
                            selectedTabIndex = uiState.activeTab,
                            containerColor = SurfaceLight,
                            contentColor = DeepBluePrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab]),
                                    color = DeepBluePrimary
                                )
                            }
                        ) {
                            Tab(
                                selected = uiState.activeTab == 0,
                                onClick = { viewModel.setActiveTab(0) },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "Avtomatik topilganlar",
                                            fontWeight = if (uiState.activeTab == 0) FontWeight.Bold else FontWeight.Normal
                                        )
                                        if (uiState.duplicateGroups.isNotEmpty()) {
                                            Badge(containerColor = AmberWarning, contentColor = DeepBluePrimary) {
                                                Text("${uiState.duplicateGroups.size}")
                                            }
                                        }
                                    }
                                }
                            )
                            Tab(
                                selected = uiState.activeTab == 1,
                                onClick = { viewModel.setActiveTab(1) },
                                text = {
                                    Text(
                                        text = "Qo'lda tanlash",
                                        fontWeight = if (uiState.activeTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = DeepBluePrimary
                        )
                    } else if (uiState.activeTab == 0) {
                        // TAB 1: Avtomatik topilgan dublikatlar
                        AutoDetectedDuplicatesTab(
                            groups = uiState.duplicateGroups,
                            masterMap = uiState.selectedGroupMasterMap,
                            onSelectMaster = { groupId, masterId ->
                                viewModel.setGroupMaster(groupId, masterId)
                            },
                            onMergeGroup = { group ->
                                confirmGroupToMerge = group
                            },
                            onMergeAll = {
                                confirmMergeAll = true
                            }
                        )
                    } else {
                        // TAB 2: Qo'lda tanlash
                        ManualMergeTab(
                            candidates = uiState.allCandidates,
                            searchQuery = uiState.searchQuery,
                            selectedIds = uiState.manualSelectedWorkerIds,
                            masterId = uiState.manualMasterWorkerId,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onToggleSelect = { viewModel.toggleManualSelection(it) },
                            onSelectMaster = { viewModel.setManualMaster(it) },
                            onMergeClick = { confirmManualMerge = true }
                        )
                    }

                    // Merging overlay
                    if (uiState.isMerging) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    CircularProgressIndicator(color = DeepBluePrimary)
                                    Text(
                                        text = "Ishchilar tarixi birlashtirilmoqda...",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog: Bitta guruhni birlashtirish tasdig'i
    confirmGroupToMerge?.let { group ->
        val masterId = uiState.selectedGroupMasterMap[group.id] ?: group.suggestedMasterWorkerId
        val masterWorker = group.workers.find { it.worker.id == masterId }

        AlertDialog(
            onDismissRequest = { confirmGroupToMerge = null },
            icon = { Icon(Icons.Default.MergeType, contentDescription = null, tint = DeepBluePrimary) },
            title = { Text(text = "Profillarni birlashtirish") },
            text = {
                Text(
                    text = "${group.displayName} uchun ${group.workers.size} ta profil 1 taga birlashtiriladi.\n\n" +
                            "Asosiy profil: ${masterWorker?.worker?.name} (${masterWorker?.objectName})\n\n" +
                            "Barcha ishlagan kunlar, kunlik bonuslar, mukofotlar va to'langan pullar saqlanib, asosiy profilga o'tkaziladi. Ortiqcha dublikat profillar o'chiriladi."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val g = confirmGroupToMerge
                        confirmGroupToMerge = null
                        if (g != null) viewModel.mergeGroup(g)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary)
                ) {
                    Text("Ha, Birlashtirish")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmGroupToMerge = null }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    // Dialog: Barchasini birlashtirish tasdig'i
    if (confirmMergeAll) {
        val groupCount = uiState.duplicateGroups.size
        AlertDialog(
            onDismissRequest = { confirmMergeAll = false },
            icon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = AmberWarning) },
            title = { Text(text = "Barcha dublikatlarni birlashtirish") },
            text = {
                Text(
                    text = "Topilgan $groupCount ta guruhdan faqat TELEFON RAQAMI bilan tasdiqlangan (yuqori ishonchli) dublikatlar avtomatik birlashtiriladi.\n\n" +
                            "Faqat ismi bir xil (telefonsiz) guruhlar - ular boshqa-boshqa odam bo'lishi ham mumkinligi uchun - bu yerda o'tkazib yuboriladi, ularni pastdagi ro'yxatdan birma-bir ko'rib chiqib birlashtiring.\n\n" +
                            "Barcha kunlik davomat, bonuslar va to'lovlar 100% to'liq saqlanadi. Davom etasizmi?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmMergeAll = false
                        viewModel.mergeAllAutoDetected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary)
                ) {
                    Text("Barchasini Birlashtirish")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmMergeAll = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    // Dialog: Qo'lda tanlanganlarni birlashtirish tasdig'i
    if (confirmManualMerge) {
        val selectedCount = uiState.manualSelectedWorkerIds.size
        val masterId = uiState.manualMasterWorkerId
        val masterWorker = uiState.allCandidates.find { it.worker.id == masterId }

        AlertDialog(
            onDismissRequest = { confirmManualMerge = false },
            icon = { Icon(Icons.Default.MergeType, contentDescription = null, tint = DeepBluePrimary) },
            title = { Text(text = "Tanlanganlarni birlashtirish") },
            text = {
                Text(
                    text = "Tanlangan $selectedCount ta profil bitta profilga birlashtiriladi.\n\n" +
                            "Asosiy profil: ${masterWorker?.worker?.name} (${masterWorker?.objectName})\n\n" +
                            "Barcha ish kunlari va to'lovlar ushbu profilga ko'chiriladi. Davom etasizmi?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmManualMerge = false
                        viewModel.mergeManualSelected()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary)
                ) {
                    Text("Birlashtirish")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmManualMerge = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }
}

@Composable
private fun AutoDetectedDuplicatesTab(
    groups: List<DuplicateWorkerGroup>,
    masterMap: Map<String, String>,
    onSelectMaster: (groupId: String, masterId: String) -> Unit,
    onMergeGroup: (DuplicateWorkerGroup) -> Unit,
    onMergeAll: () -> Unit
) {
    if (groups.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(EmeraldSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = EmeraldSuccess,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dublikat profillar topilmadi! 🎉",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = DeepBluePrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Barcha ishchilar ro'yxati toza va tartibli. Agar bir xil ishchi boshqa ism bilan kiritilgan bo'lsa, 'Qo'lda tanlash' bo'limidan foydalanishingiz mumkin.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header summary
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AmberWarning.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${groups.size} ta dublikat guruh aniqlandi",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepBluePrimary
                            )
                            Text(
                                text = "Har bir guruhda 1 ta asosiy profil tanlanadi",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = onMergeAll,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Barchasini Birlashtirish", fontSize = 13.sp)
                        }
                    }
                }
            }

            // Duplicate Group Cards
            items(groups, key = { it.id }) { group ->
                val currentMasterId = masterMap[group.id] ?: group.suggestedMasterWorkerId
                DuplicateGroupCard(
                    group = group,
                    selectedMasterId = currentMasterId,
                    onSelectMaster = { onSelectMaster(group.id, it) },
                    onMergeClick = { onMergeGroup(group) }
                )
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateWorkerGroup,
    selectedMasterId: String,
    onSelectMaster: (String) -> Unit,
    onMergeClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    Text(
                        text = "Aniqlanish sababi: ${group.matchReason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AmberWarning,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BackgroundLight,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Text(
                        text = "${group.workers.size} ta profil",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Asosiy saqlanadigan profilni tanlang:",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                group.workers.forEach { candidate ->
                    val isMaster = candidate.worker.id == selectedMasterId
                    CandidateItemRow(
                        candidate = candidate,
                        isMaster = isMaster,
                        onClick = { onSelectMaster(candidate.worker.id) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onMergeClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ushbu guruhni 1 taga birlashtirish", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CandidateItemRow(
    candidate: WorkerCandidateInfo,
    isMaster: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isMaster) DeepBluePrimary else BorderColor
    val bgColor = if (isMaster) DeepBluePrimary.copy(alpha = 0.04f) else BackgroundLight

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (isMaster) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Yuqori qator: Radio tugma + Ism + Obyekt
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isMaster,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = DeepBluePrimary)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = candidate.worker.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        if (isMaster) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DeepBluePrimary
                            ) {
                                Text(
                                    text = "ASOSIY",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🏢 ${candidate.objectName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!candidate.worker.phone.isNullOrBlank()) {
                            Text(
                                text = "• 📞 ${candidate.worker.phone}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 2. Pastki qator: Qulay 3 ta ko'rsatkich katakchasi (Hech qachon sig'may qolmaydi)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Ishlagan kun
                Surface(
                    color = SurfaceLight,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Ishlagan",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${candidate.daysWorkedCount} kun",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = DeepBluePrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Hisoblangan
                Surface(
                    color = SurfaceLight,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.weight(1.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Hisoblangan",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = CurrencyFormatter.formatAmount(candidate.totalEarned),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // To'langan
                Surface(
                    color = SurfaceLight,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderColor),
                    modifier = Modifier.weight(1.35f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "To'langan",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = CurrencyFormatter.formatAmount(candidate.totalPaid),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (candidate.totalPaid > 0) EmeraldSuccess else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualMergeTab(
    candidates: List<WorkerCandidateInfo>,
    searchQuery: String,
    selectedIds: Set<String>,
    masterId: String?,
    onSearchChange: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectMaster: (String) -> Unit,
    onMergeClick: () -> Unit
) {
    val filteredCandidates = remember(candidates, searchQuery) {
        if (searchQuery.isBlank()) {
            candidates
        } else {
            val q = searchQuery.trim().lowercase()
            candidates.filter {
                it.worker.name.lowercase().contains(q) ||
                        it.objectName.lowercase().contains(q) ||
                        (it.worker.phone?.contains(q) == true) ||
                        (it.worker.position?.lowercase()?.contains(q) == true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Filter header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceLight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ishchi ismi, telefon yoki obyekti...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Tozalash")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                text = "1. Birlashtiriladigan ishchilarni belgilang (kamida 2 ta).\n2. Asosiy bo'lib qoladigan profilni tanlang.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredCandidates, key = { it.worker.id }) { candidate ->
                val isSelected = selectedIds.contains(candidate.worker.id)
                val isMaster = candidate.worker.id == masterId

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DeepBluePrimary.copy(alpha = 0.04f) else SurfaceLight
                    ),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) DeepBluePrimary else BorderColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSelect(candidate.worker.id) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Yuqori qator: Checkbox + Ism + Obyekt + Asosiy tugmasi
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { onToggleSelect(candidate.worker.id) },
                                colors = CheckboxDefaults.colors(checkedColor = DeepBluePrimary)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = candidate.worker.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    if (isMaster && isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = DeepBluePrimary
                                        ) {
                                            Text(
                                                text = "ASOSIY",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "🏢 ${candidate.objectName}" + (if (!candidate.worker.phone.isNullOrBlank()) " • 📞 ${candidate.worker.phone}" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DeepBluePrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = { onSelectMaster(candidate.worker.id) },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (isMaster) DeepBluePrimary else TextSecondary
                                    )
                                ) {
                                    Text(
                                        text = if (isMaster) "Asosiy ✓" else "Asosiy qilish",
                                        fontSize = 12.sp,
                                        fontWeight = if (isMaster) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // Ko'rsatkichlar katakchalari
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = SurfaceLight,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(text = "Ishlagan", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                                    Text(text = "${candidate.daysWorkedCount} kun", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = DeepBluePrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Surface(
                                color = SurfaceLight,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.weight(1.35f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(text = "Hisoblangan", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                                    Text(text = CurrencyFormatter.formatAmount(candidate.totalEarned), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Surface(
                                color = SurfaceLight,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, BorderColor),
                                modifier = Modifier.weight(1.35f)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                                    Text(text = "To'langan", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                                    Text(text = CurrencyFormatter.formatAmount(candidate.totalPaid), style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = if (candidate.totalPaid > 0) EmeraldSuccess else TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        AnimatedVisibility(visible = selectedIds.size >= 2) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SurfaceLight,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, BorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${selectedIds.size} ta profil tanlandi",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepBluePrimary
                        )
                        val masterCandidate = candidates.find { it.worker.id == masterId }
                        Text(
                            text = "Asosiy: ${masterCandidate?.worker?.name ?: "Tanlanmagan"} (${masterCandidate?.objectName ?: ""})",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onMergeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Birlashtirish")
                    }
                }
            }
        }
    }
}
