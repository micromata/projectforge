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

package org.projectforge.rest

import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetTimerPref
import org.projectforge.business.timesheet.TimesheetTimerService
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The timer of the logged-in user (see [TimesheetTimerService]), shared by the app and the web pages: started on one,
 * stopped on the other. It books nothing; a stopped timer opens the form of a new time sheet on the client.
 */
@RestController
@RequestMapping("${Rest.URL}/timesheetTimer")
class TimesheetTimerRest {
    @Autowired
    private lateinit var timerService: TimesheetTimerService

    @Autowired
    private lateinit var taskDao: TaskDao

    /** The timer for the client; [running] is false without one. */
    class TimerInfo(
        val running: Boolean,
        /** UTC, milliseconds since 1970; the server's clock. */
        val startedAt: Long? = null,
        val taskId: Long? = null,
        val kost2Id: Long? = null,
        val description: String? = null,
        val location: String? = null,
        val reference: String? = null,
        val tag: String? = null,
        /** The server's clock now, so a client can show the elapsed time without trusting its own clock. */
        val now: Long = System.currentTimeMillis(),
    )

    /** What a started timer is booked on; everything optional. */
    class StartRequest(
        var taskId: Long? = null,
        var kost2Id: Long? = null,
        var description: String? = null,
        var location: String? = null,
        var reference: String? = null,
        var tag: String? = null,
    )

    @AccessChecked("Own user prefs (UserPrefService, scoped to the logged-in user)")
    @GetMapping
    fun get(): TimerInfo = infoOf(timerService.get())

    // State changing: POST, which carries the CSRF token.

    @AccessChecked("Own user prefs; task select access")
    @PostMapping("start")
    fun start(@RequestBody request: StartRequest?): TimerInfo {
        request?.taskId?.let { checkTaskAccess(it) }
        val what = request?.let {
            TimesheetTimerPref(
                taskId = it.taskId, kost2Id = it.kost2Id, description = it.description,
                location = it.location, reference = it.reference, tag = it.tag,
            )
        }
        return infoOf(timerService.start(what))
    }

    @AccessChecked("Own user prefs (UserPrefService, scoped to the logged-in user)")
    @PostMapping("clear")
    fun clear(): TimerInfo {
        timerService.clear()
        return infoOf(null)
    }

    /** Only a task the user may select is stored: the form of the stopped timer is filled with it. */
    private fun checkTaskAccess(taskId: Long) {
        val task = TaskTree.instance.getTaskById(taskId) ?: throw AccessException("access.exception.userHasNotRight")
        taskDao.hasLoggedInUserSelectAccess(task, true)
    }

    private fun infoOf(pref: TimesheetTimerPref?): TimerInfo {
        pref ?: return TimerInfo(running = false)
        return TimerInfo(
            running = true, startedAt = pref.startedAt, taskId = pref.taskId, kost2Id = pref.kost2Id,
            description = pref.description, location = pref.location, reference = pref.reference, tag = pref.tag,
        )
    }
}
