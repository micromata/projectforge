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

import net.fortuna.ical4j.model.*
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.property.ExDate
import org.projectforge.framework.calendar.ICal4JUtils
import org.projectforge.framework.time.DateHelper
import org.projectforge.rest.dto.CalEvent
import org.springframework.util.CollectionUtils

import java.util.ArrayList
import java.util.TimeZone

class ExDateConverter : PropertyConverter() {

    override fun toVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        if (!event.hasRecurrence || event.recurrenceExDate == null) {
            return false
        }

        val exDates = ICal4JUtils.parseCSVDatesAsICal4jDates(event.recurrenceExDate, !event.allDay, ICal4JUtils.getUTCTimeZone())

        if (CollectionUtils.isEmpty(exDates)) {
            return false
        }

        for (date in exDates!!) {
            val dateList: DateList
            if (event.allDay) {
                dateList = DateList(Value.DATE)
            } else {
                dateList = DateList()
                dateList.isUtc = true
            }

            dateList.add(date)
            val exDate: ExDate
            exDate = ExDate(dateList)
            vEvent.properties.add(exDate)
        }

        return true
    }

    override fun fromVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        val exDateProperties = vEvent.getProperties(Property.EXDATE)

        if (exDateProperties != null) {
            val isAllDay = this.isAllDay(vEvent)

            val exDateList = ArrayList<String>()
            exDateProperties.forEach { exDateProp ->
                // find timezone of exdate
                val tzidParam = exDateProp.getParameter(Parameter.TZID)
                var timezone: TimeZone? = null
                if (tzidParam != null && tzidParam.value != null) {
                    timezone = TimeZone.getTimeZone(tzidParam.value)
                }

                if (timezone == null) {
                    // ical4j uses the configured default timezone while parsing the ics file
                    timezone = TimeZone.getDefault()
                }

                // parse ExDate with inherent timezone
                val exDate = ICal4JUtils.parseICalDateString(exDateProp.value, timezone)

                // add ExDate in UTC to list
                exDateList.add(ICal4JUtils.asICalDateString(exDate, DateHelper.UTC, isAllDay))
            }

            if (exDateList.isEmpty()) {
                event.recurrenceExDate = null
            } else {
                event.recurrenceExDate = exDateList.joinToString(",")
            }
        } else {
            event.recurrenceExDate = null
        }

        return true
    }
}
