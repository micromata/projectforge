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

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.core.getList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpSession

/**
 * The order book searched by the name of the project or customer must find an order after every save the next app
 * does — which posts kunde and projekt as id-only stubs (`Auftrag.copyTo`, `Project.copyTo`) — not only after a
 * reindex.
 */
class OrderSearchStabilityTest : AbstractTestBase() {
    @Autowired
    private lateinit var orderEntityRest: OrderEntityRest

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setup() {
        logon(TEST_FINANCE_USER)
        val session = MockHttpSession()
        request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getSession(false)).thenReturn(session)
    }

    @Test
    fun `an order is found by project and customer after every save`() {
        val kunde = KundeDO().also {
            it.nummer = 860
            it.name = "Blorptronic GmbH"
        }
        kundeDao.insert(kunde, checkAccess = false)
        val projekt = ProjektDO().also {
            it.name = "Flimwazzle Support"
            it.kunde = kunde
            it.nummer = 1
        }
        projektDao.insert(projekt, checkAccess = false)
        val projektId = projekt.id!!

        val id = auftragDao.insert(posted(null, projektId, "first"), checkAccess = false)!!
        assertFound(id, "insert")

        auftragDao.update(posted(id, projektId, "second"), checkAccess = false)
        assertFound(id, "update of the order")

        // The project saved in the next app: its customer is a stub as well.
        val storedProjekt = projektDao.find(projektId, checkAccess = false)!!
        projektDao.update(ProjektDO().also {
            it.id = storedProjekt.id
            it.name = storedProjekt.name
            it.nummer = storedProjekt.nummer
            it.projektnummer = storedProjekt.projektnummer
            it.kunde = KundeDO().also { k -> k.nummer = kunde.nummer }
            it.description = "edited"
        }, checkAccess = false)
        Thread.sleep(2000) // The dependent orders are reindexed in a thread of their own.
        assertFound(id, "update of the project")

        auftragDao.update(posted(id, projektId, "third"), checkAccess = false)
        assertFound(id, "second update of the order")
    }

    private fun assertFound(id: Long, step: String) {
        listOf("flimwaz", "blorptro").forEach { search ->
            assertEquals(setOf(id), listIds(search), "After $step: not found by '$search'.")
        }
    }

    /** The order as the next app posts it: a fresh object, kunde and projekt as id-only stubs. */
    private fun posted(id: Long?, projektId: Long, bemerkung: String): AuftragDO {
        val stored = id?.let { auftragDao.find(it, attached = true, checkAccess = false) }
        return AuftragDO().also {
            it.id = id
            it.nummer = stored?.nummer ?: auftragDao.getNextNumber(it)
            it.status = AuftragsStatus.GELEGT
            it.titel = "Order $bemerkung"
            it.kunde = KundeDO().also { k -> k.nummer = 860 }
            it.projekt = ProjektDO().also { p -> p.id = projektId }
            it.bemerkung = bemerkung
            if (stored != null) {
                stored.positionenIncludingDeleted?.forEach { pos -> it.addPosition(pos) }
            } else {
                it.addPosition(AuftragsPositionDO().also { pos -> pos.status = AuftragsStatus.GELEGT; pos.titel = "Pos" })
            }
        }
    }

    private fun listIds(search: String): Set<Long> {
        val filter = MagicFilter()
        filter.searchString = search
        filter.autoWildcardSearch = true // As the list endpoints set it.
        return getList(request, orderEntityRest, auftragDao, filter).resultSet.mapNotNull { it.id }.toSet()
    }
}
