package org.projectforge.rest.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.core.RestHelper
import java.time.Month
import java.time.ZonedDateTime
import java.util.*

class ZonedDateTimeHolderTest {
    private val restHelper = RestHelper()

    @Test
    fun beginOfWeek() {
        val date = parseDate("2019-03-31T23:00:00.000Z")
        checkDate(date.dateTime, 2019, Month.MARCH, 31, false)

        val beginOfDay = date.getBeginOfDay().dateTime
        checkDate(beginOfDay, 2019, Month.MARCH, 31, true)
        val endOfDay = date.getEndOfDay().dateTime
        checkDate(endOfDay, 2019, Month.APRIL, 2, true)

        val beginOfWeek = date.getBeginOfWeek().dateTime
        checkDate(beginOfWeek, 2019, Month.APRIL, 8, true)
        val endOfWeek = date.getEndOfWeek().dateTime
        checkDate(endOfWeek, 2019, Month.APRIL, 15, true)

        val beginOfMonth = date.getBeginOfMonth().dateTime
        checkDate(beginOfMonth, 2019, Month.APRIL, 1, true)
        val endOfMonth = date.getEndOfMonth().dateTime
        checkDate(endOfMonth, 2019, Month.MAY, 1, true)
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

    private fun parseDate(str: String): ZonedDateTimeHolder {
        val dateTime = restHelper.parseDateTime(str)
        return ZonedDateTimeHolder.from(dateTime)
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