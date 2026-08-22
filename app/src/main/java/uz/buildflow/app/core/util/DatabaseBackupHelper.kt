package uz.buildflow.app.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import uz.buildflow.app.core.database.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseBackupHelper {

    fun exportDatabase(context: Context, database: AppDatabase) {
        try {
            // 1. WAL jurnaldagi ma'lumotlarni .db faylga flush qilish
            val db = database.openHelper.writableDatabase
            val cursor = db.query("PRAGMA wal_checkpoint(FULL)")
            cursor.moveToFirst()
            cursor.close()

            val currentDbPath = context.getDatabasePath("buildflow.db")
            if (!currentDbPath.exists()) {
                Toast.makeText(context, "Baza fayli topilmadi!", Toast.LENGTH_SHORT).show()
                return
            }

            val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
            val exportFileName = "buildflow_backup_$timeStamp.db"
            val backupFile = File(context.cacheDir, exportFileName)

            FileInputStream(currentDbPath).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                backupFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BuildFlow SQLite Baza Nusxasi ($timeStamp)")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Baza faylini saqlash yoki yuborish")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Eksportda xatolik: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun importDatabase(context: Context, sourceUri: Uri, onSuccess: () -> Unit) {
        try {
            val currentDbPath = context.getDatabasePath("buildflow.db")
            val walFile = File(currentDbPath.path + "-wal")
            val shmFile = File(currentDbPath.path + "-shm")

            // Eski WAL va SHM fayllarni o'chirish
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(currentDbPath).use { output ->
                    input.copyTo(output)
                }
            }

            Toast.makeText(context, "Baza muvaffaqiyatli tiklandi! Ilova yangilanmoqda...", Toast.LENGTH_SHORT).show()
            onSuccess()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Tiklashda xatolik: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
