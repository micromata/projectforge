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

package org.projectforge.rest.fibu

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
import org.projectforge.business.fibu.customergroup.BusinessUnit
import org.projectforge.business.fibu.customergroup.CustomerDirectory
import org.projectforge.business.fibu.customergroup.CustomerGroup
import org.projectforge.business.fibu.customergroup.CustomerGroupConfig
import org.projectforge.business.fibu.customergroup.CustomerGroupIndex
import org.projectforge.business.fibu.customergroup.ProjectRef
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.springframework.beans.factory.annotation.Autowired

/**
 * The business-unit criteria give the same orders in SQL as in memory, above all for the remainder
 * ([BusinessUnitChecklistFilter.NONE_KEY]): its negations over a missing customer, free text or project are not
 * true in SQL, so the rows without one must be asked for explicitly.
 */
class BusinessUnitChecklistFilterDbTest : AbstractTestBase() {
    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Test
    fun `sql and memory agree on the rows of each business unit and of the remainder`() {
        val acme = insertKunde(9101, "ACME Logistics")
        val retail = insertKunde(9102, "Retail AG")
        val internal = insertKunde(9103, "Internal")
        val other = insertKunde(9104, "Other GmbH")
        val retailProject = insertProjekt("retail-project")
        val otherProject = insertProjekt("other-project")
        val orders = listOf(
            insertOrder(acme),
            insertOrder(retail),
            insertOrder(other),
            insertOrder(internal, project = retailProject),
            insertOrder(internal, project = otherProject),
            insertOrder(text = "Müller Spedition"),
            insertOrder(text = "Somebody"),
            insertOrder(text = "Somebody", project = retailProject),
            insertOrder(acme, project = retailProject),
            insertOrder(),
        )
        val index = CustomerGroupIndex(
            CustomerGroupConfig(
                groups = mutableListOf(CustomerGroup().also {
                    it.key = "acmeg1"
                    it.name = "ACME"
                    it.customers = mutableListOf(acme.nummer!!)
                }),
                businessUnits = mutableListOf(
                    BusinessUnit().also {
                        it.key = "logbu1"
                        it.name = "Logistics"
                        it.groups = mutableListOf("acmeg1")
                        it.texts = mutableListOf("*Spedition")
                    },
                    BusinessUnit().also {
                        it.key = "retbu2"
                        it.name = "Retail"
                        it.customers = mutableListOf(retail.nummer!!)
                        it.tasks = mutableListOf(10)
                    },
                ),
            ),
            // The retail project lies below task 10 (Retail), the other project below no business unit's task.
            CustomerDirectory(
                projects = mapOf(retailProject.id!! to ProjectRef(null, 11), otherProject.id!! to ProjectRef(null, 30)),
                taskPath = { mapOf(11L to listOf(1L, 10L, 11L), 30L to listOf(1L, 30L))[it] },
            ),
        )
        val filter = BusinessUnitChecklistFilter("order/businessUnitFilterValues", groupIndex = { index })
        val ids = orders.map { it.id!! }
        listOf(
            arrayOf("b:logbu1"),
            arrayOf("b:retbu2"),
            arrayOf(BusinessUnitChecklistFilter.NONE_KEY),
            arrayOf("b:retbu2", BusinessUnitChecklistFilter.NONE_KEY),
        ).forEach { keys ->
            val predicate = filter.buildPredicate(keys)!!
            val inMemory = orders.filter { predicate.match(it) }.map { it.id!! }.toSet()
            val inSql = auftragDao.select(QueryFilter().add(predicate).add(DBPredicate.IsIn("id", ids)), checkAccess = false)
                .map { it.id!! }.toSet()
            assertEquals(inMemory, inSql, "Keys ${keys.joinToString()}")
        }
        // The remainder is exactly what no business unit claims:
        val none = filter.buildPredicate(arrayOf(BusinessUnitChecklistFilter.NONE_KEY))!!
        assertEquals(
            setOf(orders[2], orders[4], orders[6], orders[9]).map { it.id }.toSet(),
            orders.filter { none.match(it) }.map { it.id }.toSet(),
        )
    }

    private fun insertKunde(nummer: Long, name: String) = KundeDO().also {
        it.nummer = nummer
        it.name = name
        kundeDao.insert(it, checkAccess = false)
    }

    private fun insertProjekt(name: String) = ProjektDO().also {
        it.name = name
        projektDao.insert(it, checkAccess = false)
    }

    private fun insertOrder(kunde: KundeDO? = null, text: String? = null, project: ProjektDO? = null) =
        AuftragDO().also {
            it.nummer = auftragDao.nextNumber
            it.status = AuftragsStatus.GELEGT
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
