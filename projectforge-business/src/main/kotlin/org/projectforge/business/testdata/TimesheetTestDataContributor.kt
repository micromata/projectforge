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


package org.projectforge.business.testdata

import org.projectforge.business.task.TaskDao
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.time.PFDayUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalTime
import java.util.Date

/**
 * Time sheets of the last weeks (two blocks per working day and user, none on vacation), and task budgets
 * fitting the booked hours, so the task tree shows different progress.
 */
@Service
open class TimesheetTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    override val order = 40

    /** About 20 seconds, the others together take some. */
    override val weight = 40

    /** The project and task of the morning and afternoon block of a user. */
    private class Plan(val user: String, val am: Pair<String, String>, val pm: Pair<String, String>)

    override fun seed(ctx: TestDataContext) {
        val bookedHours = mutableMapOf<Long, Double>()
        val today = ctx.today
        PLANS.forEachIndexed { index, p ->
            ctx.reportProgress(index.toDouble() / PLANS.size)
            val user = ctx.user(p.user)
            val absences = ctx.absences.filter { it.first == p.user }
            var day = today.minusWeeks(WEEKS * ctx.scale)
            while (!day.isAfter(today)) {
                val off = !PFDayUtils.isWorkingDay(day) ||
                        absences.any { !day.isBefore(it.second) && !day.isAfter(it.third) }
                if (!off) {
                    val blocks = if (day == today) {
                        listOf(p.am to (LocalTime.of(8, 30) to LocalTime.of(12, 0)))
                    } else {
                        listOf(
                            p.am to (LocalTime.of(8, 30) to LocalTime.of(12, 30)),
                            p.pm to (LocalTime.of(13, 15) to LocalTime.of(17, 15 + ctx.random.nextInt(3) * 15)),
                        )
                    }
                    blocks.forEach { (target, time) ->
                        val (projekt, title) = target
                        val task = ctx.task(projekt, title)
                        val meeting = title == "Specification" && ctx.random.nextInt(4) == 0
                        val kost2ArtId = if (meeting) {
                            ProjectTestDataContributor.KOST2_ART_MEETINGS
                        } else {
                            ProjectTestDataContributor.KOST2_ART_REALIZATION
                        }
                        val texts = TEXTS[title]!!
                        val timesheet = TimesheetDO().also {
                            it.user = user
                            it.task = task
                            it.kost2 = ctx.kost2(projekt, kost2ArtId)
                            it.startTime = Date.from(day.atTime(time.first).atZone(ctx.zone).toInstant())
                            it.stopTime = Date.from(day.atTime(time.second).atZone(ctx.zone).toInstant())
                            it.timeZone = ctx.zone.id
                            it.location = when (ctx.random.nextInt(3)) {
                                0 -> "Customer site"
                                1 -> "Office"
                                else -> "Remote"
                            }
                            it.description = texts[ctx.random.nextInt(texts.size)]
                        }
                        timesheetDao.insert(timesheet, checkAccess = false)
                        val hours = (timesheet.stopTime!!.time - timesheet.startTime!!.time) / 3_600_000.0
                        bookedHours.merge(task.id!!, hours, Double::plus)
                    }
                }
                day = day.plusDays(1)
            }
        }
        updateBudgets(ctx, bookedHours)
    }

    /** Budgets of 42% up to 107% booked, a task without bookings gets 80 hours. */
    private fun updateBudgets(ctx: TestDataContext, bookedHours: Map<Long, Double>) {
        var i = 0
        ctx.projekte.forEach { (name, projekt) ->
            var total = 0
            ProjectTestDataContributor.SUB_TASKS.forEach { (title, _) ->
                val task = taskDao.find(ctx.task(name, title).id, checkAccess = false)!!
                val booked = bookedHours[task.id] ?: 0.0
                val max = if (booked > 0) (booked / RATIOS[i++ % RATIOS.size]).toInt().coerceAtLeast(8) else 80
                task.maxHours = max
                total += max
                taskDao.update(task, checkAccess = false)
            }
            val projectTask = taskDao.find(projekt.task!!.id, checkAccess = false)!!
            projectTask.maxHours = (total * 1.1).toInt()
            taskDao.update(projectTask, checkAccess = false)
        }
    }

    companion object {
        /** Weeks with time sheets, multiplied by projectforge.testdata.scale. */
        private const val WEEKS = 26L

        private val RATIOS = listOf(0.42, 0.68, 0.83, 0.94, 1.07, 0.55, 0.77)

        private val PLANS = listOf(
            Plan("laura", "Claims Portal" to "Specification", "Fleet Tracking Platform" to "Go-live"),
            Plan("alex", "Fleet Tracking Platform" to "Development", "Fleet Tracking Platform" to "Testing"),
            Plan("ann", "Customer App" to "Design", "Shop Relaunch" to "Design"),
            Plan("chris", "Claims Portal" to "Development", "Claims Portal" to "Testing"),
            Plan("joe", "Smart Meter Analytics" to "Specification", "Patient Scheduling" to "Specification"),
            Plan("julia", "Patient Scheduling" to "Development", "Smart Meter Analytics" to "Development"),
            Plan("max", "Shop Relaunch" to "Development", "Customer App" to "Development"),
            Plan("mona", "Smart Meter Analytics" to "Development", "Shop Relaunch" to "Testing"),
        )

        private val TEXTS = mapOf(
            "Specification" to listOf(
                "Requirements workshop with the customer", "User stories and acceptance criteria",
                "Refined the backlog for the next sprint", "Specification of the interfaces",
            ),
            "Design" to listOf(
                "Wireframes for the dashboard", "Clickable prototype, feedback round",
                "Design system: buttons and forms", "Usability test with five users",
            ),
            "Development" to listOf(
                "Implemented the REST API for orders", "Code review and refactoring", "Database migration and import",
                "Fixed issues from the test", "Frontend: list and filter views", "Integration of the payment provider",
            ),
            "Testing" to listOf(
                "Regression test of the release candidate", "Automated end-to-end tests", "Load test and tuning",
                "Bug fixing after the acceptance test",
            ),
            "Go-live" to listOf(
                "Go-live planning with operations", "Release notes and training material", "Hypercare after the go-live",
            ),
        )
    }
}
