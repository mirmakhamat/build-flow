package uz.buildflow.app.core.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.buildflow.app.BuildFlowApp
import uz.buildflow.app.MainActivity
import uz.buildflow.app.core.database.AppDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object DatabaseBackupHelper {

    private const val TAG = "DatabaseBackupHelper"
    private val MAGIC_HEADER = "BFEN".toByteArray(Charsets.UTF_8)
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val GCM_TAG_LENGTH = 128
    private const val ITERATIONS = 10000
    private const val KEY_LENGTH = 256

    fun generateExportFileName(isEncrypted: Boolean): String {
        val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
        return if (isEncrypted) "buildflow_backup_$timeStamp.bfenc" else "buildflow_backup_$timeStamp.db"
    }

    /**
     * Barcha WAL ma'lumotlarini asosiy faylga birlashtirib, to'liq DB fayl baytlarini oladi.
     */
    private fun getConsolidatedDatabaseBytes(context: Context, database: AppDatabase): ByteArray {
        val tempBackupFile = File(context.cacheDir, "temp_vacuum_backup.db")
        if (tempBackupFile.exists()) {
            tempBackupFile.delete()
        }

        try {
            val db = database.openHelper.writableDatabase
            db.execSQL("VACUUM INTO '${tempBackupFile.absolutePath}';")
            if (tempBackupFile.exists() && tempBackupFile.length() > 0) {
                val bytes = tempBackupFile.readBytes()
                tempBackupFile.delete()
                return bytes
            }
        } catch (e: Exception) {
            Log.w(TAG, "VACUUM INTO ishlamadi: ${e.message}")
        }

        try {
            val db = database.openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(FULL)").close()
        } catch (e: Exception) {
            Log.e(TAG, "WAL checkpoint xatoligi: ${e.message}")
        }

        val currentDbPath = context.getDatabasePath("buildflow.db")
        if (!currentDbPath.exists()) {
            throw IllegalStateException("Baza fayli topilmadi: ${currentDbPath.absolutePath}")
        }

        return currentDbPath.readBytes()
    }

    /**
     * Bazani to'g'ridan-to'g'ri telefonning Downloads papkasiga yoki Tashqi xotirasiga saqlash.
     */
    fun exportDatabaseToDownloads(
        context: Context,
        database: AppDatabase,
        password: String? = null,
        onSuccess: (savedPath: String, savedFile: File) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val rawBytes = getConsolidatedDatabaseBytes(context, database)
            val isEncrypted = !password.isNullOrBlank()
            val fileName = generateExportFileName(isEncrypted)

            val finalBytes = if (isEncrypted) {
                encryptData(rawBytes, password!!.trim())
            } else {
                rawBytes
            }

            // 1. Avval Downloads papkasiga saqlashga harakat qilamiz
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val targetFile: File = if (downloadsDir != null && downloadsDir.exists() && downloadsDir.canWrite()) {
                File(downloadsDir, fileName)
            } else {
                // Agar umumiy Downloads ga ruxsat bo'lmasa, ilovaning tashqi fayllar papkasiga saqlaymiz
                val appExternalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
                File(appExternalDir, fileName)
            }

            FileOutputStream(targetFile).use { output ->
                output.write(finalBytes)
                output.flush()
            }

            Log.i(TAG, "Baza fayli muvaffaqiyatli saqlandi: ${targetFile.absolutePath}")
            onSuccess(targetFile.absolutePath, targetFile)

        } catch (e: Exception) {
            Log.e(TAG, "Eksportda xatolik yuz berdi", e)
            onError("Eksportda xatolik: ${e.localizedMessage}")
        }
    }

    /**
     * Fayl shifrlanganligini (BFEN header mavjudligini) tekshiradi.
     */
    fun isEncryptedBackup(context: Context, sourceUri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val header = ByteArray(4)
                val read = input.read(header)
                read == 4 && header.contentEquals(MAGIC_HEADER)
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Bazani import qilish (Background thread da xavfsiz tiklanadi va ilova qayta yuklanadi).
     */
    suspend fun importDatabase(
        context: Context,
        database: AppDatabase,
        sourceUri: Uri,
        password: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val rawBytes = context.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                    ?: throw IllegalArgumentException("Faylni o'qib bo'lmadi")

                val finalDbBytes = if (isEncryptedBytes(rawBytes)) {
                    if (password.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            onError("Ushbu zaxira nusxasi parollangan! Iltimos, parolni kiriting.")
                        }
                        return@withContext
                    }
                    try {
                        decryptData(rawBytes, password.trim())
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            onError("Maxfiy parol noto'g'ri! Baza ochilmadi.")
                        }
                        return@withContext
                    }
                } else {
                    rawBytes
                }

                // SQLite fayli ekanligini tasdiqlash (SQLite format 3 header)
                val sqliteHeader = "SQLite format 3".toByteArray(Charsets.UTF_8)
                if (finalDbBytes.size < 16 || !finalDbBytes.sliceArray(0 until 15).contentEquals(sqliteHeader)) {
                    withContext(Dispatchers.Main) {
                        onError("Fayl formati noto'g'ri yoki buzilgan SQLite bazasi!")
                    }
                    return@withContext
                }

                val currentDbPath = context.getDatabasePath("buildflow.db")
                val walFile = File(currentDbPath.path + "-wal")
                val shmFile = File(currentDbPath.path + "-shm")

                // 1. Database instance ni yopish va reset qilish
                AppDatabase.closeAndResetInstance()

                // 2. WAL va SHM fayllarni o'chirish
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()

                // 3. Yangi bazani yozish
                FileOutputStream(currentDbPath).use { output ->
                    output.write(finalDbBytes)
                    output.flush()
                }

                // 4. Yangi bazaga ulangan holda AppContainer ni qayta yaratish
                val app = context.applicationContext as? BuildFlowApp
                app?.recreateContainer()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Baza muvaffaqiyatli tiklandi! 🎉", Toast.LENGTH_SHORT).show()
                    onSuccess()

                    // 5. Ilovani toza qayta ishga tushirish (barcha eski Flow va ViewModellarni tozalash uchun)
                    val intent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    }
                    context.startActivity(intent)
                    if (context is Activity) {
                        context.finish()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Importda xatolik yuz berdi", e)
                withContext(Dispatchers.Main) {
                    onError("Tiklashda xatolik: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Baza faylini Telegram, Drive yoki boshqa ilovalar orqali yuborish / ulashish.
     */
    fun shareBackupFile(context: Context, backupFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BuildFlow Baza Zaxirasi (${backupFile.name})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Baza faylini yuborish / ulashish")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ulashishda xatolik: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun isEncryptedBytes(bytes: ByteArray): Boolean {
        if (bytes.size < 4) return false
        return bytes.sliceArray(0 until 4).contentEquals(MAGIC_HEADER)
    }

    private fun encryptData(data: ByteArray, password: String): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_SIZE).also { random.nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { random.nextBytes(it) }

        val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        val cipherText = cipher.doFinal(data)

        val outputStream = ByteArrayOutputStream()
        outputStream.write(MAGIC_HEADER)
        outputStream.write(salt)
        outputStream.write(iv)
        outputStream.write(cipherText)

        return outputStream.toByteArray()
    }

    private fun decryptData(encryptedBytes: ByteArray, password: String): ByteArray {
        var offset = 0
        offset += MAGIC_HEADER.size

        val salt = encryptedBytes.sliceArray(offset until (offset + SALT_SIZE))
        offset += SALT_SIZE

        val iv = encryptedBytes.sliceArray(offset until (offset + IV_SIZE))
        offset += IV_SIZE

        val cipherText = encryptedBytes.sliceArray(offset until encryptedBytes.size)

        val keySpec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = SecretKeySpec(factory.generateSecret(keySpec).encoded, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        return cipher.doFinal(cipherText)
    }
}
