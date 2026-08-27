package uz.buildflow.app.presentation.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.preferences.LocalPrivacyMode
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CalendarDayItem
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.presentation.common.AmountInputField
import uz.buildflow.app.presentation.common.DatePickerField
import uz.buildflow.app.presentation.common.MetricCard
import uz.buildflow.app.presentation.common.PrivacyToggleButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailScreen(
    viewModel: WorkerDetailViewModel,
    onBack: () -> Unit,
    onNavigateToPayments: (objectId: String, workerName: String) -> Unit = { _, _ -> },
    onTogglePrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val worker = uiState.worker
    val stats = uiState.stats
    val isPrivacyMode = LocalPrivacyMode.current

    // FIFO Hisob-kitobi: Har bir kun va bonus uchun to'langan/qolgan qarz summasi
    val (settlementMap, bonusPaidMap) = remember(uiState.days, uiState.payments, uiState.generalBonuses) {
        computeSettlementMap(uiState.days, uiState.payments, uiState.generalBonuses)
    }

    var currentYear by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = worker?.name ?: "Ishchi Profili",
                            style = MaterialTheme.typography.titleLarge
                        )
                        if (!worker?.position.isNullOrBlank()) {
                            Text(
                                text = worker?.position ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    PrivacyToggleButton(onToggle = onTogglePrivacy)
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
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            if (uiState.isLoading && worker == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DeepBluePrimary)
            } else if (worker == null) {
                Text(
                    text = "Ishchi topilmadi",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Ishchi Moliyaviy Metriklari (3 ta asosiy kartochka)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(
                            title = "Ishladi",
                            amount = "${stats?.workedDaysCount ?: 0} kun",
                            accentColor = DeepBlueLight,
                            icon = Icons.Default.CalendarToday,
                            modifier = Modifier.weight(1f),
                            isMoney = false
                        )
                        MetricCard(
                            title = "To'langan",
                            amount = CurrencyFormatter.formatAmountShort(stats?.totalPaid ?: 0.0, isPrivacyMode),
                            accentColor = EmeraldSuccess,
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        val debt = stats?.remainingDebtToWorker ?: 0.0
                        val advance = stats?.workerDebtToUs ?: 0.0

                        if (debt > 0) {
                            MetricCard(
                                title = "Bizning qarzimiz",
                                amount = CurrencyFormatter.formatAmountShort(debt, isPrivacyMode),
                                accentColor = RoseExpense,
                                icon = Icons.Default.AccountBalanceWallet,
                                modifier = Modifier.weight(1f)
                            )
                        } else if (advance > 0) {
                            MetricCard(
                                title = "Ishchining qarzi",
                                amount = CurrencyFormatter.formatAmountShort(advance, isPrivacyMode),
                                accentColor = EmeraldSuccess,
                                icon = Icons.Default.Savings,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            MetricCard(
                                title = "Qarz",
                                amount = "0 so'm",
                                accentColor = EmeraldSuccess,
                                icon = Icons.Default.CheckCircle,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 2. To'lovlar Tarixi va Alohida Pul To'lash Qatori
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    onNavigateToPayments(worker.objectId, worker.name)
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DeepBluePrimary.copy(alpha = 0.08f)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(DeepBluePrimary.copy(alpha = 0.3f)))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ReceiptLong,
                                        contentDescription = null,
                                        tint = DeepBluePrimary
                                    )
                                    Column {
                                        Text(
                                            text = "To'lovlar Tarixi",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = DeepBluePrimary
                                        )
                                        Text(
                                            text = "Jami: ${CurrencyFormatter.formatAmountShort(stats?.totalPaid ?: 0.0, isPrivacyMode)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = DeepBluePrimary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.openAddPayment() },
                            modifier = Modifier.height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("To'lash", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    // 3. Oylik Kalendar boshqaruvi
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Oy va Yil sarlavhasi hamda almashtirish
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = DateUtil.getMonthYearTitle(currentYear, currentMonth),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DeepBluePrimary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (currentMonth == 1) {
                                                currentMonth = 12
                                                currentYear -= 1
                                            } else {
                                                currentMonth -= 1
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Oldingi oy")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (currentMonth == 12) {
                                                currentMonth = 1
                                                currentYear += 1
                                            } else {
                                                currentMonth += 1
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Keyingi oy")
                                    }
                                }
                            }

                            // Dinamik interaktiv kalendar kataklari
                            DynamicCalendarSection(
                                daysList = uiState.days,
                                generalBonuses = uiState.generalBonuses,
                                settlementMap = settlementMap,
                                year = currentYear,
                                month = currentMonth,
                                onDayClick = { clickedDateIso ->
                                    viewModel.selectDateForEdit(clickedDateIso)
                                }
                            )

                            // Ranglar ko'rsatkichi (Legend)
                            CalendarLegend()
                        }
                    }
                }
            }
        }
    }

    // Kunlik Davomat va Bonuslar Modal Sheet
    if (uiState.isDayEditSheetOpen && uiState.selectedDate != null) {
        val targetDate = uiState.selectedDate!!
        val daySettlement = settlementMap[targetDate]

        DayDetailBottomSheet(
            date = targetDate,
            existingRecord = uiState.selectedDayRecord,
            bonusesOnDay = uiState.selectedDateBonuses,
            daySettlement = daySettlement,
            bonusPaidMap = bonusPaidMap,
            defaultRate = worker?.defaultRate ?: 300000.0,
            onDismiss = { viewModel.closeDayEditSheet() },
            onDeleteDay = { rec -> viewModel.deleteDayRecord(rec) },
            onSaveDay = { status, payment, note ->
                viewModel.saveDayRecord(status, payment, note)
            },
            onAddBonusClick = { viewModel.openAddBonus(uiState.selectedDate) },
            onEditBonusClick = { b -> viewModel.openEditBonus(b) },
            onDeleteBonusClick = { b -> viewModel.deleteGeneralBonus(b) }
        )
    }

    // Bonus berish/tahrirlash Modal Sheet
    if (uiState.isBonusSheetOpen) {
        AddBonusBottomSheet(
            existingBonus = uiState.selectedBonus,
            defaultDate = uiState.selectedDate ?: DateUtil.today(),
            onDismiss = { viewModel.closeBonusSheet() },
            onDelete = { b -> viewModel.deleteGeneralBonus(b) },
            onSave = { amount, date, reason ->
                viewModel.saveGeneralBonus(amount, date, reason)
            }
        )
    }

    // To'lov berish/tahrirlash Modal Sheet
    if (uiState.isPaymentSheetOpen) {
        AddWorkerPaymentSheet(
            existingPayment = uiState.selectedPayment,
            defaultDate = uiState.selectedDate ?: DateUtil.today(),
            availableObjects = uiState.availableObjects,
            currentObjectId = worker?.objectId ?: "",
            onDismiss = { viewModel.closePaymentSheet() },
            onDelete = { p -> viewModel.deletePayment(p) },
            onSave = { amount, date, type, desc, isPaid, payerObjId, paymentDate ->
                viewModel.savePayment(amount, date, type, desc, isPaid, payerObjId, paymentDate)
            }
        )
    }
}

data class DaySettlementInfo(
    val dateIso: String,
    val salaryAccrued: Double,
    val salaryPaid: Double,
    val salaryDebt: Double,
    val bonusAccrued: Double,
    val bonusPaid: Double,
    val bonusDebt: Double,
    val totalAccrued: Double,
    val totalPaid: Double,
    val totalRemainingDebt: Double,
    val isFullyPaid: Boolean
)

sealed class UnifiedFIFOItem(val date: String, val amount: Double, val priority: Int) {
    class DaySalaryItem(val day: WorkerDay) : UnifiedFIFOItem(day.date, day.paymentAmount, 0)
    class BonusItem(val bonus: GeneralBonus) : UnifiedFIFOItem(bonus.date, bonus.amount, 1)
}

fun computeSettlementMap(
    daysList: List<WorkerDay>,
    paymentsList: List<WorkerPayment>,
    generalBonuses: List<GeneralBonus>
): Pair<Map<String, DaySettlementInfo>, Map<String, Double>> {
    val totalPaid = paymentsList.sumOf { it.amount }

    val items = mutableListOf<UnifiedFIFOItem>()
    daysList.filter { it.status != AttendanceStatus.ABSENT && it.paymentAmount > 0 }.forEach {
        items.add(UnifiedFIFOItem.DaySalaryItem(it))
    }
    generalBonuses.filter { it.amount > 0 }.forEach {
        items.add(UnifiedFIFOItem.BonusItem(it))
    }

    // Bir xil sanada: Avval kunlik asosiy maosh (priority 0), keyin bonuslar (priority 1) qoplanadi!
    items.sortWith(compareBy<UnifiedFIFOItem> { it.date }.thenBy { it.priority })

    var budget = totalPaid
    val daySalaryPaidMap = mutableMapOf<String, Double>() // dayId -> paidAmount
    val bonusPaidMap = mutableMapOf<String, Double>()     // bonusId -> paidAmount

    for (item in items) {
        if (budget <= 0.0) break
        val pay = minOf(budget, item.amount)
        when (item) {
            is UnifiedFIFOItem.DaySalaryItem -> {
                daySalaryPaidMap[item.day.id] = pay
            }
            is UnifiedFIFOItem.BonusItem -> {
                bonusPaidMap[item.bonus.id] = pay
            }
        }
        budget -= pay
    }

    val daysByDate = daysList.associateBy { it.date }
    val bonusesByDate = generalBonuses.groupBy { it.date }

    val allDates = (daysByDate.keys + bonusesByDate.keys).toSet()
    val settlementMap = mutableMapOf<String, DaySettlementInfo>()

    for (date in allDates) {
        val dayRec = daysByDate[date]
        val bonuses = bonusesByDate[date] ?: emptyList()

        val salaryAccrued = if (dayRec != null && dayRec.status != AttendanceStatus.ABSENT) dayRec.paymentAmount else 0.0
        val salaryPaid = if (dayRec != null) (daySalaryPaidMap[dayRec.id] ?: 0.0) else 0.0
        val salaryDebt = (salaryAccrued - salaryPaid).coerceAtLeast(0.0)

        val bonusAccrued = bonuses.sumOf { it.amount }
        val bonusPaid = bonuses.sumOf { bonusPaidMap[it.id] ?: 0.0 }
        val bonusDebt = (bonusAccrued - bonusPaid).coerceAtLeast(0.0)

        val totalAccrued = salaryAccrued + bonusAccrued
        val totalDayPaid = salaryPaid + bonusPaid
        val totalDebt = (totalAccrued - totalDayPaid).coerceAtLeast(0.0)

        val isFullyPaid = totalAccrued > 0 && totalDebt == 0.0

        settlementMap[date] = DaySettlementInfo(
            dateIso = date,
            salaryAccrued = salaryAccrued,
            salaryPaid = salaryPaid,
            salaryDebt = salaryDebt,
            bonusAccrued = bonusAccrued,
            bonusPaid = bonusPaid,
            bonusDebt = bonusDebt,
            totalAccrued = totalAccrued,
            totalPaid = totalDayPaid,
            totalRemainingDebt = totalDebt,
            isFullyPaid = isFullyPaid
        )
    }

    return Pair(settlementMap, bonusPaidMap)
}

@Composable
fun DynamicCalendarSection(
    daysList: List<WorkerDay>,
    generalBonuses: List<GeneralBonus>,
    settlementMap: Map<String, DaySettlementInfo>,
    year: Int,
    month: Int,
    onDayClick: (String) -> Unit
) {
    val daysGrid = remember(year, month) {
        DateUtil.getCalendarGrid(year, month)
    }

    val daysMap = remember(daysList) {
        daysList.associateBy { it.date }
    }

    val bonusesMap = remember(generalBonuses) {
        generalBonuses.groupBy { it.date }
    }

    val weekDays = listOf("Du", "Se", "Chor", "Pay", "Jum", "Sha", "Yak")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Hafta kunlari nomlari
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            weekDays.forEach { dayName ->
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 7 ustunli kalendar jadvali
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            userScrollEnabled = false
        ) {
            items(daysGrid) { item ->
                if (item == null) {
                    Box(modifier = Modifier.aspectRatio(1f))
                } else {
                    val record = daysMap[item.dateIso]
                    val dayBonuses = bonusesMap[item.dateIso] ?: emptyList()
                    val daySettlement = settlementMap[item.dateIso]

                    CalendarDayCell(
                        dayItem = item,
                        record = record,
                        bonuses = dayBonuses,
                        settlementInfo = daySettlement,
                        onClick = { onDayClick(item.dateIso) }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarDayCell(
    dayItem: CalendarDayItem,
    record: WorkerDay?,
    bonuses: List<GeneralBonus>,
    settlementInfo: DaySettlementInfo?,
    onClick: () -> Unit
) {
    val totalAccrued = settlementInfo?.totalAccrued ?: 0.0
    val totalDebt = settlementInfo?.totalRemainingDebt ?: 0.0
    val isFullyPaid = settlementInfo?.isFullyPaid ?: false

    val isAbsent = record?.status == AttendanceStatus.ABSENT
    val hasAccrual = totalAccrued > 0.0

    // Fon rangi:
    // 1. Kelmadi (va bonus ham yo'q) -> Qizil
    // 2. Ishladi/bonus bor va to'liq to'langan -> Yashil
    // 3. Ishladi/bonus bor va qisman yoki to'lanmagan -> Sariq
    // 4. Boshqa -> Neytral kulrang
    val backgroundColor = when {
        isAbsent && totalAccrued == 0.0 -> RoseExpense.copy(alpha = 0.85f)
        hasAccrual && isFullyPaid -> EmeraldSuccess
        hasAccrual && !isFullyPaid -> AmberWarning
        bonuses.isNotEmpty() -> PurpleBonus
        else -> SurfaceVariantLight.copy(alpha = 0.6f)
    }

    // Matn rangi
    val textColor = when {
        isAbsent || hasAccrual || bonuses.isNotEmpty() -> Color.White
        dayItem.isToday -> DeepBluePrimary
        else -> TextPrimary
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${dayItem.dayNumber}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )

            if (hasAccrual) {
                // Agar qarz bo'lsa -> Aynan qolgan QARZ summasi (masalan: 300k, 200k, 100k) chiqadi!
                // Agar to'liq to'langan bo'lsa -> Jami ishlab topilgan summa chiqadi!
                val isPrivacyMode = LocalPrivacyMode.current
                val displayAmount = if (isFullyPaid) totalAccrued else totalDebt
                Text(
                    text = CurrencyFormatter.formatAmountShort(displayAmount, isPrivacyMode),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = textColor.copy(alpha = 0.95f)
                )
            }
        }
    }
}

@Composable
fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendItem(color = EmeraldSuccess, label = "To'langan")
        LegendItem(color = AmberWarning, label = "Qarz")
        LegendItem(color = RoseExpense, label = "Kelmadi")
        LegendItem(color = PurpleBonus, label = "Bonus")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, shape = CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

data class DisplayBonusItem(
    val id: String,
    val amount: Double,
    val paidAmount: Double,
    val remainingDebt: Double,
    val date: String,
    val reason: String?,
    val isPaid: Boolean,
    val rawGeneralBonus: GeneralBonus?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    date: String,
    existingRecord: WorkerDay?,
    bonusesOnDay: List<GeneralBonus>,
    daySettlement: DaySettlementInfo?,
    bonusPaidMap: Map<String, Double> = emptyMap(),
    defaultRate: Double,
    onDismiss: () -> Unit,
    onDeleteDay: ((WorkerDay) -> Unit)?,
    onSaveDay: (status: AttendanceStatus, paymentAmount: Double, note: String?) -> Unit,
    onAddBonusClick: () -> Unit,
    onEditBonusClick: (GeneralBonus) -> Unit,
    onDeleteBonusClick: (GeneralBonus) -> Unit
) {
    val isPrivacyMode = LocalPrivacyMode.current
    var isEditingAttendanceForm by remember { mutableStateOf(existingRecord == null) }
    var status by remember { mutableStateOf(existingRecord?.status ?: AttendanceStatus.WORKED) }
    var paymentStr by remember {
        mutableStateOf(
            if (existingRecord != null) {
                if (existingRecord.paymentAmount == 0.0) "0" else existingRecord.paymentAmount.toLong().toString()
            } else {
                defaultRate.toLong().toString()
            }
        )
    }
    var note by remember { mutableStateOf(existingRecord?.note ?: "") }

    val hasAnyRecords = existingRecord != null || bonusesOnDay.isNotEmpty()

    val displayBonusItems = remember(bonusesOnDay, bonusPaidMap) {
        bonusesOnDay.map { gb ->
            val paid = bonusPaidMap[gb.id] ?: 0.0
            val debt = (gb.amount - paid).coerceAtLeast(0.0)
            DisplayBonusItem(
                id = gb.id,
                amount = gb.amount,
                paidAmount = paid,
                remainingDebt = debt,
                date = gb.date,
                reason = gb.reason,
                isPaid = gb.amount > 0.0 && debt == 0.0,
                rawGeneralBonus = gb
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sarlavha
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = DateUtil.formatToFullDisplay(date),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            // 1. DAVOMAT FORMASI / KARTOCHKASI
            if (isEditingAttendanceForm) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight.copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderColor))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (existingRecord != null) "Davomatni Tahrirlash" else "Davomat Kiritish",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepBluePrimary
                            )
                            if (hasAnyRecords) {
                                TextButton(onClick = { isEditingAttendanceForm = false }) {
                                    Text("Bekor qilish", color = TextSecondary)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                AttendanceStatus.WORKED to "Ishladi",
                                AttendanceStatus.HALF_DAY to "Yarim kun",
                                AttendanceStatus.ABSENT to "Kelmadi"
                            ).forEach { (st, label) ->
                                FilterChip(
                                    selected = status == st,
                                    onClick = {
                                        status = st
                                        if (st == AttendanceStatus.ABSENT) paymentStr = "0"
                                        if (st == AttendanceStatus.HALF_DAY) paymentStr = (defaultRate / 2).toLong().toString()
                                        if (st == AttendanceStatus.WORKED) paymentStr = defaultRate.toLong().toString()
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        AmountInputField(
                            value = paymentStr,
                            onValueChange = { paymentStr = it },
                            label = "Kunlik stavka"
                        )

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Izoh (ixtiyoriy)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        var isSaving by remember { mutableStateOf(false) }

                        Button(
                            onClick = {
                                if (!isSaving) {
                                    isSaving = true
                                    val amt = paymentStr.toDoubleOrNull() ?: 0.0
                                    onSaveDay(status, amt, note.trim().ifBlank { null })
                                    onDismiss()
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary, contentColor = Color.White)
                        ) {
                            Text("Davomatni Saqlash", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else {
                // DAVOMAT KARTOCHKASI (Mavjud bo'lsa)
                if (existingRecord != null) {
                    val salaryDebt = daySettlement?.salaryDebt ?: existingRecord.paymentAmount
                    val salaryPaid = daySettlement?.salaryPaid ?: 0.0
                    val isSalaryPaid = existingRecord.status == AttendanceStatus.ABSENT || salaryDebt == 0.0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isSalaryPaid) AmberLight.copy(alpha = 0.4f) else SurfaceLight
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (!isSalaryPaid) AmberWarning.copy(alpha = 0.5f) else BorderColor)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = when (existingRecord.status) {
                                            AttendanceStatus.WORKED -> "Ishladi (To'liq kun)"
                                            AttendanceStatus.HALF_DAY -> "Yarim kun"
                                            AttendanceStatus.ABSENT -> "Kelmadi"
                                            else -> "Boshqa"
                                        },
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = TextPrimary
                                    )
                                    if (existingRecord.status != AttendanceStatus.ABSENT) {
                                        if (salaryDebt == 0.0) {
                                            Text(text = "To'langan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                                        } else if (salaryPaid > 0.0) {
                                            Text(text = "Qarz (${CurrencyFormatter.formatAmountShort(salaryDebt, isPrivacyMode)} qoldi)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AmberWarning)
                                        } else {
                                            Text(text = "Qarz", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AmberWarning)
                                        }
                                    }
                                }
                                Text(
                                    text = "Stavka: " + CurrencyFormatter.formatAmount(existingRecord.paymentAmount, isPrivacyMode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepBluePrimary
                                )
                                if (salaryPaid > 0.0 && salaryDebt > 0.0) {
                                    Text(
                                        text = "To'langan: ${CurrencyFormatter.formatAmount(salaryPaid, isPrivacyMode)} | Qolgan qarz: ${CurrencyFormatter.formatAmount(salaryDebt, isPrivacyMode)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = AmberWarning
                                    )
                                }
                                if (!existingRecord.note.isNullOrBlank()) {
                                    Text(text = existingRecord.note, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isEditingAttendanceForm = true }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Tahrirlash", tint = DeepBluePrimary, modifier = Modifier.size(18.dp))
                                }

                                if (onDeleteDay != null) {
                                    IconButton(onClick = { onDeleteDay(existingRecord) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "O'chirish", tint = RoseExpense, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { isEditingAttendanceForm = true },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shu kunga Davomat kiritish")
                    }
                }
            }

            // 2. BONUSLAR BO'LIMI
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bonuslar (${displayBonusItems.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = DeepBluePrimary
                        )
                        IconButton(onClick = onAddBonusClick) {
                            Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Bonus qo'shish", tint = DeepBluePrimary)
                        }
                    }

                    if (displayBonusItems.isEmpty()) {
                        Text(text = "Ushbu kunda bonuslar yo'q.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    } else {
                        displayBonusItems.forEach { bonusItem ->
                            val isPaid = bonusItem.isPaid
                            val itemColor = if (isPaid) EmeraldSuccess else AmberWarning

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (bonusItem.rawGeneralBonus != null) {
                                            onEditBonusClick(bonusItem.rawGeneralBonus)
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPaid) SurfaceLight else AmberLight.copy(alpha = 0.35f)
                                ),
                                border = if (!isPaid) CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(AmberWarning.copy(alpha = 0.6f))
                                ) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Bonus: " + CurrencyFormatter.formatAmount(bonusItem.amount, isPrivacyMode),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = itemColor
                                            )
                                            if (isPaid) {
                                                Text(
                                                    text = "To'langan",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = EmeraldSuccess
                                                )
                                            } else if (bonusItem.paidAmount > 0.0) {
                                                Text(
                                                    text = "Qarz (${CurrencyFormatter.formatAmountShort(bonusItem.remainingDebt, isPrivacyMode)} qoldi)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = AmberWarning
                                                )
                                            } else {
                                                Text(
                                                    text = "Qarz",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = AmberWarning
                                                )
                                            }
                                        }
                                        if (bonusItem.paidAmount > 0.0 && bonusItem.remainingDebt > 0.0) {
                                            Text(
                                                text = "To'langan: ${CurrencyFormatter.formatAmount(bonusItem.paidAmount, isPrivacyMode)} | Qolgan qarz: ${CurrencyFormatter.formatAmount(bonusItem.remainingDebt, isPrivacyMode)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = AmberWarning
                                            )
                                        }
                                        if (!bonusItem.reason.isNullOrBlank()) {
                                            Text(text = bonusItem.reason, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            if (bonusItem.rawGeneralBonus != null) {
                                                onEditBonusClick(bonusItem.rawGeneralBonus)
                                            }
                                        }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Tahrirlash", tint = DeepBluePrimary, modifier = Modifier.size(18.dp))
                                        }

                                        IconButton(onClick = {
                                            if (bonusItem.rawGeneralBonus != null) {
                                                onDeleteBonusClick(bonusItem.rawGeneralBonus)
                                            }
                                        }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "O'chirish", tint = RoseExpense, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBonusBottomSheet(
    existingBonus: GeneralBonus? = null,
    defaultDate: String = DateUtil.today(),
    onDismiss: () -> Unit,
    onDelete: ((GeneralBonus) -> Unit)? = null,
    onSave: (amount: Double, date: String, reason: String?) -> Unit
) {
    val targetDate = remember { existingBonus?.date ?: defaultDate }
    var amountStr by remember(existingBonus) {
        mutableStateOf(if (existingBonus != null && existingBonus.amount > 0) existingBonus.amount.toLong().toString() else "")
    }
    var reason by remember(existingBonus) {
        mutableStateOf(existingBonus?.reason ?: "")
    }

    val isEditMode = existingBonus != null && existingBonus.amount > 0

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
                        text = if (isEditMode) "Bonusni Tahrirlash" else "Ishchiga Bonus Berish",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = DateUtil.formatToFullDisplay(targetDate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DeepBluePrimary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Yopish")
                }
            }

            AmountInputField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "Bonus summasi (so'm)"
            )

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Sababi (masalan: Erta topshirgani uchun)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            var isSaving by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    if (!isSaving) {
                        isSaving = true
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        onSave(amt, targetDate, reason.trim().ifBlank { null })
                        onDismiss()
                    }
                },
                enabled = !isSaving && amountStr.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary, contentColor = Color.White)
            ) {
                Text("Bonusni Saqlash", fontWeight = FontWeight.Bold, color = Color.White)
            }

            if (isEditMode && onDelete != null && existingBonus != null) {
                OutlinedButton(
                    onClick = { onDelete(existingBonus) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = RoseExpense
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "O'chirish"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
