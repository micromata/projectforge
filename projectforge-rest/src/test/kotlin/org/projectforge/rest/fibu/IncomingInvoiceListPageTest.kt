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
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.rest.core.ListPageCache
import org.projectforge.rest.core.getList
import org.projectforge.rest.core.getListPage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpSession
import java.math.BigDecimal

/**
 * The invariant server-side paging rests on, for the incoming invoice (creditor) list: a paged result
 * ([getListPage]) is the same result the non-paged `POST list` ([getList]) returns, only cut into pages. See
 * [IncomingInvoiceEntityRest.sortIds] / [IncomingInvoiceEntityRest.aggregate] and `MIGRATION-list-paging.md`.
 */
class IncomingInvoiceListPageTest : AbstractTestBase() {
    @Autowired
    private lateinit var incomingInvoiceEntityRest: IncomingInvoiceEntityRest

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var listPageCache: ListPageCache

    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setup() {
        logon(TEST_FINANCE_USER)
        val session = MockHttpSession()
        request = Mockito.mock(HttpServletRequest::class.java)
        Mockito.`when`(request.getSession(false)).thenReturn(session)
    }

    @Test
    fun `paged result concatenates to the whole list in default order`() {
        insertInvoices(500, 200, 900, 100, 700)
        assertPagesMatchWholeList(MagicFilter(), pageSize = 2)
    }

    @Test
    fun `paged result concatenates to the whole list sorted by the computed net sum`() {
        // Two equal sums (200) so the datum tie-break is exercised identically by filterList and sortIds.
        insertInvoices(500, 200, 900, 200, 100, 700)
        val filter = MagicFilter()
        filter.sortProperties.add(SortProperty.desc("netSum"))
        assertPagesMatchWholeList(filter, pageSize = 2)
    }

    @Test
    fun `a deleted invoice drops out of the paged list without a gap and shrinks the total`() {
        val ids = insertInvoices(500, 200, 900, 100, 700)
        val before = collectPaged(MagicFilter(), pageSize = 2)
        val removed = ids[2]
        eingangsrechnungDao.markAsDeleted(eingangsrechnungDao.find(removed, checkAccess = false)!!, checkAccess = false)

        val after = collectPaged(MagicFilter(), pageSize = 2)
        assertEquals(before.totalSize - 1, after.totalSize, "The whole-result total drops by the deleted invoice.")
        assertTrue(removed !in after.ids, "The deleted invoice is no longer in any page.")
        assertEquals(after.ids.distinct(), after.ids, "No row appears twice across the page boundaries.")
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
            val page = getListPage(
                request, incomingInvoiceEntityRest, eingangsrechnungDao, listPageCache, filter.clone(), offset,
                pageSize, false,
            )
            total = page.totalSize ?: 0
            maxPageSize = maxOf(maxPageSize, page.resultSet.size)
            collected += page.resultSet.mapNotNull { it.id }
            offset += pageSize
            if (offset >= total) break
        }
        return PagedResult(collected, total, maxPageSize)
    }

    private fun wholeListIds(filter: MagicFilter): List<Long> =
        getList(request, incomingInvoiceEntityRest, eingangsrechnungDao, filter.clone()).resultSet.mapNotNull { it.id }

    /** Inserts one invoice per net sum (each with a single position), returning their ids in insert order. */
    private fun insertInvoices(vararg netSums: Int): List<Long> {
        return netSums.map { net ->
            val invoice = EingangsrechnungDO().also {
                it.datum = java.time.LocalDate.now()
                it.faelligkeit = java.time.LocalDate.now()
                it.kreditor = "Creditor $net"
                it.addPosition(EingangsrechnungsPositionDO().also { pos ->
                    pos.menge = BigDecimal.ONE
                    pos.einzelNetto = net.toBigDecimal()
                    pos.vat = BigDecimal.ZERO
                    pos.text = "Pos $net"
                })
            }
            eingangsrechnungDao.insert(invoice, checkAccess = false)
            invoice.id!!
        }
    }
}
