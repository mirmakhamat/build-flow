package uz.buildflow.app

import android.app.Application
import uz.buildflow.app.core.database.AppDatabase
import uz.buildflow.app.core.notification.AttendanceReminderScheduler
import uz.buildflow.app.di.AppContainer

class BuildFlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Bildirishnoma kanalini va davomat eslatmasini boshlaymiz
        AttendanceReminderScheduler.createNotificationChannel(this)
        AttendanceReminderScheduler.scheduleNextReminder(this)
    }

    fun recreateContainer() {
        AppDatabase.closeAndResetInstance()
        container = AppContainer(this)
    }
}

