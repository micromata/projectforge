/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.timesheet

import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.annotations.XStreamAsAttribute

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("timesheetPrefEntry")
data class TimesheetRecentEntry
@JvmOverloads
constructor(
        @XStreamAsAttribute
        var taskId: Long? = null,
        @XStreamAsAttribute
        var userId: Long? = null,
        @XStreamAsAttribute
        var kost2Id: Long? = null,
        @XStreamAsAttribute
        var location: String? = null,
        @XStreamAsAttribute
        var reference: String? = null,
        @XStreamAsAttribute
        var tag: String? = null,
        @XStreamAsAttribute
        var description: String? = null) {

    constructor(timesheet: TimesheetDO) : this(
            taskId = timesheet.taskId,
            userId = timesheet.userId,
            kost2Id = timesheet.kost2Id,
            location = timesheet.location,
            reference = timesheet.reference,
            tag = timesheet.tag,
            description = timesheet.description)
}
