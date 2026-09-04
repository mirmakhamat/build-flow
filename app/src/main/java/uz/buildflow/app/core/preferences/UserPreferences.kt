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

    companion object {
        private const val PREFS_NAME = "buildflow_user_prefs"
        private const val KEY_PRIVACY_MODE = "key_privacy_mode"
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_APP_LOCK_TIMEOUT = "key_app_lock_timeout"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
