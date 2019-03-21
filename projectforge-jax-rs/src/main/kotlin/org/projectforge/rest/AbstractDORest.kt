package org.projectforge.rest

import com.google.gson.annotations.SerializedName
import org.projectforge.business.DOUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.ui.Layout
import org.projectforge.rest.ui.ValidationError
import org.projectforge.ui.UILayout
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * This is the base class for all fronted functionality regarding query, editing etc. It also serves layout
 * data for the frontend.
 * <br>
 * For each entity type such as users, addresses, timesheets etc. an own class is inherited for doing customizations.
 * It's recommended for the frontend to develop generic list and edit pages by using the layout information served
 * by these rest services.
 */
@Controller
abstract class AbstractDORest<O : ExtendedBaseDO<Int>, B : BaseDao<O>, F : BaseSearchFilter> {
    private val log = org.slf4j.LoggerFactory.getLogger(AbstractDORest::class.java)

    /**
     * Contains the layout data returned for the frontend regarding edit pages.
     */
    private data class EditLayoutData(val data: Any?, val ui: UILayout?)

    /**
     * Contains the data including the result list (matching the filter) served by getList methods ([getInitialList] and [getList]).
     */
    private data class ListData<O : ExtendedBaseDO<Int>>(
            @SerializedName("result-set")
            val resultSet: List<O>)

    /**
     * Contains the data, layout and filter settings served by [getInitialList].
     */
    private data class InitialListData<O : ExtendedBaseDO<Int>>(val ui: UILayout?, val data: ListData<O>, val filter: BaseSearchFilter)

    @Autowired
    open var accessChecker: AccessChecker? = null

    @Autowired
    open var historyService: HistoryService? = null

    @Autowired
    open var listFilterService: ListFilterService? = null

    abstract fun getBaseDao(): BaseDao<O>

    abstract fun newBaseDO(): O

    abstract fun getFilterClass(): Class<F>;

    open protected fun validate(obj: O): List<ValidationError>? {
        return null
    }

    /**
     * Will be called by clone service. Override this method, if your edit page
     * should support the clone functionality.
     * @return false at default, if clone is not supported, otherwise true.
     */
    open fun prepareClone(obj: O): Boolean {
        return false
    }

    /**
     * Get the current filter from the server, all matching items and the layout of the list page.
     */
    @GET
    @Path("initial-list")
    @Produces(MediaType.APPLICATION_JSON)
    fun getInitialList(@Context request: HttpServletRequest): Response {
        val filter = listFilterService!!.getSearchFilter(request.session, getFilterClass())
        filter.maxRows = 10
        val list = RestHelper.getList(getBaseDao(), filter)
        list.forEach { processItemBeforeExport(it) }
        val layout = Layout.getListLayout(getBaseDao())
        val listData = ListData(resultSet = list)
        return RestHelper.buildResponse(InitialListData(ui = layout, data = listData, filter = filter))
    }

    /**
     * Get the list of all items matching the given filter.
     */
    @POST
    @Path(RestPaths.LIST)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun <O> getList(@Context request: HttpServletRequest, filter: F): Response {
        val list = RestHelper.getList(getBaseDao(), filter)
        list.forEach { processItemBeforeExport(it) }
        val listData = ListData(resultSet = list)
        val storedFilter = listFilterService!!.getSearchFilter(request.session, getFilterClass())
        BeanUtils.copyProperties(filter, storedFilter)
        return RestHelper.buildResponse(listData)
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
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return The clone object ([BaseDO.getId] is null and [ExtendedBaseDO.isDeleted] = false)
     */
    @POST
    @Path("clone")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun clone(obj: O): Response {
        obj.id = null
        obj.isDeleted = false
        obj.id = null
        val json = JsonUtils.toJson(obj)
        return Response.ok(json).build()
    }

    /**
     * Use this service for adding new items as well as updating existing items (id isn't null).
     */
    @PUT
    @Path(RestPaths.SAVE_OR_UDATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun saveOrUpdate(obj: O): Response {
        return RestHelper.saveOrUpdate(getBaseDao(), obj, validate(obj))
    }

    /**
     * The given object (marked as deleted) will be undeleted.
     */
    @PUT
    @Path(RestPaths.UNDELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun undelete(obj: O): Response {
        return RestHelper.undelete(getBaseDao(), obj, validate(obj))
    }

    /**
     * The given object is marked as deleted.
     */
    @DELETE
    @Path(RestPaths.MARK_AS_DELETED)
    @Consumes(MediaType.APPLICATION_JSON)
    fun markAsDeleted(obj: O): Response {
        return RestHelper.markAsDeleted(getBaseDao(), obj, validate(obj))
    }

    /**
     * The filters are reset and the default returned.
     */
    @GET
    @Path(RestPaths.FILTER_RESET)
    @Produces(MediaType.APPLICATION_JSON)
    fun filterReset(@Context request: HttpServletRequest): Response {
        return RestHelper.buildResponse(listFilterService!!.getSearchFilter(request.session, getFilterClass()).reset())
    }
}
