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

package org.projectforge.business.fibu.customergroup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

/**
 * The customers worked for are read from the orders (and timesheets) of the database, and only these are
 * offered by the customer-group editor: a customer known only from invoices (a debtor) is left out.
 */
class CustomerUsageCacheDbTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var customerUsageCache: CustomerUsageCache

    @Autowired
    private lateinit var customerGroupService: CustomerGroupService

    @Test
    fun `orders make customers, projects without one take their orders' customers`() {
        val ordered = insertKunde(9201, "Ordered GmbH")
        insertKunde(9202, "Debitor GmbH")
        val project = ProjektDO().also {
            it.name = "usage-project"
            projektDao.insert(it, checkAccess = false)
        }
        insertOrder(ordered, date = LocalDate.of(2024, 3, 1))
        insertOrder(text = " Freitext GmbH ", project = project, date = LocalDate.of(2025, 3, 1))

        val usage = customerUsageCache.all
        assertEquals(LocalDate.of(2024, 3, 1), usage.orders[CustomerKey(9201) to null])
        assertEquals(LocalDate.of(2025, 3, 1), usage.orders[CustomerKey(null, "Freitext GmbH") to project.id])
        assertEquals(
            listOf(CustomerKey(null, "Freitext GmbH")),
            usage.projectCustomers(mapOf(project.id!! to null))[project.id],
        )

        val matches = customerGroupService.matches(listOf("*GmbH"))
        assertEquals(listOf("9201 - Ordered GmbH"), matches.customers.filter { it.endsWith("GmbH") })
        assertEquals(listOf("Freitext GmbH"), matches.freeTexts)
    }

    private fun insertKunde(nummer: Long, name: String) = KundeDO().also {
        it.nummer = nummer
        it.name = name
        kundeDao.insert(it, checkAccess = false)
    }

    private fun insertOrder(kunde: KundeDO? = null, text: String? = null, project: ProjektDO? = null, date: LocalDate) =
        AuftragDO().also {
            it.nummer = auftragDao.nextNumber
            it.status = AuftragsStatus.GELEGT
            it.angebotsDatum = date
            it.addPosition(AuftragsPositionDO().also { pos ->
                pos.titel = "Pos 1"
                pos.status = AuftragsStatus.GELEGT
            })
            it.kunde = kunde
            it.kundeText = text
            it.projekt = project
            auftragDao.insert(it, checkAccess = false)
        }
}
