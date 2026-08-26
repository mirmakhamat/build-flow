package uz.buildflow.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = 4,
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

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `expense_categories` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `is_default` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_categories_name` ON `expense_categories` (`name`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expenses` ADD COLUMN `payer_object_id` TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_payer_object_id` ON `expenses` (`payer_object_id`)")

                db.execSQL("ALTER TABLE `worker_payments` ADD COLUMN `payer_object_id` TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_payments_payer_object_id` ON `worker_payments` (`payer_object_id`)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `worker_payments` ADD COLUMN `payment_date` TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
            }
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "buildflow.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_1_3, MIGRATION_1_4)
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
