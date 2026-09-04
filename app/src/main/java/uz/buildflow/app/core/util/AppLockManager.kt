package uz.buildflow.app.core.util

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.buildflow.app.core.preferences.UserPreferences

class AppLockManager(
    private val context: Context,
    private val userPreferences: UserPreferences
) : DefaultLifecycleObserver {

    private var lastPausedTimestamp: Long = 0L
    private val _isLocked = MutableStateFlow(
        BiometricHelper.canAuthenticate(context) && userPreferences.isAppLockEnabled.value
    )
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        checkAndLock()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        lastPausedTimestamp = System.currentTimeMillis()
    }

    fun checkAndLock() {
        // Agar qurilmada barmoq izi yoki PIN-kod o'rnatilmagan bo'lsa -> qulflash ishlamaydi
        if (!BiometricHelper.canAuthenticate(context) || !userPreferences.isAppLockEnabled.value) {
            _isLocked.value = false
            return
        }

        // Birinchi marta ochilganda (lastPausedTimestamp == 0) yoki timeout o'tganda qulflanadi
        if (lastPausedTimestamp == 0L) {
            _isLocked.value = true
            return
        }

        val elapsedSeconds = (System.currentTimeMillis() - lastPausedTimestamp) / 1000
        val timeoutSeconds = userPreferences.appLockTimeoutSeconds.value

        if (elapsedSeconds >= timeoutSeconds) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
        lastPausedTimestamp = 0L
    }

    fun lockImmediately() {
        if (BiometricHelper.canAuthenticate(context) && userPreferences.isAppLockEnabled.value) {
            _isLocked.value = true
        }
    }
}
