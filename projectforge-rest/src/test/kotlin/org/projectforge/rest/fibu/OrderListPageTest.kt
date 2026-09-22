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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.rest.core.ListPageCache
import org.projectforge.rest.core.getList
import org.projectforge.rest.core.getListPage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpSession

/**
 * The invariant server-side paging rests on: a paged order list ([getListPage]) is the same result the
 * non-paged `POST list` ([getList]) returns, only cut into pages — same rows, same order, same total. If
 * the two ever drift, the footer's total or a page boundary would lie. See `MIGRATION-list-paging.md`.
 */
class OrderListPageTest : AbstractTestBase() {
    @Autowired
    private lateinit var orderEntityRest: OrderEntityRest

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var listPageCache: ListPageCache

    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setup() {
        logon(TEST_FINANCE_USER)
        // ListPageCache stores the materialized id list in the session; a real session lets a cache hit
        // (page 2 reusing page 1's list) be exercised as it is in production.
        val session = MockHttpSession()
        request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getSession(false)).thenReturn(session)
    }

    @Test
    fun `paged result concatenates to the whole list in default order`() {
        insertOrders(500, 200, 900, 100, 700)
        assertPagesMatchWholeList(MagicFilter(), pageSize = 2)
    }

    @Test
    fun `paged result concatenates to the whole list sorted by the computed net sum`() {
        // Two equal sums (200) so the nummer tie-break is exercised: filterList and sortIds must resolve
        // it identically, or the two orderings would part at the tie.
        insertOrders(500, 200, 900, 200, 100, 700)
        val filter = MagicFilter()
        filter.sortProperties.add(SortProperty.desc("nettoSumme"))
        assertPagesMatchWholeList(filter, pageSize = 2)
    }

    @Test
    fun `a deleted order drops out of the paged list without a gap and shrinks the total`() {
        val ids = insertOrders(500, 200, 900, 100, 700)
        // Materialize and cache the id list first, so the delete happens behind an existing cache — the
        // change counter must then rebuild it rather than serve a page pointing at the gone row.
        val before = collectPaged(MagicFilter(), pageSize = 2)
        val removed = ids[2]
        auftragDao.markAsDeleted(auftragDao.find(removed, checkAccess = false)!!, checkAccess = false)

        val after = collectPaged(MagicFilter(), pageSize = 2)
        assertEquals(before.totalSize - 1, after.totalSize, "The whole-result total drops by the deleted order.")
        assertTrue(removed !in after.ids, "The deleted order is no longer in any page.")
        assertEquals(after.ids.distinct(), after.ids, "No row appears twice across the page boundaries.")
        // Still the same result the non-paged path returns, now over one fewer order.
        assertEquals(wholeListIds(MagicFilter()), after.ids)
    }

    /** Asserts that walking every page of [filter] yields exactly the non-paged `POST list` result. */
    private fun assertPagesMatchWholeList(filter: MagicFilter, pageSize: Int) {
        val whole = wholeListIds(filter)
        val paged = collectPaged(filter, pageSize)
        assertEquals(whole.size, paged.totalSize, "totalSize is the size of the whole result, not of a page.")
        assertEquals(whole, paged.ids, "The pages concatenate to the whole list in the same order.")
        assertTrue(paged.maxPageSize <= pageSize, "No page is larger than the requested page size.")
    }

    private data class PagedResult(val ids: List<Long>, val totalSize: Int, val maxPageSize: Int)

    private fun collectPaged(filter: MagicFilter, pageSize: Int): PagedResult {
        val collected = mutableListOf<Long>()
        var offset = 0
        var total: Int
        var maxPageSize = 0
        while (true) {
            // A fresh filter per page: getListPage builds its query filter destructively, and the client
            // sends a new one each time as well. The fingerprint (hence the cache) is the same either way.
            val page = getListPage(request, orderEntityRest, auftragDao, listPageCache, filter.clone(), offset, pageSize, false)
            total = page.totalSize ?: 0
            maxPageSize = maxOf(maxPageSize, page.resultSet.size)
            collected += page.resultSet.mapNotNull { it.id }
            offset += pageSize
            if (offset >= total) break
        }
        return PagedResult(collected, total, maxPageSize)
    }

    private fun wholeListIds(filter: MagicFilter): List<Long> =
        getList(request, orderEntityRest, auftragDao, filter.clone()).resultSet.mapNotNull { it.id }

    /** Inserts one order per net sum (each with a single position), returning their ids in insert order. */
    private fun insertOrders(vararg netSums: Int): List<Long> {
        return netSums.map { net ->
            val order = AuftragDO().also {
                it.nummer = auftragDao.nextNumber
                it.status = AuftragsStatus.GELEGT
                it.addPosition(AuftragsPositionDO().also { pos ->
                    pos.titel = "Pos $net"
                    pos.nettoSumme = net.toBigDecimal()
                    pos.status = AuftragsStatus.GELEGT
                })
            }
            auftragDao.insert(order, checkAccess = false)
            order.id!!
        }
    }
}
