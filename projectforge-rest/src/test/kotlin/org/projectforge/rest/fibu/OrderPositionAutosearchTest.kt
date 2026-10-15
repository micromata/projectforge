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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.springframework.beans.factory.annotation.Autowired

/**
 * `order/positionAutosearch`, the search behind the "which order position does this invoice position bill"
 * field of the invoice form (Wicket's `AuftragsPositionFormComponent`).
 *
 * Three things are worth a test here, and all three are ways the endpoint could look like it works:
 * the `###.##` shortcut has to win over the text search (a term of digits would otherwise match every
 * order whose title contains them), a hit has to carry the order behind the position (the client shows
 * `<order>.<position>` and links to the order, and has no other source for either), and both paths have to
 * check the access - the `###.##` one goes through [AuftragDao.getAuftragsPosition], which queries the
 * database directly and checks nothing at all.
 */
class OrderPositionAutosearchTest : AbstractTestBase() {

    @Autowired
    private lateinit var orderEntityRest: OrderEntityRest

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Test
    fun `an order position is found by its formatted number`() {
        logon(TEST_FINANCE_USER)
        val order = persistenceService.runInTransaction { _ -> newOrder("autosearch by number") }

        val hits = orderEntityRest.positionAutosearch("${order.nummer}.1", maxResults = null)

        assertEquals(1, hits.size, "The `###.##` shortcut names exactly one position.")
        val hit = hits.single()
        assertEquals(order.id, hit.auftragId, "The order behind the position: the row header links to it.")
        assertEquals(order.nummer, hit.auftragNummer)
        assertEquals(1.toShort(), hit.number)
        assertNotNull(hit.id, "The id is what the invoice position stores.")
        // Wicket's label, built server side because it is a sentence about the order.
        assertTrue(
            hit.displayName!!.startsWith("${order.nummer}.1: "),
            "Named by order and position number first: ${hit.displayName}",
        )
        assertTrue(hit.displayName!!.contains("autosearch by number"), hit.displayName)
    }

    @Test
    fun `an order is found by the name of its project, with a hit per position`() {
        logon(TEST_FINANCE_USER)
        val order = persistenceService.runInTransaction { _ ->
            val projekt = ProjektDO()
            projekt.nummer = 81
            projekt.name = "Autosearchproject"
            projekt.internKost2_4 = 81
            projektDao.insert(projekt)
            newOrder("autosearch by project", projekt, positions = 2)
        }

        val hits = orderEntityRest.positionAutosearch("Autosearchproject", maxResults = null)

        // Both positions of the order, not the order itself: the field picks a position.
        assertEquals(2, hits.count { it.auftragId == order.id }, "Every position of the matching order.")
        assertTrue(hits.all { it.id != null })
    }

    @Test
    fun `nothing is searched without a term, and nothing found for a term that matches nothing`() {
        logon(TEST_FINANCE_USER)
        // A field being cleared must not answer with the whole order book.
        assertTrue(orderEntityRest.positionAutosearch(null, maxResults = null).isEmpty())
        assertTrue(orderEntityRest.positionAutosearch("   ", maxResults = null).isEmpty())
        assertTrue(orderEntityRest.positionAutosearch("999999.1", maxResults = null).isEmpty())
    }

    @Test
    fun `a user without the order right may not search at all`() {
        logon(TEST_USER)
        // A plain `@GetMapping` gets none of `AbstractPagesRest`'s checks, so the endpoint has to ask itself.
        assertThrows<AccessException> { orderEntityRest.positionAutosearch("1.1", maxResults = null) }
    }

    @Test
    fun `an order the user may not read is not named by its number either`() {
        logon(TEST_FINANCE_USER)
        val order = persistenceService.runInTransaction { _ -> newOrder("autosearch foreign order") }

        // A project manager who is neither contact person nor member of the project's manager group has no
        // select access to this order (`AuftragRight.hasAccess`). The direct hit must not leak it: it would
        // answer with the customer, the project and the title of an order the user may not open.
        logon(TEST_PROJECT_MANAGER_USER)
        val hits = orderEntityRest.positionAutosearch("${order.nummer}.1", maxResults = null)

        // Empty rather than an exception: whether an order with this number exists is itself privileged, and
        // Wicket's field says "not found" for an unknown and for a forbidden number alike.
        assertTrue(hits.isEmpty(), "No hit for an order without select access.")
    }

    @Test
    fun `maxResults bounds what a two character term may produce`() {
        logon(TEST_FINANCE_USER)
        persistenceService.runInTransaction { _ ->
            val projekt = ProjektDO()
            projekt.nummer = 82
            projekt.name = "Autosearchlimited"
            projekt.internKost2_4 = 82
            projektDao.insert(projekt)
            newOrder("autosearch limit", projekt, positions = 3)
        }

        val hits = orderEntityRest.positionAutosearch("Autosearchlimited", maxResults = 2)

        assertEquals(2, hits.size, "The flattened positions of every matching order are easily thousands.")
    }

    private fun newOrder(title: String, projekt: ProjektDO? = null, positions: Int = 1): AuftragDO {
        val order = AuftragDO()
        order.status = AuftragsStatus.GELEGT
        order.titel = title
        order.projekt = projekt
        order.nummer = auftragDao.getNextNumber(order)
        repeat(positions) { i ->
            order.addPosition(AuftragsPositionDO().also {
                it.status = AuftragsStatus.GELEGT
                // Titled, and it has to be: `AuftragDao.onInsertOrModify` drops an unsaved position it
                // considers empty (`AuftragsPositionDO.isEmpty`), so an untitled one never reaches the search.
                it.titel = "$title, position ${i + 1}"
            })
        }
        auftragDao.insert(order)
        return order
    }
}
