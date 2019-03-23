package org.projectforge.rest.core

import com.google.gson.annotations.SerializedName
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.JsonUtils
import org.projectforge.rest.LayoutRegistry
import org.projectforge.ui.UILayout
import org.projectforge.ui.ValidationError
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
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
    constructor(baseDaoClazz: Class<B>,
                filterClazz: Class<F>) {
        this.baseDaoClazz = baseDaoClazz
        this.filterClazz = filterClazz
    }

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

    private var initialized = false

    private var _baseDao: B? = null

    val baseDao: B
        get() {
            if (_baseDao == null) {
                _baseDao = applicationContext!!.getBean(baseDaoClazz)
            }
            return _baseDao ?: throw AssertionError("Set to null by another thread")
        }

    var baseDaoClazz: Class<B>

    var filterClazz: Class<F>

    @Autowired
    open var accessChecker: AccessChecker? = null

    @Autowired
    open var applicationContext: ApplicationContext? = null

    @Autowired
    open var historyService: HistoryService? = null

    @Autowired
    open var listFilterService: ListFilterService? = null

    abstract fun newBaseDO(): O

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
        val filter = listFilterService!!.getSearchFilter(request.session, filterClazz)
        filter.maxRows = 10
        val list = RestHelper.getList(baseDao, filter)
        list.forEach { processItemBeforeExport(it) }
        val layout = LayoutRegistry.getListLayout(baseDao)
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
        val list = RestHelper.getList(baseDao, filter)
        list.forEach { processItemBeforeExport(it) }
        val listData = ListData(resultSet = list)
        val storedFilter = listFilterService!!.getSearchFilter(request.session, filterClazz)
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
        val item = baseDao.getById(id)
        processItemBeforeExport(item)
        return item
    }

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * @param inlineLabels If true (default) all labels and additionalLabels will be attached to the input fields, otherwise
     * a group with a separate label and input field will be generated.
     * layout will be also included if the id is not given.
     */
    @GET
    @Path("edit")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItemAndLayout(@QueryParam("id") id: Int?, @QueryParam("inlineLabels") inlineLabels : Boolean?): Response {
        val item: O
        if (id != null) {
            item = getById(id)
        } else item = newBaseDO()
        val result = EditLayoutData(item, LayoutRegistry.getEditLayout(item, inlineLabels = !(inlineLabels == false)))
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
        val historyEntries = baseDao.getHistoryEntries(item);
        return RestHelper.buildResponse(historyService!!.format(historyEntries))
    }


    open protected fun processItemBeforeExport(item: O) {
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
        return RestHelper.saveOrUpdate(baseDao, obj, validate(obj))
    }

    /**
     * The given object (marked as deleted) will be undeleted.
     */
    @PUT
    @Path(RestPaths.UNDELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun undelete(obj: O): Response {
        return RestHelper.undelete(baseDao, obj, validate(obj))
    }

    /**
     * The given object is marked as deleted.
     */
    @DELETE
    @Path(RestPaths.MARK_AS_DELETED)
    @Consumes(MediaType.APPLICATION_JSON)
    fun markAsDeleted(obj: O): Response {
        return RestHelper.markAsDeleted(baseDao, obj, validate(obj))
    }

    /**
     * The filters are reset and the default returned.
     */
    @GET
    @Path(RestPaths.FILTER_RESET)
    @Produces(MediaType.APPLICATION_JSON)
    fun filterReset(@Context request: HttpServletRequest): Response {
        return RestHelper.buildResponse(listFilterService!!.getSearchFilter(request.session, filterClazz).reset())
    }
}
