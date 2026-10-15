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

package org.projectforge.rest.hr

import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.humanresources.HRPlanningDao
import org.projectforge.business.humanresources.HRPlanningEntryStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.HRPlanning
import org.projectforge.rest.dto.HRPlanningEntry
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Project
import org.projectforge.rest.dto.User
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The planned week saved through the edit page of projectforge-next ([HRPlanningEntityRest]) and listed by its
 * list ([HRPlanningEntryEntityRest]): the entries travel with their week, a deleted one is posted (not left
 * out), and the checks the removed Wicket edit form did.
 */
class HRPlanningEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var hrPlanningEntityRest: HRPlanningEntityRest

    @Autowired
    private lateinit var hrPlanningEntryEntityRest: HRPlanningEntryEntityRest

    @Autowired
    private lateinit var hrPlanningDao: HRPlanningDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    override fun beforeAll() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val kunde = KundeDO().also {
                it.name = "HR planning rest ltd."
                it.id = 61L
            }
            kundeDao.insert(kunde)
            projekt = initTestDB.addProjekt(kunde, 0, "HR planning portal")
        }
    }

    @Test
    fun `a week is saved with its entries, and a deleted entry stays as deleted`() {
        logon(TEST_FINANCE_USER)
        val planning = newPlanning(WEEK)
        planning.entries = mutableListOf(
            projectEntry(BigDecimal(8)),
            HRPlanningEntry(status = HRPlanningEntryStatus.OTHER).also { it.fridayHours = BigDecimal(4) },
        )
        val response = save(planning)
        assertEquals(200, response.statusCode.value(), errors(response))

        val id = hrPlanningDao.getEntry(getUser(TEST_USER).id, WEEK)?.id
        assertNotNull(id)
        val stored = hrPlanningEntityRest.getItem(id).body as HRPlanning
        assertEquals(2, stored.entries?.size)
        assertEquals(projekt.id, stored.entries!!.single { it.status == null }.projekt?.id)

        // The edit page posts a removed entry with deleted = true: one left out of the collection would be
        // removed physically.
        stored.entries!!.single { it.status == HRPlanningEntryStatus.OTHER }.deleted = true
        assertEquals(200, save(stored).statusCode.value())
        val reloaded = hrPlanningDao.find(id)!!
        assertEquals(2, reloaded.entries?.size)
        assertTrue(reloaded.entries!!.single { it.status == HRPlanningEntryStatus.OTHER }.deleted)
        assertFalse(reloaded.entries!!.single { it.projekt != null }.deleted)
    }

    @Test
    fun `an entry needs either a project or a status`() {
        logon(TEST_FINANCE_USER)
        val neither = newPlanning(WEEK.plusWeeks(1)).also { it.entries = mutableListOf(HRPlanningEntry()) }
        assertRefused(save(neither), "entries[0].projekt")

        val both = newPlanning(WEEK.plusWeeks(1)).also {
            it.entries = mutableListOf(projectEntry(BigDecimal.ONE).also { entry -> entry.status = HRPlanningEntryStatus.ILL })
        }
        assertRefused(save(both), "entries[0].projekt")

        // A deleted entry isn't checked: it may be an empty row the user removed again.
        val deleted = newPlanning(WEEK.plusWeeks(1)).also {
            it.entries = mutableListOf(projectEntry(BigDecimal.ONE), HRPlanningEntry().also { entry -> entry.deleted = true })
        }
        assertEquals(200, save(deleted).statusCode.value(), errors(save(deleted)))
    }

    @Test
    fun `there is one planned week per employee and week`() {
        logon(TEST_FINANCE_USER)
        val week = WEEK.plusWeeks(2)
        assertEquals(200, save(newPlanning(week).also { it.entries = mutableListOf(projectEntry(BigDecimal.ONE)) }).statusCode.value())
        // Any day of the same week is the same week.
        val again = newPlanning(week.plusDays(2)).also { it.entries = mutableListOf(projectEntry(BigDecimal.ONE)) }
        assertRefused(save(again), "week")
    }

    @Test
    fun `the list shows the entries of the period and employee, grouped by week on request`() {
        logon(TEST_FINANCE_USER)
        val week = WEEK.plusWeeks(3)
        val planning = newPlanning(week).also {
            it.entries = mutableListOf(
                projectEntry(BigDecimal(3)),
                HRPlanningEntry(status = HRPlanningEntryStatus.OTHER).also { entry -> entry.mondayHours = BigDecimal(2) },
            )
        }
        assertEquals(200, save(planning).statusCode.value())
        val planningId = hrPlanningDao.getEntry(getUser(TEST_USER).id, week)!!.id

        val rows = list(week, grouped = false)
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.planningId == planningId })
        assertEquals(BigDecimal(5), rows.fold(BigDecimal.ZERO) { sum, row -> sum + row.totalHours!! }.stripTrailingZeros())

        // Grouped: one row per week, linking to the week.
        val grouped = list(week, grouped = true)
        assertEquals(1, grouped.size)
        assertEquals(planningId, grouped.single().planningId)
        assertEquals(planningId, grouped.single().id)

        // Another week isn't in the period.
        assertEquals(0, list(week.plusWeeks(1), grouped = false).size)
    }

    @Test
    fun `the project of an entry is shown and filtered by`() {
        logon(TEST_FINANCE_USER)
        val week = WEEK.plusWeeks(5)
        val planning = newPlanning(week).also {
            it.entries = mutableListOf(
                projectEntry(BigDecimal(3)),
                HRPlanningEntry(status = HRPlanningEntryStatus.OTHER).also { entry -> entry.mondayHours = BigDecimal(2) },
            )
        }
        assertEquals(200, save(planning).statusCode.value())
        val planningId = hrPlanningDao.getEntry(getUser(TEST_USER).id, week)!!.id!!

        // The edit page shows the project's name in its picker, not only its id.
        val stored = hrPlanningEntityRest.getItem(planningId).body as HRPlanning
        assertEquals(projekt.displayName, stored.entries!!.single { it.status == null }.projekt?.displayName)

        // The project filter of the list picks any project (an autocompletion, as the legacy list's) and narrows
        // the list to its entries.
        val filter = listFilter(week, grouped = false)
        filter.entries.add(MagicFilterEntry("project").also { it.value.id = projekt.id })
        @Suppress("UNCHECKED_CAST")
        val rows = hrPlanningEntryEntityRest.getList(newRequest(), filter).resultSet as List<HRPlanningEntry>
        assertEquals(1, rows.size)
        assertEquals(projekt.id, rows.single().projekt?.id)
    }

    @Test
    fun `the list loads its rows with a constant number of statements, not one per row`() {
        logon(TEST_FINANCE_USER)
        val firstWeek = WEEK.plusWeeks(10)
        for (i in 0L until 4L) {
            val planning = newPlanning(firstWeek.plusWeeks(i)).also {
                it.entries = mutableListOf(projectEntry(BigDecimal(1)), projectEntry(BigDecimal(2)))
            }
            assertEquals(200, save(planning).statusCode.value())
        }
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val wasEnabled = statistics.isStatisticsEnabled
        statistics.isStatisticsEnabled = true
        try {
            fun statementsOf(weeks: Long, grouped: Boolean): Long {
                statistics.clear()
                val rows = list(firstWeek, grouped, weeks)
                assertEquals(if (grouped) weeks.toInt() else 2 * weeks.toInt(), rows.size)
                if (!grouped) {
                    rows.forEach { assertEquals("HR planning rest ltd.", it.kunde) }
                }
                return statistics.prepareStatementCount
            }
            listOf(false, true).forEach { grouped ->
                statementsOf(1, grouped) // Warms up caches (users, rights) not belonging to the list itself.
                assertEquals(statementsOf(1, grouped), statementsOf(4, grouped), "grouped=$grouped")
            }
        } finally {
            statistics.isStatisticsEnabled = wasEnabled
        }
    }

    private fun list(week: LocalDate, grouped: Boolean, weeks: Long = 1): List<HRPlanningEntry> {
        val result = hrPlanningEntryEntityRest.getList(newRequest(), listFilter(week, grouped, weeks))
        @Suppress("UNCHECKED_CAST")
        return result.resultSet as List<HRPlanningEntry>
    }

    private fun listFilter(week: LocalDate, grouped: Boolean, weeks: Long = 1): MagicFilter {
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry("period").also {
            it.value.fromValue = week.toString()
            it.value.toValue = week.plusWeeks(weeks - 1).plusDays(6).toString()
        })
        filter.entries.add(MagicFilterEntry("user").also { it.value.id = getUser(TEST_USER).id })
        if (grouped) {
            filter.entries.add(MagicFilterEntry("groupEntries").also { it.value.value = "true" })
        }
        return filter
    }

    private fun newPlanning(week: LocalDate): HRPlanning {
        return HRPlanning(week = week, user = User(getUser(TEST_USER)))
    }

    private fun projectEntry(mondayHours: BigDecimal): HRPlanningEntry {
        return HRPlanningEntry(projekt = Project(projekt.id)).also { it.mondayHours = mondayHours }
    }

    private fun save(planning: HRPlanning): ResponseEntity<ResponseAction> {
        val request = newRequest()
        val postData = PostData(
            data = planning,
            watchFieldsTriggered = null,
            serverData = sessionCsrfService.createServerData(request),
        )
        return hrPlanningEntityRest.saveOrUpdate(request, postData)
    }

    private fun newRequest() = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }

    private fun assertRefused(response: ResponseEntity<ResponseAction>, fieldId: String) {
        assertEquals(406, response.statusCode.value())
        assertTrue(response.body?.validationErrors?.any { it.fieldId == fieldId } == true, errors(response))
    }

    private fun errors(response: ResponseEntity<ResponseAction>): String {
        return response.body?.validationErrors?.joinToString { "${it.fieldId}: ${it.message}" } ?: "${response.body}"
    }

    companion object {
        /** Set in [beforeAll], which JUnit runs on an instance of its own. */
        private lateinit var projekt: ProjektDO

        /** Far from any other test's planning: a Monday. */
        private val WEEK = LocalDate.of(2031, 3, 3)
    }
}
