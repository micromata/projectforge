package org.projectforge.rest.core

import org.apache.commons.beanutils.PropertyUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.JsonUtils
import org.projectforge.rest.MessageType
import org.projectforge.rest.ResponseData
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * This is the base class for all fronted functionality regarding query, editing etc. It also serves layout
 * data for the frontend.
 * <br>
 * For each entity type such as users, addresses, time sheets etc. an own class is inherited for doing customizations.
 * It's recommended for the frontend to develop generic list and edit pages by using the layout information served
 * by these rest services.
 */
@Component
abstract class AbstractStandardRest<O : ExtendedBaseDO<Int>, B : BaseDao<O>, F : BaseSearchFilter>(
        private val baseDaoClazz: Class<B>,
        private val filterClazz: Class<F>,
        private val i18nKeyPrefix: String) {

    @PostConstruct
    private fun postConstruct() {
        this.lc = LayoutContext(baseDao.doClass)
    }

    companion object {
        const val GEAR_MENU = "GEAR"
    }

    /**
     * Contains the layout data returned for the frontend regarding edit pages.
     * @param variables Additional variables / data provided for the edit page.
     */
    class EditLayoutData(val data: Any?, val ui: UILayout?, var variables: Map<String, Any>? = null)

    /**
     * Contains the data, layout and filter settings served by [getInitialList].
     */
    private data class InitialListData(val ui: UILayout?, val data: ResultSet<*>, val filter: BaseSearchFilter)

    private var initialized = false

    private var _baseDao: B? = null

    private var restPath: String? = null

    /**
     * The layout context is needed to examine the data objects for maxLength, nullable, dataType etc.
     */
    protected lateinit var lc: LayoutContext

    protected val baseDao: B
        get() {
            if (_baseDao == null) {
                _baseDao = applicationContext.getBean(baseDaoClazz)
            }
            return _baseDao ?: throw AssertionError("Set to null by another thread")
        }

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

    open fun newBaseDO(request: HttpServletRequest): O {
        return baseDao.doClass.newInstance()
    }

    open fun createListLayout(): UILayout {
        val layout = UILayout("$i18nKeyPrefix.list")
        val gearMenu = MenuItem(GEAR_MENU, title = "*")
        gearMenu.add(MenuItem("reindexNewestDatabaseEntries",
                i18nKey = "menu.reindexNewestDatabaseEntries",
                tooltip = "menu.reindexNewestDatabaseEntries.tooltip.content",
                tooltipTitle = "menu.reindexNewestDatabaseEntries.tooltip.title",
                url = "${getRestPath()}/reindexNewest",
                type = MenuItemTargetType.RESTCALL))
        if (accessChecker.isLoggedInUserMemberOfAdminGroup)
            gearMenu.add(MenuItem("reindexAllDatabaseEntries",
                    i18nKey = "menu.reindexAllDatabaseEntries",
                    tooltip = "menu.reindexAllDatabaseEntries.tooltip.content",
                    tooltipTitle = "menu.reindexAllDatabaseEntries.tooltip.title",
                    url = "${getRestPath()}/reindexFull",
                    type = MenuItemTargetType.RESTCALL))
        layout.add(gearMenu)
        return layout
    }

    fun getRestPath(): String {
        if (restPath == null) {
            val path = this::class.annotations.find { it is Path } as? Path
            restPath = path?.value
        }
        return restPath!!
    }

    fun getFullRestPath(): String {
        return getRestPath()
    }

    open fun createEditLayout(dataObject: O): UILayout {
        val titleKey = if (dataObject?.id != null) "$i18nKeyPrefix.edit" else "$i18nKeyPrefix.add"
        return UILayout(titleKey)
    }

    open fun validate(validationErrors: MutableList<ValidationError>, obj: O) {
    }

    fun validate(obj: O): List<ValidationError>? {
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
                            if (value.isBlank()) {
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
    @Path("initialList")
    @Produces(MediaType.APPLICATION_JSON)
    fun getInitialList(@Context request: HttpServletRequest): Response {
        val filter: F = listFilterService.getSearchFilter(request.session, filterClazz) as F
        if (filter.maxRows <= 0)
            filter.maxRows = 50
        filter.setSortAndLimitMaxRowsWhileSelect(true)
        val resultSet = restHelper.getList(this, baseDao, filter)
        processResultSetBeforeExport(resultSet)
        val layout = createListLayout()
                .addTranslations("table.showing")
        layout.add(LayoutListFilterUtils.createNamedContainer(baseDao, lc))
        layout.postProcessPageMenu()
        return restHelper.buildResponse(InitialListData(ui = layout, data = resultSet, filter = filter))
    }

    /**
     * Rebuilds the index by the search engine for the newest entries.
     * @see [BaseDao.rebuildDatabaseIndex4NewestEntries]
     */
    @GET
    @Path("reindexNewest")
    fun reindexNewest(): Response {
        baseDao.rebuildDatabaseIndex4NewestEntries()
        return restHelper.buildResponse(ResponseData("administration.reindexNewest.successful", messageType = MessageType.TOAST, style = UIStyle.SUCCESS))
    }

    /**
     * Rebuilds the index by the search engine for all entries.
     * @see [BaseDao.rebuildDatabaseIndex]
     */
    @GET
    @Path("reindexFull")
    fun reindexFull(): Response {
        baseDao.rebuildDatabaseIndex()
        return restHelper.buildResponse(ResponseData("administration.reindexFull.successful", messageType = MessageType.TOAST, style = UIStyle.SUCCESS))
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

    open fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
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
    fun getItem(@PathParam("id") id: Int?): Response {
        val item = getById(id) ?: return restHelper.buildResponseItemNotFound()
        return restHelper.buildResponse(item)
    }

    protected fun getById(id: Int?): O? {
        val item = baseDao.getById(id) ?: return null
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
    fun getItemAndLayout(@Context request: HttpServletRequest, @QueryParam("id") id: Int?): Response {
        val item = (if (null != id) getById(id) else newBaseDO(request))
                ?: return restHelper.buildResponseItemNotFound()
        val layout = createEditLayout(item)
        layout.addTranslations("changes", "tooltip.selectMe")
        layout.postProcessPageMenu()
        val result = EditLayoutData(item, layout)
        onGetItemAndLayout(request, item, result)
        val additionalVariables = addVariablesForEditPage(item)
        if (additionalVariables != null)
            result.variables = additionalVariables
        return restHelper.buildResponse(result)
    }

    protected open fun onGetItemAndLayout(request: HttpServletRequest, item: O, editLayoutData: EditLayoutData) {
    }

    /**
     * Use this method to add customized variables for your edit page for the initial call.
     */
    protected open fun addVariablesForEditPage(item: O): Map<String, Any>? {
        return null
    }

    /**
     * Gets the autocompletion list for the given property and search string.
     * @param property The property (field of the data) used to search.
     * @param searchString
     * @return list of strings as json.
     */
    @GET
    @Path("ac")
    @Produces(MediaType.APPLICATION_JSON)
    fun getAutoCompletion(@QueryParam("property") property: String?, @QueryParam("search") searchString: String?): Response {
        val result = baseDao.getAutocompletion(property, searchString)
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
            return Response.status(Response.Status.BAD_REQUEST).build()
        }
        val item = getById(id) ?: return Response.status(Response.Status.BAD_REQUEST).build()
        val historyEntries = baseDao.getHistoryEntries(item)
        return restHelper.buildResponse(historyService.format(historyEntries))
    }


    open fun processItemBeforeExport(item: Any) {
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
    fun saveOrUpdate(@Context request: HttpServletRequest, obj: O): Response {
        return restHelper.saveOrUpdate(request, baseDao, obj, this, validate(obj))
    }

    /**
     * The given object (marked as deleted before) will be undeleted.
     */
    @PUT
    @Path(RestPaths.UNDELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun undelete(obj: O): Response {
        return restHelper.undelete(baseDao, obj, this, validate(obj))
    }

    /**
     * The given object will be deleted.
     * Please note, if you try to delete a historizable data base object, an exception will be thrown.
     */
    @DELETE
    @Path(RestPaths.MARK_AS_DELETED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun markAsDeleted(obj: O): Response {
        return restHelper.markAsDeleted(baseDao, obj, this, validate(obj))
    }

    /**
     * The given object is marked as deleted.
     * Please note, if you try to mark a non-historizable data base object, an exception will be thrown.
     */
    @DELETE
    @Path(RestPaths.DELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun delete(obj: O): Response {
        return restHelper.delete(baseDao, obj, this, validate(obj))
    }

    /**
     * Use this service for cancelling editing. The purpose of this method is only, to tell the client where
     * to redirect after cancellation.
     * @return ResponseAction
     */
    @POST
    @Path(RestPaths.CANCEL)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun cancelEdit(@Context request: HttpServletRequest, obj: O): Response {
        return cancelEdit(request, obj, getRestPath())
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

    internal open fun beforeSaveOrUpdate(request: HttpServletRequest, obj: O) {
    }

    internal open fun afterSaveOrUpdate(obj: O) {
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterSave(obj: O): Response {
        return afterEdit(obj)
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterUpdate(obj: O): Response {
        return afterEdit(obj)
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterDelete(obj: O): Response {
        return afterEdit(obj)
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterMarkAsDeleted(obj: O): Response {
        return afterEdit(obj)
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterUndelete(obj: O): Response {
        return afterEdit(obj)
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun cancelEdit(request: HttpServletRequest, obj: O, restPath: String): Response {
        return afterEdit(obj)
    }

    /**
     * Will be called after create, update, delete, markAsDeleted, undelete and cancel.
     * @return ResponseAction with the url of the standard list page.
     */
    internal open fun afterEdit(obj: O): Response {
        return Response.ok(ResponseAction(restPath).addVariable("id", obj.id ?: -1)).build()
    }

    internal open fun filterList(resultSet: MutableList<O>, filter: F): List<O> {
        if (filter.maxRows > 0 && resultSet.size > filter.maxRows) {
            return resultSet.take(filter.maxRows)
        }
        return resultSet
    }
}
