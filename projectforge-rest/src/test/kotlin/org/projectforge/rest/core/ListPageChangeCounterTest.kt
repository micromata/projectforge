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

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookStatus
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

/**
 * A change bumps the counter of its entity before the commit and once more when its transaction ends: a paged id
 * list cached in between (with the old database and index state) must not count as current afterwards.
 */
class ListPageChangeCounterTest : AbstractTestBase() {
    @Autowired
    private lateinit var bookDao: BookDao

    @Autowired
    private lateinit var changeCounter: ListPageChangeCounter

    @Test
    fun `the counter moves on again after the commit`() {
        logon(TEST_ADMIN_USER)
        val before = changeCounter.getCounter(BookDO::class.java)
        var inTransaction = 0L
        persistenceService.runInTransaction {
            bookDao.insert(BookDO().also {
                it.title = "ListPageChangeCounterTest"
                it.status = BookStatus.PRESENT
            })
            inTransaction = changeCounter.getCounter(BookDO::class.java)
        }
        assertTrue(inTransaction > before, "bumped by the change")
        assertTrue(changeCounter.getCounter(BookDO::class.java) > inTransaction, "bumped again after the commit")
    }
}
