package org.projectforge.rest

import org.projectforge.business.DOUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.ui.Layout
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Controller
abstract class AbstractDORest<O : ExtendedBaseDO<Int>, B : BaseDao<O>> {
    private val log = org.slf4j.LoggerFactory.getLogger(AbstractDORest::class.java)

    private data class EditLayoutData(val data: Any?, val ui: UILayout?)

    @Autowired
    open var accessChecker: AccessChecker? = null

    @Autowired
    open var historyService : HistoryService? = null

    abstract fun getBaseDao(): BaseDao<O>

    abstract fun newBaseDO(): O

    /**
     * Get the list of all items matching the given filter.
     */
    @POST
    @Path(RestPaths.LIST)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun getList(filter: BaseSearchFilter): Response {
        var list = RestHelper.getList(getBaseDao(), filter)
        list.forEach { processItemBeforeExport(it) }
        return RestHelper.buildResponse(list)
    }

    @GET
    @Path("list-test")
    @Produces(MediaType.APPLICATION_JSON)
    fun getListTest(@QueryParam("search") search: String?): Response {
        val filter: BaseSearchFilter = BaseSearchFilter()
        filter.searchString = search
        var list = RestHelper.getList(getBaseDao(), filter)
        list.forEach { processItemBeforeExport(it) }
        return RestHelper.buildResponse(list)
    }

    /**
     * Gets the item from the database.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * layout will be also included if the id is not given.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItem(@PathParam("id") id: Int): Response {
        val item = getById(id)
        return RestHelper.buildResponse(item)
    }

    private fun getById(id: Int): O {
        val item = getBaseDao().getById(id)
        processItemBeforeExport(item)
        return item
    }

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * layout will be also included if the id is not given.
     */
    @GET
    @Path("edit")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItemAndLayout(@QueryParam("id") id: Int?): Response {
        val item: O
        if (id != null) {
            item = getById(id)
        } else item = newBaseDO()
        val result = EditLayoutData(item, Layout.getEditLayout(item))
        return RestHelper.buildResponse(result)
    }

    /**
     * Gets the history items of the given entity.
     * @param id Id of the item to get the history entries for.
     */
    @GET
    @Path("history/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getHistory(@PathParam("id") id: Int?): Response {
        if (id == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        val item = getById(id)
        if (item == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        val historyEntries = getBaseDao().getHistoryEntries(item);
        return RestHelper.buildResponse(historyService!!.format(historyEntries))
    }


    open protected fun processItemBeforeExport(item: O) {
        item.tenant = DOUtils.cloneMinimal(item.tenant)
    }

    /**
     * Use this service for adding new items as well as updating existing items (id isn't null).
     */
    @PUT
    @Path(RestPaths.SAVE_OR_UDATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun saveOrUpdate(obj: O): Response {
        return RestHelper.saveOrUpdate(getBaseDao(), obj)
    }

    /**
     * The given object (marked as deleted) will be undeleted.
     */
    @PUT
    @Path(RestPaths.UNDELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun undelete(obj: O): Response {
        return RestHelper.undelete(getBaseDao(), obj)
    }

    /**
     * The given object is marked as deleted.
     */
    @DELETE
    @Path(RestPaths.MARK_AS_DELETED)
    @Consumes(MediaType.APPLICATION_JSON)
    fun markAsDeleted(obj: O): Response {
        return RestHelper.markAsDeleted(getBaseDao(), obj)
    }
}