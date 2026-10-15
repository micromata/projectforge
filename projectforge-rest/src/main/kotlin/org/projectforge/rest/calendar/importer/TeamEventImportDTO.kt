/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.calendar.importer

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.business.teamcal.event.model.TeamEventDO
import org.projectforge.common.DateFormatType
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDay
import org.projectforge.rest.importer.ImportPairEntry
import java.time.ZoneOffset
import java.util.Date
import kotlin.reflect.KProperty

/**
 * One event of an uploaded ics file (or its stored counterpart in the target calendar), as the import preview
 * shows it. The fields are display values: [period] is formatted here in the user's locale and time zone, so
 * the frontend shows it as it is and the reconcile compares the same text on both sides.
 *
 * The parsed [event] itself travels along (not serialized), so the import job persists exactly what was read.
 *
 * @author Kai Reinhard
 */
class TeamEventImportDTO(
    /** The iCalendar uid, the key the reconcile matches an existing event of the target calendar by. */
    var uid: String? = null,
    var subject: String? = null,
    var location: String? = null,
    /** Begin and end, date only for an all-day event. */
    var period: String? = null,
    /** The raw RRULE, e.g. `FREQ=WEEKLY;BYDAY=MO`. */
    var recurrence: String? = null,
    var note: String? = null,
    /** The id of the matching stored event, null for a new one. */
    var id: Long? = null,
    @get:JsonIgnore
    val event: TeamEventDO? = null,
) : ImportPairEntry.Modified<TeamEventImportDTO> {

    override val properties: Array<KProperty<*>>
        get() = arrayOf(
            TeamEventImportDTO::subject,
            TeamEventImportDTO::location,
            TeamEventImportDTO::period,
            TeamEventImportDTO::recurrence,
            TeamEventImportDTO::note,
        )

    companion object {
        /** The read side: [event] is kept for the import. */
        fun read(event: TeamEventDO): TeamEventImportDTO = from(event, keepEvent = true)

        /** The stored side: only its values and id are needed. */
        fun stored(event: TeamEventDO): TeamEventImportDTO = from(event, keepEvent = false)

        private fun from(event: TeamEventDO, keepEvent: Boolean) = TeamEventImportDTO(
            uid = event.uid,
            subject = event.subject,
            location = event.location,
            period = formatPeriod(event.allDay, event.startDate, event.endDate),
            recurrence = event.recurrenceRule,
            note = event.note,
            id = event.id,
            event = if (keepEvent) event else null,
        )

        /**
         * All-day events are stored at midnight UTC with the exclusive end of the ics file (the day after), so
         * they are shown by their UTC dates and with the last day inclusive; a one-day event shows one date.
         */
        internal fun formatPeriod(allDay: Boolean, start: Date?, end: Date?): String? {
            start ?: return null
            if (allDay) {
                val startDay = utcDay(start)
                val lastDay = end?.let { utcDay(it).plusDays(-1) }
                if (lastDay == null || !lastDay.isAfter(startDay)) {
                    return startDay.format()
                }
                return "${startDay.format()} – ${lastDay.format()}"
            }
            val begin = PFDateTime.from(start).format(DateFormatType.DATE_TIME_MINUTES)
            end ?: return begin
            return "$begin – ${PFDateTime.from(end).format(DateFormatType.DATE_TIME_MINUTES)}"
        }

        private fun utcDay(date: Date): PFDay = PFDay.from(date.toInstant().atZone(ZoneOffset.UTC).toLocalDate())
    }
}
