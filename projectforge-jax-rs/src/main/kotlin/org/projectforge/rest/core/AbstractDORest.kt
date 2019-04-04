package org.projectforge.rest.core

import org.apache.commons.beanutils.PropertyUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.JsonUtils
import org.projectforge.ui.ElementsRegistry
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UILayout
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
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
@Component
abstract class AbstractDORest<O : ExtendedBaseDO<Int>, B : BaseDao<O>, F : BaseSearchFilter> {
    constructor(baseDaoClazz: Class<B>,
                filterClazz: Class<F>,
                i18nKeyPrefix: String) {
        this.baseDaoClazz = baseDaoClazz
        this.filterClazz = filterClazz
        this.i18nKeyPrefix = i18nKeyPrefix
        this.lc = LayoutContext(newBaseDO()::class.java)
    }

    private val log = org.slf4j.LoggerFactory.getLogger(AbstractDORest::class.java)

    /**
     * Contains the layout data returned for the frontend regarding edit pages.
     */
    private data class EditLayoutData(val data: Any?, val ui: UILayout?)

    /**
     * Contains the data, layout and filter settings served by [getInitialList].
     */
    private data class InitialListData(val ui: UILayout?, val data: ResultSet<*>, val filter: BaseSearchFilter)

    private var initialized = false

    private var _baseDao: B? = null

    /**
     * The layout context is needed to examine the data objects for maxLength, nullable, dataType etc.
     */
    protected val lc: LayoutContext

    protected val i18nKeyPrefix: String

    protected val baseDao: B
        get() {
            if (_baseDao == null) {
                _baseDao = applicationContext.getBean(baseDaoClazz)
            }
            return _baseDao ?: throw AssertionError("Set to null by another thread")
        }

    protected var baseDaoClazz: Class<B>

    protected var filterClazz: Class<F>

    /**
     * The React frontend works with local dates.
     */
    protected var restHelper = RestHelper()

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var listFilterService: ListFilterService

    abstract fun newBaseDO(): O

    open fun createListLayout(): UILayout {
        return UILayout("$i18nKeyPrefix.list")
    }

    open fun createEditLayout(dataObject: O?): UILayout {
        val titleKey = if (dataObject?.id != null) "$i18nKeyPrefix.edit" else "$i18nKeyPrefix.add"
        return UILayout(titleKey)
    }

    open fun validate(validationErrors: MutableList<ValidationError>, obj: O) {
    }

    protected fun validate(obj: O): List<ValidationError>? {
        val validationErrors = mutableListOf<ValidationError>()
        val propertiesMap = ElementsRegistry.getProperties(obj::class.java)!!
        propertiesMap.forEach {
            val property = it.key
            val elementInfo = it.value
            val value = PropertyUtils.getProperty(obj, property)
            if (elementInfo.required == true) {
                var error = false
                if (value == null) {
                    error = true
                } else {
                    when (value) {
                        is String -> {
                            if (value.isNullOrBlank()) {
                                error = true
                            }
                        }
                    }
                }
                if (error)
                    validationErrors.add(ValidationError(translateMsg("validation.error.fieldRequired", translate(elementInfo.i18nKey)),
                            fieldId = property))
            }
        }
        validate(validationErrors, obj)
        if (validationErrors.isEmpty()) return null
        return validationErrors
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
        val filter: F = listFilterService.getSearchFilter(request.session, filterClazz) as F
        if (filter.maxRows <= 0)
            filter.maxRows = 50
        filter.setSortAndLimitMaxRowsWhileSelect(true)
        val resultSet = restHelper.getList(this, baseDao, filter)
        processResultSetBeforeExport(resultSet)
        val layout = createListLayout()
                .addTranslation("table.showing")
        layout.add(LayoutListFilterUtils.createNamedContainer(baseDao, lc))
        return restHelper.buildResponse(InitialListData(ui = layout, data = resultSet, filter = filter))
    }

    /**
     * Get the list of all items matching the given filter.
     */
    @POST
    @Path(RestPaths.LIST)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun <O> getList(@Context request: HttpServletRequest, filter: F): Response {
        val resultSet = restHelper.getList(this, baseDao, filter)
        processResultSetBeforeExport(resultSet)
        val storedFilter = listFilterService.getSearchFilter(request.session, filterClazz)
        BeanUtils.copyProperties(filter, storedFilter)
        return restHelper.buildResponse(resultSet)
    }

    protected open fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        resultSet.resultSet.forEach { processItemBeforeExport(it) }
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
        return restHelper.buildResponse(item)
    }

    private fun getById(id: Int): O {
        val item = baseDao.getById(id)
        processItemBeforeExport(item)
        return item
    }

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * a group with a separate label and input field will be generated.
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
        val layout = createEditLayout(item)
        layout.addTranslation("changes")
        val result = EditLayoutData(item, layout)
        return restHelper.buildResponse(result)
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
        return restHelper.buildResponse(historyService.format(historyEntries))
    }


    open protected fun processItemBeforeExport(item: Any) {
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
        return restHelper.saveOrUpdate(baseDao, obj, this, validate(obj))
    }

    /**
     * The given object (marked as deleted) will be undeleted.
     */
    @PUT
    @Path(RestPaths.UNDELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun undelete(obj: O): Response {
        return restHelper.undelete(baseDao, obj, validate(obj))
    }

    /**
     * The given object is marked as deleted.
     */
    @DELETE
    @Path(RestPaths.MARK_AS_DELETED)
    @Consumes(MediaType.APPLICATION_JSON)
    fun markAsDeleted(obj: O): Response {
        return restHelper.markAsDeleted(baseDao, obj, validate(obj))
    }

    /**
     * The filters are reset and the default returned.
     */
    @GET
    @Path(RestPaths.FILTER_RESET)
    @Produces(MediaType.APPLICATION_JSON)
    fun filterReset(@Context request: HttpServletRequest): Response {
        return restHelper.buildResponse(listFilterService.getSearchFilter(request.session, filterClazz).reset())
    }

    internal open fun afterSaveOrUpdate(obj: O) {
    }

    internal open fun afterSave(obj: O) {
    }

    internal open fun afterUpdate(obj: O) {
    }

    internal open fun filterList(resultSet: MutableList<O>, filter: F): List<O> {
        if (filter.maxRows > 0 && resultSet.size > filter.maxRows) {
            return resultSet.take(filter.maxRows)
        }
        return resultSet
    }
}
