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

package org.projectforge.business.timesheet

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.util.*

/**
 * Verifies the overlap rule for shared cost elements ([org.projectforge.business.task.TaskDO.allowTimeOverlap]):
 * two time sheets of the same user may overlap in time only if at least one of them sits on a released task
 * (the flag is inherited by the subtree) **and** the two do not belong to the same project. Overlap within the
 * same project stays forbidden even when released — that would be a double booking.
 */
class TimesheetOverlapRuleTest : AbstractTestBase() {
    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Test
    fun overlapRule() {
        persistenceService.runInTransaction { _ ->
            // Finance group required to create customers/projects; time sheets below are inserted with
            // checkAccess = false, so this login is only about the master data setup.
            logon(AbstractTestBase.TEST_FINANCE_USER)
            val kunde = KundeDO().also {
                it.name = "Overlap-Kunde"
                it.id = 70
                kundeDao.insert(it)
            }
            val projektA = initTestDB.addProjekt(kunde, 70, "Overlap-Projekt-A")
            val projektB = initTestDB.addProjekt(kunde, 71, "Overlap-Projekt-B")

            // Released task carrying project A; a child inherits both the release and the project.
            val rootA = initTestDB.addTask("ovl-root-A", "root")
            rootA.allowTimeOverlap = true
            taskDao.update(rootA, checkAccess = false)
            projektDao.setTask(projektA, rootA.id)
            projektDao.update(projektA)
            initTestDB.addTask("ovl-A-sub", "ovl-root-A")

            // Not released, project B.
            val rootB = initTestDB.addTask("ovl-root-B", "root")
            projektDao.setTask(projektB, rootB.id)
            projektDao.update(projektB)

            // No project, not released.
            initTestDB.addTask("ovl-plain", "root")

            // Released, but no project attached.
            val releasedNp = initTestDB.addTask("ovl-released-np", "root")
            releasedNp.allowTimeOverlap = true
            taskDao.update(releasedNp, checkAccess = false)

            val user = initTestDB.addUser("ovl-user")

            // (a) Released (inherited by the child) + different project => allowed.
            insert("ovl-A-sub", user, date(10, 8), date(10, 16))
            insert("ovl-root-B", user, date(10, 15), date(10, 18)) // overlaps 15:00-16:00, must be accepted.

            // (b) Same project (both resolve project A), even though released => forbidden.
            insert("ovl-root-A", user, date(11, 8), date(11, 16))
            assertOverlapRejected("ovl-A-sub", user, date(11, 15), date(11, 18))

            // (c) Neither task released => forbidden regardless of the differing projects.
            insert("ovl-root-B", user, date(12, 8), date(12, 16))
            assertOverlapRejected("ovl-plain", user, date(12, 15), date(12, 18))

            // (d) Released + neither has a project (null project is never "the same project") => allowed.
            insert("ovl-released-np", user, date(13, 8), date(13, 16))
            insert("ovl-plain", user, date(13, 15), date(13, 18)) // overlaps, must be accepted.
            null
        }
    }

    private fun insert(taskName: String, user: PFUserDO, start: Date, stop: Date) {
        val ts = TimesheetDO()
        ts.task = initTestDB.getTask(taskName)
        ts.user = user
        ts.startTime = start
        ts.stopTime = stop
        timesheetDao.insert(ts, checkAccess = false)
    }

    private fun assertOverlapRejected(taskName: String, user: PFUserDO, start: Date, stop: Date) {
        try {
            insert(taskName, user, start, stop)
            Assertions.fail<Unit>("Overlapping time sheet on '$taskName' should have been rejected.")
        } catch (ex: UserException) {
            Assertions.assertEquals("timesheet.error.timeperiodOverlapDetection", ex.i18nKey)
        }
    }

    private fun date(day: Int, hour: Int, minute: Int = 0): Date =
        Date.from(LocalDateTime.of(2031, Month.MARCH, day, hour, minute).atZone(ZoneId.of("UTC")).toInstant())
}
