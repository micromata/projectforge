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

package org.projectforge.rest.task

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

/**
 * The kost2 preview of the task form: what the black/white list the user typed but did not save resolves
 * to, and what a cost unit picked from the list is appended as.
 *
 * The bar is equality with Wicket, which computes all of this locally from its form model
 * (`TaskEditForm`) - a hand built page has to ask, which is what this endpoint is for.
 */
class TaskServicesRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var taskServicesRest: TaskServicesRest

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var kostCache: KostCache

    @Test
    fun `the preview resolves an unsaved black white list`() {
        // Only the writes are in the transaction: the preview reads through KostCache/ProjektCache, and
        // ProjektCache.refresh loads on a second connection (runIsolatedReadOnly). Against an uncommitted
        // writer on the same tables that deadlocks, so the assertions run after the commit.
        val (task, kost2a, orphan) = persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val task = initTestDB.addTask("kost2Preview", "root")
            val projekt = ProjektDO()
            projekt.name = "kost2Preview"
            projekt.internKost2_4 = 234
            projekt.nummer = 7
            projekt.task = task
            val projektId = projektDao.insert(projekt)
            // 4.234.07.01 and 4.234.07.02 of that project.
            val kost2a = insertKost2(projektDao.find(projektId)!!, 1)
            insertKost2(projektDao.find(projektId)!!, 2)
            // A task without any project, for the last two assertions.
            val orphan = initTestDB.addTask("kost2PreviewOrphan", "root")
            Triple(task, kost2a, orphan)
        }
        kostCache.setExpired()
        logon(TEST_FINANCE_USER)

        // No list at all: every active cost unit of the project.
        preview(task.id).let {
            assertEquals("4.234.07", it.projektKost)
            assertEquals("4.234.07.0*", it.kost2WildCard)
            assertEquals(2, lines(it.kost2ListAsLines).size)
        }
        // A white list picks, a black list drops - the suffix match of TaskTree.getKost2List.
        preview(task.id, kost2BlackWhiteList = "01").let {
            assertEquals("4.234.07.01", it.kost2WildCard)
            assertEquals(1, lines(it.kost2ListAsLines).size)
        }
        preview(task.id, kost2BlackWhiteList = "01", kost2IsBlackList = true).let {
            assertEquals("4.234.07.02", it.kost2WildCard)
        }
        // "*" as a black list drops everything, so there is nothing left to resolve.
        preview(task.id, kost2BlackWhiteList = "*", kost2IsBlackList = true).let {
            assertNull(it.kost2WildCard)
            assertNull(it.kost2ListAsLines)
        }
        // The list is answered normalized and sorted, as TaskHelper does it on save.
        assertEquals(
            ".89,02,4.234.07.01",
            preview(task.id, kost2BlackWhiteList = "4.234.07.01, 02;  .89, 02").kost2BlackWhiteList,
        )

        // A picked cost unit is abbreviated to its two Kost2Art digits, because its number starts with
        // the project's kost - TaskHelper.addKost2.
        assertEquals("01", preview(task.id, addKost2Id = kost2a.id).kost2BlackWhiteList)
        // The other branch: no id, but a parent. Then the whole number is appended, even though the
        // project resolves to the same one. A TypeScript copy would not have guessed this.
        assertEquals(
            "4.234.07.01",
            preview(id = null, parentTaskId = task.id, addKost2Id = kost2a.id).kost2BlackWhiteList,
        )
        // Still the parent's project, so the block shows the same units.
        assertEquals("4.234.07", preview(id = null, parentTaskId = task.id).projektKost)

        // A task without any project: nothing to resolve, and a white list of numbers is taken as it is.
        preview(orphan.id).let {
            assertNull(it.projektKost)
            assertNull(it.kost2WildCard)
        }
        preview(orphan.id, kost2BlackWhiteList = "4.234.07.01").let {
            assertNull(it.projektKost)
            assertEquals("4.234.07.01", it.kost2WildCard)
        }
    }

    /**
     * `info/{id}` carries the resolved project and whether cost units are configured at all - the gate for
     * showing the kost2 block. The project is the ancestor's for a task that has none of its own.
     */
    @Test
    fun `the task info carries the resolved project`() {
        val (task, child) = persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val task = initTestDB.addTask("kost2PreviewInfo", "root")
            val child = initTestDB.addTask("kost2PreviewInfoChild", "kost2PreviewInfo")
            val projekt = ProjektDO()
            projekt.name = "kost2PreviewInfo"
            projekt.internKost2_4 = 235
            projekt.nummer = 8
            projekt.task = task
            projektDao.insert(projekt)
            task to child
        }
        logon(TEST_FINANCE_USER)

        TaskServicesRest.createTask(task.id).let {
            assertEquals("4.235.08", it!!.projekt?.kost)
            assertEquals("kost2PreviewInfo", it.projekt?.name)
        }
        // The child has no project of its own, so it reports its ancestor's - that is where its cost
        // units come from (TaskTree.getProjekt walks up).
        assertEquals("4.235.08", TaskServicesRest.createTask(child.id)!!.projekt?.kost)
    }

    private fun insertKost2(projekt: ProjektDO, kost2Art: Long): Kost2DO {
        val kost2 = Kost2DO()
        kost2.nummernkreis = 4
        kost2.bereich = projekt.internKost2_4!!
        kost2.teilbereich = projekt.nummer
        kost2.projekt = projekt
        kost2.kost2Art = Kost2ArtDO().withId(kost2Art)
        return kost2Dao.find(kost2Dao.insert(kost2))!!
    }

    private fun preview(
        id: Long?,
        parentTaskId: Long? = null,
        kost2BlackWhiteList: String? = null,
        kost2IsBlackList: Boolean = false,
        addKost2Id: Long? = null,
    ): TaskServicesRest.Kost2Preview {
        val request = TaskServicesRest.Kost2PreviewRequest(
            id = id,
            parentTaskId = parentTaskId,
            kost2BlackWhiteList = kost2BlackWhiteList,
            kost2IsBlackList = kost2IsBlackList,
            addKost2Id = addKost2Id,
        )
        return taskServicesRest.getKost2Preview(request).body!!
    }

    private fun lines(text: String?) = text?.lines()?.filter { it.isNotBlank() } ?: emptyList()
}
