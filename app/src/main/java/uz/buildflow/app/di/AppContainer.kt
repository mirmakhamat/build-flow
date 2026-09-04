package uz.buildflow.app.di

import android.content.Context
import uz.buildflow.app.core.database.AppDatabase
import uz.buildflow.app.core.preferences.UserPreferences
import uz.buildflow.app.data.repository.*
import uz.buildflow.app.domain.repository.*
import uz.buildflow.app.domain.usecase.*

class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)
    val userPreferences: UserPreferences = UserPreferences.getInstance(context)
    val appLockManager: uz.buildflow.app.core.util.AppLockManager = uz.buildflow.app.core.util.AppLockManager(context, userPreferences)

    val objectRepository: ObjectRepository by lazy {
        ObjectRepositoryImpl(database.objectDao())
    }

    val workerRepository: WorkerRepository by lazy {
        WorkerRepositoryImpl(database.workerDao())
    }

    val workerDayRepository: WorkerDayRepository by lazy {
        WorkerDayRepositoryImpl(
            database.workerDayDao(),
            database.dailyBonusDao(),
            database.generalBonusDao()
        )
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepositoryImpl(database.expenseDao())
    }

    val expenseCategoryRepository: ExpenseCategoryRepository by lazy {
        ExpenseCategoryRepositoryImpl(database.expenseCategoryDao())
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            database.moneyTransactionDao(),
            database.workerPaymentDao()
        )
    }

    val getObjectFinancialSummaryUseCase by lazy {
        GetObjectFinancialSummaryUseCase(
            objectRepository,
            workerRepository,
            workerDayRepository,
            expenseRepository,
            transactionRepository
        )
    }

    val getWorkerStatsUseCase by lazy {
        GetWorkerStatsUseCase(
            workerRepository,
            workerDayRepository,
            transactionRepository
        )
    }

    val getGlobalFinancialSummaryUseCase by lazy {
        GetGlobalFinancialSummaryUseCase(
            objectRepository = objectRepository,
            getObjectFinancialSummaryUseCase = getObjectFinancialSummaryUseCase,
            transactionRepository = transactionRepository,
            expenseRepository = expenseRepository,
            workerRepository = workerRepository
        )
    }
}
