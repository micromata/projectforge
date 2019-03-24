package org.projectforge.rest

import org.projectforge.Const
import org.projectforge.business.book.*
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUserId
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.RestHelper
import org.projectforge.ui.*
import org.projectforge.ui.Formatter
import org.springframework.stereotype.Component
import java.util.*
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Component
@Path("books")
open class BookRest() : AbstractDORest<BookDO, BookDao, BookFilter>(BookDao::class.java, BookFilter::class.java) {

    private val log = org.slf4j.LoggerFactory.getLogger(BookRest::class.java)

    /**
     * Initializes new books for adding.
     */
    override fun newBaseDO(): BookDO {
        val book = BookDO()
        book.status = BookStatus.PRESENT
        book.type = BookType.BOOK
        return book
    }

    override fun validate(obj: BookDO): List<ValidationError>? {
        val errorsList = mutableListOf<ValidationError>()
        try {
            val year = Integer.parseInt(obj.yearOfPublishing)
            if (year < Const.MINYEAR || year > Const.MAXYEAR) {
                errorsList.add(ValidationError(translate("error.yearOutOfRange", Const.MINYEAR, Const.MAXYEAR), fieldId = "yearOfPublishing"))
            }
        } catch (ex: NumberFormatException) {
            errorsList.add(ValidationError(translate("book.error.number"), fieldId = "yearOfPublishing"))
        }
        if (baseDao.doesSignatureAlreadyExist(obj)) {
            errorsList.add(ValidationError(translate("book.error.signatureAlreadyExists"), fieldId = "signature"))
        }
        if (errorsList.isEmpty()) return null
        return errorsList
    }

    /**
     * Lends the given book out by the logged-in user.
     */
    @POST
    @Path("lendOut")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun lendOut(book: BookDO): Response {
        book.setLendOutDate(Date())
        baseDao.setLendOutBy(book, getUserId())
        return RestHelper.saveOrUpdate(baseDao, book, validate(book))
    }

    /**
     * Lends the given book out by the logged-in user.
     */
    @POST
    @Path("returnBook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun returnBook(book: BookDO): Response {
        book.lendOutBy = null
        book.lendOutDate = null
        book.lendOutComment = null
        return RestHelper.saveOrUpdate(baseDao, book, validate(book))
    }


    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val lc = LayoutContext(BookDO::class.java)
        val layout = UILayout("book.title.list")
                .add(UITable("result-set")
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
    override fun createEditLayout(dataObject: BookDO?, inlineLabels: Boolean): UILayout {
        val titleKey = if (dataObject?.id != null) "book.title.edit" else "book.title.add"
        val lc = LayoutContext(BookDO::class.java, inlineLabels)
        val layout = UILayout(titleKey)
                .add(lc, "title", "authors")
                .add(UIRow()
                        .add(UICol(6)
                                .add(lc, "type", "yearOfPublishing", "status", "signature"))
                        .add(UICol(6)
                                .add(lc, "isbn", "keywords", "publisher", "editor")))
                .add(UIGroup()
                        .add(UILabel("book.lending", "lendOutComponent"))
                        .add(UICustomized("lendOutComponent")))
                .add(lc, "lendOutComment", "abstractText", "comment")
        layout.getInputById("title").focus = true
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
