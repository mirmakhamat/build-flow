package uz.buildflow.app.core.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uz.buildflow.app.MainActivity
import uz.buildflow.app.R
import uz.buildflow.app.core.preferences.UserPreferences
import java.util.Calendar

object AttendanceReminderScheduler {

    const val CHANNEL_ID = "attendance_reminder_channel"
    const val NOTIFICATION_ID = 1001
    const val REQUEST_CODE_ALARM = 2001
    const val ACTION_SHOW_REMINDER = "uz.buildflow.app.ACTION_SHOW_ATTENDANCE_REMINDER"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Davomat Eslatmalari"
            val descriptionText = "Har ish kuni davomat qilishni eslatish"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNextReminder(context: Context) {
        val userPreferences = UserPreferences.getInstance(context)
        val isEnabled = userPreferences.isAttendanceReminderEnabled.value
        val reminderTimes = userPreferences.reminderTimes.value
        val reminderDays = userPreferences.reminderDays.value

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AttendanceReminderReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_ALARM,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (!isEnabled || reminderDays.isEmpty() || reminderTimes.isEmpty()) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val targetCalendar = calculateNextTriggerTime(reminderTimes, reminderDays)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    targetCalendar.timeInMillis,
                    pendingIntent
                )
            } else {
                AlarmManagerCompat.setExactAndAllowWhileIdle(
                    alarmManager,
                    AlarmManager.RTC_WAKEUP,
                    targetCalendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Agar EXACT_ALARM ruxsati bo'lmasa, oddiy inexact alarm qo'yamiz
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                targetCalendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun calculateNextTriggerTime(reminderTimes: List<String>, reminderDays: Set<Int>): Calendar {
        val now = Calendar.getInstance()

        val parsedTimes = reminderTimes.mapNotNull { timeStr ->
            val parts = timeStr.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull()
                val m = parts[1].toIntOrNull()
                if (h != null && m != null) Pair(h, m) else null
            } else null
        }.sortedWith(compareBy({ it.first }, { it.second }))

        if (parsedTimes.isEmpty()) {
            return Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        for (dayOffset in 0..7) {
            val checkDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val dayOfWeek = checkDate.get(Calendar.DAY_OF_WEEK)

            if (reminderDays.contains(dayOfWeek)) {
                for ((h, m) in parsedTimes) {
                    val candidate = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, dayOffset)
                        set(Calendar.HOUR_OF_DAY, h)
                        set(Calendar.MINUTE, m)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (candidate.after(now)) {
                        return candidate
                    }
                }
            }
        }

        // Agar topilmasa
        val first = parsedTimes.first()
        return Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, first.first)
            set(Calendar.MINUTE, first.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun showAttendanceNotification(context: Context) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "objects")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Davomat eslatmasi 👷‍♂️")
            .setContentText("Bugungi ishchilar davomatini to'ldirishni unutmang!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Barcha qurilish obyektlari bo'yicha ishchilarning bugungi davomati va xarajatlarini qayd etish vaqti keldi."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF1E3A8A.toInt()) // DeepBluePrimary
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Android 13+ da ruxsat bo'lmasa exception berishi mumkin
        }
    }

    fun showTestNotification(context: Context) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            99,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("BuildFlow: Test Bildirishnoma ✅")
            .setContentText("Bildirishnomalar muvaffaqiyatli ishlayapti!")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Davomat eslatmasi sozlamalaringiz to'g'ri o'rnatildi. Belgilangan kun va soatda avtomatik eslatma keladi."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF1E3A8A.toInt())
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
        } catch (e: SecurityException) {
            // Android 13+ da ruxsat bo'lmasa
        }
    }
}
