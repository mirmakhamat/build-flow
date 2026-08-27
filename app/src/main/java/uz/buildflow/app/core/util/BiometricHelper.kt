package uz.buildflow.app.core.util

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    /**
     * Qurilmada biometrika (Touch ID / Face ID) yoki Ekran PIN/Paroli mavjudligini tekshiradi.
     */
    fun canAuthenticate(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Maxfiylik rejimini ochish uchun biometrik autentifikatsiyani ishga tushiradi.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Maxfiylik Rejimi",
        subtitle: String = "Summalarni ko'rish uchun shaxsingizni tasdiqlang",
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val biometricManager = BiometricManager.from(activity)

        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        } else {
            BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        }

        val canAuthStatus = biometricManager.canAuthenticate(authenticators)

        // Agar telefonda umuman biometrika / ekran paroli yo'q bo'lsa yoki apparat qo'llab-quvvatlamasa:
        if (canAuthStatus != BiometricManager.BIOMETRIC_SUCCESS) {
            when (canAuthStatus) {
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                    Toast.makeText(
                        activity,
                        "Telefonda barmoq izi yoki PIN-kod o'rnatilmagan",
                        Toast.LENGTH_SHORT
                    ).show()
                    onSuccess()
                }
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
                BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                    // Biometrika apparati yo'q qurilmalarda to'g'ridan-to'g'ri ochiladi
                    onSuccess()
                }
                else -> {
                    onSuccess()
                }
            }
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                // Foydalanuvchi bekor qilgan holatlar (USER_CANCELED, NEGATIVE_BUTTON, CANCELED)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    // Hech qanday xatolik chiqarmaymiz, maxfiylik yopiq qoladi
                    return
                }
                onError?.invoke(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Noto'g'ri barmoq tekkanda tizim o'zi tebranish (vibrate) beradi
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            // Android 10 va undan past versiyalarda DEVICE_CREDENTIAL qo'yilganda negativeButtonText taqiqlanadi
            promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
        }

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            // Fallback: Agar biror nozik qurilmada DEVICE_CREDENTIAL xatolik bersa, oddiy dialog bilan ochiladi
            try {
                val fallbackPromptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText("Bekor qilish")
                    .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)
                    .build()
                biometricPrompt.authenticate(fallbackPromptInfo)
            } catch (ex: Exception) {
                onSuccess()
            }
        }
    }
}
