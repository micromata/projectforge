package org.projectforge.rest.calendar

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CalendarStyleTest {
    @Test
    fun hexToRGBTest() {
        checkRGB(0, 17, 34, CalendarStyle.hexToRGB("#012"))
        checkRGB(0, 17, 34, CalendarStyle.hexToRGB("#001122"))
        checkRGB(163, 39, 255, CalendarStyle.hexToRGB("#a327ff"))
    }

    @Test
    fun validateHexCodeTest() {
        assertTrue(CalendarStyle.validateHexCode("#123"))
        assertTrue(CalendarStyle.validateHexCode("#0ff"))
        assertTrue(CalendarStyle.validateHexCode("#123456"))
        assertTrue(CalendarStyle.validateHexCode("#a789bc"))

        assertFalse(CalendarStyle.validateHexCode("123"))
        assertFalse(CalendarStyle.validateHexCode("abc123"))
        assertFalse(CalendarStyle.validateHexCode("#12"))
        assertFalse(CalendarStyle.validateHexCode("#1"))
        assertFalse(CalendarStyle.validateHexCode("#12345"))
        assertFalse(CalendarStyle.validateHexCode("#1234567"))
        assertFalse(CalendarStyle.validateHexCode("#gff"))
    }

    private fun checkRGB(r:Int, g:Int, b:Int, rgb: CalendarStyle.RGB) {
        assertEquals(r, rgb.r)
        assertEquals(g, rgb.g)
        assertEquals(b, rgb.b)
    }
}
