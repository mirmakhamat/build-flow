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
        ObjectWorkerCrossRefEntity::class,
        WorkerDayEntity::class,
        DailyBonusEntity::class,
        GeneralBonusEntity::class,
        ExpenseEntity::class,
        MoneyTransactionEntity::class,
        WorkerPaymentEntity::class,
        ExpenseCategoryEntity::class
    ],
    version = 6,
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. object_workers jadvalini yaratamiz
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `object_workers` (
                        `object_id` TEXT NOT NULL,
                        `worker_id` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`object_id`, `worker_id`),
                        FOREIGN KEY(`object_id`) REFERENCES `objects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`worker_id`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_object_workers_object_id` ON `object_workers` (`object_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_object_workers_worker_id` ON `object_workers` (`worker_id`)")

                // 2. worker_days jadvaliga object_id ustunini qo'shamiz
                db.execSQL("ALTER TABLE `worker_days` ADD COLUMN `object_id` TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_days_object_id` ON `worker_days` (`object_id`)")

                // 3. Mavjud workers ma'lumotlarini object_workers ga ko'chiramiz
                db.execSQL("""
                    INSERT OR IGNORE INTO `object_workers` (`object_id`, `worker_id`, `created_at`)
                    SELECT `object_id`, `id`, `created_at` FROM `workers` WHERE `object_id` IS NOT NULL
                """)

                // 4. Ishchilar to'lovlari yoki bonuslari orqali boshqa obyektlarga ham avtomatik biriktiramiz
                db.execSQL("""
                    INSERT OR IGNORE INTO `object_workers` (`object_id`, `worker_id`, `created_at`)
                    SELECT `object_id`, `worker_id`, `created_at` FROM `worker_payments` WHERE `object_id` IS NOT NULL AND `worker_id` IS NOT NULL
                """)
                db.execSQL("""
                    INSERT OR IGNORE INTO `object_workers` (`object_id`, `worker_id`, `created_at`)
                    SELECT `object_id`, `worker_id`, `created_at` FROM `general_bonuses` WHERE `object_id` IS NOT NULL AND `worker_id` IS NOT NULL
                """)

                // 5. worker_days.object_id ni to'ldiramiz
                db.execSQL("""
                    UPDATE `worker_days` 
                    SET `object_id` = (SELECT `object_id` FROM `workers` WHERE `workers`.`id` = `worker_days`.`worker_id`)
                    WHERE `object_id` IS NULL
                """)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ESKI MA'LUMOTLARNI TUZATISH: ilgari (eski kod bilan) boshqa obyektga
                // ko'chirilgan ishchilar, agar shu obyektda kun/to'lov/bonus tarixi bo'lsa,
                // endi o'sha obyektda "doimiy" ko'rinib turishi uchun object_workers
                // jadvaliga qayta bog'lanadi - hech qanday summalar o'zgartirilmaydi,
                // faqat ishchining qaysi obyektlarga tegishli ekanligi tiklanadi.
                //
                // Baza v5 ga main branchdagi boshqa 4->5 migratsiya bilan kelgan bo'lishi mumkin
                // (object_workers jadvali va worker_days.object_id ustunisiz), shuning uchun
                // avval sxemani yetishmayotgan joylari bilan to'ldiramiz.
                ensureObjectWorkersSchema(db)

                db.execSQL("""
                    INSERT OR IGNORE INTO `object_workers` (`object_id`, `worker_id`, `created_at`)
                    SELECT `object_id`, `worker_id`, `created_at` FROM `worker_days`
                    WHERE `object_id` IS NOT NULL AND `worker_id` IS NOT NULL
                """)
                db.execSQL("""
                    INSERT OR IGNORE INTO `object_workers` (`object_id`, `worker_id`, `created_at`)
                    SELECT `object_id`, `worker_id`, `created_at` FROM `worker_payments`
                    WHERE `object_id` IS NOT NULL AND `worker_id` IS NOT NULL
                """)
                db.execSQL("""
                    INSERT OR IGNORE INTO `object_workers` (`object_id`, `worker_id`, `created_at`)
                    SELECT `object_id`, `worker_id`, `created_at` FROM `general_bonuses`
                    WHERE `object_id` IS NOT NULL AND `worker_id` IS NOT NULL
                """)
                db.execSQL("""
                    INSERT OR IGNORE INTO `object_workers` (`object_id`, `worker_id`, `created_at`)
                    SELECT `object_id`, `id`, `created_at` FROM `workers`
                    WHERE `object_id` IS NOT NULL
                """)
            }
        }

        private fun ensureObjectWorkersSchema(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `object_workers` (
                    `object_id` TEXT NOT NULL,
                    `worker_id` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    PRIMARY KEY(`object_id`, `worker_id`),
                    FOREIGN KEY(`object_id`) REFERENCES `objects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`worker_id`) REFERENCES `workers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_object_workers_object_id` ON `object_workers` (`object_id`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_object_workers_worker_id` ON `object_workers` (`worker_id`)")

            val hasObjectIdColumn = db.query("PRAGMA table_info(`worker_days`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                var found = false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "object_id") {
                        found = true
                        break
                    }
                }
                found
            }
            if (!hasObjectIdColumn) {
                db.execSQL("ALTER TABLE `worker_days` ADD COLUMN `object_id` TEXT DEFAULT NULL")
            }
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_worker_days_object_id` ON `worker_days` (`object_id`)")
            db.execSQL("""
                UPDATE `worker_days`
                SET `object_id` = (SELECT `object_id` FROM `workers` WHERE `workers`.`id` = `worker_days`.`worker_id`)
                WHERE `object_id` IS NULL
            """)
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

        val MIGRATION_1_5 = object : Migration(1, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_2_5 = object : Migration(2, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_3_5 = object : Migration(3, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
            }
        }

        val MIGRATION_1_6 = object : Migration(1, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_1_2.migrate(db)
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        val MIGRATION_2_6 = object : Migration(2, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_2_3.migrate(db)
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        val MIGRATION_3_6 = object : Migration(3, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_3_4.migrate(db)
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        val MIGRATION_4_6 = object : Migration(4, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                MIGRATION_4_5.migrate(db)
                MIGRATION_5_6.migrate(db)
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "buildflow.db"
                )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                    MIGRATION_1_3, MIGRATION_1_4, MIGRATION_1_5,
                    MIGRATION_2_5, MIGRATION_3_5,
                    MIGRATION_1_6, MIGRATION_2_6, MIGRATION_3_6, MIGRATION_4_6
                )
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

        fun closeAndResetInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (e: Exception) {
                    // Ignore
                }
                INSTANCE = null
            }
        }
    }
}
