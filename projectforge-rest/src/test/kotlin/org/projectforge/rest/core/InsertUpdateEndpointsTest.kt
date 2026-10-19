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

import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookStatus
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.BookEntityRest
import org.projectforge.rest.BookServicesRest
import org.projectforge.rest.dto.Book
import org.projectforge.rest.dto.Customer
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.fibu.CustomerPagesRest
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession

/**
 * The separate `insert` and `update` endpoints of [AbstractEntityRest] (and the `operation` request parameter of the
 * form actions): an update never becomes an insert because the id got lost, an insert never overwrites an entry.
 */
class InsertUpdateEndpointsTest : AbstractTestBase() {
    @Autowired
    private lateinit var bookEntityRest: BookEntityRest

    @Autowired
    private lateinit var bookServicesRest: BookServicesRest

    @Autowired
    private lateinit var bookDao: BookDao

    @Autowired
    private lateinit var customerPagesRest: CustomerPagesRest

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Test
    fun `update without id is rejected and inserts nothing`() {
        logon(TEST_FINANCE_USER)
        val title = "InsertUpdateEndpointsTest update without id"
        val book = newBook(title)
        assertEquals(200, write(book) { request, postData -> bookEntityRest.insert(request, postData) })
        val stored = findBooks(title).single()

        // The form lost the id on the way: the update is refused, no copy is created.
        val lost = newBook(title, comment = "changed")
        assertEquals(406, write(lost) { request, postData -> bookEntityRest.update(request, postData) })
        assertEquals(listOf(stored.id), findBooks(title).map { it.id })
        assertEquals(null, bookDao.find(stored.id)!!.comment)

        // With the id it is an update of the very entry.
        val edited = newBook(title, comment = "changed").also { it.id = stored.id }
        assertEquals(200, write(edited) { request, postData -> bookEntityRest.update(request, postData) })
        assertEquals(listOf(stored.id), findBooks(title).map { it.id })
        assertEquals("changed", bookDao.find(stored.id)!!.comment)
    }

    @Test
    fun `insert of an existing entry is rejected and changes nothing`() {
        logon(TEST_FINANCE_USER)
        val title = "InsertUpdateEndpointsTest insert existing"
        assertEquals(200, write(newBook(title)) { request, postData -> bookEntityRest.insert(request, postData) })
        val stored = findBooks(title).single()

        val again = newBook(title, comment = "overwritten").also { it.id = stored.id }
        assertEquals(406, write(again) { request, postData -> bookEntityRest.insert(request, postData) })
        assertEquals(null, bookDao.find(stored.id)!!.comment)
    }

    @Test
    fun `a form action declared as update is rejected without id`() {
        logon(TEST_FINANCE_USER)
        val title = "InsertUpdateEndpointsTest action without id"
        val response = write(newBook(title), operation = "update") { request, postData ->
            bookServicesRest.lendOut(request, postData)
        }
        assertEquals(406, response)
        assertEquals(emptyList<BookDO>(), findBooks(title))

        // Declared as insert (the action on a new entry), it goes through.
        assertEquals(200, write(newBook(title), operation = "insert") { request, postData ->
            bookServicesRest.lendOut(request, postData)
        })
        assertEquals(1, findBooks(title).size)
    }

    @Test
    fun `a customer is inserted with its user-assigned number and updated by it`() {
        logon(TEST_FINANCE_USER)
        val number = 98_765L // Not taken by other tests sharing the database (AutoCompleteObjectsTest has 4711).
        val customer = Customer(id = number, nummer = number, name = "InsertUpdateEndpointsTest customer")
        assertEquals(200, write(customer) { request, postData -> customerPagesRest.insert(request, postData) })
        val stored = kundeDao.find(number)
        assertNotNull(stored)

        // Once stored, the same number is no new customer any more (the posted entry has its creation date).
        val again = Customer(id = number, nummer = number, name = "overwritten").also { it.created = stored!!.created }
        assertEquals(406, write(again) { request, postData -> customerPagesRest.insert(request, postData) })

        val edited = Customer(id = number, nummer = number, name = "renamed")
        assertEquals(200, write(edited) { request, postData -> customerPagesRest.update(request, postData) })
        assertEquals("renamed", kundeDao.find(number)!!.name)
    }

    private fun newBook(title: String, comment: String? = null) =
        Book(title = title, comment = comment, status = BookStatus.PRESENT)

    private fun findBooks(title: String): List<BookDO> =
        bookDao.selectAllNotDeleted(checkAccess = false).filter { it.title == title }

    /** Posts [dto] as the frontend does (with a valid CSRF token) and answers the HTTP status. */
    private fun <DTO : Any> write(
        dto: DTO,
        operation: String? = null,
        call: (HttpServletRequest, PostData<DTO>) -> ResponseEntity<ResponseAction>,
    ): Int {
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        operation?.let { request.addParameter(OPERATION_PARAM, it) }
        val postData = PostData(data = dto, watchFieldsTriggered = null, serverData = sessionCsrfService.createServerData(request))
        return call(request, postData).statusCode.value()
    }
}
