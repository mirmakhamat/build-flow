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
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CalendarDayItem
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.presentation.common.AmountInputField
import uz.buildflow.app.presentation.common.DatePickerField
import uz.buildflow.app.presentation.common.MetricCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailScreen(
    viewModel: WorkerDetailViewModel,
    onBack: () -> Unit,
    onNavigateToPayments: (objectId: String, workerName: String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    val worker = uiState.worker
    val stats = uiState.stats

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
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "To'langan",
                            amount = CurrencyFormatter.formatAmountShort(stats?.totalPaid ?: 0.0),
                            accentColor = EmeraldSuccess,
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        val debt = stats?.remainingDebtToWorker ?: 0.0
                        val advance = stats?.workerDebtToUs ?: 0.0

                        if (debt > 0) {
                            MetricCard(
                                title = "Bizning qarzimiz",
                                amount = CurrencyFormatter.formatAmountShort(debt),
                                accentColor = RoseExpense,
                                icon = Icons.Default.AccountBalanceWallet,
                                modifier = Modifier.weight(1f)
                            )
                        } else if (advance > 0) {
                            MetricCard(
                                title = "Ishchining qarzi",
                                amount = CurrencyFormatter.formatAmountShort(advance),
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

                    // 2. To'lovlar Tarixi Tugmasi
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                        text = "To'lovlar Tarixi (Barcha to'lovlar va avanslar)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = DeepBluePrimary
                                    )
                                    Text(
                                        text = "Jami berilgan: ${CurrencyFormatter.formatAmount(stats?.totalPaid ?: 0.0)}",
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
                                paymentsList = uiState.payments,
                                generalBonuses = uiState.generalBonuses,
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

    // Kunlik Batafsil Davomat, To'lovlar va Bonuslar Modal Sheet
    if (uiState.isDayEditSheetOpen && uiState.selectedDate != null) {
        DayDetailBottomSheet(
            date = uiState.selectedDate!!,
            existingRecord = uiState.selectedDayRecord,
            paymentsOnDay = uiState.selectedDatePayments,
            bonusesOnDay = uiState.selectedDateBonuses,
            availableObjects = uiState.availableObjects,
            currentObjectId = worker?.objectId ?: "",
            defaultRate = worker?.defaultRate ?: 300000.0,
            onDismiss = { viewModel.closeDayEditSheet() },
            onDeleteDay = { rec -> viewModel.deleteDayRecord(rec) },
            onSaveDay = { status, payment, isPaid, note, payerObjId, paymentDate ->
                viewModel.saveDayRecord(status, payment, isPaid, note, payerObjId, paymentDate)
            },
            onPayDaySalary = { rec, payerObjId -> viewModel.payForDaySalary(rec, payerObjId) },
            onMarkDayUnpaid = { rec -> viewModel.markDayAsUnpaid(rec) },
            onPayBonus = { b, payerObjId -> viewModel.payForBonus(b, payerObjId) },
            onMarkBonusUnpaid = { b -> viewModel.markBonusAsUnpaid(b) },
            onAddPaymentClick = { viewModel.openAddPayment(uiState.selectedDate) },
            onEditPaymentClick = { p -> viewModel.openEditPayment(p) },
            onDeletePaymentClick = { p -> viewModel.deletePayment(p) },
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
            availableObjects = uiState.availableObjects,
            currentObjectId = worker?.objectId ?: "",
            onDismiss = { viewModel.closeBonusSheet() },
            onDelete = { b -> viewModel.deleteGeneralBonus(b) },
            onSave = { amount, date, reason, isPaid, payerObjId ->
                viewModel.saveGeneralBonus(amount, date, reason, isPaid, payerObjId)
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

@Composable
fun DynamicCalendarSection(
    daysList: List<WorkerDay>,
    paymentsList: List<WorkerPayment>,
    generalBonuses: List<GeneralBonus>,
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

    val paymentsMap = remember(paymentsList) {
        paymentsList.groupBy { it.date }
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
                    val dayPayments = paymentsMap[item.dateIso] ?: emptyList()
                    val dayBonuses = bonusesMap[item.dateIso] ?: emptyList()

                    CalendarDayCell(
                        dayItem = item,
                        record = record,
                        payments = dayPayments,
                        bonuses = dayBonuses,
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
    payments: List<WorkerPayment>,
    bonuses: List<GeneralBonus>,
    onClick: () -> Unit
) {
    val isDayPaid = record != null && record.status != AttendanceStatus.ABSENT && record.paymentStatus == PaymentStatus.PAID
    val isDayUnpaid = record != null && record.status != AttendanceStatus.ABSENT && record.paymentStatus == PaymentStatus.UNPAID
    val hasBonuses = bonuses.isNotEmpty() || payments.any { it.type == PaymentType.BONUS_PAYOUT }

    // Fon rangi
    val backgroundColor = when {
        isDayPaid -> EmeraldSuccess
        isDayUnpaid -> AmberWarning
        record?.status == AttendanceStatus.ABSENT -> RoseExpense.copy(alpha = 0.85f)
        hasBonuses -> PurpleBonus
        else -> SurfaceVariantLight.copy(alpha = 0.6f)
    }

    // Matn rangi
    val textColor = when {
        isDayPaid || isDayUnpaid || record?.status == AttendanceStatus.ABSENT || hasBonuses -> Color.White
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

            // Belgilar (Davomat turi yoki bonus/avans nuqtalari)
            if (record != null) {
                Text(
                    text = CurrencyFormatter.formatAmountShort(record.paymentAmount),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = textColor.copy(alpha = 0.9f)
                )
            } else if (hasBonuses) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(modifier = Modifier.size(4.dp).background(PurpleBonus, CircleShape))
                }
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
        LegendItem(color = DeepBluePrimary, label = "Avans")
        LegendItem(color = PurpleBonus, label = "Bonus")
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
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
    val date: String,
    val reason: String?,
    val isPaid: Boolean,
    val rawGeneralBonus: GeneralBonus?,
    val rawPayment: WorkerPayment?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailBottomSheet(
    date: String,
    existingRecord: WorkerDay?,
    paymentsOnDay: List<WorkerPayment>,
    bonusesOnDay: List<GeneralBonus>,
    availableObjects: List<BuildObject> = emptyList(),
    currentObjectId: String = "",
    defaultRate: Double,
    onDismiss: () -> Unit,
    onDeleteDay: ((WorkerDay) -> Unit)?,
    onSaveDay: (status: AttendanceStatus, paymentAmount: Double, isPaid: Boolean, note: String?, payerObjectId: String?, paymentDate: String?) -> Unit,
    onPayDaySalary: (WorkerDay, payerObjectId: String?) -> Unit,
    onMarkDayUnpaid: (WorkerDay) -> Unit,
    onPayBonus: (GeneralBonus, payerObjectId: String?) -> Unit,
    onMarkBonusUnpaid: (GeneralBonus) -> Unit,
    onAddPaymentClick: () -> Unit,
    onEditPaymentClick: (WorkerPayment) -> Unit,
    onDeletePaymentClick: (WorkerPayment) -> Unit,
    onAddBonusClick: () -> Unit,
    onEditBonusClick: (GeneralBonus) -> Unit,
    onDeleteBonusClick: (GeneralBonus) -> Unit
) {
    var status by remember(existingRecord) { mutableStateOf(existingRecord?.status ?: AttendanceStatus.WORKED) }
    var paymentStr by remember(existingRecord) {
        mutableStateOf(
            existingRecord?.paymentAmount?.toLong()?.toString()
                ?: defaultRate.toLong().toString()
        )
    }
    var actualPaymentDate by remember { mutableStateOf(DateUtil.today()) }
    var note by remember(existingRecord) { mutableStateOf(existingRecord?.note ?: "") }
    var selectedPayerObjectId by remember { mutableStateOf<String?>(null) }
    var isObjectMenuExpanded by remember { mutableStateOf(false) }

    // 1. FAQAT SOF TO'LOVLAR (Ish haqi va Avanslar)
    val directPayments = remember(paymentsOnDay) {
        paymentsOnDay.filter { it.type != PaymentType.BONUS_PAYOUT }
    }

    // Haqiqiy to'langanlik holati (WorkerPayment borligi tekshiriladi)
    val salaryPaymentOnDay = remember(paymentsOnDay) {
        paymentsOnDay.find { it.type == PaymentType.SALARY && it.amount > 0 }
    }
    val isDayPaid = existingRecord != null && (
        existingRecord.paymentStatus == PaymentStatus.PAID && salaryPaymentOnDay != null
    )
    val isDayUnpaid = existingRecord != null && existingRecord.status != AttendanceStatus.ABSENT && !isDayPaid

    // 2. BONUSLAR BO'LIMI UCHUN BARCHA BONUSLAR
    val displayBonusItems = remember(bonusesOnDay, paymentsOnDay) {
        val list = mutableListOf<DisplayBonusItem>()
        val bonusPayments = paymentsOnDay.filter { it.type == PaymentType.BONUS_PAYOUT }
        val usedPaymentIds = mutableSetOf<String>()

        bonusesOnDay.forEach { gb ->
            val matchingPayment = bonusPayments.find { !usedPaymentIds.contains(it.id) && it.amount == gb.amount }
            if (matchingPayment != null) {
                usedPaymentIds.add(matchingPayment.id)
                list.add(
                    DisplayBonusItem(
                        id = gb.id,
                        amount = gb.amount,
                        date = gb.date,
                        reason = gb.reason,
                        isPaid = true,
                        rawGeneralBonus = gb,
                        rawPayment = matchingPayment
                    )
                )
            } else {
                list.add(
                    DisplayBonusItem(
                        id = gb.id,
                        amount = gb.amount,
                        date = gb.date,
                        reason = gb.reason,
                        isPaid = false,
                        rawGeneralBonus = gb,
                        rawPayment = null
                    )
                )
            }
        }

        bonusPayments.forEach { bp ->
            if (!usedPaymentIds.contains(bp.id)) {
                list.add(
                    DisplayBonusItem(
                        id = bp.id,
                        amount = bp.amount,
                        date = bp.date,
                        reason = bp.description ?: "Bonus to'lovi",
                        isPaid = true,
                        rawGeneralBonus = null,
                        rawPayment = bp
                    )
                )
            }
        }
        list
    }

    val hasAnyRecords = existingRecord != null || directPayments.isNotEmpty() || displayBonusItems.isNotEmpty()
    var isEditingAttendanceForm by remember { mutableStateOf(!hasAnyRecords) }

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

            // HOLAT 1: Shu kuni hech narsa bo'lmagan bo'lsa YOKI Davomat formasi tahrirlanayotgan bo'lsa -> FAQAT FORMA CHIQADI!
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

                        DatePickerField(
                            value = actualPaymentDate,
                            onDateSelected = { actualPaymentDate = it },
                            label = "Pul berilgan sana (agar bugun berilsa)"
                        )

                        // KROSS-OBYEKT KASSA SELEKTORI
                        if (availableObjects.isNotEmpty()) {
                            val selectedObj = availableObjects.find { it.id == selectedPayerObjectId }
                            val currentObj = availableObjects.find { it.id == currentObjectId }
                            val displayName = selectedObj?.name ?: "${currentObj?.name ?: "Ushbu obyekt"} (O'z kassasidan)"

                            ExposedDropdownMenuBox(
                                expanded = isObjectMenuExpanded,
                                onExpandedChange = { isObjectMenuExpanded = !isObjectMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("To'lov manbasi (Obyekt kassasi)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isObjectMenuExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = isObjectMenuExpanded,
                                    onDismissRequest = { isObjectMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("${currentObj?.name ?: "Ushbu obyekt"} (O'z kassasidan)") },
                                        onClick = {
                                            selectedPayerObjectId = null
                                            isObjectMenuExpanded = false
                                        }
                                    )
                                    availableObjects.filter { it.id != currentObjectId }.forEach { objItem ->
                                        DropdownMenuItem(
                                            text = { Text("${objItem.name} kassasidan") },
                                            onClick = {
                                                selectedPayerObjectId = objItem.id
                                                isObjectMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Izoh (ixtiyoriy)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        var isSaving by remember { mutableStateOf(false) }

                        // TUGMALAR
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!isSaving) {
                                        isSaving = true
                                        val amt = paymentStr.toDoubleOrNull() ?: 0.0
                                        onSaveDay(status, amt, true, note.trim().ifBlank { null }, selectedPayerObjectId, actualPaymentDate.trim().ifBlank { null })
                                        onDismiss()
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
                            ) {
                                Text("Pul berildi", color = Color.White, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (!isSaving) {
                                        isSaving = true
                                        val amt = paymentStr.toDoubleOrNull() ?: 0.0
                                        onSaveDay(status, amt, false, note.trim().ifBlank { null }, selectedPayerObjectId, null)
                                        onDismiss()
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f).height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepBluePrimary)
                            ) {
                                Text("Qarzga yozish", color = DeepBluePrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            } else {
                // HOLAT 2: Shu kuni to'lov/davomat/bonus bo'lsa -> FAQAT RO'YXAT VA TAFSILOTLAR CHIQADI!

                // 1. DAVOMAT KARTOCHKASI (Agar davomat bo'lsa)
                if (existingRecord != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDayUnpaid) AmberLight.copy(alpha = 0.4f) else SurfaceLight
                        ),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = androidx.compose.ui.graphics.SolidColor(if (isDayUnpaid) AmberWarning.copy(alpha = 0.5f) else BorderColor)
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
                                    if (isDayUnpaid) {
                                        Text(text = "Qarz", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AmberWarning)
                                    } else if (isDayPaid) {
                                        Text(text = "To'langan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                                    }
                                }
                                Text(
                                    text = "Stavka: " + CurrencyFormatter.formatAmount(existingRecord.paymentAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DeepBluePrimary
                                )
                                if (!existingRecord.note.isNullOrBlank()) {
                                    Text(text = existingRecord.note, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isDayUnpaid) {
                                    IconButton(onClick = { onPayDaySalary(existingRecord, null) }) {
                                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "To'lash", tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                    }
                                } else if (isDayPaid) {
                                    IconButton(onClick = { onMarkDayUnpaid(existingRecord) }) {
                                        Icon(imageVector = Icons.Default.Pending, contentDescription = "Qarz qilish", tint = AmberWarning, modifier = Modifier.size(20.dp))
                                    }
                                }

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

                // 2. BO'LIM: SHU KUNI BERILGAN TO'LOVLAR & AVANSLAR
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
                                text = "To'lovlar va Avanslar (${directPayments.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = DeepBluePrimary
                            )
                            IconButton(onClick = onAddPaymentClick) {
                                Icon(imageVector = Icons.Default.AddCircle, contentDescription = "To'lov qo'shish", tint = EmeraldSuccess)
                            }
                        }

                        if (directPayments.isEmpty()) {
                            Text(text = "Ushbu kunda to'lovlar qayd qilinmagan.", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        } else {
                            directPayments.forEach { payment ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onEditPaymentClick(payment) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = when (payment.type) {
                                                    PaymentType.SALARY -> "Ish haqi"
                                                    PaymentType.ADVANCE -> "Avans"
                                                    else -> "To'lov"
                                                } + ": " + CurrencyFormatter.formatAmount(payment.amount),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldSuccess
                                            )
                                            if (!payment.paymentDate.isNullOrBlank() && payment.paymentDate != payment.date) {
                                                Text(
                                                    text = "Berilgan sana: ${DateUtil.formatToDisplay(payment.paymentDate)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DeepBluePrimary
                                                )
                                            }
                                            if (!payment.description.isNullOrBlank()) {
                                                Text(text = payment.description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { onEditPaymentClick(payment) }) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Tahrirlash", tint = DeepBluePrimary, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { onDeletePaymentClick(payment) }) {
                                                Icon(imageVector = Icons.Default.Delete, contentDescription = "O'chirish", tint = RoseExpense, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. BO'LIM: SHU KUNI BERILGAN BONUSLAR
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
                                            } else if (bonusItem.rawPayment != null) {
                                                onEditPaymentClick(bonusItem.rawPayment)
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
                                            Text(
                                                text = "Bonus: " + CurrencyFormatter.formatAmount(bonusItem.amount),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = itemColor
                                            )
                                            if (!bonusItem.reason.isNullOrBlank()) {
                                                Text(text = bonusItem.reason, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!isPaid && bonusItem.rawGeneralBonus != null) {
                                                IconButton(onClick = { onPayBonus(bonusItem.rawGeneralBonus, null) }) {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "To'lash", tint = EmeraldSuccess, modifier = Modifier.size(20.dp))
                                                }
                                            } else if (isPaid && bonusItem.rawGeneralBonus != null) {
                                                IconButton(onClick = { onMarkBonusUnpaid(bonusItem.rawGeneralBonus) }) {
                                                    Icon(imageVector = Icons.Default.Pending, contentDescription = "Qarz qilish", tint = AmberWarning, modifier = Modifier.size(20.dp))
                                                }
                                            }

                                            IconButton(onClick = {
                                                if (bonusItem.rawGeneralBonus != null) {
                                                    onEditBonusClick(bonusItem.rawGeneralBonus)
                                                } else if (bonusItem.rawPayment != null) {
                                                    onEditPaymentClick(bonusItem.rawPayment)
                                                }
                                            }) {
                                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Tahrirlash", tint = DeepBluePrimary, modifier = Modifier.size(18.dp))
                                            }

                                            IconButton(onClick = {
                                                if (bonusItem.rawGeneralBonus != null) {
                                                    onDeleteBonusClick(bonusItem.rawGeneralBonus)
                                                } else if (bonusItem.rawPayment != null) {
                                                    onDeletePaymentClick(bonusItem.rawPayment)
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
    availableObjects: List<BuildObject> = emptyList(),
    currentObjectId: String = "",
    onDismiss: () -> Unit,
    onDelete: ((GeneralBonus) -> Unit)? = null,
    onSave: (amount: Double, date: String, reason: String?, isPaid: Boolean, payerObjectId: String?) -> Unit
) {
    val targetDate = remember { existingBonus?.date ?: defaultDate }
    var amountStr by remember(existingBonus) {
        mutableStateOf(if (existingBonus != null && existingBonus.amount > 0) existingBonus.amount.toLong().toString() else "")
    }
    var reason by remember(existingBonus) {
        mutableStateOf(existingBonus?.reason ?: "")
    }
    var selectedPayerObjectId by remember { mutableStateOf<String?>(null) }
    var isObjectMenuExpanded by remember { mutableStateOf(false) }

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

            // KROSS-OBYEKT KASSA SELEKTORI
            if (availableObjects.isNotEmpty()) {
                val selectedObj = availableObjects.find { it.id == selectedPayerObjectId }
                val currentObj = availableObjects.find { it.id == currentObjectId }
                val displayName = selectedObj?.name ?: "${currentObj?.name ?: "Ushbu obyekt"} (O'z kassasidan)"

                ExposedDropdownMenuBox(
                    expanded = isObjectMenuExpanded,
                    onExpandedChange = { isObjectMenuExpanded = !isObjectMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To'lov manbasi (Obyekt kassasi)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isObjectMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isObjectMenuExpanded,
                        onDismissRequest = { isObjectMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("${currentObj?.name ?: "Ushbu obyekt"} (O'z kassasidan)") },
                            onClick = {
                                selectedPayerObjectId = null
                                isObjectMenuExpanded = false
                            }
                        )
                        availableObjects.filter { it.id != currentObjectId }.forEach { objItem ->
                            DropdownMenuItem(
                                text = { Text("${objItem.name} kassasidan") },
                                onClick = {
                                    selectedPayerObjectId = objItem.id
                                    isObjectMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Sababi (masalan: Erta topshirgani uchun)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            var isSaving by remember { mutableStateOf(false) }

            // ASOSIY TUGMALAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            onSave(amt, targetDate, reason.trim().ifBlank { null }, true, selectedPayerObjectId)
                            onDismiss()
                        }
                    },
                    enabled = !isSaving && amountStr.isNotBlank(),
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
                ) {
                    Text("Pul berildi", color = Color.White, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        if (!isSaving) {
                            isSaving = true
                            val amt = amountStr.toDoubleOrNull() ?: 0.0
                            onSave(amt, targetDate, reason.trim().ifBlank { null }, false, selectedPayerObjectId)
                            onDismiss()
                        }
                    },
                    enabled = !isSaving && amountStr.isNotBlank(),
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepBluePrimary)
                ) {
                    Text("Qarzga yozish", color = DeepBluePrimary, fontSize = 13.sp)
                }
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
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = RoseExpense)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bonusni o'chirish", color = RoseExpense, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
