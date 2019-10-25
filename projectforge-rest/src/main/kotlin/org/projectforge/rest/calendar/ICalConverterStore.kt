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

package org.projectforge.rest.calendar

import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import org.projectforge.rest.calendar.converter.*

import java.util.*

class ICalConverterStore private constructor() {

    private val vEventConverters: MutableMap<String, VEventComponentConverter>

    init {
        this.vEventConverters = HashMap()

        this.registerVEventConverters()
    }

    fun registerVEventConverter(name: String, converter: VEventComponentConverter) {
        if (this.vEventConverters.containsKey(name)) {
            throw IllegalArgumentException(String.format("A converter with name '%s' already exisits", name))
        }

        this.vEventConverters[name] = converter
    }

    fun getVEventConverter(name: String): VEventComponentConverter? {
        return this.vEventConverters[name]
    }

    private fun registerVEventConverters() {
        this.registerVEventConverter(VEVENT_DTSTART, DTStartConverter())
        this.registerVEventConverter(VEVENT_DTEND, DTEndConverter())
        this.registerVEventConverter(VEVENT_SUMMARY, SummaryConverter())
        this.registerVEventConverter(VEVENT_UID, UidConverter())
        this.registerVEventConverter(VEVENT_LOCATION, LocationConverter())
        this.registerVEventConverter(VEVENT_CREATED, CreatedConverter())
        this.registerVEventConverter(VEVENT_DTSTAMP, DTStampConverter())
        this.registerVEventConverter(VEVENT_LAST_MODIFIED, LastModifiedConverter())
        this.registerVEventConverter(VEVENT_SEQUENCE, SequenceConverter())
        this.registerVEventConverter(VEVENT_ORGANIZER, OrganizerConverter(false))
        this.registerVEventConverter(VEVENT_ORGANIZER_EDITABLE, OrganizerConverter(true))
        this.registerVEventConverter(VEVENT_TRANSP, TransparencyConverter())
        this.registerVEventConverter(VEVENT_ALARM, AlarmConverter())
        this.registerVEventConverter(VEVENT_DESCRIPTION, DescriptionConverter())
        this.registerVEventConverter(VEVENT_ATTENDEES, AttendeeConverter())
        this.registerVEventConverter(VEVENT_RRULE, RRuleConverter())
        this.registerVEventConverter(VEVENT_RECURRENCE_ID, RecurrenceIdConverter())
        this.registerVEventConverter(VEVENT_EX_DATE, ExDateConverter())
    }

    companion object {
        // TODO: Please do not delete, this is still required by the ICSParser.
        val TIMEZONE_REGISTRY = TimeZoneRegistryFactory.getInstance().createRegistry()!!

        const val VEVENT_DTSTART = "VEVENT_DTSTART"
        const val VEVENT_DTEND = "VEVENT_DTEND"
        const val VEVENT_SUMMARY = "VEVENT_SUMMARY"
        const val VEVENT_UID = "VEVENT_UID"
        const val VEVENT_LOCATION = "VEVENT_LOCATION"
        const val VEVENT_CREATED = "VEVENT_CREATED"
        const val VEVENT_DTSTAMP = "VEVENT_DTSTAMP"
        const val VEVENT_LAST_MODIFIED = "VEVENT_LAST_MODIFIED"
        const val VEVENT_SEQUENCE = "VEVENT_SEQUENCE"
        const val VEVENT_ORGANIZER = "VEVENT_ORGANIZER"
        const val VEVENT_ORGANIZER_EDITABLE = "VEVENT_ORGANIZER_EDITABLE"
        const val VEVENT_TRANSP = "VEVENT_TRANSP"
        const val VEVENT_ALARM = "VEVENT_VALARM"
        const val VEVENT_DESCRIPTION = "VEVENT_DESCRIPTION"
        const val VEVENT_ATTENDEES = "VEVENT_ATTENDEE"
        const val VEVENT_RRULE = "VEVENT_RRULE"
        const val VEVENT_RECURRENCE_ID = "VEVENT_RECURRENCE_ID"
        const val VEVENT_EX_DATE = "VEVENT_EX_DATE"

        val FULL_LIST: List<String> = ArrayList(
                listOf(VEVENT_DTSTART, VEVENT_DTEND, VEVENT_SUMMARY, VEVENT_UID, VEVENT_CREATED, VEVENT_LOCATION, VEVENT_DTSTAMP, VEVENT_LAST_MODIFIED, VEVENT_SEQUENCE, VEVENT_ORGANIZER, VEVENT_TRANSP, VEVENT_ALARM, VEVENT_DESCRIPTION, VEVENT_ATTENDEES, VEVENT_RRULE, VEVENT_RECURRENCE_ID, VEVENT_EX_DATE))

        val instance = ICalConverterStore()
    }
}
