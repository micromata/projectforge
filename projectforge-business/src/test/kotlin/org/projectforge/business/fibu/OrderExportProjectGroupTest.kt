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

package org.projectforge.business.fibu

import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.customergroup.CustomerGroupConfig
import org.projectforge.business.fibu.customergroup.CustomerGroupConfigTest.Companion.projectGroup
import org.projectforge.business.fibu.customergroup.CustomerGroupService
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.i18n.translate
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream
import java.time.LocalDate

/** The order book's Excel export has a project group column only if one of the exported orders has a group. */
class OrderExportProjectGroupTest : AbstractTestBase() {
    @Autowired
    private lateinit var customerGroupService: CustomerGroupService

    @Autowired
    private lateinit var orderExport: OrderExport

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Test
    fun `the project group column is there only if an order has a group`() {
        logon(TEST_FINANCE_USER)
        val projekt = ProjektDO().also {
            it.nummer = 8
            it.name = "OrderExportProjectGroupTest"
            projektDao.insert(it, checkAccess = false)
        }
        val order = AuftragDO().also {
            it.id = 3L
            it.nummer = 3
            it.angebotsDatum = LocalDate.now()
            it.titel = "Project group order"
            it.status = AuftragsStatus.BEAUFTRAGT
            it.projekt = projekt
            it.addPosition(AuftragsPositionDO().also { pos -> pos.titel = "Position" })
        }
        val header = translate("fibu.projectGroup")
        assertFalse(header in headers(orderExport.export(listOf(order)), 0), "No group, no column.")
        try {
            val errors = customerGroupService.save(
                CustomerGroupConfig(projectGroups = mutableListOf(projectGroup(null, "Parent", listOf(projekt.id!!)))),
                customerGroupService.lastUpdate,
            )
            assertEquals(emptyList<String>(), errors.map { it.messageKey })
            val export = orderExport.export(listOf(order))
            listOf(0, 1).forEach { sheetIndex ->
                val headers = headers(export, sheetIndex)
                val col = headers.indexOf(header)
                assertEquals(headers.indexOf(translate("fibu.projekt")) + 1, col, "Behind the project, sheet $sheetIndex.")
                WorkbookFactory.create(ByteArrayInputStream(export)).use {
                    assertEquals("Parent", it.getSheetAt(sheetIndex).getRow(1).getCell(col).stringCellValue)
                }
            }
        } finally {
            customerGroupService.save(CustomerGroupConfig(), customerGroupService.lastUpdate)
        }
    }

    private fun headers(export: ByteArray?, sheetIndex: Int): List<String> =
        WorkbookFactory.create(ByteArrayInputStream(export)).use { workbook ->
            workbook.getSheetAt(sheetIndex).getRow(0)
                .map { if (it.cellType == CellType.STRING) it.stringCellValue else "" }
        }
}
