/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.projectforge.business.timesheet.TimesheetDO
import java.util.*

@JsonIgnoreProperties(value = ["reminderDuration", "reminderDurationUnit"])
class Timesheet(var task: Task? = null,
                var location: String? = null,
                var reference: String? = null,
                var description: String? = null,
                var user: User? = null,
                var kost2: Kost2? = null,
                var startTime: Date? = null,
                var stopTime: Date? = null,
                /**
                 * A counter (incremented by one for each recent entry) usable by React as key.
                 */
                var counter: Int? = null
) : BaseDTO<TimesheetDO>()
