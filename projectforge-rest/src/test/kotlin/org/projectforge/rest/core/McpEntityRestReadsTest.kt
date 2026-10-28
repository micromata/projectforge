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

package org.projectforge.rest.core

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.time.PFDateTime.Companion.withDate
import org.projectforge.rest.AddressEntityRest
import org.projectforge.rest.TimesheetEntityRest
import org.projectforge.rest.fibu.CustomerPagesRest
import org.projectforge.rest.fibu.OutgoingInvoiceEntityRest
import org.projectforge.rest.fibu.ProjectEntityRest
import org.projectforge.rest.task.TaskEntityRest
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month

/**
 * The reads the MCP server (`org.projectforge.mcp.McpDatasets` in projectforge-application) makes through the rest
 * classes: the filter fields of the list page, the list rows ([AbstractEntityRest.getResultList] and
 * [AbstractEntityRest.createListRow]) and the edit page's entry ([AbstractEntityRest.getItem]), all serializable
 * as JSON, and refused for a user without the right. The reads of invoices with their attachments, which need a JCR
 * repository, are tested in `OutgoingInvoicePdfTest`.
 */
class McpEntityRestReadsTest : AbstractTestBase() {
    @Autowired
    private lateinit var projectRest: ProjectEntityRest

    @Autowired
    private lateinit var customerRest: CustomerPagesRest

    @Autowired
    private lateinit var taskRest: TaskEntityRest

    @Autowired
    private lateinit var timesheetRest: TimesheetEntityRest

    @Autowired
    private lateinit var addressRest: AddressEntityRest

    @Autowired
    private lateinit var invoiceRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    private val mapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun `every dataset offers its filter fields and serializable list rows`() {
        logon(TEST_FINANCE_USER)
        listOf(projectRest, customerRest, taskRest, timesheetRest, addressRest, invoiceRest).forEach { rest ->
            assertTrue(filterElements(rest).isNotEmpty(), "Filter fields of ${rest::class.simpleName}")
            erased(rest).getResultList(MagicFilter(maxRows = 5)).forEach { mapper.writeValueAsString(erased(rest).createListRow(it)) }
        }
        assertTrue(filterElements(timesheetRest).any { it.id == "period" && it.filterType == UIFilterElement.FilterType.DATE })
    }

    @Test
    fun `invoices are listed for the finance user and refused for an ordinary user`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice("MCP read")

        val rows = erased(invoiceRest).getResultList(MagicFilter(searchString = "MCP read"))
        assertEquals(listOf(id), rows.map { it.id })
        assertTrue(mapper.writeValueAsString(erased(invoiceRest).createListRow(rows.first())).contains("MCP read"))

        logon(TEST_USER)
        assertThrows(AccessException::class.java) { invoiceRest.getResultList(MagicFilter(searchString = "MCP read")) }
    }

    @Test
    fun `time sheets are found by period and user`() {
        val prefix = "mcp-reads-"
        persistenceService.runInTransaction { _ ->
            logon(TEST_ADMIN_USER)
            initTestDB.addTask(prefix + "task", "root")
            initTestDB.addUser(prefix + "user")
            timesheetDao.insert(TimesheetDO().also {
                it.startTime = withDate(2012, Month.APRIL, 3, 9, 0, 0).sqlTimestamp
                it.stopTime = withDate(2012, Month.APRIL, 3, 10, 0, 0).sqlTimestamp
                it.task = initTestDB.getTask(prefix + "task")
                it.user = getUser(prefix + "user")
                it.description = "MCP sheet"
            }, checkAccess = false)
        }
        logon(TEST_ADMIN_USER)
        val userId = getUserId(prefix + "user")
        fun search(from: String, to: String): List<TimesheetDO> = timesheetRest.getResultList(MagicFilter(entries = mutableListOf(
            MagicFilterEntry("period").also { it.value.fromValue = from; it.value.toValue = to },
            MagicFilterEntry("user").also { it.value.id = userId },
        )))

        val found = search("2012-04-01", "2012-04-30")
        assertEquals(listOf("MCP sheet"), found.map { it.description })
        assertNotNull(mapper.writeValueAsString(timesheetRest.createListRow(found.first())))
        assertEquals(HttpStatus.OK, timesheetRest.getItem(found.first().id).statusCode)
        assertTrue(search("2012-05-01", "2012-05-31").isEmpty())
        // Without a period or a task the list stays empty, as on the list page.
        assertTrue(timesheetRest.getResultList(MagicFilter(entries = mutableListOf(
            MagicFilterEntry("user").also { it.value.id = userId })
        )).isEmpty())
    }

    private fun insertInvoice(subject: String): Long = rechnungDao.insert(RechnungDO().also {
        it.status = RechnungStatus.GEPLANT
        it.typ = RechnungTyp.RECHNUNG
        it.datum = LocalDate.of(2026, 3, 2)
        it.betreff = subject
        it.kundeText = "MCP customer"
        it.positionen = mutableListOf(RechnungsPositionDO().also { pos ->
            pos.rechnung = it
            pos.number = 1
            pos.menge = BigDecimal.ONE
            pos.einzelNetto = BigDecimal("100.00")
            pos.vat = BigDecimal("0.19")
        })
    })

    private fun filterElements(rest: AbstractEntityRest<*, *, *>): List<UIFilterElement> =
        LayoutListFilterUtils.createNamedSearchFilterContainer(rest, LayoutContext(rest.baseDao.doClass)).content
            .filterIsInstance<UIFilterElement>()

    @Suppress("UNCHECKED_CAST")
    private fun erased(rest: AbstractEntityRest<*, *, *>) =
        rest as AbstractEntityRest<ExtendedBaseDO<Long>, Any, BaseDao<ExtendedBaseDO<Long>>>
}
