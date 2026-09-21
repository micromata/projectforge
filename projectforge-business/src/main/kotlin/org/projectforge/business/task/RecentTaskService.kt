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

package org.projectforge.business.task

import org.projectforge.business.user.service.UserPrefService
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.RecentQueue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The tasks the user selected most recently in the (Next) task picker, as an MRU list of task ids per
 * user.
 *
 * Unlike [org.projectforge.business.timesheet.TimesheetRecentService], which derives its recent task ids
 * from the last year of the user's timesheets and stores them only as a volatile, rebuilt-on-miss cache,
 * this queue records every pick made in the task select element regardless of where it is used and is
 * stored **persistently**, so it survives a restart or a re-login.
 */
@Service
open class RecentTaskService {
    @Autowired
    private lateinit var userPrefService: UserPrefService

    /** The recent task ids, newest first. */
    open fun getRecentTaskIds(): List<Long> {
        return getQueue().recentList ?: emptyList()
    }

    /** Moves the given task id to the front of the recent list, trimming it to [MAX_RECENT] entries. */
    open fun addRecentTaskId(taskId: Long) {
        val queue = getQueue()
        queue.append(taskId)
        // Re-put so the mutated queue is written back: unlike the timesheet queue this entry is
        // persistent, and only putEntry marks it for the next flush to the database.
        userPrefService.putEntry(PREF_AREA, PREF_NAME, queue)
    }

    private fun getQueue(): RecentQueue<Long> {
        @Suppress("UNCHECKED_CAST")
        return userPrefService.ensureEntry(
            PREF_AREA,
            PREF_NAME,
            RecentQueue<Long>(MAX_RECENT),
        ) as RecentQueue<Long>
    }

    companion object {
        private const val MAX_RECENT = 25
        private const val PREF_NAME = "recent.taskIds"
        private val PREF_AREA = RecentTaskService::class.java.name
    }
}
