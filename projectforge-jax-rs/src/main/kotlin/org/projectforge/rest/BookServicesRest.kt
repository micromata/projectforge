package org.projectforge.rest

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.getUserId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.RestHelper
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

    private val restHelper = RestHelper()
    /**
     * Lends the given book out by the logged-in user.
     */
    @PostMapping("lendOut")
    fun lendOut(request: HttpServletRequest, @RequestBody book: BookDO): ResponseEntity<ResponseAction> {
        book.lendOutDate = Date()
        bookDao.setLendOutBy(book, getUserId())
        return restHelper.saveOrUpdate(request, bookDao, book, bookRest, bookRest.validate(book))
    }

    /**
     * Returns the given book by the logged-in user.
     */
    @PostMapping("returnBook")
    fun returnBook( request: HttpServletRequest, @RequestBody book: BookDO): ResponseEntity<ResponseAction> {
        book.lendOutBy = null
        book.lendOutDate = null
        book.lendOutComment = null
        return restHelper.saveOrUpdate(request, bookDao, book, bookRest, bookRest.validate(book))
    }
}
