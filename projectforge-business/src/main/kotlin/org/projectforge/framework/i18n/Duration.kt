/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.i18n

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

/**
 * Duration formatted user friendly.
 *
 * Used i18n properties:
 * * duration.days={0} days
 * * duration.days.one=1 day
 * * duration.hours={0} hours
 * * duration.hours.one=1 hour
 * * duration.minutes={0} minutes
 * * duration.minutes.one=1 minute
 * * duration.seconds={0} seconds
 * * duration.seconds.one=1 second
 *
 */
object Duration {
  /**
   * @param durationMillis Duration of time in millis.
   * @param locale Locale to use for translation.
   * @param maxUnit If given (e. g. DAY then the highest unit used is days: "5 hours", "5 days", "720 days")
   * @return Duration message or an empty string, if no durationMillis is given.
   */
  @JvmStatic
  @JvmOverloads
  fun getMessage(durationMillis: Long?, locale: Locale? = null): String {
    durationMillis ?: return ""
    val sb = StringBuilder()
    var remaining = if (durationMillis < 0) {
      sb.append("-")
      BigDecimal(-durationMillis)
    } else {
      BigDecimal(durationMillis)
    }
    remaining = handleUnit(sb, Unit.DAY, remaining, locale)
    remaining = handleUnit(sb, Unit.HOUR, remaining, locale)
    remaining = handleUnit(sb, Unit.MINUTE, remaining, locale)
    handleUnit(sb, Unit.SECOND, remaining, locale)
    return sb.toString()
  }

  private fun handleUnit(sb: StringBuilder, unit: Unit, remaining: BigDecimal, locale: Locale?): BigDecimal {
    if (remaining < unit.bdMillis) {
      return remaining
    }
    val amount = remaining.divide(unit.bdMillis, 0, RoundingMode.DOWN)
    val intAmount = amount.toInt()
    if (sb.isNotEmpty()) {
      sb.append(" ")
    }
    if (intAmount == 1) {
      sb.append(translate(locale, "duration.${unit.unit}.one"))
    } else {
      sb.append(translateMsg(locale, "duration.${unit.unit}", intAmount))
    }
    return remaining.minus(amount.multiply(unit.bdMillis))
  }

  private enum class Unit(val millis: Int, val unit: String) {
    SECOND(1000, "seconds"),
    MINUTE(SECOND.millis * 60, "minutes"),
    HOUR(MINUTE.millis * 60, "hours"),
    DAY(HOUR.millis * 24, "days");

    var bdMillis = BigDecimal(millis)
  }


}
