package uz.buildflow.app.domain.model

enum class ObjectStatus {
    PLANNED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

enum class WorkerStatus {
    ACTIVE,
    INACTIVE,
    FIRED
}

enum class AttendanceStatus {
    WORKED,      // To'liq ishladi
    HALF_DAY,    // Yarim kun ishladi
    ABSENT,      // Kelmadi
    SICK,        // Kasal
    LEAVE,       // Javob olgan
    OTHER        // Boshqa
}

enum class PaymentStatus {
    UNPAID,
    PAID
}

enum class ExpenseCategory(val displayName: String) {
    TRANSPORT("Yo'l kira"),
    MATERIAL("Material"),
    FOOD("Ovqatlanish"),
    EQUIPMENT("Uskuna va ijara"),
    OTHER("Boshqa xarajat")
}

enum class TransactionType {
    INCOME,       // Mijozdan tushgan pul
    REFUND,       // Qaytarilgan pul
    ADJUSTMENT    // Tuzatish
}

enum class PaymentType {
    SALARY,       // Ish haqi to'lovi
    ADVANCE,      // Avans
    BONUS_PAYOUT, // Bonus to'lovi
    OTHER         // Boshqa
}
