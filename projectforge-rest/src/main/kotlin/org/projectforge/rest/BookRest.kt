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

import org.projectforge.Const
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookStatus
import org.projectforge.business.book.BookType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.Validation
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/book")
class BookRest() : AbstractDORest<BookDO, BookDao, BaseSearchFilter>(BookDao::class.java, BaseSearchFilter::class.java, "book.title") {

    /**
     * Initializes new books for adding.
     */
    override fun newBaseDO(request: HttpServletRequest?): BookDO {
        val book = super.newBaseDO(request)
        book.status = BookStatus.PRESENT
        book.type = BookType.BOOK
        return book
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: BookDO) {
        Validation.validateInteger(validationErrors, "yearOfPublishing", dto.yearOfPublishing, Const.MINYEAR, Const.MAXYEAR, formatNumber = false)
        if (baseDao.doesSignatureAlreadyExist(dto))
            validationErrors.add(ValidationError(translate("book.error.signatureAlreadyExists"), fieldId = "signature"))
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "created", "yearOfPublishing", "signature", "authors", "title", "keywords", "lendOutBy"))
        layout.getTableColumnById("lendOutBy").formatter = Formatter.USER
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: BookDO): UILayout {
        val layout = super.createEditLayout(dto)
                .add(lc, "title", "authors")
                .add(UIRow()
                        .add(UICol(6)
                                .add(UIRow()
                                        .add(UICol(6).add(lc, "type"))
                                        .add(UICol(6).add(lc, "status")))
                                .add(lc, "yearOfPublishing", "signature"))
                        .add(UICol(6)
                                .add(lc, "isbn", "publisher", "editor")))
                .add(lc, "keywords")

        if (dto.id != null) // Show lend out functionality only for existing books:
            layout.add(UIFieldset(title = "book.lending")
                    .add(UICustomized("book.lendOutComponent"))
                    .add(lc, "lendOutComment"))
        layout.add(lc, "abstractText", "comment")
        layout.getInputById("title").focus = true
        layout.getTextAreaById("authors").rows = 1
        layout.addTranslations("book.lendOut")
                .addTranslations("book.returnBook")
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
