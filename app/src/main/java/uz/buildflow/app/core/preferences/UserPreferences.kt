package uz.buildflow.app.core.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    companion object {
        private const val PREFS_NAME = "buildflow_user_prefs"
        private const val KEY_PRIVACY_MODE = "key_privacy_mode"

        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
