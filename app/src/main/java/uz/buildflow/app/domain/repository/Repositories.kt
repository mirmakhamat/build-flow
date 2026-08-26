package uz.buildflow.app.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.buildflow.app.domain.model.*

interface ObjectRepository {
    fun getAllObjects(): Flow<List<BuildObject>>
    fun getObjectById(id: String): Flow<BuildObject?>
    suspend fun insertObject(obj: BuildObject)
    suspend fun updateObject(obj: BuildObject)
    suspend fun deleteObject(obj: BuildObject)
}

interface WorkerRepository {
    fun getAllWorkers(): Flow<List<Worker>>
    fun getWorkersByObject(objectId: String): Flow<List<Worker>>
    fun getWorkerById(id: String): Flow<Worker?>
    fun getActiveWorkerCount(objectId: String): Flow<Int>
    suspend fun insertWorker(worker: Worker)
    suspend fun updateWorker(worker: Worker)
    suspend fun transferWorkers(workerIds: List<String>, targetObjectId: String)
    suspend fun deleteWorker(worker: Worker)
}

interface WorkerDayRepository {
    fun getDaysByWorker(workerId: String): Flow<List<WorkerDay>>
    suspend fun getDayByWorkerAndDate(workerId: String, date: String): WorkerDay?
    fun getDaysByObjectAndDate(objectId: String, date: String): Flow<List<WorkerDay>>
    fun getTotalSalaryByObject(objectId: String): Flow<Double>
    fun getTotalWorkedDaysCountByObject(objectId: String): Flow<Int>
    fun getTotalSalaryByWorker(workerId: String): Flow<Double>
    fun getWorkedDaysCountByWorker(workerId: String): Flow<Int>
    suspend fun saveWorkerDay(workerDay: WorkerDay): Long
    suspend fun saveWorkerDaysBatch(workerDays: List<WorkerDay>)
    suspend fun deleteWorkerDay(workerDay: WorkerDay)
    
    // Bonuses
    fun getDailyBonusesByWorker(workerId: String): Flow<List<DailyBonus>>
    fun getDailyBonusByDay(workerDayId: String): Flow<DailyBonus?>
    fun getTotalDailyBonusesByObject(objectId: String): Flow<Double>
    fun getTotalDailyBonusesByWorker(workerId: String): Flow<Double>
    suspend fun insertDailyBonus(bonus: DailyBonus)
    suspend fun updateDailyBonus(bonus: DailyBonus)
    suspend fun deleteDailyBonus(bonus: DailyBonus)

    fun getGeneralBonusesByObject(objectId: String): Flow<List<GeneralBonus>>
    fun getGeneralBonusesByWorker(workerId: String): Flow<List<GeneralBonus>>
    fun getTotalGeneralBonusesByObject(objectId: String): Flow<Double>
    fun getTotalGeneralBonusesByWorker(workerId: String): Flow<Double>
    suspend fun insertGeneralBonus(bonus: GeneralBonus)
    suspend fun updateGeneralBonus(bonus: GeneralBonus)
    suspend fun deleteGeneralBonus(bonus: GeneralBonus)
}

interface ExpenseRepository {
    fun getExpensesByObject(objectId: String): Flow<List<Expense>>
    fun getExpensesByObjectAndDate(objectId: String, date: String): Flow<List<Expense>>
    fun getTotalExpenseByObject(objectId: String): Flow<Double>
    fun getTotalCashExpensePaidByObject(objectId: String): Flow<Double>
    fun getTotalExpensesPaidForOtherObjects(objectId: String): Flow<Double>
    fun getTotalExpensesPaidByOtherObjects(objectId: String): Flow<Double>
    fun getExpenseSumByCategory(objectId: String, category: String): Flow<Double>
    fun getCategoryBreakdowns(objectId: String): Flow<List<CategoryExpenseBreakdown>>
    suspend fun insertExpense(expense: Expense)
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expense: Expense)
}

interface TransactionRepository {
    fun getTransactionsByObject(objectId: String): Flow<List<MoneyTransaction>>
    fun getTotalIncomeByObject(objectId: String): Flow<Double>
    suspend fun insertTransaction(transaction: MoneyTransaction)
    suspend fun updateTransaction(transaction: MoneyTransaction)
    suspend fun deleteTransaction(transaction: MoneyTransaction)

    // Worker Payments
    fun getPaymentsByWorker(workerId: String): Flow<List<WorkerPayment>>
    fun getTotalPaidByWorker(workerId: String): Flow<Double>
    fun getTotalPaidForWorkersOfObject(objectId: String): Flow<Double>
    fun getTotalPaidByObject(objectId: String): Flow<Double>
    fun getTotalPaidForOtherObjectsWorkers(objectId: String): Flow<Double>
    fun getTotalPaidByOtherObjectsForThisWorkers(objectId: String): Flow<Double>
    suspend fun insertWorkerPayment(payment: WorkerPayment)
    suspend fun updateWorkerPayment(payment: WorkerPayment)
    suspend fun deleteWorkerPayment(payment: WorkerPayment)
}
