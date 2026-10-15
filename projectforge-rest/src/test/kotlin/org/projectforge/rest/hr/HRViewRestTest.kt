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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.humanresources.HRPlanningEntryStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.HRPlanning
import org.projectforge.rest.dto.HRPlanningEntry
import org.projectforge.rest.dto.HRView
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Project
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.math.BigDecimal
import java.time.LocalDate

/**
 * The HR view of projectforge-next ([HRViewRest]): the planned days of the employees per project or customer, the
 * employees not planned, its access and its number of statements.
 */
class HRViewRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var hrViewRest: HRViewRest

    @Autowired
    private lateinit var hrPlanningEntityRest: HRPlanningEntityRest

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
                it.name = "HR view ltd."
                it.id = 62L
            }
            kundeDao.insert(kunde)
            portal = initTestDB.addProjekt(kunde, 0, "HR view portal")
            shop = initTestDB.addProjekt(kunde, 1, "HR view shop")
        }
    }

    @Test
    fun `the planned days are shown per project, the status entries as rest`() {
        logon(TEST_FINANCE_USER)
        val week = WEEK
        plan(TEST_USER, week, entry(portal, BigDecimal(16)), entry(shop, BigDecimal(4)), statusEntry(BigDecimal(8)))
        val view = view(week)
        val row = view.rows.single { it.userId == getUser(TEST_USER).id }
        assertTrue(row.planningId != null)
        assertDays("3.5", row.sum.planned)
        assertDays("1", row.rest.planned)
        assertNull(row.sum.actual, "Booked time sheets aren't shown by default.")
        assertDays("2", row.cells[projectColumn(view, portal)]?.planned)
        assertDays("0.5", row.cells[projectColumn(view, shop)]?.planned)
        assertTrue(view.fullAccess)

        // Grouped by customer: one column for both projects.
        val grouped = view(week, allProjectsGroupedByCustomer = true)
        val customerKey = grouped.columns.single { it.label == "HR view ltd." }.key
        assertTrue(grouped.columns.none { it.key.startsWith("p") })
        val groupedRow = grouped.rows.single { it.userId == getUser(TEST_USER).id }
        assertDays("2.5", groupedRow.cells[customerKey]?.planned)
        assertDays("1", groupedRow.rest.planned)

        // A planned employee isn't listed as unplanned, an unplanned one is.
        assertFalse(view.unplannedUsers.any { it.id == getUser(TEST_USER).id })
        assertTrue(view.unplannedUsers.any { it.id == getUser(TEST_USER2).id })
    }

    @Test
    fun `the view needs the right of the HR planning`() {
        logon(TEST_USER)
        assertThrows<AccessException> { view(WEEK) }
    }

    @Test
    fun `the view costs a constant number of statements, not one per planning`() {
        logon(TEST_FINANCE_USER)
        val single = WEEK.plusWeeks(10)
        plan(TEST_USER, single, entry(portal, BigDecimal(8)), entry(shop, BigDecimal(8)))
        val many = WEEK.plusWeeks(20)
        listOf(TEST_USER, TEST_USER2, TEST_EMPLOYEE_USER, TEST_HR_USER).forEach { user ->
            for (i in 0L until 2L) {
                plan(user, many.plusWeeks(i), entry(portal, BigDecimal(8)), entry(shop, BigDecimal(8)))
            }
        }
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        val wasEnabled = statistics.isStatisticsEnabled
        statistics.isStatisticsEnabled = true
        try {
            fun statementsOf(week: LocalDate, weeks: Long, expectedRows: Int): Long {
                statistics.clear()
                val view = view(week, stopDay = week.plusWeeks(weeks - 1).plusDays(6), showBookedTimesheets = true)
                assertEquals(expectedRows, view.rows.size)
                return statistics.prepareStatementCount
            }
            statementsOf(single, 1, 1) // Warms up caches (users, rights) not belonging to the view itself.
            assertEquals(statementsOf(single, 1, 1), statementsOf(many, 2, 4))
        } finally {
            statistics.isStatisticsEnabled = wasEnabled
        }
    }

    private fun view(
        week: LocalDate,
        stopDay: LocalDate? = null,
        showBookedTimesheets: Boolean = false,
        allProjectsGroupedByCustomer: Boolean = false,
    ): HRView {
        return hrViewRest.getView(
            startDay = week.toString(),
            stopDay = stopDay?.toString(),
            showPlanning = true,
            showBookedTimesheets = showBookedTimesheets,
            onlyMyProjects = false,
            allProjectsGroupedByCustomer = allProjectsGroupedByCustomer,
            otherProjectsGroupedByCustomer = false,
        )
    }

    private fun projectColumn(view: HRView, projekt: ProjektDO): String {
        return view.columns.single { it.key == "p${projekt.id}" }.key
    }

    private fun assertDays(expected: String, actual: BigDecimal?) {
        assertEquals(BigDecimal(expected).stripTrailingZeros(), actual?.stripTrailingZeros())
    }

    private fun plan(user: String, week: LocalDate, vararg entries: HRPlanningEntry) {
        val planning = HRPlanning(week = week, user = User(getUser(user))).also { it.entries = entries.toMutableList() }
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val postData = PostData(
            data = planning,
            watchFieldsTriggered = null,
            serverData = sessionCsrfService.createServerData(request),
        )
        val response = hrPlanningEntityRest.saveOrUpdate(request, postData)
        assertEquals(200, response.statusCode.value(), "${response.body?.validationErrors?.map { it.message }}")
    }

    private fun entry(projekt: ProjektDO, mondayHours: BigDecimal): HRPlanningEntry {
        return HRPlanningEntry(projekt = Project(projekt.id)).also { it.mondayHours = mondayHours }
    }

    private fun statusEntry(fridayHours: BigDecimal): HRPlanningEntry {
        return HRPlanningEntry(status = HRPlanningEntryStatus.OTHER).also { it.fridayHours = fridayHours }
    }

    companion object {
        /** Set in [beforeAll], which JUnit runs on an instance of its own. */
        private lateinit var portal: ProjektDO

        private lateinit var shop: ProjektDO

        /** Far from any other test's planning: a Monday. */
        private val WEEK = LocalDate.of(2032, 3, 1)
    }
}
