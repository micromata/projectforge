package org.projectforge.rest

import org.projectforge.Const
import org.projectforge.business.book.*
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUserId
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.rest.core.Validation
import org.projectforge.ui.*
import org.projectforge.ui.Formatter
import org.springframework.stereotype.Component
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Component
@Path("book")
class BookRest() : AbstractDORest<BookDO, BookDao, BookFilter>(BookDao::class.java, BookFilter::class.java, "book.title") {

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

    override fun validate(validationErrors: MutableList<ValidationError>, obj: BookDO) {
        Validation.validateInteger(validationErrors, "yearOfPublishing", obj.yearOfPublishing, Const.MINYEAR, Const.MAXYEAR, formatNumber = false)
        if (baseDao.doesSignatureAlreadyExist(obj)) {
            validationErrors.add(ValidationError(translate("book.error.signatureAlreadyExists"), fieldId = "signature"))
        }
    }

    /**
     * Lends the given book out by the logged-in user.
     */
    @POST
    @Path("lendOut")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun lendOut(@Context request: HttpServletRequest, book: BookDO): Response {
        book.setLendOutDate(Date())
        baseDao.setLendOutBy(book, getUserId())
        return restHelper.saveOrUpdate(request, baseDao, book, this, validate(book))
    }

    /**
     * Returns the given book by the logged-in user.
     */
    @POST
    @Path("returnBook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun returnBook(@Context request: HttpServletRequest, book: BookDO): Response {
        book.lendOutBy = null
        book.lendOutDate = null
        book.lendOutComment = null
        return restHelper.saveOrUpdate(request, baseDao, book, this, validate(book))
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
            layout.add(UIGroup()
                    .add(UILabel("book.lending", "lendOutComponent"))
                    .add(UICustomized("lendOutComponent")))
                    .add(lc, "lendOutComment")
        layout.add(lc, "abstractText", "comment")
        layout.getInputById("title").focus = true
        layout.getTextAreaById("authors").rows = 1
        layout.addTranslations("book.lendOut")
                .addTranslations("book.returnBook")
        return LayoutUtils.processEditPage(layout, dataObject)
    }
}
