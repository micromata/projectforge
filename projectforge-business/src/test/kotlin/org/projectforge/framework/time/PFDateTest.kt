package org.projectforge.framework.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Month
import java.util.*

class PFDateTest {

    @Test
    fun convertTest() {
        // User's time zone is "Europe/Berlin": "UTC+2". Therefore local date should be 2019-04-01 00:00:00
        var date = PFDate.from(LocalDate.of(2019, Month.APRIL, 10))

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
        formatter.timeZone = TimeZone.getTimeZone("UTC")

        var sqlDate = date.asSqlDate()
        assertEquals("2019-04-10", sqlDate.toString())

        date = PFDate.from(sqlDate)
        checkDate(date.date, 2019, Month.APRIL, 10)
    }

    private fun checkDate(date: LocalDate, year: Int, month: Month, dayOfMonth: Int) {
        assertEquals(year, date.year, "Year check failed.")
        assertEquals(month, date.month, "Month check failed.")
        assertEquals(dayOfMonth, date.dayOfMonth, "Day check failed.")
    }
}