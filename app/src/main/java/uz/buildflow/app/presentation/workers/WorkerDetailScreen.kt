package uz.buildflow.app.presentation.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.presentation.common.AmountInputField
import java.util.Calendar

data class DisplayBonusItem(
    val id: String,
    val amount: Double,
    val date: String,
    val reason: String?,
    val isPaid: Boolean,
    val rawGeneralBonus: GeneralBonus? = null,
    val rawPayment: WorkerPayment? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerDetailScreen(
    viewModel: WorkerDetailViewModel,
    onBack: () -> Unit,
    onNavigateToPayments: (objectId: String, workerName: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val worker = uiState.worker
    val stats = uiState.stats

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = worker?.name ?: "Ishchi Profili",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Yangilash (Refresh)", tint = DeepBluePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading || worker == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepBluePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BackgroundLight)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Ishchi Statistikasi va Balansi
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Sarlavha
                        Column {
                            Text(text = worker.name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                            Text(
                                text = "${worker.position ?: "Usta"} · Standart: ${CurrencyFormatter.formatAmount(worker.defaultRate)} / kun",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        HorizontalDivider(color = BorderColor)

                        // Kun, Ish haqi, Bonuslar qatori
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Ishlagan kun", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(
                                    text = "${stats?.workedDaysCount ?: 0} kun",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = DeepBluePrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(text = "Ish haqi", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatAmount(stats?.totalSalaryEarned ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1
                                )
                            }
                            Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                                Text(text = "Bonuslar", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatAmount((stats?.totalDailyBonuses ?: 0.0) + (stats?.totalGeneralBonuses ?: 0.0)),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess,
                                    maxLines = 1
                                )
                            }
                        }

                        HorizontalDivider(color = BorderColor)

                        // Berilgan Pul, Qarz kunlar va Qolgan Qarz qatori
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.1f)) {
                                Text(text = "Berilgan Pul", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatAmount(stats?.totalPaid ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldSuccess,
                                    maxLines = 1
                                )
                            }

                            Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "Qarz kunlar", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatAmount(stats?.totalUnpaidAccrued ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = AmberWarning,
                                    maxLines = 1
                                )
                            }

                            Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                                Text(text = "Sof Qarz", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = CurrencyFormatter.formatAmount(stats?.remainingDebtToWorker ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if ((stats?.remainingDebtToWorker ?: 0.0) > 0) RoseExpense else EmeraldSuccess,
                                    maxLines = 1
                                )
                            }
                        }

                        // To'lovlar Tarixi Tugmasi
                        Button(
                            onClick = { onNavigateToPayments(worker.objectId, worker.name) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepBluePrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("To'lovlar va Avanslar Tarixi", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                        }
                    }
                }

                // 2. Dinamik va Rangli Kalendar (Asosiy Boshqaruv Markazi)
                DynamicCalendarSection(
                    daysList = uiState.days,
                    paymentsList = uiState.payments,
                    bonusesList = uiState.generalBonuses,
                    onDateClick = { date -> viewModel.selectDateForEdit(date) }
                )
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
            defaultRate = worker?.defaultRate ?: 300000.0,
            onDismiss = { viewModel.closeDayEditSheet() },
            onDeleteDay = { rec -> viewModel.deleteDayRecord(rec) },
            onSaveDay = { status, payment, isPaid, note ->
                viewModel.saveDayRecord(status, payment, isPaid, note)
            },
            onPayDaySalary = { rec -> viewModel.payForDaySalary(rec) },
            onMarkDayUnpaid = { rec -> viewModel.markDayAsUnpaid(rec) },
            onPayBonus = { b -> viewModel.payForBonus(b) },
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
            onDismiss = { viewModel.closeBonusSheet() },
            onDelete = { b -> viewModel.deleteGeneralBonus(b) },
            onSave = { amount, date, reason, isPaid ->
                viewModel.saveGeneralBonus(amount, date, reason, isPaid)
            }
        )
    }

    // To'lov berish/tahrirlash Modal Sheet
    if (uiState.isPaymentSheetOpen) {
        AddWorkerPaymentSheet(
            existingPayment = uiState.selectedPayment,
            defaultDate = uiState.selectedDate ?: DateUtil.today(),
            onDismiss = { viewModel.closePaymentSheet() },
            onDelete = { p -> viewModel.deletePayment(p) },
            onSave = { amount, date, type, desc, isPaid ->
                viewModel.savePayment(amount, date, type, desc, isPaid)
            }
        )
    }
}

@Composable
fun DynamicCalendarSection(
    daysList: List<WorkerDay>,
    paymentsList: List<WorkerPayment>,
    bonusesList: List<GeneralBonus>,
    onDateClick: (String) -> Unit
) {
    val currentCal = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(currentCal.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(currentCal.get(Calendar.MONTH) + 1) }

    val daysGrid = remember(selectedYear, selectedMonth) {
        DateUtil.getCalendarGrid(selectedYear, selectedMonth)
    }
    val daysMap = remember(daysList) { daysList.associateBy { it.date } }

    val dailyAccruedMap = remember(daysList, bonusesList) {
        val map = mutableMapOf<String, Double>()
        daysList.forEach { d ->
            if (d.status != AttendanceStatus.ABSENT) {
                map[d.date] = (map[d.date] ?: 0.0) + d.paymentAmount
            }
        }
        bonusesList.forEach { b ->
            map[b.date] = (map[b.date] ?: 0.0) + b.amount
        }
        map
    }

    val dailyPaidMap = remember(paymentsList) {
        val map = mutableMapOf<String, Double>()
        paymentsList.forEach { p ->
            map[p.date] = (map[p.date] ?: 0.0) + p.amount
        }
        map
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (selectedMonth == 1) {
                            selectedMonth = 12
                            selectedYear -= 1
                        } else {
                            selectedMonth -= 1
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = "Oldingi oy", tint = DeepBluePrimary)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = DateUtil.getMonthYearTitle(selectedYear, selectedMonth),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                }

                IconButton(
                    onClick = {
                        if (selectedMonth == 12) {
                            selectedMonth = 1
                            selectedYear += 1
                        } else {
                            selectedMonth += 1
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Default.ChevronRight, contentDescription = "Keyingi oy", tint = DeepBluePrimary)
                }
            }

            // Hafta kunlari
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Du", "Se", "Ch", "Pa", "Ju", "Sh", "Ya").forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                daysGrid.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { item ->
                            if (item == null) {
                                Spacer(modifier = Modifier.weight(1f))
                            } else {
                                val record = daysMap[item.dateIso]
                                val accruedOnThisDay = dailyAccruedMap[item.dateIso] ?: 0.0
                                val totalPaidOnThisDay = dailyPaidMap[item.dateIso] ?: 0.0

                                val isAbsent = record?.status == AttendanceStatus.ABSENT

                                // Qarz bormi: hisoblangan summa to'langanidan ko'p bo'lsa yoki to'lov bo'lmasa
                                val isUnpaidDebt = !isAbsent && (
                                    (accruedOnThisDay > totalPaidOnThisDay) ||
                                    (record?.paymentStatus == PaymentStatus.UNPAID && record.paymentAmount > 0)
                                )

                                // To'liq to'langanmi
                                val isFullyPaid = !isAbsent && (accruedOnThisDay > 0 || totalPaidOnThisDay > 0) && (totalPaidOnThisDay >= accruedOnThisDay) && (record?.paymentStatus != PaymentStatus.UNPAID)

                                val bgColor = when {
                                    isAbsent -> RoseLight
                                    isUnpaidDebt -> AmberLight
                                    isFullyPaid -> EmeraldLight
                                    else -> if (item.isToday) DeepBluePrimary.copy(alpha = 0.08f) else SurfaceVariantLight.copy(alpha = 0.5f)
                                }

                                val textColor = when {
                                    isAbsent -> RoseExpense
                                    isUnpaidDebt -> AmberWarning
                                    isFullyPaid -> EmeraldSuccess
                                    else -> if (item.isToday) DeepBluePrimary else TextPrimary
                                }

                                // SARIQ bo'lganda to'lanmagan summa (qarz), YASHIL bo'lganda to'liq to'langan summa
                                val displayAmount = when {
                                    isUnpaidDebt -> {
                                        if (accruedOnThisDay > totalPaidOnThisDay) accruedOnThisDay - totalPaidOnThisDay
                                        else (record?.paymentAmount ?: accruedOnThisDay)
                                    }
                                    isFullyPaid -> totalPaidOnThisDay
                                    totalPaidOnThisDay > 0 -> totalPaidOnThisDay
                                    accruedOnThisDay > 0 -> accruedOnThisDay
                                    else -> 0.0
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgColor)
                                        .then(
                                            if (item.isToday) Modifier.border(1.5.dp, DeepBluePrimary, RoundedCornerShape(10.dp))
                                            else Modifier
                                        )
                                        .clickable { onDateClick(item.dateIso) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = item.dayNumber.toString(),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                            color = textColor
                                        )
                                        if (displayAmount > 0) {
                                            Text(
                                                text = CurrencyFormatter.formatAmountShort(displayAmount),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (week.size < 7) {
                            for (k in 0 until (7 - week.size)) {
                                Spacer(modifier = Modifier.weight(1f))
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
fun DayDetailBottomSheet(
    date: String,
    existingRecord: WorkerDay?,
    paymentsOnDay: List<WorkerPayment>,
    bonusesOnDay: List<GeneralBonus>,
    defaultRate: Double,
    onDismiss: () -> Unit,
    onDeleteDay: ((WorkerDay) -> Unit)?,
    onSaveDay: (status: AttendanceStatus, paymentAmount: Double, isPaid: Boolean, note: String?) -> Unit,
    onPayDaySalary: (WorkerDay) -> Unit,
    onMarkDayUnpaid: (WorkerDay) -> Unit,
    onPayBonus: (GeneralBonus) -> Unit,
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
    var note by remember(existingRecord) { mutableStateOf(existingRecord?.note ?: "") }

    val isEditMode = existingRecord != null

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

            // 1. BO'LIM: DAVOMAT VA KUNLIK ISH HAQI
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDayUnpaid) AmberLight.copy(alpha = 0.4f) else SurfaceVariantLight.copy(alpha = 0.5f)
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isDayUnpaid) AmberWarning.copy(alpha = 0.5f) else BorderColor)
                )
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
                        Text(text = "Davomat va Ish Haqi", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = DeepBluePrimary)
                        if (isDayUnpaid) {
                            Text(text = "Qarz", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = AmberWarning)
                        } else if (isDayPaid) {
                            Text(text = "To'langan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
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

                    // TUGMALAR LOGIKASI
                    if (!isEditMode) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!isSaving) {
                                        isSaving = true
                                        val amt = paymentStr.toDoubleOrNull() ?: 0.0
                                        onSaveDay(status, amt, true, note.trim().ifBlank { null })
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
                                        onSaveDay(status, amt, false, note.trim().ifBlank { null })
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
                    } else {
                        Button(
                            onClick = {
                                if (!isSaving) {
                                    isSaving = true
                                    val amt = paymentStr.toDoubleOrNull() ?: 0.0
                                    onSaveDay(status, amt, isDayPaid, note.trim().ifBlank { null })
                                    onDismiss()
                                }
                            },
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary, contentColor = Color.White)
                        ) {
                            Text("O'zgarishlarni saqlash", color = Color.White, fontSize = 13.sp)
                        }

                        if (isDayUnpaid && existingRecord != null) {
                            Button(
                                onClick = {
                                    if (!isSaving) {
                                        isSaving = true
                                        onPayDaySalary(existingRecord)
                                        onDismiss()
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Color.White)
                            ) {
                                Icon(imageVector = Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ish haqini to'lash (Pul berildi)", color = Color.White, fontSize = 13.sp)
                            }
                        } else if (isDayPaid && existingRecord != null) {
                            OutlinedButton(
                                onClick = {
                                    if (!isSaving) {
                                        isSaving = true
                                        onMarkDayUnpaid(existingRecord)
                                        onDismiss()
                                    }
                                },
                                enabled = !isSaving,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning)
                            ) {
                                Text("Qarzga o'tkazish", color = AmberWarning, fontSize = 13.sp)
                            }
                        }

                        if (onDeleteDay != null && existingRecord != null) {
                            TextButton(
                                onClick = { onDeleteDay(existingRecord) },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = RoseExpense)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Davomatni o'chirish", color = RoseExpense, fontSize = 13.sp)
                            }
                        }
                    }
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
                                modifier = Modifier.fillMaxWidth(),
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
                                        if (!payment.description.isNullOrBlank()) {
                                            Text(text = payment.description, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Qarz qilish (to'lovni bekor qilish)
                                        IconButton(onClick = { onDeletePaymentClick(payment) }) {
                                            Icon(
                                                imageVector = Icons.Default.Pending,
                                                contentDescription = "Qarz qilish",
                                                tint = AmberWarning,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Tahrirlash
                                        IconButton(onClick = { onEditPaymentClick(payment) }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Tahrirlash", tint = DeepBluePrimary, modifier = Modifier.size(18.dp))
                                        }

                                        // O'chirish
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
                                modifier = Modifier.fillMaxWidth(),
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
                                        // Yagona Iconli Holat Tugmasi (To'lash / Qarz qilish)
                                        if (!isPaid && bonusItem.rawGeneralBonus != null) {
                                            IconButton(onClick = { onPayBonus(bonusItem.rawGeneralBonus) }) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "To'lash",
                                                    tint = EmeraldSuccess,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        } else if (isPaid && bonusItem.rawGeneralBonus != null) {
                                            IconButton(onClick = { onMarkBonusUnpaid(bonusItem.rawGeneralBonus) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Pending,
                                                    contentDescription = "Qarz qilish",
                                                    tint = AmberWarning,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        // Tahrirlash
                                        IconButton(onClick = {
                                            if (bonusItem.rawGeneralBonus != null) {
                                                onEditBonusClick(bonusItem.rawGeneralBonus)
                                            } else if (bonusItem.rawPayment != null) {
                                                onEditPaymentClick(bonusItem.rawPayment)
                                            }
                                        }) {
                                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Tahrirlash", tint = DeepBluePrimary, modifier = Modifier.size(18.dp))
                                        }

                                        // O'chirish
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
    onSave: (amount: Double, date: String, reason: String?, isPaid: Boolean) -> Unit
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
                            onSave(amt, targetDate, reason.trim().ifBlank { null }, true)
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
                            onSave(amt, targetDate, reason.trim().ifBlank { null }, false)
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
