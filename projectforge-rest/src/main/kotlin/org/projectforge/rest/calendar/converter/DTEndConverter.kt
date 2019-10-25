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

import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.DtEnd
import org.projectforge.framework.calendar.CalendarUtils
import org.projectforge.framework.calendar.ICal4JUtils

import java.sql.Timestamp

import org.projectforge.business.teamcal.event.ical.ICalConverterStore.TIMEZONE_REGISTRY
import org.projectforge.rest.dto.CalEvent

class DTEndConverter : PropertyConverter() {
    override fun toVEvent(event: CalEvent): Property {
        val date: net.fortuna.ical4j.model.Date

        if (event.allDay) {
            val endUtc = CalendarUtils.getUTCMidnightDate(event.endDate)
            val jodaTime = org.joda.time.DateTime(endUtc)
            // TODO sn should not be done
            // requires plus 1 because one day will be omitted by calendar.
            val fortunaEndDate = net.fortuna.ical4j.model.Date(jodaTime.plusDays(1).toDate())
            date = net.fortuna.ical4j.model.Date(fortunaEndDate.time)
        } else {
            date = DateTime(event.endDate!!)
            date.timeZone = TIMEZONE_REGISTRY.getTimeZone(event.creator?.timeZoneObject?.id)
        }

        return DtEnd(date)
    }

    override fun fromVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        val isAllDay = this.isAllDay(vEvent)

        if (vEvent.properties.getProperties(Property.DTEND).isEmpty()) {
            return false
        }

        if (isAllDay) {
            // TODO sn change behaviour to iCal standard
            val jodaTime = org.joda.time.DateTime(vEvent.endDate.date)
            val fortunaEndDate = net.fortuna.ical4j.model.Date(jodaTime.plusDays(-1).toDate())
            event.endDate = Timestamp(fortunaEndDate.time)
        } else {
            event.endDate = ICal4JUtils.getSqlTimestamp(vEvent.endDate.date)
        }

        return true
    }
}
