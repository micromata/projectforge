package org.projectforge.rest

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import javax.ws.rs.*

@Controller
@Path("books")
open class BookRest() : AbstractDORest<BookDO, BookDao>() {
    private val log = org.slf4j.LoggerFactory.getLogger(BookRest::class.java)

    @Autowired
    open var bookDao: BookDao? = null

    override fun getBaseDao() : BookDao {
        return bookDao!!
    }
}