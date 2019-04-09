package org.projectforge.rest.calendar

import org.junit.jupiter.api.Test
import org.projectforge.rest.core.RestHelper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext

class LocalDateTimeHolderTest {
    private val restHelper = RestHelper()

    @Test
    fun beginOfWeek() {
        val date = parseDate("2019-04-07T23:00:00.000Z")
        val beginOfWeek = date.getStartOfWeek()
        assertEquals(7, beginOfWeek.dateTime.dayOfMonth)
    }

    private fun parseDate(str: String): LocalDateTimeHolder {
        return LocalDateTimeHolder.from(restHelper.parseDateTime(str))
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            ConfigXml(".")
        }
    }
}