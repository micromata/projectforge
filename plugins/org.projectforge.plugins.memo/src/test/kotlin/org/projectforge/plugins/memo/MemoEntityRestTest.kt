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


package org.projectforge.plugins.memo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.memo.rest.MemoEntityRest
import org.projectforge.rest.core.AbstractEntityRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest

/**
 * The memos behind the next page ([MemoEntityRest]): a memo is the owner's only - nobody else lists, reads,
 * changes or deletes it, and a posted owner never takes over a memo.
 */
class MemoEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var memoDao: MemoDao

    @Autowired
    private lateinit var memoEntityRest: MemoEntityRest

    init {
        MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(MemoDO::class.java.canonicalName)
    }

    @Test
    fun `a new memo belongs to the logged-in user`() {
        val own = logon(TEST_USER)
        assertEquals(own.id, memoEntityRest.newBaseDO(null).owner?.id)
    }

    @Test
    fun `the memos of others are neither listed nor readable`() {
        val own = getUser(TEST_USER)
        val other = getUser(TEST_USER2)
        val ownMemo = insert(own, "own")
        val otherMemo = insert(other, "other")
        logon(own)
        val listed = list()
        assertTrue(listed.contains(ownMemo.id))
        assertFalse(listed.contains(otherMemo.id), "A memo of another user is not listed.")
        assertNotNull(memoEntityRest.getItem(ownMemo.id).body)
        assertThrows(AccessException::class.java) { memoEntityRest.getItem(otherMemo.id) }
        logon(other)
        assertNotNull(memoEntityRest.getItem(otherMemo.id).body, "The owner reads the own memo.")
    }

    @Test
    fun `the memos of others can't be changed or deleted, the own ones can`() {
        val own = getUser(TEST_USER)
        val other = getUser(TEST_USER2)
        val otherMemo = insert(other, "foreign")
        logon(own)
        val posted = memo(own, "taken over?", otherMemo.id)
        assertThrows(AccessException::class.java) { memoDao.update(memoEntityRest.transformForDB(posted)) }
        assertThrows(AccessException::class.java) { memoDao.delete(memo(own, "deleted?", otherMemo.id)) }
        val stored = memoDao.find(otherMemo.id, checkAccess = false)!!
        assertEquals(other.id, stored.ownerId)
        assertEquals(otherMemo.subject, stored.subject)

        logon(other)
        val changed = memo(other, "changed", otherMemo.id)
        memoDao.update(memoEntityRest.transformForDB(changed))
        assertEquals("changed", memoDao.find(otherMemo.id, checkAccess = false)!!.subject)
        memoDao.delete(memoDao.find(otherMemo.id)!!)
        assertEquals(null, memoDao.find(otherMemo.id, checkAccess = false))
    }

    @Test
    fun `nobody inserts a memo for somebody else`() {
        logon(TEST_USER)
        val forOther = memo(getUser(TEST_USER2), "for somebody else?")
        assertThrows(AccessException::class.java) { memoDao.insert(memoEntityRest.transformForDB(forOther)) }
    }

    private fun list(): List<Long?> = memoEntityRest.listPage(
        MockHttpServletRequest(), AbstractEntityRest.ListPageRequest(filter = MagicFilter(), refresh = true)
    ).resultSet.map { (it as MemoDO).id }

    private fun memo(owner: PFUserDO, subject: String, id: Long? = null): MemoDO {
        val memo = MemoDO()
        memo.id = id
        memo.owner = owner
        memo.subject = subject
        memo.memo = "text of $subject"
        return memo
    }

    /** Inserted by the [owner] themselves: [MemoDao] stores every memo for the logged-in user. */
    private fun insert(owner: PFUserDO, subject: String): MemoDO {
        logon(owner)
        val memo = memo(owner, "$subject-${System.nanoTime()}")
        memoDao.insert(memo, checkAccess = false)
        return memo
    }
}
