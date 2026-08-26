package uz.buildflow.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import uz.buildflow.app.domain.model.*

@Entity(
    tableName = "objects",
    indices = [Index(value = ["status"])]
)
data class ObjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    @ColumnInfo(name = "total_price") val totalPrice: Double,
    val currency: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String?,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "workers",
    foreignKeys = [
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["object_id"]),
        Index(value = ["status"])
    ]
)
data class WorkerEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "object_id") val objectId: String,
    val name: String,
    val phone: String?,
    val position: String?,
    @ColumnInfo(name = "default_rate") val defaultRate: Double,
    val status: String,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "end_date") val endDate: String?,
    val notes: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "worker_days",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["worker_id", "date"], unique = true),
        Index(value = ["date"])
    ]
)
data class WorkerDayEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "worker_id") val workerId: String,
    val date: String,
    val status: String,
    @ColumnInfo(name = "payment_amount") val paymentAmount: Double,
    @ColumnInfo(name = "payment_status") val paymentStatus: String,
    val note: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "daily_bonuses",
    foreignKeys = [
        ForeignKey(
            entity = WorkerDayEntity::class,
            parentColumns = ["id"],
            childColumns = ["worker_day_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["worker_day_id"]),
        Index(value = ["worker_id"])
    ]
)
data class DailyBonusEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "worker_id") val workerId: String,
    @ColumnInfo(name = "worker_day_id") val workerDayId: String,
    val amount: Double,
    val date: String,
    val reason: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "general_bonuses",
    foreignKeys = [
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["object_id"]),
        Index(value = ["worker_id"])
    ]
)
data class GeneralBonusEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "worker_id") val workerId: String,
    @ColumnInfo(name = "object_id") val objectId: String,
    val amount: Double,
    val date: String,
    val reason: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["object_id"]),
        Index(value = ["payer_object_id"]),
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "object_id") val objectId: String,
    @ColumnInfo(name = "payer_object_id") val payerObjectId: String? = null,
    @ColumnInfo(name = "worker_id") val workerId: String?,
    val category: String,
    val amount: Double,
    val date: String,
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(
    tableName = "money_transactions",
    foreignKeys = [
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["object_id"]),
        Index(value = ["date"])
    ]
)
data class MoneyTransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "object_id") val objectId: String,
    val type: String,
    val amount: Double,
    val date: String,
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

@Entity(
    tableName = "worker_payments",
    foreignKeys = [
        ForeignKey(
            entity = WorkerEntity::class,
            parentColumns = ["id"],
            childColumns = ["worker_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ObjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["object_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["worker_id"]),
        Index(value = ["object_id"]),
        Index(value = ["payer_object_id"])
    ]
)
data class WorkerPaymentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "worker_id") val workerId: String,
    @ColumnInfo(name = "object_id") val objectId: String,
    @ColumnInfo(name = "payer_object_id") val payerObjectId: String? = null,
    val amount: Double,
    val date: String,
    @ColumnInfo(name = "payment_date") val paymentDate: String? = null,
    val type: String,
    val description: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long
)
