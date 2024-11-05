/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.common.BaseUserGroupRightService
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.rest.dto.Addressbook
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired

class AddressbookTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Test
    fun testHistory() {
        var book = AddressbookDO().apply { title = "AddressbookTest-history" }
        book.owner = getUser(TEST_USER)
        val oldValue = getUserList(TEST_USER, TEST_USER2, TEST_ADMIN_USER)
        val svc = BaseUserGroupRightService.instance
        svc.setFullAccessUsers(book, oldValue)
        val hist = createHistoryTester()

        val bookId = addressbookDao.insert(book, false)
        hist.loadRecentHistoryEntries(1, 0)
        book = addressbookDao.find(bookId, false)!!
        val newValue = getUserList(TEST_USER, TEST_USER2, TEST_HR_USER)
        svc.setFullAccessUsers(book,  newValue)
        hist.reset()

        addressbookDao.update(book, false)
        hist.loadRecentHistoryEntries(1, 1)

        val historyEntries = addressbookDao.loadHistory(book, false).sortedEntries
        assertEquals(2, historyEntries.size)
        val test = hist.selectHistory(bookId)
        test.find { it.entityOpType == EntityOpType.Update }.also {
            assertNotNull(it)
            assertEquals(1, it!!.attributes!!.size)
            it.attributes!!.first().also {
                assertEquals(newValue, it.value, "fullAccessUserIds: new value")
                assertEquals(oldValue, it.oldValue, "fullAccessUserIds: old value")
            }
        }
        assertEquals(2, test.size)
    }

    @Test
    fun listToStringTest() {
        var bookDO = AddressbookDO()
        val svc = BaseUserGroupRightService.instance
        svc.setFullAccessGroups(bookDO, "1")
        svc.setFullAccessUsers(bookDO, "2, 1, 3")
        svc.setReadonlyAccessGroups(bookDO, "")
        svc.setReadonlyAccessUsers(bookDO, "1, ,3")

        val book = Addressbook()
        book.copyFrom(bookDO)

        checkLongList(book.fullAccessGroups, 1)
        checkLongList(book.fullAccessUsers, 1, 2, 3)
        assertNull(book.readonlyAccessGroups)
        checkLongList(book.readonlyAccessUsers, 1, 3)

        bookDO = AddressbookDO()
        book.copyTo(bookDO)
        assertEquals("1", bookDO.fullAccessGroupIds)
        assertEquals("1,2,3", bookDO.fullAccessUserIds)
        assertNull(bookDO.readonlyAccessGroupIds)
        assertEquals("1,3", bookDO.readonlyAccessUserIds)
    }

    private fun checkLongList(longList: List<BaseDTO<*>>?, vararg expected: Long) {
        assertNotNull(longList)
        if (longList == null) {
            return
        }
        assertEquals(expected.size, longList.size)
        longList.forEachIndexed { index, element ->
            assertEquals(expected[index], element.id)
        }
    }

    private fun getUserList(vararg users: String): String? {
        return BaseUserGroupRightService.asSortedIdStrings(users.map { getUser(it) })
    }
}
