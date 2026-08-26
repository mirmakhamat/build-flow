package uz.buildflow.app.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.buildflow.app.data.local.*
import uz.buildflow.app.data.local.dao.*
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.domain.repository.*

class ObjectRepositoryImpl(private val objectDao: ObjectDao) : ObjectRepository {
    override fun getAllObjects(): Flow<List<BuildObject>> =
        objectDao.getAllObjects().map { list -> list.map { it.toDomain() } }

    override fun getObjectById(id: String): Flow<BuildObject?> =
        objectDao.getObjectById(id).map { it?.toDomain() }

    override suspend fun insertObject(obj: BuildObject) = objectDao.insertObject(obj.toEntity())
    override suspend fun updateObject(obj: BuildObject) = objectDao.updateObject(obj.toEntity())
    override suspend fun deleteObject(obj: BuildObject) = objectDao.deleteObject(obj.toEntity())
}

class WorkerRepositoryImpl(private val workerDao: WorkerDao) : WorkerRepository {
    override fun getAllWorkers(): Flow<List<Worker>> =
        workerDao.getAllWorkers().map { list -> list.map { it.toDomain() } }

    override fun getWorkersByObject(objectId: String): Flow<List<Worker>> =
        workerDao.getWorkersByObject(objectId).map { list -> list.map { it.toDomain() } }

    override fun getWorkerById(id: String): Flow<Worker?> =
        workerDao.getWorkerById(id).map { it?.toDomain() }

    override fun getActiveWorkerCount(objectId: String): Flow<Int> =
        workerDao.getActiveWorkerCount(objectId)

    override suspend fun insertWorker(worker: Worker) = workerDao.insertWorker(worker.toEntity())
    override suspend fun updateWorker(worker: Worker) = workerDao.updateWorker(worker.toEntity())
    override suspend fun transferWorkers(workerIds: List<String>, targetObjectId: String) =
        workerDao.transferWorkers(workerIds, targetObjectId)
    override suspend fun deleteWorker(worker: Worker) = workerDao.deleteWorker(worker.toEntity())
}

class WorkerDayRepositoryImpl(
    private val workerDayDao: WorkerDayDao,
    private val dailyBonusDao: DailyBonusDao,
    private val generalBonusDao: GeneralBonusDao
) : WorkerDayRepository {

    override fun getDaysByWorker(workerId: String): Flow<List<WorkerDay>> =
        workerDayDao.getDaysByWorker(workerId).map { list -> list.map { it.toDomain() } }

    override suspend fun getDayByWorkerAndDate(workerId: String, date: String): WorkerDay? =
        workerDayDao.getDayByWorkerAndDate(workerId, date)?.toDomain()

    override fun getDaysByObjectAndDate(objectId: String, date: String): Flow<List<WorkerDay>> =
        workerDayDao.getDaysByObjectAndDate(objectId, date).map { list -> list.map { it.toDomain() } }

    override fun getTotalSalaryByObject(objectId: String): Flow<Double> =
        workerDayDao.getTotalSalaryByObject(objectId)

    override fun getTotalWorkedDaysCountByObject(objectId: String): Flow<Int> =
        workerDayDao.getTotalWorkedDaysCountByObject(objectId)

    override fun getTotalSalaryByWorker(workerId: String): Flow<Double> =
        workerDayDao.getTotalSalaryByWorker(workerId)

    override fun getWorkedDaysCountByWorker(workerId: String): Flow<Int> =
        workerDayDao.getWorkedDaysCountByWorker(workerId)

    override suspend fun saveWorkerDay(workerDay: WorkerDay): Long =
        workerDayDao.insertWorkerDay(workerDay.toEntity())

    override suspend fun saveWorkerDaysBatch(workerDays: List<WorkerDay>) =
        workerDayDao.insertWorkerDaysBatch(workerDays.map { it.toEntity() })

    override suspend fun deleteWorkerDay(workerDay: WorkerDay) =
        workerDayDao.deleteWorkerDay(workerDay.toEntity())

    // Daily Bonuses
    override fun getDailyBonusesByWorker(workerId: String): Flow<List<DailyBonus>> =
        dailyBonusDao.getBonusesByWorker(workerId).map { list -> list.map { it.toDomain() } }

    override fun getDailyBonusByDay(workerDayId: String): Flow<DailyBonus?> =
        dailyBonusDao.getBonusByDay(workerDayId).map { it?.toDomain() }

    override fun getTotalDailyBonusesByObject(objectId: String): Flow<Double> =
        dailyBonusDao.getTotalDailyBonusesByObject(objectId)

    override fun getTotalDailyBonusesByWorker(workerId: String): Flow<Double> =
        dailyBonusDao.getTotalDailyBonusesByWorker(workerId)

    override suspend fun insertDailyBonus(bonus: DailyBonus) =
        dailyBonusDao.insertBonus(bonus.toEntity())

    override suspend fun updateDailyBonus(bonus: DailyBonus) =
        dailyBonusDao.updateBonus(bonus.toEntity())

    override suspend fun deleteDailyBonus(bonus: DailyBonus) =
        dailyBonusDao.deleteBonus(bonus.toEntity())

    // General Bonuses
    override fun getGeneralBonusesByObject(objectId: String): Flow<List<GeneralBonus>> =
        generalBonusDao.getBonusesByObject(objectId).map { list -> list.map { it.toDomain() } }

    override fun getGeneralBonusesByWorker(workerId: String): Flow<List<GeneralBonus>> =
        generalBonusDao.getBonusesByWorker(workerId).map { list -> list.map { it.toDomain() } }

    override fun getTotalGeneralBonusesByObject(objectId: String): Flow<Double> =
        generalBonusDao.getTotalGeneralBonusesByObject(objectId)

    override fun getTotalGeneralBonusesByWorker(workerId: String): Flow<Double> =
        generalBonusDao.getTotalGeneralBonusesByWorker(workerId)

    override suspend fun insertGeneralBonus(bonus: GeneralBonus) =
        generalBonusDao.insertBonus(bonus.toEntity())

    override suspend fun updateGeneralBonus(bonus: GeneralBonus) =
        generalBonusDao.updateBonus(bonus.toEntity())

    override suspend fun deleteGeneralBonus(bonus: GeneralBonus) =
        generalBonusDao.deleteBonus(bonus.toEntity())
}

class ExpenseRepositoryImpl(private val expenseDao: ExpenseDao) : ExpenseRepository {
    override fun getExpensesByObject(objectId: String): Flow<List<Expense>> =
        expenseDao.getExpensesByObject(objectId).map { list -> list.map { it.toDomain() } }

    override fun getExpensesByObjectAndDate(objectId: String, date: String): Flow<List<Expense>> =
        expenseDao.getExpensesByObjectAndDate(objectId, date).map { list -> list.map { it.toDomain() } }

    override fun getTotalExpenseByObject(objectId: String): Flow<Double> =
        expenseDao.getTotalExpenseByObject(objectId)

    override fun getTotalCashExpensePaidByObject(objectId: String): Flow<Double> =
        expenseDao.getTotalCashExpensePaidByObject(objectId)

    override fun getTotalExpensesPaidForOtherObjects(objectId: String): Flow<Double> =
        expenseDao.getTotalExpensesPaidForOtherObjects(objectId)

    override fun getTotalExpensesPaidByOtherObjects(objectId: String): Flow<Double> =
        expenseDao.getTotalExpensesPaidByOtherObjects(objectId)

    override fun getExpenseSumByCategory(objectId: String, category: String): Flow<Double> =
        expenseDao.getExpenseSumByCategory(objectId, category)

    override fun getCategoryBreakdowns(objectId: String): Flow<List<CategoryExpenseBreakdown>> =
        expenseDao.getCategoryBreakdowns(objectId).map { list ->
            list.map { CategoryExpenseBreakdown(it.category, it.totalAmount) }
        }

    override suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense.toEntity())
    override suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense.toEntity())
    override suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense.toEntity())
    override suspend fun deleteExpenseById(id: String) = expenseDao.deleteExpenseById(id)
}

class TransactionRepositoryImpl(
    private val moneyTransactionDao: MoneyTransactionDao,
    private val workerPaymentDao: WorkerPaymentDao
) : TransactionRepository {

    override fun getTransactionsByObject(objectId: String): Flow<List<MoneyTransaction>> =
        moneyTransactionDao.getTransactionsByObject(objectId).map { list -> list.map { it.toDomain() } }

    override fun getTotalIncomeByObject(objectId: String): Flow<Double> =
        moneyTransactionDao.getTotalIncomeByObject(objectId)

    override suspend fun insertTransaction(transaction: MoneyTransaction) =
        moneyTransactionDao.insertTransaction(transaction.toEntity())

    override suspend fun updateTransaction(transaction: MoneyTransaction) =
        moneyTransactionDao.updateTransaction(transaction.toEntity())

    override suspend fun deleteTransaction(transaction: MoneyTransaction) =
        moneyTransactionDao.deleteTransaction(transaction.toEntity())

    override fun getPaymentsByWorker(workerId: String): Flow<List<WorkerPayment>> =
        workerPaymentDao.getPaymentsByWorker(workerId).map { list -> list.map { it.toDomain() } }

    override fun getTotalPaidByWorker(workerId: String): Flow<Double> =
        workerPaymentDao.getTotalPaidByWorker(workerId)

    override fun getTotalPaidForWorkersOfObject(objectId: String): Flow<Double> =
        workerPaymentDao.getTotalPaidForWorkersOfObject(objectId)

    override fun getTotalPaidByObject(objectId: String): Flow<Double> =
        workerPaymentDao.getTotalPaidByObject(objectId)

    override fun getTotalPaidForOtherObjectsWorkers(objectId: String): Flow<Double> =
        workerPaymentDao.getTotalPaidForOtherObjectsWorkers(objectId)

    override fun getTotalPaidByOtherObjectsForThisWorkers(objectId: String): Flow<Double> =
        workerPaymentDao.getTotalPaidByOtherObjectsForThisWorkers(objectId)

    override suspend fun insertWorkerPayment(payment: WorkerPayment) =
        workerPaymentDao.insertPayment(payment.toEntity())

    override suspend fun updateWorkerPayment(payment: WorkerPayment) =
        workerPaymentDao.updatePayment(payment.toEntity())

    override suspend fun deleteWorkerPayment(payment: WorkerPayment) =
        workerPaymentDao.deletePayment(payment.toEntity())
}
