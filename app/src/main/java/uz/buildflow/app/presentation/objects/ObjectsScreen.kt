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

import android.widget.Toast
import java.io.File
import kotlinx.coroutines.launch
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import uz.buildflow.app.core.database.AppDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectsScreen(
    viewModel: ObjectsViewModel,
    database: AppDatabase,
    onObjectClick: (String) -> Unit,
    onNavigateToGlobalReports: () -> Unit,
    onTogglePrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isPrivacyMode = LocalPrivacyMode.current

    var isExportDialogOpen by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var isExportPasswordVisible by remember { mutableStateOf(false) }
    var exportSuccessPath by remember { mutableStateOf<String?>(null) }
    var exportSavedFile by remember { mutableStateOf<File?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }

    var selectedImportUri by remember { mutableStateOf<Uri?>(null) }
    var isImportPasswordDialogOpen by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var isImportPasswordVisible by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val isEncrypted = DatabaseBackupHelper.isEncryptedBackup(context, uri)
            if (isEncrypted) {
                selectedImportUri = uri
                importPassword = ""
                isImportPasswordDialogOpen = true
            } else {
                isImporting = true
                coroutineScope.launch {
                    DatabaseBackupHelper.importDatabase(
                        context = context,
                        database = database,
                        sourceUri = uri,
                        onSuccess = {
                            isImporting = false
                        },
                        onError = { errMsg ->
                            isImporting = false
                            Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                }
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
                    IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Zaxira Nusxadan Tiklash (Import DB)",
                            tint = DeepBluePrimary
                        )
                    }
                    IconButton(onClick = {
                        exportPassword = ""
                        isExportDialogOpen = true
                    }) {
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

    // 1. Eksport Parol Dialogi
    if (isExportDialogOpen) {
        AlertDialog(
            onDismissRequest = { isExportDialogOpen = false },
            title = {
                Text(
                    text = "Baza Nusxasini Saqlash",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ixtiyoriy: Baza nusxasini begonalardan himoyalash uchun maxfiy parol kiriting. Agar parol kiritmasangiz, ochiq formatda saqlanadi.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Maxfiy parol (ixtiyoriy)") },
                        singleLine = true,
                        visualTransformation = if (isExportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isExportPasswordVisible = !isExportPasswordVisible }) {
                                Icon(
                                    imageVector = if (isExportPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isExportDialogOpen = false
                        DatabaseBackupHelper.exportDatabaseToDownloads(
                            context = context,
                            database = database,
                            password = exportPassword.ifBlank { null },
                            onSuccess = { savedPath, savedFile ->
                                exportSuccessPath = savedPath
                                exportSavedFile = savedFile
                            },
                            onError = { errMsg ->
                                Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBluePrimary)
                ) {
                    Text("Saqlash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { isExportDialogOpen = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    // 1.1 Eksport Muvaffaqiyatli Saqlandi Dialogi
    if (exportSuccessPath != null) {
        AlertDialog(
            onDismissRequest = {
                exportSuccessPath = null
                exportSavedFile = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EmeraldSuccess
                    )
                    Text(
                        text = "Baza Nusxasi Saqlandi 🎉",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Zaxira nusxasi telefon xotirasiga muvaffaqiyatli saqlandi:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = SurfaceVariantLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportSuccessPath ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(10.dp),
                            color = DeepBluePrimary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = exportSavedFile
                        if (file != null) {
                            DatabaseBackupHelper.shareBackupFile(context, file)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ulashish (Yuborish)", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    exportSuccessPath = null
                    exportSavedFile = null
                }) {
                    Text("Yopish")
                }
            }
        )
    }

    // 2. Shifrlangan Bazani Tiklash (Import) Parol Dialogi
    if (isImportPasswordDialogOpen && selectedImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                isImportPasswordDialogOpen = false
                selectedImportUri = null
            },
            title = {
                Text(
                    text = "Shifrlangan Baza Nusxasi",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ushbu zaxira nusxasi maxfiy parol bilan himoyalangan. Bazani tiklash uchun parolni kiriting:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("Maxfiy parol") },
                        singleLine = true,
                        visualTransformation = if (isImportPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isImportPasswordVisible = !isImportPasswordVisible }) {
                                Icon(
                                    imageVector = if (isImportPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = selectedImportUri ?: return@Button
                        isImportPasswordDialogOpen = false
                        isImporting = true
                        coroutineScope.launch {
                            DatabaseBackupHelper.importDatabase(
                                context = context,
                                database = database,
                                sourceUri = uri,
                                password = importPassword,
                                onSuccess = {
                                    isImporting = false
                                    selectedImportUri = null
                                },
                                onError = { errMsg ->
                                    isImporting = false
                                    selectedImportUri = null
                                    Toast.makeText(context, errMsg, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess)
                ) {
                    Text("Tiklash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    isImportPasswordDialogOpen = false
                    selectedImportUri = null
                }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    // 3. Baza Yangilanmoqda Loading Dialogi (Foydalanuvchi boshqa joyni bosolmaydi)
    if (isImporting) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {},
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceLight,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = DeepBluePrimary,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Baza tiklanmoqda...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Iltimos, kuting. Ma'lumotlar qayta tekshirilib, ilova yangilanmoqda.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
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
