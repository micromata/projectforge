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

package org.projectforge.business.teamcal.event.ical

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.util.CompatibilityHints
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.teamcal.TeamCalConfig
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import java.io.StringReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import java.util.Date
import javax.swing.plaf.basic.BasicHTML.propertyKey

/**
 * Utility functions for working with iCalendar VEvent objects.
 */
object VEventUtils {
    init {
        // Set compatibility hints to ensure proper behavior
        CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true)
    }

    @JvmOverloads
    @JvmStatic
    fun convertToTeamEventDO(component: VEvent, storeOriginalIcsEntry: Boolean = true): TeamEventDO {
        return TeamEventDO().apply {
            if (storeOriginalIcsEntry) {
                originalIcsEntry = component.toString()
            }
            subject = component.summary?.orElse(null)?.value
            location = component.location?.orElse(null)?.value
            note = component.description?.orElse(null)?.value
            val startTemporal = component.getDateTimeStart<Temporal>()?.orElse(null)?.date
            val endTemporal = component.getDateTimeEnd<Temporal>()?.orElse(null)?.date
            val dtTemporal = component.getDateTimeStamp()?.orElse(null)?.date
            allDay = startTemporal is LocalDate
            startDate = temporalToUTCDate(startTemporal)
            endDate = temporalToUTCDate(endTemporal)
            dtStamp = temporalToUTCDate(dtTemporal)
            val rrule = component.getProperty<RRule<Temporal>>(Property.RRULE)?.orElse(null)
            recurrenceRule = rrule?.value
            recurrenceExDate = component.getProperty<ExDate<Temporal>>(Property.EXDATE)?.orElse(null)?.value
            organizer = component.organizer?.orElse(null)?.value
            organizerAdditionalParams = component.organizer?.orElse(null)?.getParameters()?.joinToString()
            sequence = component.sequence?.orElse(null)?.sequenceNo
            uid = component.uid?.orElse(null)?.value
        }
    }

    fun convertToVEvent(event: ICalendarEvent): VEvent {
        if (event is TeamEventDO) {
            event.originalIcsEntry?.let { ics ->
                val vEvent = parseVEventFromIcs(event.originalIcsEntry)
                if (vEvent != null) {
                    return vEvent
                }
            }
        }
        // Handle all-day events or date-time events
        val startDate = if (event.allDay) {
            PFDay.fromOrNow(event.startDate).localDate
        } else {
            PFDateTime.fromOrNow(event.startDate).dateTime
        }
        val endDate = if (event.allDay) {
            PFDay.fromOrNow(event.endDate).localDate
        } else {
            PFDateTime.fromOrNow(event.endDate).dateTime
        }
        val vEvent = VEvent(startDate, endDate, event.subject ?: "")
        setUid(vEvent, event.uid)
        val propList = vEvent.propertyList
        addLocation(vEvent, event.location)
        addDescription(vEvent, event.note)
        if (event is TeamEventDO) {
            event.organizer?.let {
                if (it.isNotBlank()) {
                    propList.add(Organizer(it).also { organizer ->
                        // event.organizerAdditionalParams?.let { organizer.add(Parameter(it)) }
                    })
                }
            }
            event.recurrenceRule?.let { rrule ->
                if (rrule.isNotBlank()) propList.add(RRule<Temporal>(rrule))
            }
        }
        return vEvent
    }

    fun parseVEventFromIcs(icsString: String?): VEvent? {
        if (icsString.isNullOrBlank()) {
            return null
        }
        val stringReader = StringReader(icsString)
        val calendarBuilder = CalendarBuilder()
        val calendar = calendarBuilder.build(stringReader)
        return calendar.getComponents<VEvent>(Component.VEVENT).firstOrNull() as? VEvent
    }

    fun createSetupEvent(): VEvent {
        val now = PFDateTime.now()
        val setupEvent = VEvent(now.dateTime, now.plusHours(1).dateTime, TeamCalConfig.SETUP_EVENT)
        setUid(setupEvent)
        return setupEvent
    }

    fun createAllDayEvent(startDay: LocalDate, endDay: LocalDate, title: String, uid: String? = null): VEvent {
        val event = VEvent(startDay, endDay, title)
        setUid(event, uid)
        return event
    }

    fun addDescription(event: VEvent, description: String?) {
        if (description.isNullOrBlank()) return
        event.propertyList.add(Description(description))
    }

    fun addLocation(event: VEvent, location: String?) {
        if (location.isNullOrBlank()) return
        event.propertyList.add(Location(location))
    }

    /**
     * Creates a new VEvent object with the given parameters.
     * Creates a [ZonedDateTime] object from the given [Date] objects by using the logged-in-user's timezone.
     * @param startDate The start date and time of the event.
     * @param endDate The end date and time of the event.
     * @param title The title of the event.
     * @param uid The UID of the event, or null to generate a new one.
     * @return The new VEvent object.
     */
    fun createEvent(startDate: Date, endDate: Date, title: String, uid: String? = null): VEvent {
        val startDateTime = PFDateTime.fromOrNow(startDate).dateTime
        val endDateTime = PFDateTime.fromOrNow(endDate).dateTime
        val event = VEvent(startDateTime, endDateTime, title)
        setUid(event, uid)
        return event
    }

    fun isSetupEvent(vEvent: VEvent): Boolean {
        return vEvent.summary?.orElse(null)?.value == TeamCalConfig.SETUP_EVENT
    }

    fun setUid(event: VEvent, uid: String? = null) {
        val propList = event.propertyList
        propList.add(Uid(uid ?: TeamCalConfig.get().createEventUid()))
    }

    /**
     * Converts a [Temporal] object to a [Date] object in UTC.
     * @param temporal The [Temporal] object to convert.
     * @return The [Date] object in UTC, or null if the [Temporal] object is not recognized.
     */
    fun temporalToUTCDate(temporal: Temporal?): Date? {
        return when (temporal) {
            is ZonedDateTime ->
                // Convert to UTC
                Date.from(temporal.withZoneSameInstant(ZoneOffset.UTC).toInstant())

            is LocalDateTime ->
                // Assume UTC and convert
                Date.from(temporal.atZone(ZoneOffset.UTC).toInstant())

            is LocalDate ->
                // Assume start of day (00:00) in UTC
                Date.from(temporal.atStartOfDay(ZoneOffset.UTC).toInstant())

            else -> null // Unknown type, return null
        }
    }

    fun getPropertyValue(event: VEvent, propertyKey: String): String? {
        return event.getProperty<Property>(propertyKey)?.orElse(null)?.value
    }

    fun getRRuleString(event: VEvent): String? {
        val rrule = event.getProperty<RRule<Temporal>>(Property.RRULE)?.orElse(null)
        return rrule?.value
    }


    @JvmStatic
    fun temporalToUtcIsoString(temporal: Temporal?): String? {
        return when (temporal) {
            is ZonedDateTime -> PFDateTime.from(temporal).isoString
            is LocalDateTime -> temporal.toString()
            is LocalDate -> temporal.toString()
            else -> null
        }
    }
}
