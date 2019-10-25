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

package org.projectforge.rest.calendar.converter

import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import org.projectforge.framework.time.DateHelper
import org.projectforge.rest.dto.CalEvent

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class RecurrenceIdConverter : PropertyConverter() {
    private val format: SimpleDateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss")
    private val formatInclZ: SimpleDateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")

    override fun toVEvent(event: CalEvent): Property? {
        // TODO
        return null
    }

    override fun fromVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        val recurrenceId = vEvent.recurrenceId ?: return false

        try {
            synchronized(format) {
                if (recurrenceId.timeZone != null) {
                    val timezone = TimeZone.getTimeZone(recurrenceId.timeZone.id)
                    format.timeZone = timezone ?: DateHelper.UTC
                    formatInclZ.timeZone = timezone ?: DateHelper.UTC

                    var date: Date? = null
                    try {
                        date = format.parse(recurrenceId.value)
                    } catch (e: ParseException) {
                        date = formatInclZ.parse(recurrenceId.value)
                    }

                    if (date != null) {
                        format.timeZone = DateHelper.UTC
                        event.recurrenceReferenceId = format.format(date)
                    }
                } else {
                    format.timeZone = DateHelper.UTC
                    event.recurrenceReferenceId = format.format(recurrenceId.date)
                }
            }
        } catch (e: Exception) {
            return false
        }

        return true
    }
}
