package uz.buildflow.app.core.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import uz.buildflow.app.BuildConfig

object DeviceSecurityManager {

    // local.properties yoki environment variable orqali beriladigan maxfiy tuz (Source code ichida saqlanmaydi)
    private val MASTER_SECRET_SALT = BuildConfig.MASTER_SECRET_SALT
    private const val PREFS_NAME = "buildflow_device_security"
    private const val KEY_ACTIVATION_SIGNATURE = "activation_signature"

    /**
     * Telefonning unikal Apparat Identifikatorini (Hardware Device ID) oladi.
     * Masalan: BF-8A42-99F1
     */
    fun getDeviceId(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_ID"
            val hardwareInfo = "${Build.BOARD}_${Build.MANUFACTURER}_${Build.MODEL}_${Build.DEVICE}"
            val rawCombined = "$androidId:$hardwareInfo"

            val md = MessageDigest.getInstance("SHA-256")
            val hashBytes = md.digest(rawCombined.toByteArray(Charsets.UTF_8))
            val hex = hashBytes.joinToString("") { "%02X".format(it) }

            // 8 xonali qulay format: BF-XXXX-XXXX
            "BF-${hex.substring(0, 4)}-${hex.substring(4, 8)}"
        } catch (e: Exception) {
            "BF-8888-9999"
        }
    }

    /**
     * Berilgan Device ID uchun to'g'ri Aktivatsiya Kodini (HMAC-SHA256 asosida) hisoblaydi.
     * Format: XXXX-XXXX (8 xonali harf-raqam)
     */
    fun calculateActivationKey(deviceId: String): String {
        val cleanId = deviceId.trim().uppercase()
        val hmac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(MASTER_SECRET_SALT.toByteArray(Charsets.UTF_8), "HmacSHA256")
        hmac.init(secretKey)

        val hash = hmac.doFinal(cleanId.toByteArray(Charsets.UTF_8))
        val hex = hash.joinToString("") { "%02X".format(it) }

        // 8 xonali kalit: XXXX-XXXX
        return "${hex.substring(0, 4)}-${hex.substring(4, 8)}"
    }

    /**
     * Kiritilgan kalitni tekshiradi va to'g'ri bo'lsa xotiraga saqlaydi.
     */
    fun verifyAndActivate(context: Context, inputKey: String): Boolean {
        val deviceId = getDeviceId(context)
        val expectedKey = calculateActivationKey(deviceId)

        val cleanInput = inputKey.trim().uppercase().replace(" ", "").replace("-", "")
        val cleanExpected = expectedKey.replace("-", "")

        if (cleanInput == cleanExpected) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val signature = generateDeviceSignature(deviceId, expectedKey)
            prefs.edit().putString(KEY_ACTIVATION_SIGNATURE, signature).apply()
            return true
        }

        return false
    }

    /**
     * Ushbu qurilma tasdiqlangan (aktivatsiya qilingan) yoki yo'qligini tekshiradi.
     */
    fun isDeviceActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSignature = prefs.getString(KEY_ACTIVATION_SIGNATURE, null) ?: return false

        val deviceId = getDeviceId(context)
        val expectedKey = calculateActivationKey(deviceId)
        val expectedSignature = generateDeviceSignature(deviceId, expectedKey)

        // Saqlangan imzo aynan shu qurilma bilan mos kelishi shart
        return savedSignature == expectedSignature
    }

    private fun generateDeviceSignature(deviceId: String, key: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = "$deviceId:$key:$MASTER_SECRET_SALT"
        return md.digest(combined.toByteArray(Charsets.UTF_8)).joinToString("") { "%02X".format(it) }
    }
}
