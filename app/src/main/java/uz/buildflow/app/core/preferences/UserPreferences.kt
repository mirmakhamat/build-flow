package uz.buildflow.app.core.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 1. Maxfiylik Rejimi (Privacy Mode)
    private val _isPrivacyMode = MutableStateFlow(prefs.getBoolean(KEY_PRIVACY_MODE, false))
    val isPrivacyMode: StateFlow<Boolean> = _isPrivacyMode.asStateFlow()

    fun togglePrivacyMode() {
        val newState = !_isPrivacyMode.value
        _isPrivacyMode.value = newState
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, newState).apply()
    }

    fun setPrivacyMode(enabled: Boolean) {
        _isPrivacyMode.value = enabled
        prefs.edit().putBoolean(KEY_PRIVACY_MODE, enabled).apply()
    }

    // 2. Ilovaga kirishda Avtomatik Qulflash (App Lock)
    private val _isAppLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, true))
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    fun setAppLockEnabled(enabled: Boolean) {
        _isAppLockEnabled.value = enabled
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    // 3. Qulflash Vaqti (Soniya: 0 = Darhol, 60 = 1 daqiqa, 300 = 5 daqiqa)
    private val _appLockTimeoutSeconds = MutableStateFlow(prefs.getInt(KEY_APP_LOCK_TIMEOUT, 0))
    val appLockTimeoutSeconds: StateFlow<Int> = _appLockTimeoutSeconds.asStateFlow()

    fun setAppLockTimeoutSeconds(seconds: Int) {
        _appLockTimeoutSeconds.value = seconds
        prefs.edit().putInt(KEY_APP_LOCK_TIMEOUT, seconds).apply()
    }

    // 4. Davomat Eslatmasi Bildirishnomalari (Attendance Reminders)
    private val _isAttendanceReminderEnabled = MutableStateFlow(prefs.getBoolean(KEY_ATTENDANCE_REMINDER_ENABLED, true))
    val isAttendanceReminderEnabled: StateFlow<Boolean> = _isAttendanceReminderEnabled.asStateFlow()

    fun setAttendanceReminderEnabled(enabled: Boolean) {
        _isAttendanceReminderEnabled.value = enabled
        prefs.edit().putBoolean(KEY_ATTENDANCE_REMINDER_ENABLED, enabled).apply()
    }

    // Eslatish Vaqtlari (Ro'yxat, masalan: ["09:00", "18:00"], maksimal 3 ta)
    private val defaultTimes = listOf("09:00", "18:00")
    private val _reminderTimes = MutableStateFlow(
        prefs.getString(KEY_REMINDER_TIMES_CSV, null)
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.matches(Regex("^\\d{2}:\\d{2}$")) }
            ?.sorted()
            ?.ifEmpty { defaultTimes }
            ?: defaultTimes
    )
    val reminderTimes: StateFlow<List<String>> = _reminderTimes.asStateFlow()

    fun setReminderTimes(times: List<String>) {
        val cleaned = times.map { it.trim() }
            .filter { it.matches(Regex("^\\d{2}:\\d{2}$")) }
            .distinct()
            .sorted()
            .take(3)
            .ifEmpty { listOf("18:00") }

        _reminderTimes.value = cleaned
        prefs.edit().putString(KEY_REMINDER_TIMES_CSV, cleaned.joinToString(",")).apply()
    }

    fun addReminderTime(time: String) {
        val current = _reminderTimes.value.toMutableList()
        if (current.size < 3 && !current.contains(time)) {
            current.add(time)
            setReminderTimes(current)
        }
    }

    fun removeReminderTime(time: String) {
        val current = _reminderTimes.value.toMutableList()
        if (current.size > 1) {
            current.remove(time)
            setReminderTimes(current)
        }
    }

    fun updateReminderTime(oldTime: String, newTime: String) {
        val current = _reminderTimes.value.map { if (it == oldTime) newTime else it }
        setReminderTimes(current)
    }

    // Eslatish Kunlari (1=Yakshanba, 2=Dushanba, ... 7=Shanba)
    private val defaultWorkdays = setOf("2", "3", "4", "5", "6", "7") // Dushanba - Shanba
    private val _reminderDays = MutableStateFlow(
        (prefs.getStringSet(KEY_REMINDER_DAYS, defaultWorkdays) ?: defaultWorkdays)
            .mapNotNull { it.toIntOrNull() }
            .toSet()
    )
    val reminderDays: StateFlow<Set<Int>> = _reminderDays.asStateFlow()

    fun setReminderDays(days: Set<Int>) {
        _reminderDays.value = days
        val strSet = days.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_REMINDER_DAYS, strSet).apply()
    }

    companion object {
        private const val PREFS_NAME = "buildflow_user_prefs"
        private const val KEY_PRIVACY_MODE = "key_privacy_mode"
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_APP_LOCK_TIMEOUT = "key_app_lock_timeout"
        private const val KEY_ATTENDANCE_REMINDER_ENABLED = "key_attendance_reminder_enabled"
        private const val KEY_REMINDER_TIMES_CSV = "key_reminder_times_csv"
        private const val KEY_REMINDER_DAYS = "key_reminder_days"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
