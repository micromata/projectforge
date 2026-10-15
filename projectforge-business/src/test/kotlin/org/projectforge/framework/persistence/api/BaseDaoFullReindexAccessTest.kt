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

package org.projectforge.framework.persistence.api

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.database.EntityCountCache
import org.springframework.beans.factory.annotation.Autowired

/**
 * Who may start a full re-index of an entity ([BaseDao.isFullReindexAllowedForLoggedInUser]): admins always,
 * everybody else below [BaseDao.FULL_REINDEX_MAX_ENTRIES_FOR_NON_ADMINS] entries.
 */
class BaseDaoFullReindexAccessTest : AbstractTestBase() {
    @Autowired
    private lateinit var bookDao: BookDao

    @Autowired
    private lateinit var entityCountCache: EntityCountCache

    @Test
    fun `count is cached until invalidated`() {
        logon(TEST_ADMIN_USER)
        val before = bookDao.countReindexEntries()
        bookDao.insert(BookDO().also {
            it.title = "Re-index count"
            it.status = BookStatus.PRESENT
        })
        Assertions.assertEquals(before, bookDao.countReindexEntries(), "cached")
        entityCountCache.invalidate(BookDO::class.java)
        Assertions.assertEquals(before + 1, bookDao.countReindexEntries(), "recounted")
    }

    @Test
    fun `small entity for everybody, large one for admins only`() {
        logon(TEST_USER)
        Assertions.assertTrue(bookDao.isFullReindexAllowedForLoggedInUser(), "few books")
        val large = largeBookDao(BaseDao.FULL_REINDEX_MAX_ENTRIES_FOR_NON_ADMINS)
        Assertions.assertFalse(large.isFullReindexAllowedForLoggedInUser(), "limit reached")
        Assertions.assertTrue(
            largeBookDao(BaseDao.FULL_REINDEX_MAX_ENTRIES_FOR_NON_ADMINS - 1).isFullReindexAllowedForLoggedInUser(),
            "just below the limit",
        )
        logon(TEST_ADMIN_USER)
        Assertions.assertTrue(large.isFullReindexAllowedForLoggedInUser(), "admin")
    }

    private fun largeBookDao(count: Long): BookDao {
        return object : BookDao() {
            override fun countReindexEntries(): Long = count
        }.also { it.accessChecker = bookDao.accessChecker }
    }
}
