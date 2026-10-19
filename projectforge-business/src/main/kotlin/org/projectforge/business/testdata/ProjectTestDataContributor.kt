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

import org.projectforge.business.fibu.ContactUserIds
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.KundeStatus
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.ProjektStatus
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.access.AccessDao
import org.projectforge.framework.access.AccessType
import org.projectforge.framework.access.GroupTaskAccessDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Cost 2 types, customers, their projects with task trees and cost 2, and the task access of the groups.
 */
@Service
open class ProjectTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var accessDao: AccessDao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    override val order = 20

    private class Project(val kunde: Long, val nr: Int, val name: String, val identifier: String)

    override fun seed(ctx: TestDataContext) {
        kost2Arts().forEach { art ->
            kost2ArtDao.insert(art, checkAccess = false)
            ctx.kost2Arts[art.id!!] = art
        }
        KUNDEN.forEach { (nummer, name) ->
            val kunde = KundeDO().also {
                it.nummer = nummer
                it.name = name
                it.identifier = name.substringBefore(' ')
                it.status = KundeStatus.ACTIVE
            }
            kundeDao.insert(kunde, checkAccess = false)
            ctx.kunden[nummer] = kunde
        }
        val root = taskTree.rootTaskNode.task
        val company = addTask(root, UserTestDataContributor.ORGANIZATION, "Internal tasks of the company")
        ctx.companyTask = company
        listOf("Vacation", "Sick leave", "Training", "Administration").forEach { addTask(company, it) }
        val kundeTasks = mutableMapOf<Long, TaskDO>()
        PROJECTS.forEach { p ->
            val kunde = ctx.kunden[p.kunde]!!
            val kundeTask = kundeTasks.getOrPut(p.kunde) { addTask(root, kunde.name!!) }
            val projectTask = addTask(kundeTask, p.name, "Project lead: Joe Stone", maxHours = 2400)
            SUB_TASKS.forEach { (title, hours) ->
                ctx.projectTasks[p.name to title] = addTask(projectTask, title, maxHours = hours)
            }
            val projekt = ProjektDO().also {
                it.nummer = p.nr
                it.name = p.name
                it.identifier = p.identifier
                it.kunde = kunde
                it.status = ProjektStatus.BUILD
                it.task = projectTask
                it.contactPerson = ctx.user("joe")
                it.additionalContactUserIds = ContactUserIds.normalize(it.contactPerson?.id, listOf(ctx.user("laura").id))
            }
            projektDao.insert(projekt, checkAccess = false)
            ctx.projekte[p.name] = projekt
            // Kost2 only for a project of a number range (pfDev.sh setup configures them, see NumberRangeConfig);
            // without, the project number is free and isn't made of Kost2.
            if (projekt.nummernkreis == null) {
                return@forEach
            }
            ctx.kost2Arts.values.filter { it.projektStandard }.forEach { art ->
                val kost2 = Kost2DO().also {
                    // Of the managed project, not Kost2Dao.setProjekt: its find would detach it from this transaction.
                    it.nummernkreis = projekt.nummernkreis!!
                    it.bereich = projekt.bereich!!
                    it.teilbereich = projekt.teilbereich!!
                    it.kost2Art = art
                    it.projekt = projekt
                    it.kostentraegerStatus = KostentraegerStatus.ACTIVE
                    it.description = art.name
                }
                kost2Dao.insert(kost2, checkAccess = false)
                ctx.kost2s[p.name to art.id!!] = kost2
            }
        }
        // Employees book their own time sheets everywhere and see the others', project leads may do everything.
        addAccess(ctx.groups[UserTestDataContributor.ORGANIZATION]!!, root, manage = false)
        addAccess(ctx.groups["Delivery leads"]!!, root, manage = true)
    }

    private fun addTask(parent: TaskDO?, title: String, description: String? = null, maxHours: Int? = null): TaskDO {
        val task = TaskDO().also {
            it.parentTask = parent
            it.title = title
            it.shortDescription = description
            it.status = TaskStatus.O
            it.maxHours = maxHours
        }
        taskDao.insert(task, checkAccess = false)
        return task
    }

    private fun addAccess(group: GroupDO, task: TaskDO, manage: Boolean) {
        val access = GroupTaskAccessDO().also {
            it.group = group
            it.task = task
            it.recursive = true
        }
        access.ensureAndGetAccessEntry(AccessType.TASKS).setAccess(true, manage, manage, manage)
        access.ensureAndGetAccessEntry(AccessType.TASK_ACCESS_MANAGEMENT).setAccess(manage, manage, manage, manage)
        access.ensureAndGetAccessEntry(AccessType.TIMESHEETS).setAccess(true, manage, manage, manage)
        access.ensureAndGetAccessEntry(AccessType.OWN_TIMESHEETS).setAccess(true, true, true, true)
        accessDao.insert(access, checkAccess = false)
    }

    companion object {
        const val KOST2_ART_ACQUISITION = 0L
        const val KOST2_ART_MEETINGS = 1L
        const val KOST2_ART_REALIZATION = 2L

        /** New instances per run: inserting them makes them persistent. */
        private fun kost2Arts() = listOf(
            kost2Art(KOST2_ART_ACQUISITION, "Acquisition", fakturiert = false),
            kost2Art(KOST2_ART_MEETINGS, "Meetings", fakturiert = true),
            kost2Art(KOST2_ART_REALIZATION, "Realization", fakturiert = true),
            kost2Art(10L, "Travelling", fakturiert = false, workFraction = BigDecimal("0.5")),
            kost2Art(99L, "Misc", fakturiert = false, projektStandard = false),
        )

        private fun kost2Art(
            id: Long, name: String, fakturiert: Boolean, projektStandard: Boolean = true, workFraction: BigDecimal? = null,
        ) = Kost2ArtDO().also {
            it.id = id
            it.name = name
            it.fakturiert = fakturiert
            it.projektStandard = projektStandard
            it.workFraction = workFraction
        }

        private val KUNDEN = listOf(
            300L to "Bluefin Logistics GmbH",
            301L to "Alpine Insurance AG",
            302L to "Greenfield Energy GmbH",
            303L to "Harbor Medical Systems",
            304L to "Lumen Retail Group",
        )

        private val PROJECTS = listOf(
            Project(300, 1, "Fleet Tracking Platform", "Fleet"),
            Project(301, 1, "Claims Portal", "Claims"),
            Project(301, 2, "Customer App", "App"),
            Project(302, 1, "Smart Meter Analytics", "Meter"),
            Project(303, 1, "Patient Scheduling", "Scheduling"),
            Project(304, 1, "Shop Relaunch", "Shop"),
        )

        val SUB_TASKS = listOf(
            "Specification" to 300, "Design" to 250, "Development" to 1200, "Testing" to 300, "Go-live" to 120,
        )
    }
}
