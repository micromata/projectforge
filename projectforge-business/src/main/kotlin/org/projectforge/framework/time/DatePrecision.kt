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
package org.projectforge.framework.time

import java.time.ZonedDateTime

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
enum class DatePrecision {
    /** Precision millisecond (nanos will be a multiplier of thousands).  */
    MILLISECOND {
        override fun ensurePrecision(dateTime: ZonedDateTime): ZonedDateTime {
            val nanos = dateTime.nano / 1000 * 1000
            return dateTime.withNano(nanos)
        }
    },
    /** Nanos/milliseconds will be set to zero.  */
    SECOND {
        override fun ensurePrecision(dateTime: ZonedDateTime): ZonedDateTime {
            return dateTime.withNano(0)
        }
    },
    /** Milliseconds and seconds will be set to zero.  */
    MINUTE {
        override fun ensurePrecision(dateTime: ZonedDateTime): ZonedDateTime {
            return SECOND.ensurePrecision(dateTime).withSecond(0)
        }
    },
    /** Milliseconds and seconds will be set to zero, minutes to 0, 5, 10, 15 etc.  */
    MINUTE_5 {
        override fun ensurePrecision(dateTime: ZonedDateTime): ZonedDateTime {
            val minute = dateTime.minute
            var i = 3
            var newMinute = minute
            while (i < 60) {
                if (minute < i) {
                    newMinute = i - 3
                    break
                }
                i += 5
            }
            if (newMinute > 57) {
                return MINUTE.ensurePrecision(dateTime).withMinute(0).plusHours(1)
            }
            return MINUTE.ensurePrecision(dateTime).withMinute(newMinute)
        }
    },
    /**
     * Milliseconds and seconds will be set to zero, minutes to 0, 15, 30 or 45.
     */
    MINUTE_15 {
        override fun ensurePrecision(dateTime: ZonedDateTime): ZonedDateTime {
            val dt = MINUTE.ensurePrecision(dateTime)
            val minute = dt.minute
            if (minute < 8) {
               return dt.withMinute(0)
            } else if (minute < 23) {
                return dt.withMinute(15)
            } else if (minute < 38) {
                return dt.withMinute(30)
            } else if (minute < 53) {
                return dt.withMinute(45)
            } else {
                return dt.withMinute(0).plusHours(1)
            }
        }
    },
    /** Milliseconds, seconds and minutes will be set to zero.  */
    HOUR_OF_DAY {
        override fun ensurePrecision(dateTime: ZonedDateTime): ZonedDateTime {
            return MINUTE.ensurePrecision(dateTime).withMinute(0)
        }
    },
    /** Milliseconds, seconds, minutes and hours will be set to zero.  */
    DAY {
        override fun ensurePrecision(dateTime: ZonedDateTime): ZonedDateTime {
            return HOUR_OF_DAY.ensurePrecision(dateTime).withHour(0)
        }
    };

    abstract fun ensurePrecision(dateTime: ZonedDateTime): ZonedDateTime
}
