package uz.buildflow.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.buildflow.app.core.notification.AttendanceReminderScheduler
import java.util.Calendar

class AttendanceReminderTest {

    @Test
    fun testWorkdaySetLogic() {
        val defaultWorkdays = setOf(
            Calendar.MONDAY,
            Calendar.TUESDAY,
            Calendar.WEDNESDAY,
            Calendar.THURSDAY,
            Calendar.FRIDAY,
            Calendar.SATURDAY
        )

        assertEquals(6, defaultWorkdays.size)
        assertTrue(defaultWorkdays.contains(Calendar.MONDAY))
        assertTrue(defaultWorkdays.contains(Calendar.SATURDAY))
        assertTrue(!defaultWorkdays.contains(Calendar.SUNDAY))
    }

    @Test
    fun testMultiTimeCalculation() {
        val times = listOf("09:00", "18:00", "21:00")
        val days = setOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY)

        val nextCal = AttendanceReminderScheduler.calculateNextTriggerTime(times, days)
        assertNotNull(nextCal)
        assertTrue(nextCal.timeInMillis > System.currentTimeMillis() - 1000)
        assertTrue(days.contains(nextCal.get(Calendar.DAY_OF_WEEK)))
    }
}
