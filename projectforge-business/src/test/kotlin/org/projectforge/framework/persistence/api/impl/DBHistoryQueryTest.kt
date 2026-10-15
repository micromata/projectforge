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

package org.projectforge.framework.persistence.api.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.time.PFDateTime
import org.springframework.beans.factory.annotation.Autowired

/**
 * Searching the change history of an entity: by the user who modified it, by the period it was modified in
 * and by a value the history holds.
 *
 * The value search is the interesting one: it looks at HistoryEntryAttrDO, not at HistoryEntryDO, and used to
 * run as a full text query over the latter's index — where `oldValue` doesn't exist, so Hibernate Search threw
 * `Unknown field 'oldValue'`, DBQuery swallowed it and the list came back empty for every term.
 */
class DBHistoryQueryTest : AbstractTestBase() {
    @Autowired
    private lateinit var bookDao: BookDao

    @Test
    fun `search the history of a book by value, user and period`() {
        logon(TEST_ADMIN_USER)
        val before = PFDateTime.now().minusDays(1)
        val book = BookDO().also {
            it.title = "History query test"
            it.authors = "Ada Lovelace"
            it.status = BookStatus.PRESENT
        }
        val id = bookDao.insert(book)
        book.authors = "Grace Hopper"
        bookDao.update(book)

        // The new value of the update, which only HistoryEntryAttrDO knows.
        Assertions.assertEquals(listOf(id), idsOf(searchHistory = "Grace Hopper"))
        // …and the old one it replaced.
        Assertions.assertEquals(listOf(id), idsOf(searchHistory = "Ada Lovelace"))
        // A part of a value is enough: the term is wrapped in wildcards unless it carries one itself.
        Assertions.assertEquals(listOf(id), idsOf(searchHistory = "race Hop"))
        // Case is irrelevant, as everywhere else in the search.
        Assertions.assertEquals(listOf(id), idsOf(searchHistory = "grace hopper"))
        Assertions.assertTrue(idsOf(searchHistory = "Charles Babbage").isEmpty())

        // The other two criteria, and the value combined with them — one query answers all three.
        val userId = getUserId(TEST_ADMIN_USER)
        Assertions.assertEquals(listOf(id), idsOf(modifiedByUserId = userId))
        Assertions.assertEquals(listOf(id), idsOf(modifiedFrom = before))
        Assertions.assertEquals(
            listOf(id),
            idsOf(searchHistory = "Grace Hopper", modifiedByUserId = userId, modifiedFrom = before)
        )
        // A period the change cannot fall into excludes it, even though the value matches.
        Assertions.assertTrue(
            idsOf(searchHistory = "Grace Hopper", modifiedTo = before).isEmpty()
        )
    }

    /** The ids of the books the given history criteria select, restricted to the one this test created. */
    private fun idsOf(
        searchHistory: String? = null,
        modifiedByUserId: Long? = null,
        modifiedFrom: PFDateTime? = null,
        modifiedTo: PFDateTime? = null,
    ): List<Long?> {
        val filter = QueryFilter().also {
            it.searchHistory = searchHistory
            it.modifiedByUserId = modifiedByUserId
            it.modifiedFrom = modifiedFrom
            it.modifiedTo = modifiedTo
        }
        return bookDao.select(filter).filter { it.title == "History query test" }.map { it.id }
    }
}
