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

import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.data.ParserException
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Component
import net.fortuna.ical4j.model.component.CalendarComponent
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.Method
import org.apache.commons.lang3.StringUtils
import org.projectforge.business.teamcal.TeamCalConfig
import org.projectforge.business.teamcal.event.ical.ICalParser
import org.projectforge.rest.dto.CalEvent
import java.io.IOException
import java.io.StringReader
import java.util.ArrayList
import kotlin.Boolean
import kotlin.Comparator
import kotlin.String

class ICSParser {
    private val log = org.slf4j.LoggerFactory.getLogger(ICalParser::class.java)

    //------------------------------------------------------------------------------------------------------------
    // None static part
    //------------------------------------------------------------------------------------------------------------

    private var parseVEvent: List<String>? = null
    private var events: MutableList<VEvent>? = null
    var extractedEvents: MutableList<CalEvent>? = null
    private var method: Method? = null

    private fun reset() {
        events = ArrayList()
        this.extractedEvents = ArrayList()
    }

    fun parse(iCalString: String): Boolean {
        val iCalReader = StringReader(iCalString)
        this.reset()
        val builder = CalendarBuilder()

        val calendar: Calendar
        try {
            // parse calendar
            calendar = builder.build(iCalReader)
        } catch (e: IOException) {
            log.error("An unknown error occurred while parsing an ICS file", e)
            return false
        } catch (e: ParserException) {
            log.error("An unknown error occurred while parsing an ICS file", e)
            return false
        }

        return this.parse(calendar)
    }

    fun parse(calendar: Calendar): Boolean {
        this.method = calendar.method

        val list = calendar.getComponents<CalendarComponent>(Component.VEVENT)
        if (list.size == 0) {
            // no events found
            return true
        }

        for (c in list) {
            val vEvent = c as VEvent

            // skip setup event!
            if (vEvent.summary != null && StringUtils.equals(vEvent.summary.value, TeamCalConfig.SETUP_EVENT)) {
                continue
            }

            val event = this.parse(vEvent)

            if (event != null) {
                this.events!!.add(vEvent)
                this.extractedEvents!!.add(event)
            }
        }

        // sorting events
        this.extractedEvents!!.sortWith(Comparator { o1, o2 ->
            val startDate1 = o1.startDate
            val startDate2 = o2.startDate
            if (startDate1 == null) {
                return@Comparator if (startDate2 == null) {
                    0
                } else -1
            }
            startDate1.compareTo(startDate2!!)
        })

        return true
    }

    private fun parse(vEvent: VEvent): CalEvent {
        val store = ICalConverterStore.instance

        // create vEvent
        val event = CalEvent()

        for (extract in this.parseVEvent!!) {
            val converter = store.getVEventConverter(extract)

            if (converter == null) {
                log.warn(String.format("No converter found for '%s', converter is skipped", extract))
                continue
            }

            converter.fromVEvent(event, vEvent)
        }

        return event
    }
}
