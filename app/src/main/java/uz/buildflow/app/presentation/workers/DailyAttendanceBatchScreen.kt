package uz.buildflow.app.presentation.workers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.core.util.NumberAmountVisualTransformation
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.domain.repository.TransactionRepository
import uz.buildflow.app.domain.repository.WorkerDayRepository
import uz.buildflow.app.domain.repository.WorkerRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class BatchAttendanceItem(
    val worker: Worker,
    var isPresent: Boolean,
    var isPaid: Boolean = true,
    var paymentAmount: String,
    var status: AttendanceStatus
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyAttendanceBatchScreen(
    objectId: String,
    workerRepository: WorkerRepository,
    workerDayRepository: WorkerDayRepository,
    transactionRepository: TransactionRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var date by remember { mutableStateOf(DateUtil.today()) }
    var items by remember { mutableStateOf<List<BatchAttendanceItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(objectId, date) {
        workerRepository.getWorkersByObject(objectId).collect { workers ->
            items = workers.filter { it.status == WorkerStatus.ACTIVE }.map { w ->
                BatchAttendanceItem(
                    worker = w,
                    isPresent = true,
                    isPaid = true,
                    paymentAmount = w.defaultRate.toLong().toString(),
                    status = AttendanceStatus.WORKED
                )
            }
            isLoading = false
        }
    }

    fun saveBatch(isAllPaid: Boolean) {
        coroutineScope.launch {
            val listToSave = items.map { item ->
                val shouldPay = if (isAllPaid) item.isPaid else false
                val amt = if (item.isPresent) (item.paymentAmount.toDoubleOrNull() ?: 0.0) else 0.0
                val isPaidNow = shouldPay && amt > 0

                // Shu kun uchun mavjud to'lov bo'lsa - yangilaymiz/o'chiramiz, takror yozmaymiz
                val existingPayment = transactionRepository.getPaymentsByWorker(item.worker.id).firstOrNull()
                    ?.find { it.date == date && it.type == PaymentType.SALARY }
                if (isPaidNow) {
                    if (existingPayment != null) {
                        transactionRepository.updateWorkerPayment(existingPayment.copy(amount = amt))
                    } else {
                        transactionRepository.insertWorkerPayment(
                            WorkerPayment(
                                workerId = item.worker.id,
                                objectId = objectId,
                                amount = amt,
                                date = date,
                                type = PaymentType.SALARY,
                                description = "${DateUtil.formatToDisplay(date)} guruhli davomat to'lovi"
                            )
                        )
                    }
                } else if (existingPayment != null) {
                    transactionRepository.deleteWorkerPayment(existingPayment)
                }

                val status = if (item.isPresent) item.status else AttendanceStatus.ABSENT
                val paymentStatus = if (isPaidNow) PaymentStatus.PAID else PaymentStatus.UNPAID
                val existingDay = workerDayRepository.getDayByWorkerAndDate(item.worker.id, date)
                existingDay?.copy(
                    status = status,
                    paymentAmount = amt,
                    paymentStatus = paymentStatus,
                    updatedAt = System.currentTimeMillis()
                ) ?: WorkerDay(
                    workerId = item.worker.id,
                    date = date,
                    status = status,
                    paymentAmount = amt,
                    paymentStatus = paymentStatus
                )
            }
            workerDayRepository.saveWorkerDaysBatch(listToSave)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Guruhli Davomat", style = MaterialTheme.typography.titleLarge)
                        Text(text = DateUtil.formatToDisplay(date), style = MaterialTheme.typography.bodyMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Orqaga")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceLight,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { saveBatch(isAllPaid = true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Saqlash va Barchasiga To'lash", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = { saveBatch(isAllPaid = false) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DeepBluePrimary
                        )
                    ) {
                        Text("To'lovsiz Saqlash (Qarzga)", style = MaterialTheme.typography.titleMedium, color = DeepBluePrimary)
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = DeepBluePrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(BackgroundLight),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(items) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Checkbox(
                                checked = item.isPresent,
                                onCheckedChange = { checked ->
                                    val updated = items.toMutableList()
                                    updated[index] = item.copy(
                                        isPresent = checked,
                                        paymentAmount = if (checked) item.worker.defaultRate.toLong().toString() else "0"
                                    )
                                    items = updated
                                }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.worker.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = item.worker.position ?: "Ishchi", style = MaterialTheme.typography.bodyMedium)
                            }

                            if (item.isPresent) {
                                OutlinedTextField(
                                    value = item.paymentAmount,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.length <= 12) {
                                            val updated = items.toMutableList()
                                            updated[index] = item.copy(paymentAmount = digits)
                                            items = updated
                                        }
                                    },
                                    label = { Text("To'lov") },
                                    visualTransformation = NumberAmountVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(140.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
