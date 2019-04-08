package org.projectforge.rest

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUserId
import org.projectforge.rest.core.RestHelper
import org.springframework.beans.factory.annotation.Autowired
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
class BookServicesRest() {

    @Autowired
    private lateinit var bookDao: BookDao

    @Autowired
    private lateinit var bookRest: BookRest

    private val restHelper = RestHelper()
    /**
     * Lends the given book out by the logged-in user.
     */
    @POST
    @Path("lendOut")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun lendOut(@Context request: HttpServletRequest, book: BookDO): Response {
        book.lendOutDate = Date()
        bookDao.setLendOutBy(book, getUserId())
        return restHelper.saveOrUpdate(request, bookDao, book, bookRest, bookRest.validate(book))
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
        return restHelper.saveOrUpdate(request, bookDao, book, bookRest, bookRest.validate(book))
    }
}
