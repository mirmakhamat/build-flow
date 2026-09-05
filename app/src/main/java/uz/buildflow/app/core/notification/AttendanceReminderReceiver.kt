package uz.buildflow.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import uz.buildflow.app.core.preferences.UserPreferences
import java.util.Calendar

class AttendanceReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val userPreferences = UserPreferences.getInstance(context)

        when (action) {
            AttendanceReminderScheduler.ACTION_SHOW_REMINDER -> {
                val isEnabled = userPreferences.isAttendanceReminderEnabled.value
                val reminderDays = userPreferences.reminderDays.value
                val todayDayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

                // Agar eslatma yoqilgan bo'lsa va bugun tanlangan ish kunlariga to'g'ri kelsa
                if (isEnabled && reminderDays.contains(todayDayOfWeek)) {
                    AttendanceReminderScheduler.showAttendanceNotification(context)
                }

                // Keyingi eslatmani rejalashtiramiz
                AttendanceReminderScheduler.scheduleNextReminder(context)
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                // Telefon qayta yoqilganda yoki vaqt o'zgarganda eslatmani qayta rejalashtiramiz
                AttendanceReminderScheduler.scheduleNextReminder(context)
            }
        }
    }
}
