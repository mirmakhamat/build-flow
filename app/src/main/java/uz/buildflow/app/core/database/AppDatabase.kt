package uz.buildflow.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.buildflow.app.data.local.dao.*
import uz.buildflow.app.data.local.entity.*

@Database(
    entities = [
        ObjectEntity::class,
        WorkerEntity::class,
        WorkerDayEntity::class,
        DailyBonusEntity::class,
        GeneralBonusEntity::class,
        ExpenseEntity::class,
        MoneyTransactionEntity::class,
        WorkerPaymentEntity::class,
        ExpenseCategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun objectDao(): ObjectDao
    abstract fun workerDao(): WorkerDao
    abstract fun workerDayDao(): WorkerDayDao
    abstract fun dailyBonusDao(): DailyBonusDao
    abstract fun generalBonusDao(): GeneralBonusDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun moneyTransactionDao(): MoneyTransactionDao
    abstract fun workerPaymentDao(): WorkerPaymentDao
    abstract fun expenseCategoryDao(): ExpenseCategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "buildflow.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val defaultCategories = listOf(
                                ExpenseCategoryEntity("cat_transport", "Yo'l kira", true),
                                ExpenseCategoryEntity("cat_material", "Material", true),
                                ExpenseCategoryEntity("cat_food", "Ovqatlanish", true),
                                ExpenseCategoryEntity("cat_equipment", "Uskuna va ijara", true),
                                ExpenseCategoryEntity("cat_other", "Boshqa xarajat", true)
                            )
                            getInstance(context).expenseCategoryDao().insertCategories(defaultCategories)
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
