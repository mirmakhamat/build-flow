package uz.buildflow.app.domain.usecase

import androidx.room.withTransaction
import uz.buildflow.app.core.database.AppDatabase
import uz.buildflow.app.domain.model.MergeResult

class MergeWorkersUseCase(
    private val database: AppDatabase
) {
    suspend operator fun invoke(
        targetWorkerId: String,
        sourceWorkerIds: List<String>
    ): Result<MergeResult> = runCatching {
        val cleanSourceIds = sourceWorkerIds.filter { it != targetWorkerId }.distinct()
        if (cleanSourceIds.isEmpty()) {
            return@runCatching MergeResult(targetWorkerId, emptyList(), 0, 0, 0)
        }

        database.withTransaction {
            val workerDao = database.workerDao()
            val workerDayDao = database.workerDayDao()
            val dailyBonusDao = database.dailyBonusDao()
            val generalBonusDao = database.generalBonusDao()
            val expenseDao = database.expenseDao()
            val paymentDao = database.workerPaymentDao()

            val targetWorker = workerDao.getWorkerByIdDirect(targetWorkerId)
                ?: throw IllegalArgumentException("Asosiy ishchi topilmadi: $targetWorkerId")

            var updatedTargetWorker = targetWorker
            var daysMergedCount = 0
            var paymentsMergedCount = 0
            var bonusesMergedCount = 0

            for (sourceId in cleanSourceIds) {
                val sourceWorker = workerDao.getWorkerByIdDirect(sourceId) ?: continue

                // 1. Yetishmayotgan ma'lumotlarni to'ldirish (telefon, kasb, izoh)
                var targetModified = false
                if (updatedTargetWorker.phone.isNullOrBlank() && !sourceWorker.phone.isNullOrBlank()) {
                    updatedTargetWorker = updatedTargetWorker.copy(phone = sourceWorker.phone)
                    targetModified = true
                }
                if (updatedTargetWorker.position.isNullOrBlank() && !sourceWorker.position.isNullOrBlank()) {
                    updatedTargetWorker = updatedTargetWorker.copy(position = sourceWorker.position)
                    targetModified = true
                }
                if (updatedTargetWorker.notes.isNullOrBlank() && !sourceWorker.notes.isNullOrBlank()) {
                    updatedTargetWorker = updatedTargetWorker.copy(notes = sourceWorker.notes)
                    targetModified = true
                }
                if (targetModified) {
                    workerDao.updateWorker(updatedTargetWorker)
                }

                // 2. Obyektlarga biriktirishni ta'minlash (Asosiy ishchi ikkala obyektda ham qolishi uchun)
                workerDao.insertObjectWorker(
                    uz.buildflow.app.data.local.entity.ObjectWorkerCrossRefEntity(
                        objectId = updatedTargetWorker.objectId,
                        workerId = targetWorkerId
                    )
                )
                workerDao.insertObjectWorker(
                    uz.buildflow.app.data.local.entity.ObjectWorkerCrossRefEntity(
                        objectId = sourceWorker.objectId,
                        workerId = targetWorkerId
                    )
                )
                workerDao.reassignObjectWorkers(sourceId, targetWorkerId)

                // 3. Ish kunlarini birlashtirish (worker_days)
                val sourceDays = workerDayDao.getDaysListByWorker(sourceId)
                for (sourceDay in sourceDays) {
                    val dayObjId = sourceDay.objectId ?: sourceWorker.objectId
                    val targetDay = workerDayDao.getDayByWorkerAndDate(targetWorkerId, sourceDay.date)
                    if (targetDay == null) {
                        // Asosiy ishchida bu kunda yozuv yo'q -> to'g'ridan-to'g'ri bog'laymiz va obyektini saqlaymiz
                        workerDayDao.updateWorkerDay(
                            sourceDay.copy(
                                workerId = targetWorkerId,
                                objectId = dayObjId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        daysMergedCount++
                    } else {
                        // Aynan bir kunda ikkala profilda ham yozuv bor -> birlashtiramiz
                        // Avval kunlik bonuslarni yangi worker_day ga bog'laymiz (cascade delete bo'lmasligi uchun)
                        dailyBonusDao.reassignDailyBonusesByDay(
                            sourceDayId = sourceDay.id,
                            targetDayId = targetDay.id,
                            targetWorkerId = targetWorkerId
                        )

                        val mergedAmount = targetDay.paymentAmount + sourceDay.paymentAmount
                        val mergedStatus = if (targetDay.status == "WORKED" || sourceDay.status == "WORKED") {
                            "WORKED"
                        } else if (targetDay.status == "HALF_DAY" || sourceDay.status == "HALF_DAY") {
                            "HALF_DAY"
                        } else {
                            targetDay.status
                        }
                        val combinedNote = listOfNotNull(targetDay.note, sourceDay.note)
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString("; ")
                            .ifBlank { null }

                        workerDayDao.updateWorkerDay(
                            targetDay.copy(
                                paymentAmount = mergedAmount,
                                status = mergedStatus,
                                note = combinedNote,
                                objectId = targetDay.objectId ?: dayObjId,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        // Dublikat kunni o'chiramiz
                        workerDayDao.deleteWorkerDayById(sourceDay.id)
                        daysMergedCount++
                    }
                }

                // 4. Qolgan kunlik bonuslarni qayta bog'lash
                val dailyBonuses = dailyBonusDao.getBonusesListByWorker(sourceId)
                bonusesMergedCount += dailyBonuses.size
                dailyBonusDao.reassignDailyBonuses(sourceId, targetWorkerId)

                // 5. Umumiy bonuslarni qayta bog'lash
                val generalBonuses = generalBonusDao.getBonusesListByWorker(sourceId)
                bonusesMergedCount += generalBonuses.size
                generalBonusDao.reassignGeneralBonuses(sourceId, targetWorkerId)

                // 6. Ishchiga berilgan to'lovlar va avanslarni qayta bog'lash
                val payments = paymentDao.getPaymentsListByWorker(sourceId)
                paymentsMergedCount += payments.size
                paymentDao.reassignPayments(sourceId, targetWorkerId)

                // 7. Xarajatlar jadvalida ishchi bog'langan bo'lsa qayta yo'naltirish
                expenseDao.reassignWorkerExpenses(sourceId, targetWorkerId)

                // 8. Dublikat ishchi yozuvini xavfsiz o'chirish
                workerDao.deleteWorkerById(sourceId)
            }

            MergeResult(
                targetWorkerId = targetWorkerId,
                mergedWorkerIds = cleanSourceIds,
                daysMergedCount = daysMergedCount,
                paymentsMergedCount = paymentsMergedCount,
                bonusesMergedCount = bonusesMergedCount
            )
        }
    }
}
