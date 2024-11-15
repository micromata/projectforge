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

import org.projectforge.Constants
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookStatus
import org.projectforge.business.book.BookType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.Validation
import org.projectforge.rest.dto.Book
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/book")
class BookPagesRest : AbstractDTOPagesRest<BookDO, Book, BookDao>(BookDao::class.java, "book.title") {

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

  /**
   * LAYOUT List page
   */
  override fun createListLayout(
    request: HttpServletRequest,
    layout: UILayout,
    magicFilter: MagicFilter,
    userAccess: UILayout.UserAccess
  ) {
    val table = agGridSupport.prepareUIGrid4ListPage(request, layout, magicFilter, this, userAccess = userAccess)
    table.add(
      lc,
      "created",
      "yearOfPublishing",
      "signature",
      "authors",
      "title",
      "keywords",
      "lendOutBy",
      "attachmentsSizeFormatted",
    )
  }

  /**
   * LAYOUT Edit page
   */
  override fun createEditLayout(dto: Book, userAccess: UILayout.UserAccess): UILayout {
    val layout = super.createEditLayout(dto, userAccess)
      .add(lc, "title", "authors")
      .add(
        UIRow()
          .add(
            UICol(6)
              .add(
                UIRow()
                  .add(UICol(6).add(lc, "type"))
                  .add(UICol(6).add(lc, "status"))
              )
              .add(lc, "yearOfPublishing", "signature")
          )
          .add(
            UICol(6)
              .add(lc, "isbn", "publisher", "editor")
          )
      )
      .add(lc, "keywords")

    if (dto.id != null) // Show lend out functionality only for existing books:
      layout.add(
        UIFieldset(title = "book.lending")
          .add(UICustomized("book.lendOutComponent"))
          .add(lc, "lendOutComment")
      )
    layout.add(
      UIFieldset(title = "attachment.list")
        .add(UIAttachmentList(category, dto.id, maxSizeInKB = getMaxFileSizeKB()))
    )
    layout.add(lc, "abstractText", "comment")

    layout.getInputById("title").focus = true
    layout.getTextAreaById("authors").rows = 1
    layout.addTranslations("book.lendOut")
      .addTranslations("book.returnBook")
    return LayoutUtils.processEditPage(layout, dto, this)
  }
}
