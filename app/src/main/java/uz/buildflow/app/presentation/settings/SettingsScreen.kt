package uz.buildflow.app.presentation.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import uz.buildflow.app.BuildConfig
import uz.buildflow.app.R
import uz.buildflow.app.core.notification.AttendanceReminderScheduler
import uz.buildflow.app.core.preferences.UserPreferences
import uz.buildflow.app.core.theme.*
import uz.buildflow.app.core.util.AppLockManager
import uz.buildflow.app.core.util.BiometricHelper
import uz.buildflow.app.core.util.DeviceSecurityManager
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    appLockManager: AppLockManager,
    onNavigateBack: () -> Unit,
    onExportDatabase: () -> Unit,
    onImportDatabase: () -> Unit
) {
    val context = LocalContext.current
    val isAppLockEnabled by userPreferences.isAppLockEnabled.collectAsState()
    val appLockTimeout by userPreferences.appLockTimeoutSeconds.collectAsState()
    val deviceId = remember { DeviceSecurityManager.getDeviceId(context) }
    val hasDeviceSecurity = remember { BiometricHelper.canAuthenticate(context) }

    // Davomat eslatmasi bildirishnomasi
    val isAttendanceReminderEnabled by userPreferences.isAttendanceReminderEnabled.collectAsState()
    val reminderHour by userPreferences.reminderHour.collectAsState()
    val reminderMinute by userPreferences.reminderMinute.collectAsState()
    val reminderDays by userPreferences.reminderDays.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            userPreferences.setAttendanceReminderEnabled(true)
            AttendanceReminderScheduler.scheduleNextReminder(context)
            Toast.makeText(context, "Bildirishnomalarga ruxsat berildi! 🔔", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Bildirishnoma yuborish uchun ruxsat berilmadi", Toast.LENGTH_SHORT).show()
        }
    }

    val companyName = stringResource(R.string.company_name)
    val supportTelegramUrl = stringResource(R.string.support_telegram_url)
    val supportTelegramHandle = stringResource(R.string.support_telegram_handle)

    var isTimeoutDialogOpen by remember { mutableStateOf(false) }
    var isHelpDialogOpen by remember { mutableStateOf(false) }

    fun copyDeviceId() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BuildFlow Device ID", deviceId)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Qurilma ID nusxalandi: $deviceId", Toast.LENGTH_SHORT).show()
    }

    fun openTelegramSupport() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(supportTelegramUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Havola ochilmadi: $supportTelegramUrl", Toast.LENGTH_LONG).show()
        }
    }

    fun clearCache() {
        try {
            context.cacheDir.deleteRecursively()
            Toast.makeText(context, "Vaqtinchalik kesh xotirasi tozalandi! 🧹", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Xatolik: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Sozlamalar va Xavfsizlik",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DeepBluePrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Orqaga",
                            tint = DeepBluePrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceLight)
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. QURILMA VA LITSENZIYA BLOKI
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(DeepBlueLight.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = DeepBluePrimary)
                            }
                            Column {
                                Text("Ushbu Qurilma ID", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                Text(
                                    text = deviceId,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    ),
                                    color = DeepBluePrimary
                                )
                            }
                        }

                        IconButton(onClick = { copyDeviceId() }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Nusxalash", tint = DeepBluePrimary)
                        }
                    }

                    HorizontalDivider(color = BorderColor)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Litsenziya Holati", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        Surface(
                            color = EmeraldSuccess.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "✅ Faollashtirilgan (Doimiy)",
                                color = EmeraldSuccess,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 2. XAVFSIZLIK VA ILOVA QULFI (App Lock)
            SettingsSectionHeader(title = "XAVFSIZLIK VA KIRISH NAZORATI")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Column {
                    // App Lock Switch
                    SettingsRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Avtomatik Qulflash (App Lock)",
                        subtitle = if (hasDeviceSecurity) {
                            "Ilovadan chiqqanda Touch ID / Face ID / PIN so'rash"
                        } else {
                            "Qurilmada barmoq izi yoki PIN-kod o'rnatilmagan"
                        },
                        trailing = {
                            Switch(
                                checked = isAppLockEnabled && hasDeviceSecurity,
                                onCheckedChange = {
                                    if (hasDeviceSecurity) {
                                        userPreferences.setAppLockEnabled(it)
                                    }
                                },
                                enabled = hasDeviceSecurity,
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DeepBluePrimary)
                            )
                        }
                    )

                    if (!hasDeviceSecurity) {
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))
                        Surface(
                            color = AmberWarning.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(22.dp))
                                Text(
                                    text = "Qurilmangizda barmoq izi yoki PIN-kod o'rnatilmagan. Ilovani qulflash uchun avval telefon sozlamalarida ekran qulfini o'rnating.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    if (isAppLockEnabled && hasDeviceSecurity) {
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))

                        // Timeout Selection
                        val timeoutText = when (appLockTimeout) {
                            0 -> "Darhol (chiqilganda)"
                            60 -> "1 daqiqadan so'ng"
                            300 -> "5 daqiqadan so'ng"
                            else -> "$appLockTimeout soniya"
                        }
                        SettingsRow(
                            icon = Icons.Default.Timer,
                            title = "Qulflash Vaqti",
                            subtitle = timeoutText,
                            onClick = { isTimeoutDialogOpen = true }
                        )

                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))

                        // Lock Now Button
                        SettingsRow(
                            icon = Icons.Default.Lock,
                            title = "Hozir Qulflash",
                            subtitle = "Ilovani darhol qulflash va biometrika bilan tekshirish",
                            onClick = { appLockManager.lockImmediately() }
                        )
                    }
                }
            }

            // 3. BILDIRISHNOMALAR VA DAVOMAT ESLATMASI
            SettingsSectionHeader(title = "BILDIRISHNOMALAR VA DAVOMAT ESLATMASI")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Kunlik Davomat Eslatmasi",
                        subtitle = "Har ish kuni belgilangan vaqtda eslatma yuborish",
                        trailing = {
                            Switch(
                                checked = isAttendanceReminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                userPreferences.setAttendanceReminderEnabled(true)
                                                AttendanceReminderScheduler.scheduleNextReminder(context)
                                            }
                                        } else {
                                            userPreferences.setAttendanceReminderEnabled(true)
                                            AttendanceReminderScheduler.scheduleNextReminder(context)
                                        }
                                    } else {
                                        userPreferences.setAttendanceReminderEnabled(false)
                                        AttendanceReminderScheduler.scheduleNextReminder(context)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DeepBluePrimary)
                            )
                        }
                    )

                    if (isAttendanceReminderEnabled) {
                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))

                        // Eslatish Vaqti (TimePickerDialog)
                        val formattedTime = String.format("%02d:%02d", reminderHour, reminderMinute)
                        SettingsRow(
                            icon = Icons.Default.Schedule,
                            title = "Eslatish Vaqti",
                            subtitle = "$formattedTime (soat:daqiqa)",
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        userPreferences.setReminderTime(h, m)
                                        AttendanceReminderScheduler.scheduleNextReminder(context)
                                        Toast.makeText(context, "Eslatish vaqti ${String.format("%02d:%02d", h, m)} ga o'rnatildi ⏰", Toast.LENGTH_SHORT).show()
                                    },
                                    reminderHour,
                                    reminderMinute,
                                    true
                                ).show()
                            }
                        )

                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))

                        // Hafta Kunlari
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Eslatish Kunlari (Hafta kunlari)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${reminderDays.size} kun tanlangan",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepBluePrimary
                                )
                            }

                            val weekDays = listOf(
                                Calendar.MONDAY to "Du",
                                Calendar.TUESDAY to "Se",
                                Calendar.WEDNESDAY to "Chor",
                                Calendar.THURSDAY to "Pay",
                                Calendar.FRIDAY to "Jum",
                                Calendar.SATURDAY to "Shan",
                                Calendar.SUNDAY to "Yak"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weekDays.forEach { (calDay, shortLabel) ->
                                    val isSelected = reminderDays.contains(calDay)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val updated = if (isSelected) {
                                                if (reminderDays.size > 1) reminderDays - calDay else reminderDays
                                            } else {
                                                reminderDays + calDay
                                            }
                                            userPreferences.setReminderDays(updated)
                                            AttendanceReminderScheduler.scheduleNextReminder(context)
                                        },
                                        label = {
                                            Text(
                                                text = shortLabel,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = DeepBluePrimary.copy(alpha = 0.15f),
                                            selectedLabelColor = DeepBluePrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))

                        // Test Bildirishnoma Yuborish
                        SettingsRow(
                            icon = Icons.Default.Campaign,
                            title = "Test Bildirishnoma Yuborish",
                            subtitle = "Eslatma ko'rinishi va ovozini tekshirib ko'rish",
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    AttendanceReminderScheduler.showTestNotification(context)
                                    Toast.makeText(context, "Test bildirishnoma yuborildi! 🔔", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // 4. MA'LUMOTLAR VA ZAXIRA NUSXASI
            SettingsSectionHeader(title = "MA'LUMOTLAR VA ZAXIRA (BACKUP)")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.SaveAlt,
                        title = "Baza Nusxasini Saqlash (Eksport)",
                        subtitle = "Parolli yoki ochiq SQLite nusxasi yaratish va ulashish",
                        onClick = onExportDatabase
                    )

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Default.FileUpload,
                        title = "Baza Nusxasidan Tiklash (Import)",
                        subtitle = "Zaxira nusxadagi ma'lumotlarni ilovaga qaytarish",
                        onClick = onImportDatabase
                    )

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.Default.CleaningServices,
                        title = "Keshni Tozalash",
                        subtitle = "Vaqtinchalik fayllarni tozalash va xotirani bo'shatish",
                        onClick = { clearCache() }
                    )
                }
            }

            // 4. QO'LLAB-QUVVATLASH VA ALOQA
            SettingsSectionHeader(title = "QO'LLAB-QUVVATLASH VA YORDAM")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = "Telegram Orqali Aloqa",
                        subtitle = "$supportTelegramHandle orqali yordam olish",
                        onClick = { openTelegramSupport() }
                    )

                    HorizontalDivider(color = BorderColor, modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        title = "Foydalanish Qo'llanmasi (FAQ)",
                        subtitle = "Formulalar va hisob-kitoblar haqida ma'lumot",
                        onClick = { isHelpDialogOpen = true }
                    )
                }
            }

            // 5. ILOVA HAQIDA
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = companyName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = DeepBluePrimary
                    )
                    Text(
                        text = "Versiya ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "Barcha huquqlar himoyalangan © 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // Qulflash Vaqti Dialogi
    if (isTimeoutDialogOpen) {
        AlertDialog(
            onDismissRequest = { isTimeoutDialogOpen = false },
            title = { Text("Qulflash Vaqtini Tanlang") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val options = listOf(
                        0 to "Darhol (ilovani yopganda)",
                        60 to "1 daqiqadan so'ng",
                        300 to "5 daqiqadan so'ng"
                    )
                    options.forEach { (seconds, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    userPreferences.setAppLockTimeoutSeconds(seconds)
                                    isTimeoutDialogOpen = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = (appLockTimeout == seconds),
                                onClick = {
                                    userPreferences.setAppLockTimeoutSeconds(seconds)
                                    isTimeoutDialogOpen = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = DeepBluePrimary)
                            )
                            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isTimeoutDialogOpen = false }) {
                    Text("Yopish")
                }
            }
        )
    }

    // Qo'llanma Dialogi
    if (isHelpDialogOpen) {
        AlertDialog(
            onDismissRequest = { isHelpDialogOpen = false },
            title = { Text("Foydalanish Qo'llanmasi") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "• Davomat va Qarzlar: Kalendarda to'lanmagan yoki qisman to'langan kunlar qoldiq summasi bilan sariq/amber rangda aks etadi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• O'z hisobimdan: Ish haqi yoki xarajatlarni to'lashda 'O'z hisobimdan' tanlansa, obyekt kassasiga daxl qilmaydi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Maxfiylik Rejimi: Yuqoridagi ko'z tugmasi orqali barcha summalarni berkitish va Touch ID orqali ochish mumkin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = "• Zaxira Nusxasi: Ma'lumotlaringiz xavfsizligi uchun har hafta bazani eksport qilib Telegramingizga saqlab qo'yish tavsiya etiladi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { isHelpDialogOpen = false }) {
                    Text("Tushundim")
                }
            }
        )
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
        color = TextSecondary,
        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DeepBlueLight.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = DeepBluePrimary, modifier = Modifier.size(20.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
                if (subtitle != null) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }

        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}
