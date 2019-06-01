package org.projectforge.framework.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.text.SimpleDateFormat
import java.time.DateTimeException
import java.time.Month
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class PFDateTimeTest {

    @Test
    fun beginAndEndOfIntervalsTest() {
        // User's time zone is "Europe/Berlin": "UTC+2". Therefore local date should be 2019-04-01 00:00:00
        val date = PFDateTime.parseUTCDate("2019-03-31 22:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!
        checkDate(date.dateTime, 2019, Month.APRIL, 1, false)

        val beginOfDay = date.getBeginOfDay().dateTime
        checkDate(beginOfDay, 2019, Month.APRIL, 1, true)
        val endOfDay = date.getEndOfDay().dateTime
        checkDate(endOfDay, 2019, Month.APRIL, 2, true)

        val beginOfWeek = date.getBeginOfWeek().dateTime
        checkDate(beginOfWeek, 2019, Month.APRIL, 1, true)
        val endOfWeek = date.getEndOfWeek().dateTime
        checkDate(endOfWeek, 2019, Month.APRIL, 8, true) // Midnight of first day of next week

        val beginOfMonth = date.getBeginOfMonth().dateTime
        checkDate(beginOfMonth, 2019, Month.APRIL, 1, true)
        val endOfMonth = date.getEndOfMonth().dateTime
        checkDate(endOfMonth, 2019, Month.MAY, 1, true) // Midnight of first day of next month
    }

    @Test
    fun convertTest() {
        // User's time zone is "Europe/Berlin": "UTC+2". Therefore local date should be 2019-04-01 00:00:00
        var date = PFDateTime.parseUTCDate("2019-03-31 22:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!

        var localDate = date.asLocalDate()
        assertEquals(2019, localDate.year)
        assertEquals(Month.APRIL, localDate.month)
        assertEquals(1, localDate.dayOfMonth)

        val utilDate = date.asUtilDate()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        assertEquals("2019-03-31 22:00:00 +0000", formatter.format(utilDate))
        assertEquals(1554069600000, utilDate.time)

        date = PFDateTime.parseUTCDate("2019-04-01 15:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))!!

        localDate = date.asLocalDate()
        assertEquals(2019, localDate.year)
        assertEquals(Month.APRIL, localDate.month)
        assertEquals(1, localDate.dayOfMonth)
    }

    @Test
    fun parseTest() {
        assertEquals("2019-03-31 22:00", PFDateTime.parseUTCDate("1554069600")!!.asIsoString())
        assertEquals("2019-03-31 22:00", PFDateTime.parseUTCDate("2019-03-31 22:00:00")!!.asIsoString())
        assertEquals("2019-03-31 22:00", PFDateTime.parseUTCDate("2019-03-31 22:00")!!.asIsoString())
        assertEquals("2019-03-31 22:00", PFDateTime.parseUTCDate("2019-03-31T22:00:00.000Z")!!.asIsoString())
        try {
            PFDateTime.parseUTCDate("2019-03-31")
            fail("Exception expected, because 2019-03-31 isn't parseable due to missing time of day.")
        } catch(ex: DateTimeException) {
            // OK
        }
    }

    private fun checkDate(date: ZonedDateTime, year: Int, month: Month, dayOfMonth: Int, checkMidnight: Boolean = true) {
        assertEquals(year, date.year, "Year check failed.")
        assertEquals(month, date.month, "Month check failed.")
        assertEquals(dayOfMonth, date.dayOfMonth, "Day check failed.")
        if (checkMidnight)
            checkMidnight(date)
    }

    private fun checkMidnight(date: ZonedDateTime) {
        checkTime(date, 0, 0, 0)
    }

    private fun checkTime(date: ZonedDateTime, hour: Int, minute: Int, second: Int) {
        assertEquals(hour, date.hour, "Hour check failed.")
        assertEquals(minute, date.minute)
        assertEquals(second, date.second)
        assertEquals(0, date.nano)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            ConfigXml(".")
            val user = PFUserDO()
            user.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"))
            ThreadLocalUserContext.setUserContext(UserContext(user, null))
        }
    }
}
