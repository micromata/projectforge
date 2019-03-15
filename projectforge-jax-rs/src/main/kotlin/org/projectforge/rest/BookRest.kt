package org.projectforge.rest

import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.business.book.BookFilter
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
    fun getList(filter: BaseSearchFilter): Response {
        var list = RestHelper.getList(bookDao, filter)
        return RestHelper.buildResponse(list)
    }

    @GET
    @Path("list-test")
    @Produces(MediaType.APPLICATION_JSON)
    fun getListTest(@QueryParam("search") search: String?): Response {
        val filter: BookFilter = BookFilter()
        filter.searchString = search
        var list = RestHelper.getList(bookDao, filter)
        //list.forEach {
        //    it.comment = "Test comment" // You may manipulate the Book.
        //}
        return RestHelper.buildResponse(list)
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItem(@PathParam("id") id: Int?): Response {
        val book = bookDao!!.getById(id)
        return RestHelper.buildResponse(book)
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
        return RestHelper.saveOrUpdate(bookDao, bookDO)
    }

    /**
     * @param bookDO
     * @return
     */
    @PUT
    @Path(RestPaths.UNDELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun undelete(bookDO: BookDO): Response {
        return RestHelper.undelete(bookDao, bookDO)
    }

    @DELETE
    @Path(RestPaths.MARK_AS_DELETED)
    @Consumes(MediaType.APPLICATION_JSON)
    fun delete(addressObject: AddressObject): Response {
        return Response.ok().build()
    }
}