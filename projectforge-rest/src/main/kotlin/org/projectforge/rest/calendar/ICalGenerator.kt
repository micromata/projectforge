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

import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import org.projectforge.framework.calendar.CalendarUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.*

import org.projectforge.rest.dto.CalEvent

class ICalGenerator {

    private var exportsVEvent: MutableList<String>? = null
    var calendar: Calendar? = null
    private var user: PFUserDO? = null
    private var locale: Locale? = null
    private var timeZone: TimeZone? = null
    private var method: Method? = null

    val calendarAsByteStream: ByteArrayOutputStream?
        get() {
            try {
                val stream = ByteArrayOutputStream()
                val outputter = CalendarOutputter()

                outputter.output(this.calendar!!, stream)

                return stream
            } catch (e: IOException) {
                log.error("Error while exporting calendar " + e.message)
                return null
            }

        }

    val isEmpty: Boolean
        get() = this.calendar!!.getComponents<CalendarComponent>(Component.VEVENT).isEmpty()

    init {
        this.exportsVEvent = ArrayList()

        // set user, timezone, locale
        this.user = ThreadLocalUserContext.getUser()
        this.timeZone = ThreadLocalUserContext.getTimeZone()
        this.locale = ThreadLocalUserContext.getLocale()

        this.reset()
    }

    fun setContext(user: PFUserDO, timeZone: TimeZone, locale: Locale): ICalGenerator {
        this.user = user
        this.timeZone = timeZone
        this.locale = locale

        return this
    }

    fun reset(): ICalGenerator {
        // creating a new calendar
        this.calendar = Calendar()
        calendar!!.properties.add(ProdId("-//" + user!!.shortDisplayName + "//ProjectForge//" + locale!!.toString().toUpperCase()))
        calendar!!.properties.add(Version.VERSION_2_0)
        calendar!!.properties.add(CalScale.GREGORIAN)

        // set time zone
        if (this.timeZone != null) {
            val timezone = ICalConverterStore.TIMEZONE_REGISTRY.getTimeZone(this.timeZone!!.id)
            calendar!!.components.add(timezone.vTimeZone)
        }

        // set method
        if (this.method != null) {
            calendar!!.properties.add(method)
        }

        return this
    }

    fun writeCalendarToOutputStream(stream: OutputStream) {
        try {
            val outputter = CalendarOutputter()
            outputter.output(this.calendar!!, stream)
        } catch (e: IOException) {
            log.error("Error while exporting calendar " + e.message)
        }

    }

    fun addEvent(event: CalEvent): ICalGenerator {
        val vEvent = this.convertVEvent(event)
        this.calendar!!.components.add(vEvent)
        return this
    }

    fun addEvent(vEvent: VEvent): ICalGenerator {
        this.calendar!!.components.add(vEvent)
        return this
    }

    fun addEvent(startDate: Date, endDate: Date, allDay: Boolean, summary: String, uid: String): ICalGenerator {
        this.calendar!!.components.add(this.convertVEvent(startDate, endDate, allDay, summary, uid))
        return this
    }

    fun convertVEvent(event: CalEvent): VEvent {
        val store = ICalConverterStore.instance

        // create vEvent
        val vEvent = VEvent(false)

        // set time zone
        if (this.timeZone != null) {
            val timezone = ICalConverterStore.TIMEZONE_REGISTRY.getTimeZone(this.timeZone!!.id)
            vEvent.properties.add(timezone.vTimeZone.timeZoneId)
        }

        for (export in this.exportsVEvent!!) {
            val converter = store.getVEventConverter(export)

            if (converter == null) {
                log.warn(String.format("No converter found for '%s', converter is skipped", export))
                continue
            }

            converter.toVEvent(event, vEvent)
        }

        return vEvent
    }

    fun convertVEvent(startDate: Date, endDate: Date, allDay: Boolean, summary: String, uid: String): VEvent {
        val vEvent = VEvent(false)
        val timezone = ICalConverterStore.TIMEZONE_REGISTRY.getTimeZone(timeZone!!.id)
        val fortunaStartDate: net.fortuna.ical4j.model.Date
        val fortunaEndDate: net.fortuna.ical4j.model.Date

        if (allDay) {
            val startUtc = CalendarUtils.getUTCMidnightDate(startDate)
            val endUtc = CalendarUtils.getUTCMidnightDate(endDate)
            fortunaStartDate = net.fortuna.ical4j.model.Date(startUtc)
            // TODO should not be done
            val jodaTime = org.joda.time.DateTime(endUtc)
            // requires plus 1 because one day will be omitted by calendar.
            fortunaEndDate = net.fortuna.ical4j.model.Date(jodaTime.plusDays(1).toDate())
        } else {
            fortunaStartDate = net.fortuna.ical4j.model.DateTime(startDate)
            fortunaStartDate.timeZone = timezone
            fortunaEndDate = net.fortuna.ical4j.model.DateTime(endDate)
            fortunaEndDate.timeZone = timezone
        }

        vEvent.properties.add(timezone.vTimeZone.timeZoneId)
        vEvent.properties.add(DtStart(fortunaStartDate))
        vEvent.properties.add(DtEnd(fortunaEndDate))

        vEvent.properties.add(Summary(summary))
        vEvent.properties.add(Uid(uid))

        return vEvent
    }

    fun editableVEvent(value: Boolean): ICalGenerator {
        exportVEventProperty(ICalConverterStore.VEVENT_ORGANIZER_EDITABLE, value)
        exportVEventProperty(ICalConverterStore.VEVENT_ORGANIZER, !value)
        return this
    }

    fun exportVEventAlarm(value: Boolean): ICalGenerator {
        return exportVEventProperty(ICalConverterStore.VEVENT_ALARM, value)
    }

    fun exportVEventAttendees(value: Boolean): ICalGenerator {
        return exportVEventProperty(ICalConverterStore.VEVENT_ATTENDEES, value)
    }

    fun doExportVEventProperty(value: String): ICalGenerator {
        return exportVEventProperty(value, true)
    }

    fun doNotExportVEventProperty(value: String): ICalGenerator {
        return exportVEventProperty(value, false)
    }

    fun exportVEventProperty(value: String, export: Boolean): ICalGenerator {
        if (export) {
            if (!this.exportsVEvent!!.contains(value)) {
                this.exportsVEvent!!.add(value)
            }
        } else {
            if (this.exportsVEvent!!.contains(value)) {
                this.exportsVEvent!!.remove(value)
            }
        }

        return this
    }

    fun getExportsVEvent(): List<String>? {
        return this.exportsVEvent
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(ICalGenerator::class.java)

        fun exportAllFields(): ICalGenerator {
            val generator = ICalGenerator()
            generator.exportsVEvent = ArrayList(ICalConverterStore.FULL_LIST)

            return generator
        }

        fun forMethod(method: Method): ICalGenerator {
            val generator: ICalGenerator

            if (Method.REQUEST == method) {
                generator = exportAllFields()
            } else if (Method.CANCEL == method) {
                generator = ICalGenerator()
                generator.exportsVEvent = ArrayList(listOf(ICalConverterStore.VEVENT_UID, ICalConverterStore.VEVENT_DTSTAMP, ICalConverterStore.VEVENT_DTSTART, ICalConverterStore.VEVENT_SEQUENCE, ICalConverterStore.VEVENT_ORGANIZER, ICalConverterStore.VEVENT_ATTENDEES, ICalConverterStore.VEVENT_RRULE, ICalConverterStore.VEVENT_EX_DATE))
            } else {
                throw UnsupportedOperationException(String.format("No generator for method '%s'", method.value))
            }

            generator.method = method
            return generator
        }
    }
}
