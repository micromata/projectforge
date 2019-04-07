package org.projectforge.rest

import org.projectforge.Const
import org.projectforge.business.book.*
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.core.AbstractStandardRest
import org.projectforge.rest.core.Validation
import org.projectforge.ui.*
import org.springframework.stereotype.Component
import javax.ws.rs.Path

@Component
@Path("book")
class BookRest() : AbstractStandardRest<BookDO, BookDao, BookFilter>(BookDao::class.java, BookFilter::class.java, "book.title") {
    /**
     * Initializes new books for adding.
     */
    override fun newBaseDO(): BookDO {
        val book = super.newBaseDO()
        book.status = BookStatus.PRESENT
        book.type = BookType.BOOK
        return book
    }

    override fun validate(validationErrors: MutableList<ValidationError>, obj: BookDO) {
        Validation.validateInteger(validationErrors, "yearOfPublishing", obj.yearOfPublishing, Const.MINYEAR, Const.MAXYEAR, formatNumber = false)
        if (baseDao.doesSignatureAlreadyExist(obj))
            validationErrors.add(ValidationError(translate("book.error.signatureAlreadyExists"), fieldId = "signature"))
    }

    /** Needed only for deprecated task object. */
    override fun processItemBeforeExport(item: Any) {
        super.processItemBeforeExport(item)
        (item as BookDO).task = null
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "created", "yearOfPublishing", "signature", "authors", "title", "keywords", "lendOutBy"))
        layout.getTableColumnById("created").formatter = Formatter.TIMESTAMP_MINUTES
        layout.getTableColumnById("lendOutBy").formatter = Formatter.USER
        LayoutUtils.addListFilterContainer(layout, "present", "missed", "disposed",
                filterClass = BookFilter::class.java)
        return LayoutUtils.processListPage(layout)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dataObject: BookDO?): UILayout {
        val layout = super.createEditLayout(dataObject)
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

        if (dataObject?.id != null) // Show lend out functionality only for existing books:
            layout.add(UIFieldset(title = "book.lending")
                    .add(UICustomized("lendOutComponent"))
                    .add(lc, "lendOutComment"))
        layout.add(lc, "abstractText", "comment")
        layout.getInputById("title").focus = true
        layout.getTextAreaById("authors").rows = 1
        layout.addTranslations("book.lendOut")
                .addTranslations("book.returnBook")
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
