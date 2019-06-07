/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.calendar

class CalendarStyle(var bgColor: String? = null, var fgColor: String? = null) {
    init {
        if (bgColor == null) {
            bgColor = "#777"
        }
        if (fgColor == null) {
            fgColor = if (black(bgColor)) "#666" else "#fff"
        }
    }

    internal class RGB(val r: Int, val g: Int, val b: Int)

    companion object {
        private val shortHandRegex = """([a-f\d])""".toRegex()
        private val hexRegex = """#([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})""".toRegex()

        private val shortHandPattern = """#[a-f\d]{3}""".toRegex()
        private val hexPattern = """#[a-f\d]{6}""".toRegex()

        fun validateHexCode(color: String): Boolean {
            return shortHandPattern.matches(color) || hexPattern.matches(color)
        }

        internal fun hexToRGB(color: String?): RGB {
            if (color == null) return RGB(0, 0, 0)
            val hexColor = if (color.length == 4) {
                shortHandRegex.replace(color.toLowerCase(), { m -> m.value + m.value })
            } else {
                color.toLowerCase()
            }
            val matchResult = hexRegex.find(hexColor)
            try {
                val (rh, gh, bh) = matchResult!!.destructured
                return RGB(rh.toInt(16), gh.toInt(16), bh.toInt(16))
            } catch (ex: Exception) {
                return RGB(0, 0, 0)
            }
        }

        private fun brightness(rgb: RGB): Int {
            return (rgb.r * 299 + rgb.g * 587 + rgb.b * 114) / 1000
        }

        private fun brightness(color: String?): Int {
            return brightness(hexToRGB(color))
        }

        fun black(color: String?): Boolean {
            return brightness(color) > 180
        }
    }
}
