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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.address.AddressbookDao
import org.projectforge.business.orga.PosteingangDO
import org.projectforge.business.orga.PosteingangDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.orga.PosteingangEntityRest
import org.springframework.mock.web.MockHttpServletRequest
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.springframework.beans.factory.annotation.Autowired

class HistoryEditorsServiceTest : AbstractTestBase() {
    @Autowired
    private lateinit var addressbookDao: AddressbookDao

    @Autowired
    private lateinit var historyEditorsService: HistoryEditorsService

    @Autowired
    private lateinit var posteingangDao: PosteingangDao

    @Autowired
    private lateinit var posteingangEntityRest: PosteingangEntityRest

    @Test
    fun editorsAndFilterCoverEveryoneWhoChanged() {
        val creator = logon(TEST_ADMIN_USER)
        val bookId = addressbookDao.insert(AddressbookDO().apply {
            title = "HistoryEditorsServiceTest"
            owner = getUser(TEST_USER)
        }, false)
        val untouchedId = addressbookDao.insert(AddressbookDO().apply {
            title = "HistoryEditorsServiceTest-untouched"
            owner = getUser(TEST_USER)
        }, false)
        val second = logon(TEST_USER)
        addressbookDao.find(bookId, false)!!.let {
            it.description = "changed by the owner"
            addressbookDao.update(it, false)
        }

        val editors = historyEditorsService.editorsOf(AddressbookDO::class.java, listOf(bookId, untouchedId))
        assertEquals(
            listOf(creator.displayName, second.displayName),
            editors[bookId],
            "everybody who changed it, in the order of their first change",
        )
        assertEquals(listOf(creator.displayName), editors[untouchedId])

        assertTrue(historyEditorsService.entityIdsModifiedBy(AddressbookDO::class.java, second.id!!).contains(bookId))
        assertFalse(historyEditorsService.entityIdsModifiedBy(AddressbookDO::class.java, second.id!!).contains(untouchedId))
        assertTrue(historyEditorsService.entityIdsModifiedBy(AddressbookDO::class.java, creator.id!!).containsAll(listOf(bookId, untouchedId)))
    }

    @Test
    fun filterSupportReadsThePickersUserId() {
        val user = logon(TEST_ADMIN_USER)
        val bookId = addressbookDao.insert(AddressbookDO().apply {
            title = "HistoryEditorsServiceTest-filter"
            owner = getUser(TEST_USER)
        }, false)
        val filter = MagicFilter().apply {
            entries.add(MagicFilterEntry(ModifiedByFilterSupport.FILTER_ID).also { it.value.id = user.id })
        }
        val resultFilter = ModifiedByFilterSupport.preProcessMagicFilter(filter, AddressbookDO::class.java, historyEditorsService)
        assertNotNull(resultFilter)
        assertTrue(filter.entries.single().synthetic == true)
        assertTrue(resultFilter!!.match(mutableListOf(), addressbookDao.find(bookId, false)!!))
        assertNull(
            ModifiedByFilterSupport.preProcessMagicFilter(MagicFilter(), AddressbookDO::class.java, historyEditorsService),
            "no entry, no filter",
        )
    }

    @Test
    fun listOfDataObjectsGetsItsEditorsFromTheBaseClass() {
        val creator = logon(TEST_ADMIN_USER)
        val mailId = posteingangDao.insert(PosteingangDO().apply {
            absender = "HistoryEditorsServiceTest"
            datum = java.time.LocalDate.now()
            type = org.projectforge.business.orga.PostType.BRIEF
        }, false)
        val second = logon(TEST_USER)
        posteingangDao.find(mailId, false)!!.let {
            it.person = "changed by a second user"
            posteingangDao.update(it, false)
        }
        val unsaved = PosteingangDO() // no id: nothing to look up
        val result = posteingangEntityRest.postProcessResultSet(
            ResultSet(listOf(posteingangDao.find(mailId, false)!!, unsaved), null, magicFilter = MagicFilter()),
            MockHttpServletRequest(),
            MagicFilter(),
        )
        @Suppress("UNCHECKED_CAST")
        val rows = result.resultSet as List<PosteingangDO>
        assertEquals(listOf(creator.displayName, second.displayName), rows[0].editors)
        assertNull(rows[1].editors)
    }
}
