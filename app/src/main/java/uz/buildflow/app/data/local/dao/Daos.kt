package uz.buildflow.app.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import uz.buildflow.app.data.local.entity.*

data class CategorySum(
    val category: String,
    val totalAmount: Double
)

@Dao
interface ObjectDao {
    @Query("SELECT * FROM objects ORDER BY created_at DESC")
    fun getAllObjects(): Flow<List<ObjectEntity>>

    @Query("SELECT * FROM objects WHERE id = :id LIMIT 1")
    fun getObjectById(id: String): Flow<ObjectEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(obj: ObjectEntity)

    @Update
    suspend fun updateObject(obj: ObjectEntity)

    @Delete
    suspend fun deleteObject(obj: ObjectEntity)
}

@Dao
interface WorkerDao {
    @Query("SELECT * FROM workers WHERE object_id = :objectId ORDER BY name ASC")
    fun getWorkersByObject(objectId: String): Flow<List<WorkerEntity>>

    @Query("SELECT * FROM workers WHERE id = :id LIMIT 1")
    fun getWorkerById(id: String): Flow<WorkerEntity?>

    @Query("SELECT COUNT(*) FROM workers WHERE object_id = :objectId AND status = 'ACTIVE'")
    fun getActiveWorkerCount(objectId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorker(worker: WorkerEntity)

    @Update
    suspend fun updateWorker(worker: WorkerEntity)

    @Query("UPDATE workers SET object_id = :targetObjectId, updated_at = :now WHERE id IN (:workerIds)")
    suspend fun transferWorkers(workerIds: List<String>, targetObjectId: String, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteWorker(worker: WorkerEntity)
}

@Dao
interface WorkerDayDao {
    @Query("SELECT * FROM worker_days WHERE worker_id = :workerId ORDER BY date DESC")
    fun getDaysByWorker(workerId: String): Flow<List<WorkerDayEntity>>

    @Query("SELECT * FROM worker_days WHERE worker_id = :workerId AND date = :date LIMIT 1")
    suspend fun getDayByWorkerAndDate(workerId: String, date: String): WorkerDayEntity?

    @Query("SELECT * FROM worker_days WHERE date = :date")
    fun getDaysByDate(date: String): Flow<List<WorkerDayEntity>>

    @Query("""
        SELECT wd.* FROM worker_days wd 
        INNER JOIN workers w ON wd.worker_id = w.id 
        WHERE w.object_id = :objectId AND wd.date = :date
    """)
    fun getDaysByObjectAndDate(objectId: String, date: String): Flow<List<WorkerDayEntity>>

    @Query("""
        SELECT COALESCE(SUM(wd.payment_amount), 0.0) FROM worker_days wd 
        INNER JOIN workers w ON wd.worker_id = w.id 
        WHERE w.object_id = :objectId
    """)
    fun getTotalSalaryByObject(objectId: String): Flow<Double>

    @Query("""
        SELECT COUNT(wd.id) FROM worker_days wd 
        INNER JOIN workers w ON wd.worker_id = w.id 
        WHERE w.object_id = :objectId AND wd.status IN ('WORKED', 'HALF_DAY')
    """)
    fun getTotalWorkedDaysCountByObject(objectId: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(payment_amount), 0.0) FROM worker_days WHERE worker_id = :workerId")
    fun getTotalSalaryByWorker(workerId: String): Flow<Double>

    @Query("SELECT COUNT(*) FROM worker_days WHERE worker_id = :workerId AND status IN ('WORKED', 'HALF_DAY')")
    fun getWorkedDaysCountByWorker(workerId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkerDay(workerDay: WorkerDayEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkerDaysBatch(workerDays: List<WorkerDayEntity>)

    @Update
    suspend fun updateWorkerDay(workerDay: WorkerDayEntity)

    @Delete
    suspend fun deleteWorkerDay(workerDay: WorkerDayEntity)
}

@Dao
interface DailyBonusDao {
    @Query("SELECT * FROM daily_bonuses WHERE worker_id = :workerId ORDER BY date DESC")
    fun getBonusesByWorker(workerId: String): Flow<List<DailyBonusEntity>>

    @Query("SELECT * FROM daily_bonuses WHERE worker_day_id = :workerDayId LIMIT 1")
    fun getBonusByDay(workerDayId: String): Flow<DailyBonusEntity?>

    @Query("""
        SELECT COALESCE(SUM(db.amount), 0.0) FROM daily_bonuses db
        INNER JOIN workers w ON db.worker_id = w.id
        WHERE w.object_id = :objectId
    """)
    fun getTotalDailyBonusesByObject(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM daily_bonuses WHERE worker_id = :workerId")
    fun getTotalDailyBonusesByWorker(workerId: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBonus(bonus: DailyBonusEntity)

    @Update
    suspend fun updateBonus(bonus: DailyBonusEntity)

    @Delete
    suspend fun deleteBonus(bonus: DailyBonusEntity)
}

@Dao
interface GeneralBonusDao {
    @Query("SELECT * FROM general_bonuses WHERE object_id = :objectId ORDER BY date DESC")
    fun getBonusesByObject(objectId: String): Flow<List<GeneralBonusEntity>>

    @Query("SELECT * FROM general_bonuses WHERE worker_id = :workerId ORDER BY date DESC")
    fun getBonusesByWorker(workerId: String): Flow<List<GeneralBonusEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM general_bonuses WHERE object_id = :objectId")
    fun getTotalGeneralBonusesByObject(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM general_bonuses WHERE worker_id = :workerId")
    fun getTotalGeneralBonusesByWorker(workerId: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBonus(bonus: GeneralBonusEntity)

    @Update
    suspend fun updateBonus(bonus: GeneralBonusEntity)

    @Delete
    suspend fun deleteBonus(bonus: GeneralBonusEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE object_id = :objectId ORDER BY date DESC, created_at DESC")
    fun getExpensesByObject(objectId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE object_id = :objectId AND date = :date")
    fun getExpensesByObjectAndDate(objectId: String, date: String): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE object_id = :objectId")
    fun getTotalExpenseByObject(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE (payer_object_id = :objectId) OR (payer_object_id IS NULL AND object_id = :objectId)")
    fun getTotalCashExpensePaidByObject(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE payer_object_id = :objectId AND object_id != :objectId")
    fun getTotalExpensesPaidForOtherObjects(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE object_id = :objectId AND payer_object_id IS NOT NULL AND payer_object_id != :objectId")
    fun getTotalExpensesPaidByOtherObjects(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE object_id = :objectId AND category = :category")
    fun getExpenseSumByCategory(objectId: String, category: String): Flow<Double>

    @Query("SELECT category, SUM(amount) as totalAmount FROM expenses WHERE object_id = :objectId GROUP BY category")
    fun getCategoryBreakdowns(objectId: String): Flow<List<CategorySum>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

@Dao
interface MoneyTransactionDao {
    @Query("SELECT * FROM money_transactions WHERE object_id = :objectId ORDER BY date DESC, created_at DESC")
    fun getTransactionsByObject(objectId: String): Flow<List<MoneyTransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM money_transactions WHERE object_id = :objectId AND type = 'INCOME'")
    fun getTotalIncomeByObject(objectId: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: MoneyTransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: MoneyTransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: MoneyTransactionEntity)
}

@Dao
interface WorkerPaymentDao {
    @Query("SELECT * FROM worker_payments WHERE worker_id = :workerId ORDER BY date DESC, created_at DESC")
    fun getPaymentsByWorker(workerId: String): Flow<List<WorkerPaymentEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM worker_payments WHERE worker_id = :workerId")
    fun getTotalPaidByWorker(workerId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM worker_payments WHERE object_id = :objectId")
    fun getTotalPaidForWorkersOfObject(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM worker_payments WHERE (payer_object_id = :objectId) OR (payer_object_id IS NULL AND object_id = :objectId)")
    fun getTotalPaidByObject(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM worker_payments WHERE payer_object_id = :objectId AND object_id != :objectId")
    fun getTotalPaidForOtherObjectsWorkers(objectId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM worker_payments WHERE object_id = :objectId AND payer_object_id IS NOT NULL AND payer_object_id != :objectId")
    fun getTotalPaidByOtherObjectsForThisWorkers(objectId: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: WorkerPaymentEntity)

    @Update
    suspend fun updatePayment(payment: WorkerPaymentEntity)

    @Delete
    suspend fun deletePayment(payment: WorkerPaymentEntity)
}
