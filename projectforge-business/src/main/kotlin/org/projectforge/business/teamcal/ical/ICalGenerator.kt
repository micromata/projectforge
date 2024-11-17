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

package org.projectforge.business.teamcal.ical

import mu.KotlinLogging
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import org.projectforge.ProjectForgeVersion
import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.teamcal.event.model.TeamEventDO
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Writes events as ics.
 */
class ICalGenerator @JvmOverloads constructor(val exportVAlarms: Boolean = true, val editable: Boolean = false) {
    var empty: Boolean = true
        private set

    // Create a calendar object
    private val calendar = Calendar()
        .withProdId("-//${ProjectForgeVersion.APP_ID} ${ProjectForgeVersion.VERSION_NUMBER}//iCal Generator//EN")
        .withDefaults() // add(GREGORIAN).add(VERSION_2_0)

    /**
     * Ensures at least setup event.
     */
    fun ensureNotEmpty() {
        if (empty) {
            calendar.withComponent(VEventUtils.createSetupEvent())
        }
    }

    fun add(events: List<TeamEventDO>) {
        // Convert TeamEventDO to VEvent and add to calendar
        events.forEach { event ->
            add(event)
        }
    }

    fun add(event: ICalendarEvent) {
        val vEvent = VEventUtils.convertToVEvent(event)
        // Add event to calendar
        calendar.withComponent(vEvent)
        empty = false
    }

    fun addAllDayEvent(startDay: LocalDate, endDay: LocalDate?, title: String, uid: String): VEvent {
        val vEvent = VEventUtils.createAllDayEvent(startDay, endDay ?: startDay, title, uid)
        // Add event to calendar
        calendar.withComponent(vEvent)
        empty = false
        return vEvent
    }

    fun addEvent(startDate: Date, endDate: Date, title: String, uid: String): VEvent {
        val vEvent = VEventUtils.createEvent(startDate, endDate, title, uid)
        // Add event to calendar
        calendar.withComponent(vEvent)
        empty = false
        return vEvent
    }

    val asByteArray: ByteArray?
        get() {
            try {
                ByteArrayOutputStream().use { stream ->
                    val outputter = CalendarOutputter()
                    outputter.output(this.calendar.fluentTarget, stream)
                    return stream.toByteArray()
                }
            } catch (ex: IOException) {
                log.error(ex) { "Error while exporting calendar: ${ex.message}" }
                return null;
            }
        }

    fun writeToOutputStream(stream: OutputStream) {
        try {
            val outputter = CalendarOutputter()
            outputter.output(this.calendar.fluentTarget, stream);
        } catch (ex: IOException) {
            log.error(ex) { "Error while exporting calendar: ${ex.message}" }
        }
    }

    val asString: String
        get() = calendar.toString()

}
