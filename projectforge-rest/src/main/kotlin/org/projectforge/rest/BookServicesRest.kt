/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUserId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.saveOrUpdate
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/book")
class BookServicesRest() {

    @Autowired
    private lateinit var bookDao: BookDao

    @Autowired
    private lateinit var bookRest: BookRest

    /**
     * Lends the given book out by the logged-in user.
     */
    @PostMapping("lendOut")
    fun lendOut(request: HttpServletRequest, @RequestBody book: BookDO): ResponseEntity<ResponseAction> {
        book.lendOutDate = Date()
        bookDao.setLendOutBy(book, getUserId())
        return saveOrUpdate(request, bookDao, book, book, bookRest, bookRest.validate(book))
    }

    /**
     * Returns the given book by the logged-in user.
     */
    @PostMapping("returnBook")
    fun returnBook( request: HttpServletRequest, @RequestBody book: BookDO): ResponseEntity<ResponseAction> {
        book.lendOutBy = null
        book.lendOutDate = null
        book.lendOutComment = null
        return saveOrUpdate(request, bookDao, book, book, bookRest, bookRest.validate(book))
    }
}
