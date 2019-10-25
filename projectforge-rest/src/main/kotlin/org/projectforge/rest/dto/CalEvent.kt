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

package org.projectforge.rest.dto

import org.projectforge.business.calendar.event.model.ICalendarEvent
import org.projectforge.business.calendar.event.model.SeriesModificationMode
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.model.*
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.sql.Timestamp
import java.util.*

class CalEvent(
        var seriesModificationMode: SeriesModificationMode? = null,
        /**
         * The selected event of a series (if any).
         */
        var selectedSeriesEvent: ICalendarEvent? = null,
        override var subject: String? = null,
        override var location: String? = null,
        override var allDay: Boolean = false,
        override var startDate: Timestamp? = null,
        override var endDate: Timestamp? = null,
        var lastEmail: Timestamp? = null,
        var dtStamp: Timestamp? = null,
        var calendar: TeamCalDO? = null,
        var recurrenceRule: String? = null,
        var recurrenceExDate: String? = null,
        var recurrenceReferenceDate: String? = null,
        var recurrenceReferenceId: String? = null,
        var recurrenceUntil: Date? = null,
        override var note: String? = null,
        var attendees: MutableSet<TeamEventAttendeeDO>? = null,
        var ownership: Boolean? = null,
        var organizer: String? = null,
        var organizerAdditionalParams: String? = null,
        var sequence: Int? = 0,
        override var uid: String? = null,
        var reminderDuration: Int? = null,
        var reminderDurationUnit: ReminderDurationUnit? = null,
        var reminderActionType: ReminderActionType? = null,
        var attachments: MutableSet<TeamEventAttachmentDO>? = null,
        var creator: PFUserDO? = null) : BaseDTO<CalEventDO>(), ICalendarEvent {

    val hasRecurrence: Boolean
        get() = !recurrenceRule.isNullOrBlank()
}
