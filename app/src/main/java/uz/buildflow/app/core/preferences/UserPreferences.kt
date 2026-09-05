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

    // Eslatish Vaqti (Soat: 0..23, Daqiqa: 0..59)
    private val _reminderHour = MutableStateFlow(prefs.getInt(KEY_REMINDER_HOUR, 18))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(prefs.getInt(KEY_REMINDER_MINUTE, 0))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    fun setReminderTime(hour: Int, minute: Int) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
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
        private const val KEY_REMINDER_HOUR = "key_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "key_reminder_minute"
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
