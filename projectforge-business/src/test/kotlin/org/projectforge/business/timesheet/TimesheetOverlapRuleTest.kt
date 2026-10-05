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
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
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
 *
 * A cost unit may override its task's setting ([Kost2DO.sharedCost]): true/false win over the task's flag, null leaves
 * the decision to the task.
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

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Test
    fun overlapRule() {
        persistenceService.runInTransaction { context ->
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

            // Collisions the validation would reject (legacy data), persisted directly: same project and
            // not released. Touching periods (one ends when the next starts) are no collision.
            val collision1 = persistRaw(context, "ovl-root-A", user, date(14, 8), date(14, 16))
            val collision2 = persistRaw(context, "ovl-A-sub", user, date(14, 15), date(14, 18))
            val collision3 = persistRaw(context, "ovl-root-B", user, date(15, 8), date(15, 12))
            val collision4 = persistRaw(context, "ovl-plain", user, date(15, 11), date(15, 13))
            persistRaw(context, "ovl-plain", user, date(16, 8), date(16, 12))
            persistRaw(context, "ovl-root-B", user, date(16, 12), date(16, 14))
            context.flush()

            Assertions.assertEquals(
                setOf(collision1, collision2, collision3, collision4),
                timesheetDao.getCollidingTimesheetIds(user.id!!, date(1, 0), date(31, 0)),
                "Only the forbidden overlaps are collisions, not the released ones (a), (d) nor touching periods.",
            )
            // The period limits the result; it is widened by the maximum duration of a sheet, so day 14 (ending
            // 18:00) is outside a period starting on day 15 at 12:00.
            Assertions.assertEquals(
                setOf(collision3, collision4),
                timesheetDao.getCollidingTimesheetIds(user.id!!, date(15, 12), date(15, 23)),
            )
            null
        }
    }

    @Test
    fun kost2OverridesTask() {
        persistenceService.runInTransaction { context ->
            logon(AbstractTestBase.TEST_FINANCE_USER)
            val kunde = KundeDO().also {
                it.name = "Overlap-Kost2-Kunde"
                it.id = 80
                kundeDao.insert(it)
            }
            // Each project gets two cost units (Kost2Art 0 and 1): x1 and x2.
            val projektA = initTestDB.addProjekt(kunde, 80, "Overlap-Kost2-Projekt-A", 0, 1)
            val projektB = initTestDB.addProjekt(kunde, 81, "Overlap-Kost2-Projekt-B", 0, 1)
            val (a1, a2) = kost2Pair(projektA.id!!)
            val (b1, b2) = kost2Pair(projektB.id!!)
            a1.sharedCost = true // Shared cost, although its task isn't released.
            kost2Dao.update(a1)
            b1.sharedCost = false // Explicitly not, although its task is released.
            kost2Dao.update(b1)
            // a2 and b2 keep null: their task decides.

            // Project tasks, not released; the sub task of B is released (shared cost element).
            val rootA = initTestDB.addTask("ovl-k-A", "root")
            projektDao.setTask(projektA, rootA.id)
            projektDao.update(projektA)
            val rootB = initTestDB.addTask("ovl-k-B", "root")
            projektDao.setTask(projektB, rootB.id)
            projektDao.update(projektB)
            val releasedB = initTestDB.addTask("ovl-k-B-rel", "ovl-k-B")
            releasedB.allowTimeOverlap = true
            taskDao.update(releasedB, checkAccess = false)

            val user = initTestDB.addUser("ovl-k-user")

            // (e) Cost unit says yes, task not released, different project => allowed.
            insert("ovl-k-A", user, date(20, 8), date(20, 16), a1)
            insert("ovl-k-B", user, date(20, 15), date(20, 18), b2)

            // (f) Cost unit says no on a released task, the other one isn't released either => forbidden.
            insert("ovl-k-A", user, date(21, 8), date(21, 16), a2)
            assertOverlapRejected("ovl-k-B-rel", user, date(21, 15), date(21, 18), b1)

            // (g) Cost unit without own setting on a released task => the task decides, allowed (as before).
            insert("ovl-k-A", user, date(22, 8), date(22, 16), a2)
            insert("ovl-k-B-rel", user, date(22, 15), date(22, 18), b2)

            // (h) Cost unit says yes, but both sheets belong to the same project => still forbidden.
            insert("ovl-k-A", user, date(23, 8), date(23, 16), a2)
            assertOverlapRejected("ovl-k-A", user, date(23, 15), date(23, 18), a1)

            // The list's collision search applies the same rule to legacy data: (f) and (h) collide, (e) and (g)
            // (inserted above) don't.
            val collision1 = persistRaw(context, "ovl-k-A", user, date(24, 8), date(24, 16), a2)
            val collision2 = persistRaw(context, "ovl-k-B-rel", user, date(24, 15), date(24, 18), b1)
            val collision3 = persistRaw(context, "ovl-k-A", user, date(25, 8), date(25, 16), a1)
            val collision4 = persistRaw(context, "ovl-k-A", user, date(25, 15), date(25, 18), a2)
            context.flush()
            Assertions.assertEquals(
                setOf(collision1, collision2, collision3, collision4),
                timesheetDao.getCollidingTimesheetIds(user.id!!, date(1, 0), date(31, 0)),
            )
            null
        }
    }

    /** The project's two cost units of Kost2Art 0 and 1, as [InitTestDB.addProjekt] created them. */
    private fun kost2Pair(projektId: Long): Pair<Kost2DO, Kost2DO> {
        val list = kost2Dao.getActiveKost2(projektDao.find(projektId, checkAccess = false))!!
        return list.single { it.kost2Art?.id == 0L } to list.single { it.kost2Art?.id == 1L }
    }

    private fun insert(taskName: String, user: PFUserDO, start: Date, stop: Date, kost2: Kost2DO? = null) {
        val ts = TimesheetDO()
        ts.task = initTestDB.getTask(taskName)
        ts.kost2 = kost2
        ts.user = user
        ts.startTime = start
        ts.stopTime = stop
        timesheetDao.insert(ts, checkAccess = false)
    }

    /** Persists without the validation of [TimesheetDao.insert], to simulate colliding legacy data. */
    private fun persistRaw(
        context: PfPersistenceContext, taskName: String, user: PFUserDO, start: Date, stop: Date, kost2: Kost2DO? = null,
    ): Long {
        val ts = TimesheetDO()
        ts.task = initTestDB.getTask(taskName)
        ts.kost2 = kost2
        ts.user = user
        ts.startTime = start
        ts.stopTime = stop
        ts.created = Date()
        ts.lastUpdate = ts.created
        context.insert(ts)
        return ts.id!!
    }

    private fun assertOverlapRejected(taskName: String, user: PFUserDO, start: Date, stop: Date, kost2: Kost2DO? = null) {
        try {
            insert(taskName, user, start, stop, kost2)
            Assertions.fail<Unit>("Overlapping time sheet on '$taskName' should have been rejected.")
        } catch (ex: UserException) {
            Assertions.assertEquals("timesheet.error.timeperiodOverlapDetection", ex.i18nKey)
        }
    }

    private fun date(day: Int, hour: Int, minute: Int = 0): Date =
        Date.from(LocalDateTime.of(2031, Month.MARCH, day, hour, minute).atZone(ZoneId.of("UTC")).toInstant())
}
