package uz.buildflow.app.domain.usecase

import androidx.room.withTransaction
import uz.buildflow.app.core.database.AppDatabase
import uz.buildflow.app.data.local.entity.ObjectWorkerCrossRefEntity

/**
 * Ishchini bitta obyektdan ikkinchisiga "asosiy" obyekt sifatida ko'chiradi.
 *
 * Oddiy `UPDATE workers SET object_id = ...` yetarli emas: ishchining eski, object_id
 * yozilmagan (legacy) worker_days yozuvlari bor bo'lsa, ular so'rovlarda ishchining
 * JORIY object_id'siga qarab aniqlanadi - ya'ni ko'chirishdan keyin ular ham "yangi"
 * obyektga tegishli bo'lib qoladi va u yerdagi qarz noto'g'ri ko'payadi, eski obyektdagi
 * tarix esa yo'qolib qoladi. Shu sabab ko'chirishdan oldin bunday yozuvlarni eski
 * obyektga "muhrlab" qo'yamiz va ishchini eski obyektda ham ko'rinishda qoldiramiz.
 */
class TransferWorkerUseCase(
    private val database: AppDatabase
) {
    suspend operator fun invoke(workerId: String, newObjectId: String) {
        database.withTransaction {
            val workerDao = database.workerDao()
            val workerDayDao = database.workerDayDao()

            val worker = workerDao.getWorkerByIdDirect(workerId) ?: return@withTransaction
            val oldObjectId = worker.objectId
            if (oldObjectId == newObjectId) return@withTransaction

            workerDayDao.backfillNullObjectId(workerId, oldObjectId)

            workerDao.insertObjectWorker(
                ObjectWorkerCrossRefEntity(objectId = oldObjectId, workerId = workerId)
            )

            workerDao.updateWorker(worker.copy(objectId = newObjectId, updatedAt = System.currentTimeMillis()))
        }
    }
}
