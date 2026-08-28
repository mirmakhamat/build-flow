package uz.buildflow.app

import android.app.Application
import uz.buildflow.app.core.database.AppDatabase
import uz.buildflow.app.di.AppContainer

class BuildFlowApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    fun recreateContainer() {
        AppDatabase.closeAndResetInstance()
        container = AppContainer(this)
    }
}
