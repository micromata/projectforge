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
import net.fortuna.ical4j.model.property.DtStamp
import org.projectforge.rest.dto.CalEvent

import java.sql.Timestamp

class DTStampConverter : PropertyConverter() {
    override fun toVEvent(event: CalEvent): Property? {
        val dtStampValue = DateTime(event.dtStamp!!)
        dtStampValue.isUtc = true
        return DtStamp(dtStampValue)
    }

    override fun fromVEvent(event: CalEvent, vEvent: VEvent): Boolean {
        if (vEvent.dateStamp != null) {
            event.dtStamp = Timestamp(vEvent.dateStamp.date.time)
            return true
        }

        return false
    }
}
