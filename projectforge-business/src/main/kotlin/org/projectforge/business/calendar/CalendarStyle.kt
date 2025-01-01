/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.cache.AbstractCache
import java.awt.Color

class CalendarStyle(
  baseBackgroundColor: String? = null,
) {
  /**
   * Base background color. Don't use this property as color, use backgroundColor instead.
   */
  var bgColor: String = baseBackgroundColor ?: "#777"

  /**
   * Color will be calculated from bgColor.
   */
  fun getTextColor(colorScheme: CalendarEventColorScheme?): String {
    return colorCache.getTextColor(bgColor, colorScheme)
  }

  /**
   * Color will be calculated from bgColor (bgColor with alpha value):
   * #777 -> #7773, #777777 -> #77777733
   * For classical mode, the background color is returned unmodified.
   */
  fun getBackgroundColor(calendarEventColorScheme: CalendarEventColorScheme?): String {
    if (calendarEventColorScheme == CalendarEventColorScheme.CLASSIC) {
      return bgColor
    }
    return if (bgColor.length == 4) "${bgColor}3" else "${bgColor}33"
  }

  internal class RGB(val r: Int, val g: Int, val b: Int)

  companion object {
    private val shortHandRegex = """([a-f\d])""".toRegex()
    private val hexRegex = """#([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})""".toRegex()

    private val shortHandPattern = """#[a-f\d]{3}""".toRegex()
    private val hexPattern = """#[a-f\d]{6}""".toRegex()

    private class ColorCache : AbstractCache() { // 1 hour expire time
      private var standardColorsMap: MutableMap<String, String> = mutableMapOf()
      private var classicColorsMap: MutableMap<String, String> = mutableMapOf()

      fun getTextColor(bgColor: String, colorScheme: CalendarEventColorScheme?): String {
        checkRefresh()
        val map = if (colorScheme == CalendarEventColorScheme.CLASSIC) classicColorsMap else standardColorsMap
        synchronized(map) {
          // if (!SystemStatus.isDevelopmentMode()) {
          map[bgColor]?.let { return it }
          // }
          val color = calculateTextColor(bgColor, colorScheme)
          map[bgColor] = color
          return color
        }
      }

      override fun refresh() {
        standardColorsMap = mutableMapOf()
        classicColorsMap = mutableMapOf()
      }
    }

    private val colorCache = ColorCache()

    fun validateHexCode(color: String): Boolean {
      return shortHandPattern.matches(color) || hexPattern.matches(color)
    }

    internal fun hexToRGB(color: String?): RGB {
      if (color == null) return RGB(0, 0, 0)
      val hexColor = if (color.length == 4) {
        shortHandRegex.replace(color.lowercase(), { m -> m.value + m.value })
      } else {
        color.lowercase()
      }
      val matchResult = hexRegex.find(hexColor)
      try {
        val (rh, gh, bh) = matchResult!!.destructured
        return RGB(rh.toInt(16), gh.toInt(16), bh.toInt(16))
      } catch (ex: Exception) {
        return RGB(0, 0, 0)
      }
    }

    private fun hexToColor(color: String?): Color {
      if (color == null) return Color(0, 0, 0)
      val hexColor = if (color.length == 4) {
        shortHandRegex.replace(color.lowercase(), { m -> m.value + m.value })
      } else {
        color.lowercase()
      }
      val matchResult = hexRegex.find(hexColor)
      return try {
        val (rh, gh, bh) = matchResult!!.destructured
        Color(rh.toInt(16), gh.toInt(16), bh.toInt(16))
      } catch (ex: Exception) {
        Color(0, 0, 0)
      }
    }

    fun getTextColor(backgroundColor: String?, colorScheme: CalendarEventColorScheme?): String {
      return colorCache.getTextColor(backgroundColor ?: "#000", colorScheme)
    }

    private fun calculateTextColor(backgroundColor: String?, colorScheme: CalendarEventColorScheme?): String {
      if (colorScheme == CalendarEventColorScheme.CLASSIC) {
        return if (dark(backgroundColor)) "#fff" else "#444"
      }
      val bgColor = hexToColor(backgroundColor)
      val hsbColor = Color.RGBtoHSB(bgColor.red, bgColor.green, bgColor.blue, null)
      val hue = hsbColor[0]
      val saturation = hsbColor[1]
      var brightness = hsbColor[2] - 0.6f
      if (brightness < 0.3f) {
        brightness = 0.3f
      }
      // if (hue > 0.0001 && saturation < 0.5) { // hue > 0.0001: Preserve gray colors for white.
      // saturation += 0.5f
      // }
      val color = Color(Color.HSBtoRGB(hue, saturation, brightness))
      return String.format("#%02x%02x%02x", color.red, color.green, color.blue)
    }

    private fun brightness(rgb: RGB): Int {
      return (rgb.r * 299 + rgb.g * 587 + rgb.b * 114) / 1000
    }

    private fun brightness(color: String?): Int {
      return brightness(hexToRGB(color))
    }

    fun dark(color: String?): Boolean {
      return brightness(color) < 180
    }
  }
}
