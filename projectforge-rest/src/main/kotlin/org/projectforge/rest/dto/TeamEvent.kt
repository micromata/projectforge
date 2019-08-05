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

import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.business.teamcal.event.model.*
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.sql.Timestamp
import java.util.*

class TeamEvent(
        /**
         * Modifications should have effect for all entries of this serie.
         */
        var all: Boolean? = null,
        /**
         * Modifications should have effect only for future entries of this serie.
         */
        var future: Boolean? = null,
        /**
         * Modifications should have effect only for this single event.
         */
        var single: Boolean? = null,
        var subject: String? = null,
        var location: String? = null,
        var allDay: Boolean = false,
        var startDate: Timestamp? = null,
        var endDate: Timestamp? = null,
        var lastEmail: Timestamp? = null,
        var dtStamp: Timestamp? = null,
        var calendar: TeamCalDO? = null,
        var recurrenceRule: String? = null,
        var recurrenceExDate: String? = null,
        var recurrenceReferenceDate: String? = null,
        var recurrenceReferenceId: String? = null,
        var recurrenceUntil: Date? = null,
        var note: String? = null,
        var attendees: MutableSet<TeamEventAttendeeDO>? = null,
        var ownership: Boolean? = null,
        var organizer: String? = null,
        var organizerAdditionalParams: String? = null,
        var sequence: Int? = 0,
        var uid: String? = null,
        var reminderDuration: Int? = null,
        var reminderDurationUnit: ReminderDurationUnit? = null,
        var reminderActionType: ReminderActionType? = null,
        var attachments: MutableSet<TeamEventAttachmentDO>? = null,
        var creator: PFUserDO? = null) : BaseDTO<CalEventDO>() {

    val hasRecurrence: Boolean
        get() = !recurrenceRule.isNullOrBlank()
}
