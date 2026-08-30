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

package org.projectforge.rest

import org.projectforge.Constants
import org.projectforge.business.PfCaches
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookStatus
import org.projectforge.business.book.BookType
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.Validation
import org.projectforge.rest.dto.Book
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/book")
class BookEntityRest : AbstractDTOEntityRest<BookDO, Book, BookDao>(BookDao::class.java, "book.title") {

  @Autowired
  private lateinit var caches: PfCaches

  @PostConstruct
  private fun postConstruct() {
    /**
     * Enable attachments for this entity.
     */
    enableJcr()
    JacksonConfiguration.registerAllowedUnknownProperties(Book::class.java, "statusAsString")
    JacksonConfiguration.registerAllowedUnknownProperties(Book::class.java, "typeAsString")
  }

  /**
   * Initializes new books for adding.
   */
  override fun newBaseDO(request: HttpServletRequest?): BookDO {
    val book = super.newBaseDO(request)
    book.status = BookStatus.PRESENT
    book.type = BookType.BOOK
    return book
  }

  override fun transformForDB(dto: Book): BookDO {
    val bookDO = BookDO()
    dto.copyTo(bookDO)
    return bookDO
  }

  override fun transformFromDB(obj: BookDO, editMode: Boolean): Book {
    val book = Book()
    // Resolve the lazy lendOutBy proxy from the UserGroupCache before copyFrom reads it. Otherwise every
    // book row of the list triggers its own T_PF_USER query (the N+1 the book list suffered from); the cache
    // holds all users in memory, so this is an O(1) lookup and no query.
    obj.lendOutBy = caches.getUserIfNotInitialized(obj.lendOutBy)
    book.copyFrom(obj)
    return book
  }

  override fun validate(validationErrors: MutableList<ValidationError>, dto: Book) {
    Validation.validateInteger(
      validationErrors,
      "yearOfPublishing",
      dto.yearOfPublishing,
      Constants.MINYEAR,
      Constants.MAXYEAR,
      formatNumber = false
    )
    if (baseDao.doesSignatureAlreadyExist(dto.signature, dto.id))
      validationErrors.add(ValidationError(translate("book.error.signatureAlreadyExists"), fieldId = "signature"))
  }
}
