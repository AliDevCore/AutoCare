package ir.alidevcore.autocare

import ir.alidevcore.autocare.notification.ReminderScheduler
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ReminderSchedulerTest {

    @Test
    fun testComputeTriggerTimeCalculatesExactly9AMOnTargetDay() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.OCTOBER, 25, 14, 30, 0)
        val targetDateMs = cal.timeInMillis

        // Reminder 3 days before
        val triggerTime = ReminderScheduler.computeTriggerTime(targetDateMs, daysBefore = 3)

        val triggerCal = Calendar.getInstance()
        triggerCal.timeInMillis = triggerTime

        assertEquals(2026, triggerCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, triggerCal.get(Calendar.MONTH))
        assertEquals(22, triggerCal.get(Calendar.DAY_OF_MONTH)) // 25 - 3 = 22
        assertEquals(9, triggerCal.get(Calendar.HOUR_OF_DAY))   // exactly 9 AM
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertEquals(0, triggerCal.get(Calendar.SECOND))
    }

    @Test
    fun testComputeTriggerTimeSameDayReminderAt9AM() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.NOVEMBER, 10, 18, 0, 0)
        val targetDateMs = cal.timeInMillis

        // Reminder on the exact due day (daysBefore = 0)
        val triggerTime = ReminderScheduler.computeTriggerTime(targetDateMs, daysBefore = 0)

        val triggerCal = Calendar.getInstance()
        triggerCal.timeInMillis = triggerTime

        assertEquals(10, triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, triggerCal.get(Calendar.HOUR_OF_DAY))
    }
}
