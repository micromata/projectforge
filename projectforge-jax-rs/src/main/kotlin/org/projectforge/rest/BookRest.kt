package org.projectforge.rest

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.model.rest.AddressObject
import org.projectforge.model.rest.RestPaths
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Controller
@Path("books")
open class BookRest {
    private val log = org.slf4j.LoggerFactory.getLogger(BookRest::class.java)

    @Autowired
    open var accessChecker: AccessChecker? = null

    @Autowired
    open var bookDao: BookDao? = null

    @POST
    @Path(RestPaths.LIST)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun getList(filter : BaseSearchFilter): Response {
        return RestHelper.getList(bookDao, filter)
    }

    /**
     * @param bookDO
     * @return
     */
    @PUT
    @Path(RestPaths.SAVE_OR_UDATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun saveOrUpdate(bookDO: BookDO): Response {
        val origBook = bookDao!!.getById(bookDO.id)

        val json = ""
        log.info("Save or update address REST call finished.")
        return Response.ok(json).build()
    }

    @DELETE
    @Path(RestPaths.DELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    fun delete(addressObject: AddressObject): Response {
        return Response.ok().build()
    }
}