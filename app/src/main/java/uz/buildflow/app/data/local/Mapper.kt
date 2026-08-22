package uz.buildflow.app.data.local

import uz.buildflow.app.data.local.entity.*
import uz.buildflow.app.domain.model.*

fun ObjectEntity.toDomain(): BuildObject = BuildObject(
    id = id,
    name = name,
    description = description,
    totalPrice = totalPrice,
    currency = currency,
    startDate = startDate,
    endDate = endDate,
    status = ObjectStatus.valueOf(status),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BuildObject.toEntity(): ObjectEntity = ObjectEntity(
    id = id,
    name = name,
    description = description,
    totalPrice = totalPrice,
    currency = currency,
    startDate = startDate,
    endDate = endDate,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorkerEntity.toDomain(): Worker = Worker(
    id = id,
    objectId = objectId,
    name = name,
    phone = phone,
    position = position,
    defaultRate = defaultRate,
    status = WorkerStatus.valueOf(status),
    startDate = startDate,
    endDate = endDate,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Worker.toEntity(): WorkerEntity = WorkerEntity(
    id = id,
    objectId = objectId,
    name = name,
    phone = phone,
    position = position,
    defaultRate = defaultRate,
    status = status.name,
    startDate = startDate,
    endDate = endDate,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorkerDayEntity.toDomain(): WorkerDay = WorkerDay(
    id = id,
    workerId = workerId,
    date = date,
    status = AttendanceStatus.valueOf(status),
    paymentAmount = paymentAmount,
    paymentStatus = PaymentStatus.valueOf(paymentStatus),
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorkerDay.toEntity(): WorkerDayEntity = WorkerDayEntity(
    id = id,
    workerId = workerId,
    date = date,
    status = status.name,
    paymentAmount = paymentAmount,
    paymentStatus = paymentStatus.name,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun DailyBonusEntity.toDomain(): DailyBonus = DailyBonus(
    id = id,
    workerId = workerId,
    workerDayId = workerDayId,
    amount = amount,
    date = date,
    reason = reason,
    createdAt = createdAt
)

fun DailyBonus.toEntity(): DailyBonusEntity = DailyBonusEntity(
    id = id,
    workerId = workerId,
    workerDayId = workerDayId,
    amount = amount,
    date = date,
    reason = reason,
    createdAt = createdAt
)

fun GeneralBonusEntity.toDomain(): GeneralBonus = GeneralBonus(
    id = id,
    workerId = workerId,
    objectId = objectId,
    amount = amount,
    date = date,
    reason = reason,
    createdAt = createdAt
)

fun GeneralBonus.toEntity(): GeneralBonusEntity = GeneralBonusEntity(
    id = id,
    workerId = workerId,
    objectId = objectId,
    amount = amount,
    date = date,
    reason = reason,
    createdAt = createdAt
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    objectId = objectId,
    workerId = workerId,
    category = category,
    amount = amount,
    date = date,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    objectId = objectId,
    workerId = workerId,
    category = category,
    amount = amount,
    date = date,
    description = description,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ExpenseCategoryEntity.toDomain(): ExpenseCategoryItem = ExpenseCategoryItem(
    id = id,
    name = name,
    isDefault = isDefault,
    createdAt = createdAt
)

fun ExpenseCategoryItem.toEntity(): ExpenseCategoryEntity = ExpenseCategoryEntity(
    id = id,
    name = name,
    isDefault = isDefault,
    createdAt = createdAt
)

fun MoneyTransactionEntity.toDomain(): MoneyTransaction = MoneyTransaction(
    id = id,
    objectId = objectId,
    type = TransactionType.valueOf(type),
    amount = amount,
    date = date,
    description = description,
    createdAt = createdAt
)

fun MoneyTransaction.toEntity(): MoneyTransactionEntity = MoneyTransactionEntity(
    id = id,
    objectId = objectId,
    type = type.name,
    amount = amount,
    date = date,
    description = description,
    createdAt = createdAt
)

fun WorkerPaymentEntity.toDomain(): WorkerPayment = WorkerPayment(
    id = id,
    workerId = workerId,
    objectId = objectId,
    amount = amount,
    date = date,
    type = PaymentType.valueOf(type),
    description = description,
    createdAt = createdAt
)

fun WorkerPayment.toEntity(): WorkerPaymentEntity = WorkerPaymentEntity(
    id = id,
    workerId = workerId,
    objectId = objectId,
    amount = amount,
    date = date,
    type = type.name,
    description = description,
    createdAt = createdAt
)
