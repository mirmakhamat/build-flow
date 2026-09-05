package uz.buildflow.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
    fun testTimeFormat() {
        val hour = 18
        val minute = 5
        val formatted = String.format("%02d:%02d", hour, minute)
        assertEquals("18:05", formatted)
    }
}
