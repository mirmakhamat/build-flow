package uz.buildflow.app.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDayItem(
    val dateIso: String,
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean
)

object DateUtil {
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("dd.MM.yyyy", Locale.US)
    private val readableMonthFormat = SimpleDateFormat("LLLL yyyy", Locale("uz"))
    private val fullDisplayFormat = SimpleDateFormat("d-MMMM yyyy", Locale("uz"))

    fun today(): String = isoFormat.format(Date())

    fun formatToDisplay(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            displayFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatToFullDisplay(isoDate: String): String {
        return try {
            val date = isoFormat.parse(isoDate) ?: return isoDate
            fullDisplayFormat.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun getMonthYearTitle(year: Int, month: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        return readableMonthFormat.format(calendar.time).replaceFirstChar { it.uppercase() }
    }

    fun getCalendarGrid(year: Int, month: Int): List<CalendarDayItem?> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val daysCount = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        // Java Calendar: SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        
        // Dushanba (1) dan Yakshanba (7) gacha qilish
        val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        val result = mutableListOf<CalendarDayItem?>()

        // Oldingi bo'sh kataklar
        for (i in 0 until startOffset) {
            result.add(null)
        }

        val todayIso = today()

        // Oyning kunlari
        for (day in 1..daysCount) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateIso = isoFormat.format(calendar.time)
            result.add(
                CalendarDayItem(
                    dateIso = dateIso,
                    dayNumber = day,
                    isCurrentMonth = true,
                    isToday = dateIso == todayIso
                )
            )
        }

        return result
    }
}
