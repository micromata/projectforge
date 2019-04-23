package org.projectforge.rest.calendar

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CalendarStyleTest {
    @Test
    fun hexToRGBTest() {
        checkRGB(0, 17, 34, CalendarStyle.hexToRGB("#012"))
        checkRGB(0, 17, 34, CalendarStyle.hexToRGB("#001122"))
        checkRGB(163, 39, 255, CalendarStyle.hexToRGB("#a327ff"))
    }

    private fun checkRGB(r:Int, g:Int, b:Int, rgb: CalendarStyle.RGB) {
        assertEquals(r, rgb.r)
        assertEquals(g, rgb.g)
        assertEquals(b, rgb.b)
    }
}
