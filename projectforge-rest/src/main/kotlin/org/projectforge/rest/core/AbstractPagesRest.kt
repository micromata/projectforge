/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.core

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import mu.KotlinLogging
import org.projectforge.Constants
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.NestedNullException
import org.projectforge.common.PropertyUtils
import org.projectforge.favorites.Favorites
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jcr.AttachmentsAccessChecker
import org.projectforge.framework.jcr.AttachmentsDaoAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.*
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.history.HistoryFormatService
import org.projectforge.jcr.FileSizeStandardChecker
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.dto.*
import org.projectforge.rest.dto.aggrid.AGColumnState
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.projectforge.ui.filter.UIFilterElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.Serializable

private val log = KotlinLogging.logger {}

/**
 * This is the base class for all fronted functionality regarding query, editing etc. It also serves layout
 * data for the frontend.
 * <br>
 * For each entity type such as users, addresses, time sheets etc. an own class is inherited for doing customizations.
 * It's recommended for the frontend to develop generic list and edit pages by using the layout information served
 * by these rest services.
 */
abstract class AbstractPagesRest<
        O : ExtendedBaseDO<Long>,
        DTO : Any, // DTO may be equals to O if no special data transfer objects are used.
        B : BaseDao<O>>
@JvmOverloads
constructor(
    private val baseDaoClazz: Class<B>,
    private val i18nKeyPrefix: String,
    val cloneSupport: CloneSupport = CloneSupport.NONE
) {
    enum class CloneSupport {
        /** No clone support. */
        NONE,

        /**
         * Clone button will create a copy (without saving it automatically).
         */
        CLONE,

        /**
         * Clone button will create and save a copy and close the window.
         */
        AUTOSAVE
    }

    /**
     * If [getAutoCompleteObjects] is called without a special property to search for, all properties will be searched for,
     * given by this attribute. If null, an exception is thrown, if [getAutoCompleteObjects] is called without a property.
     */
    open val autoCompleteSearchFields: Array<String>? = null

    open val addNewEntryUrl = "${Constants.REACT_APP_PATH}$category/edit"

    @PostConstruct
    private fun postConstruct() {
        this.lc = LayoutContext(baseDao.doClass)
        PagesResolver.register(category, this)
    }

    companion object {
        const val GEAR_MENU = "GEAR"
        const val CLASSIC_VERSION_MENU = "CLASSIC"
        const val CREATE_MENU = "CREATE"
        const val USER_PREF_PARAM_HIGHLIGHT_ROW = "highlightedRow"
        const val JCR_PATH_PREFIX: String = "org.projectforge"

        fun getJcrPath(identifier: String): String {
            return "$JCR_PATH_PREFIX.$identifier"
        }
    }

    class DisplayObject(val id: Any?, override val displayName: String?) : DisplayNameCapable

    /**
     * Contains the data, layout and filter settings served by [getInitialList].
     */
    class InitialListData(
        val ui: UILayout?,
        val standardEditPage: String,
        val data: ResultSet<*>,
        val filterFavorites: List<Favorites.FavoriteIdTitle>,
        val filter: MagicFilter,
        /**
         * Quickselect url for searching entries while typing search string. If given, the user may click on
         * the autocompletion results for direct editing of the object.
         */
        val quickSelectUrl: String? = null,
        var variables: Map<String, Any>? = null
    )

    private var initialized = false

    private var _baseDao: B? = null

    private var _category: String? = null

    /**
     * Category should be unique and is e. g. used as react path. At default it's the dir of the url defined in class annotation [RequestMapping].
     */
    open val category: String // open needed by Wicket's SpringBean for proxying.
        get() {
            if (_category == null) {
                _category = getRestPath().removePrefix("${Rest.URL}/")
            }
            return _category!!
        }

    /**
     * The layout context is needed to examine the data objects for maxLength, nullable, dataType etc.
     */
    protected lateinit var lc: LayoutContext

    val baseDao: B
        get() {
            if (_baseDao == null) {
                _baseDao = applicationContext.getBean(baseDaoClazz)
            }
            return _baseDao ?: throw AssertionError("Set to null by another thread")
        }

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    protected lateinit var attachmentsService: AttachmentsService

    @Autowired
    lateinit var agGridSupport: AGGridSupport

    @Autowired
    private lateinit var historyFormatService: HistoryFormatService

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Autowired
    private lateinit var userPrefService: UserPrefService

    protected fun getMaxFileSizeKB(): Int {
        return this.attachmentsAccessChecker.fileSizeChecker.maxFileSizeKB
    }

    /**
     * Override this method for initializing fields for new objects.
     * @return new instance of class ExtendedDO.
     */
    open fun newBaseDO(request: HttpServletRequest? = null): O {
        return baseDao.doClass.getDeclaredConstructor().newInstance()
    }

    /**
     * Override this method for initializing fields for new objects.
     * Creates a new dto by calling [newBaseDO] and [transformFromDB].
     */
    open fun newBaseDTO(request: HttpServletRequest? = null): DTO {
        return transformFromDB(newBaseDO(request))
    }

    internal fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
        val userAccess = UILayout.UserAccess()
        checkUserAccess(null, userAccess)
        userAccess.update = true // Assume that the user has general update access (change this, see GroupPagesRest)
        val layout = UILayout("$i18nKeyPrefix.list")
        val gearMenu = layout.ensureGearMenu()
        gearMenu.add(
            MenuItem(
                "reindexNewestDatabaseEntries",
                i18nKey = "menu.reindexNewestDatabaseEntries",
                tooltip = "menu.reindexNewestDatabaseEntries.tooltip.content",
                tooltipTitle = "menu.reindexNewestDatabaseEntries.tooltip.title",
                url = getRestPath("reindexNewest"),
                type = MenuItemTargetType.RESTCALL
            )
        )
        if (accessChecker.isLoggedInUserMemberOfAdminGroup)
            gearMenu.add(
                MenuItem(
                    "reindexAllDatabaseEntries",
                    i18nKey = "menu.reindexAllDatabaseEntries",
                    tooltip = "menu.reindexAllDatabaseEntries.tooltip.content",
                    tooltipTitle = "menu.reindexAllDatabaseEntries.tooltip.title",
                    url = getRestPath("reindexFull"),
                    type = MenuItemTargetType.RESTCALL
                )
            )
        gearMenu.add(
            MenuItem(
                "resetFilter",
                i18nKey = "menu.resetFilter",
                tooltip = "menu.resetFilter.info",
                tooltipTitle = "menu.resetFilter",
                url = getRestPath("filter/reset"),
                type = MenuItemTargetType.RESTCALL
            )
        )

        layout.addTranslations(
            "reset", "datatable.no-records-found", "date.begin", "date.end", "exportAsXls",
            "search.lastMinute", "search.lastHour", "calendar.today", "search.sinceYesterday",
            "multiselection.button",
        )
        layout.addTranslation("search.lastMinutes.10", translateMsg("search.lastMinutes", 10))
        layout.addTranslation("search.lastMinutes.30", translateMsg("search.lastMinutes", 30))
        layout.addTranslation("search.lastHours.4", translateMsg("search.lastHours", 4))
        layout.addTranslation("search.lastDays.3", translateMsg("search.lastDays", 3))
        layout.addTranslation("search.lastDays.7", translateMsg("search.lastDays", 7))
        layout.addTranslation("search.lastDays.30", translateMsg("search.lastDays", 30))
        layout.addTranslation("search.lastDays.90", translateMsg("search.lastDays", 90))
        createListLayout(request, layout, magicFilter, userAccess)
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * Starts multi selection by registering current result list.
     */
    @PostMapping(RestPaths.REST_START_MULTI_SELECTION)
    fun startMultiSelections(request: HttpServletRequest, @RequestBody filter: MagicFilter): ResponseAction {
        log.info("User wants to start multiselection")
        @Suppress("UNCHECKED_CAST")
        val list = (getList(request, filter).resultSet as List<Project>).map { it.id }
        MultiSelectionSupport.registerEntityIdsForSelection(request, this::class.java, list)
        return ResponseAction(url = PagesResolver.getMultiSelectionPageUrl(this::class.java, absolute = true))
    }

    abstract fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    )

    /**
     * If given, a link to this url is shown on the list page. This is used for accessing the classical Wicket-version
     * of the current page during migration phase.
     */
    open val classicsLinkListUrl: String? = null

    /**
     * Relative rest path (without leading /rs
     */
    fun getRestPath(subPath: String? = null): String {
        return RestResolver.getRestUrl(this::class.java, subPath, true)
    }

    /**
     * Relative rest path (without leading /rs
     */
    fun getRestRootPath(subPath: String? = null): String {
        return getRestPath(subPath)
    }

    open fun createEditLayout(dto: DTO, userAccess: UILayout.UserAccess): UILayout {
        val titleKey = if (getId(dto) != null) "$i18nKeyPrefix.edit" else "$i18nKeyPrefix.add"
        val ui = UILayout(titleKey, getRestPath())
        if (dto is BaseDTO<*>) {
            dto.layoutUid.let {
                // Now we have to handle the layout uid. By default, the layout's uid will switch on every
                // creation resulting in different html id's. This would result in client errors, e. g. if
                // element id's with tooltips are changed.
                if (it.isNullOrBlank()) {
                    dto.layoutUid = ui.uid // Preserve uid of new layout for later recovery.
                } else {
                    ui.uid = it // Recover layout uid from previous creation.
                }
            }
        }
        ui.userAccess.copyFrom(userAccess)
        return ui
    }

    open fun validate(validationErrors: MutableList<ValidationError>, dto: DTO) {
    }

    fun validate(dbObj: O): MutableList<ValidationError> {
        val validationErrors = mutableListOf<ValidationError>()
        val propertiesMap = ElementsRegistry.getProperties(dbObj::class.java)
        if (propertiesMap.isNullOrEmpty()) {
            log.error("Internal error, can't find propertiesMap for '${dbObj::class.java}' in ElementsRegistry. No validation errors will be built automatically.")
            return validationErrors
        }
        propertiesMap.forEach {
            val property = it.key
            val elementInfo = it.value
            val value =
                try {
                    PropertyUtils.getProperty(dbObj, property)
                } catch (ex: NestedNullException) {
                    null
                } catch (ex: Exception) {
                    log.warn("Unknown property '${dbObj::class.java}.$property': ${ex.message}.")
                    null
                }
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
                    validationErrors.add(
                        ValidationError(
                            translateMsg("validation.error.fieldRequired", translate(elementInfo.i18nKey)),
                            fieldId = property, messageId = elementInfo.i18nKey
                        )
                    )
            }
        }
        return validationErrors
    }

    fun validate(dbObj: O, postData: PostData<DTO>): List<ValidationError>? {
        val validationErrors = validate(dbObj)
        val dto = postData.data
        validate(validationErrors, dto)
        if (validationErrors.isEmpty()) return null
        return validationErrors
    }

    /**
     * Get the current filter from the server, all matching items and the layout of the list page.
     */
    @GetMapping("initialList")
    fun requestInitialList(request: HttpServletRequest): InitialListData {
        val result = getInitialList(request)
        val additionalVariables = addVariablesForListPage()
        if (additionalVariables != null)
            result.variables = additionalVariables
        return result
    }

    protected open fun getInitialList(request: HttpServletRequest): InitialListData {
        return getInitialList(request, getCurrentFilter())
    }

    /**
     * Removes unknown filter entries. This is useful, if after migration etc. some filter entries are stored in the user
     * pref but that didn't exist.
     */
    protected open fun removeUnknownFilterEntries(filter: MagicFilter, filterEntries: Set<String>) {
        filter.entries.removeIf {
            val field = it.field
            field != null && !filterEntries.contains(field)
        }
    }

    protected fun getInitialList(request: HttpServletRequest, filter: MagicFilter): InitialListData {
        val favorites = getFilterFavorites()
        val list = getList(request, this, baseDao, filter)
        val resultSet = postProcessResultSet(list, request, filter)
        resultSet.highlightRowId = userPrefService.getEntry(category, USER_PREF_PARAM_HIGHLIGHT_ROW, Long::class.java)
        val ui = createListLayout(request, filter)
            .addTranslations(
                "table.showing",
                "searchFilter",
                "nothingFound"
            )
        val searchFilterContainer = LayoutListFilterUtils.createNamedSearchFilterContainer(this, lc)
        val filterEntries = mutableSetOf<String>()
        searchFilterContainer.content.forEach {
            if (it is UIFilterElement) {
                filterEntries.add(it.id)
            }
        }
        removeUnknownFilterEntries(filter, filterEntries)
        ui.add(searchFilterContainer)
        if (classicsLinkListUrl != null) {
            ui.add(
                MenuItem(
                    CLASSIC_VERSION_MENU,
                    title = "*",
                    url = classicsLinkListUrl,
                    tooltip = translate("goreact.menu.classics")
                ), 0
            )
        }
        if (ui.userAccess.insert != false) {
            ui.add(MenuItem(CREATE_MENU, title = translate("add"), url = addNewEntryUrl))
        }

        return InitialListData(
            ui = ui,
            standardEditPage = getStandardEditPage(),
            quickSelectUrl = quickSelectUrl,
            data = resultSet,
            filter = filter,
            filterFavorites = favorites.idTitleList
        )
    }

    /**
     * @return the standard edit page at default.
     */
    protected open fun getStandardEditPage(): String {
        return "${Constants.REACT_APP_PATH}$category/edit/:id"
    }

    /**
     * At standard, quickSelectUrl is only given, if the doClass implements DisplayObject and autoCompleteSearchFields are given.
     */
    protected open val quickSelectUrl: String?
        get() = if (!autoCompleteSearchFields.isNullOrEmpty() && DisplayNameCapable::class.java.isAssignableFrom(baseDao.doClass)) "${
            getRestPath(
                AutoCompletion.AUTOCOMPLETE_OBJECT
            )
        }?maxResults=30&search=:search" else null

    /**
     * Add customized magic filter element in addition to the automatically detected elements.
     */
    open fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    }

    /**
     * For specific creation of QueryFilter from MagicFilter, especially for extended settings.
     * This will be called with a new QueryFilter before calling [MagicFilterProcessor.doIt].
     * @return Customized result filters to apply or null, if no such filters should be applied.
     */
    open fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<O>>? {
        return null
    }

    /**
     * For specific creation of QueryFilter from MagicFilter, especially for extended settings.
     * This will be called after calling [MagicFilterProcessor.doIt].
     */
    open fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
    }

    /**
     * Get the list of all items matching the given filter.
     * Please note: filter.deleted is ignored (entries.field == "deleted" is used instead).
     */
    @RequestMapping(RestPaths.LIST)
    fun getList(request: HttpServletRequest, @RequestBody filter: MagicFilter): ResultSet<*> {
        filter.autoWildcardSearch = true
        fixMagicFilterFromClient(filter)
        val list = getList(request, this, baseDao, filter)
        saveCurrentFilter(filter)
        val resultSet = postProcessResultSet(list, request, filter)
        resultSet.highlightRowId = userPrefService.getEntry(category, USER_PREF_PARAM_HIGHLIGHT_ROW, Long::class.java)
        return resultSet
    }

    /**
     * Get the list by ids.
     */
    open fun getListByIds(entityIds: Collection<Serializable>?): List<O> {
        return baseDao.select(entityIds) ?: listOf()
    }

    /**
     * Please note: filter.deleted is ignored (entries.field == "deleted" is used instead).
     */
    fun getResultList(filter: MagicFilter): List<O> {
        filter.autoWildcardSearch = true
        fixMagicFilterFromClient(filter)
        return getObjectList(this, baseDao, filter)
    }

    private fun getFilterFavorites(): Favorites<MagicFilter> {
        var favorites: Favorites<MagicFilter>? = null
        try {
            @Suppress("UNCHECKED_CAST")
            favorites =
                userPrefService.getEntry(
                    category,
                    Favorites.PREF_NAME_LIST,
                    Favorites::class.java
                ) as? Favorites<MagicFilter>
        } catch (ex: Exception) {
            log.error("Exception while getting user preferred favorites: ${ex.message}. This might be OK for new releases. Ignoring filter.")
        }
        if (favorites == null) {
            // Creating empty filter list (user has no filter list yet):
            favorites = Favorites()
            userPrefService.putEntry(category, Favorites.PREF_NAME_LIST, favorites)
        }
        return favorites
    }

    /**
     * Workaround of parsing effects, e. g. from and to is given as json to value.
     * deleted flag may also be given in entries.field == "deleted".
     */
    private fun fixMagicFilterFromClient(magicFilter: MagicFilter) {
        if (magicFilter.entries.isNullOrEmpty()) {
            magicFilter.deleted = false
            return
        }
        magicFilter.entries.removeIf { it.isEmpty }
    }

    fun getCurrentFilter(): MagicFilter {
        var currentFilter = userPrefService.getEntry(category, Favorites.PREF_NAME_CURRENT, MagicFilter::class.java)
        if (currentFilter == null) {
            currentFilter = MagicFilter()
            saveCurrentFilter(currentFilter)
        } else {
            currentFilter.init()
        }
        // Fixing the maxRows to the default value. Max rows was mis-used as paginationPageSize in the past.
        currentFilter.maxRows = QueryFilter.QUERY_FILTER_MAX_ROWS
        @Suppress("UNCHECKED_CAST")
        return currentFilter
    }

    private fun saveCurrentFilter(currentFilter: MagicFilter) {
        userPrefService.putEntry(category, Favorites.PREF_NAME_CURRENT, currentFilter)
    }

    @GetMapping("filter/select")
    fun selectFavoriteFilter(
        request: HttpServletRequest,
        @RequestParam("id", required = true) id: Long
    ): InitialListData {
        val favorites = getFilterFavorites()
        val currentFilter = favorites.get(id)
        if (currentFilter != null) {
            // Puts a deep copy of the current filter. Without copying, the favorite filter of the list will
            // be synchronized with the current filter.
            saveCurrentFilter(currentFilter.clone())
            currentFilter.init()
        } else {
            log.warn("Can't select filter $id, because it's not found in favorites list.")
        }
        return getInitialList(request, currentFilter ?: getCurrentFilter())
    }

    /**
     * Please note: filter.deleted is ignored (entries.field == "deleted" is used instead).
     * @return currentFilter, new filterFavorites and isFilterModified=false.
     */
    @PostMapping("filter/create")
    fun createFavoriteFilter(@RequestBody newFilter: MagicFilter): Map<String, Any> {
        fixMagicFilterFromClient(newFilter)
        val favorites = getFilterFavorites()
        favorites.add(newFilter)
        val currentFilter =
            newFilter.clone() // A clone is needed, otherwise current and favorite of list are the same object.
        saveCurrentFilter(currentFilter)
        return mapOf(
            "filter" to currentFilter,
            "filterFavorites" to favorites.idTitleList
        )
    }

    /**
     * @return new filterFavorites
     */
    @GetMapping("filter/rename")
    fun renameFavoriteFilter(
        @RequestParam("id", required = true) id: Long,
        @RequestParam("newName", required = true) newName: String
    ): Map<String, Any> {
        val favorites = getFilterFavorites()
        val filter = favorites.get(id)
        if (filter != null) {
            filter.name = newName
        } else {
            log.warn("Could not rename the user's filter. Filter with id '$id' not found for category '$category'.")
        }
        val currentFilter = getCurrentFilter()
        if (currentFilter.id == filter?.id) {
            currentFilter.name = newName
        }
        return mapOf(
            "filter" to currentFilter, // Just for the case if the current filter was renamed.
            "filterFavorites" to favorites.idTitleList
        )
    }

    /**
     * Updates the named Filter with the given values.
     * Please note: filter.deleted is ignored (entries.field == "deleted" is used instead).
     */
    @RequestMapping("filter/update")
    fun updateFavoriteFilter(@RequestBody magicFilter: MagicFilter): Map<String, Any> {
        fixMagicFilterFromClient(magicFilter)
        val favorites = getFilterFavorites()
        val id = magicFilter.id ?: return mapOf()
        favorites.remove(id)
        favorites.add(magicFilter)
        val currentFilter = magicFilter.clone() // Need a clone for having different instances
        saveCurrentFilter(currentFilter)
        return mapOf()
    }

    /**
     * @return The new list of filterFavorites (id's with titles) without the deleted filter.
     */
    @GetMapping("filter/delete")
    fun deleteFavoriteFilter(@RequestParam("id", required = true) id: Long): Map<String, Any> {
        val favorites = getFilterFavorites()
        favorites.remove(id)
        return mapOf("filterFavorites" to getFilterFavorites().idTitleList)
    }

    /**
     * Resets the current filter from the server.
     */
    @GetMapping("filter/reset")
    fun resetListFilter(): ResponseAction {
        saveCurrentFilter(MagicFilter())
        agGridSupport.resetGridState(category)
        return ResponseAction(targetType = TargetType.RELOAD)
            .addVariable("filter", MagicFilter())
    }

    /**
     * Rebuilds the index by the search engine for the newest entries.
     * @see [BaseDao.rebuildDatabaseIndex4NewestEntries]
     */
    @GetMapping("reindexNewest")
    fun reindexNewest(): ResponseAction {
        baseDao.rebuildDatabaseIndex4NewestEntries()
        return UIToast.createToast(translate("administration.reindexNewest.successful"), color = UIColor.SUCCESS)
    }

    /**
     * Rebuilds the index by the search engine for all entries.
     * @see [BaseDao.rebuildDatabaseIndex]
     */
    @GetMapping("reindexFull")
    fun reindexFull(): ResponseAction {
        baseDao.rebuildDatabaseIndex()
        return UIToast.createToast(translate("administration.reindexFull.successful"), color = UIColor.SUCCESS)
    }

    /**
     * Will be called after getting the list from the database before calling. Will be called before returning
     * list to callee (client).
     * Useful also for saving database calls by setting additional data to the list.
     * @param resultSet The result set of the list (the origin or new one).
     */
    abstract fun postProcessResultSet(
        resultSet: ResultSet<O>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*>

    /**
     * Gets the item from the database.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * layout will be also included if the id is not given.
     */
    @GetMapping("{id}")
    fun getItem(@PathVariable("id") id: Long?): ResponseEntity<Any> {
        val item = getById(id, true) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity(item, HttpStatus.OK)
    }

    protected open fun getById(
        idString: String?,
        editMode: Boolean = false,
        userAccess: UILayout.UserAccess? = null
    ): DTO? {
        if (idString == null) return null
        return getById(idString.toLong(), editMode, userAccess)
    }

    protected fun getById(id: Long?, editMode: Boolean = false, userAccess: UILayout.UserAccess? = null): DTO? {
        id ?: return null
        val item = baseDao.find(id) ?: return null
        checkUserAccess(item, userAccess)
        val result = transformFromDB(item, editMode)
        jcrPath?.let {
            if (result is AttachmentsSupport) {
                result.attachments = attachmentsService.getAttachments(it, id, attachmentsAccessChecker)
            }
        }
        return result
    }

    protected fun checkUserAccess(obj: O?, userAccess: UILayout.UserAccess?) {
        if (userAccess != null) {
            if (obj != null) {
                userAccess.history = baseDao.hasLoggedInUserHistoryAccess(obj, false)
                userAccess.update = baseDao.hasLoggedInUserUpdateAccess(obj, obj, false)
                userAccess.delete = baseDao.hasLoggedInUserDeleteAccess(obj, obj, false)
            } else {
                userAccess.history = baseDao.hasLoggedInUserHistoryAccess(false)
            }
            userAccess.insert = baseDao.hasLoggedInUserInsertAccess()
        }
    }

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * a group with a separate label and input field will be generated.
     * layout will be also included if the id is not given.
     * @param returnToCaller This optional parameter defines the caller page of this service to put in server data. After processing this page
     * the user will be redirect to this given returnToCaller.
     */
    @GetMapping(RestPaths.EDIT)
    fun getItemAndLayout(
        request: HttpServletRequest,
        @RequestParam("id") id: String?,
        @RequestParam("returnToCaller") returnToCaller: String?
    )
            : ResponseEntity<FormLayoutData> {
        val userAccess = UILayout.UserAccess()
        val item = (if (null != id) {
            getById(id, true, userAccess)
        } else {
            checkUserAccess(null, userAccess)
            newBaseDTO(request)
        })
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        onBeforeGetItemAndLayout(request, item, userAccess)
        val formLayoutData = getItemAndLayout(request, item, userAccess)
        returnToCaller?.let {
            // Fix doubled encoding:
            formLayoutData.serverData!!.returnToCaller = returnToCaller.replace("%2F", "/")
        }
        return ResponseEntity(formLayoutData, HttpStatus.OK)
    }

    /**
     * Will be called after getting the item from the database before calling [onGetItemAndLayout]. No initial layout
     * is available.
     * Does nothing at default.
     */
    protected open fun onBeforeGetItemAndLayout(
        request: HttpServletRequest,
        dto: DTO,
        userAccess: UILayout.UserAccess
    ) {
    }

    protected fun getItemAndLayout(
        request: HttpServletRequest,
        dto: DTO,
        userAccess: UILayout.UserAccess
    ): FormLayoutData {
        val ui = createEditLayout(dto, userAccess)
        ui.addTranslations("changes", "tooltip.selectMe")
        val serverData = sessionCsrfService.createServerData(request)
        val result = FormLayoutData(dto, ui, serverData)
        onGetItemAndLayout(request, dto, result)
        val additionalVariables = addVariablesForEditPage(dto)
        if (additionalVariables != null)
            result.variables = additionalVariables
        return result
    }

    /**
     * Will be called after getting the item from the database before creating the layout data. Overwrite this for
     * e. g. parsing the request and preset the item values.
     */
    protected open fun onGetItemAndLayout(request: HttpServletRequest, dto: DTO, formLayoutData: FormLayoutData) {
    }

    /**
     * Use this method to add customized variables for your edit page for the initial call.
     */
    protected open fun addVariablesForEditPage(dto: DTO): Map<String, Any>? {
        return null
    }

    /**
     * Use this method to add customized variables for your list page for the initial call.
     */
    protected open fun addVariablesForListPage(): Map<String, Any>? {
        return null
    }

    /**
     * Proxy for [BaseDao.isAutocompletionPropertyEnabled]
     */
    open fun isAutocompletionPropertyEnabled(property: String): Boolean {
        return baseDao.isAutocompletionPropertyEnabled(property)
    }

    /**
     * Gets the autocompletion list for the given property and search string.
     * <br/>
     * Please note: You must enable properties in [BaseDao], otherwise a security warning is logged and an empty
     * list is returned.
     * @param property The property (field of the data) used to search.
     * @param searchString
     * @return list of strings as json.
     * @see BaseDao.getAutocompletion
     */
    @GetMapping(AutoCompletion.AUTOCOMPLETE_TEXT)
    open fun getAutoCompletionForProperty(
        @RequestParam("property") property: String,
        @RequestParam("search") searchString: String?
    )
            : List<String> {
        searchString ?: return emptyList()
        return baseDao.getAutocompletion(property, searchString)
    }

    /**
     * Gets the quick select list for the given search string by searching in all properties defined by [autoCompleteSearchFields].
     * If [autoCompleteSearchFields] is not given an [InternalErrorException] will be thrown.
     * The result set is limited to 30 entries and only
     * @param searchString
     * @return list of found objects.
     */
    @GetMapping(AutoCompletion.AUTOCOMPLETE_OBJECT)
    open fun getAutoCompleteObjects(
        request: HttpServletRequest,
        @RequestParam("search") searchString: String?,
        @RequestParam("maxResults") maxResults: Int?
    ): List<DisplayObject> {
        if (autoCompleteSearchFields.isNullOrEmpty()) {
            throw TechnicalException(
                "Can't call getAutoCompletion without property.",
                "No autoCompleteSearchFields are configured by the developers for this entity."
            )
        }
        val filter = createAutoCompleteObjectsFilter(request)
        val modifiedSearchString = searchString
            ?.split(' ', '\t', '\n')
            ?.joinToString(" ") { if (it.startsWith("+")) it else "+$it*" }
        filter.searchString = modifiedSearchString
        filter.searchFields = autoCompleteSearchFields!!
        maxResults?.let { filter.maxRows = it }
        val list = queryAutocompleteObjects(request, filter)
        return list.map { DisplayObject(it.id, if (it is DisplayNameCapable) it.displayName else it.toString()) }
    }

    /**
     * Will create a new BaseSearchFilter. If you want to use an DO specific filter, override this method.
     */
    open fun createAutoCompleteObjectsFilter(request: HttpServletRequest): BaseSearchFilter {
        return BaseSearchFilter()
    }

    protected open fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<O> {
        return baseDao.select(filter)
    }

    /**
     * Gets the history items of the given entity.
     * @param id Id of the item to get the history entries for.
     */
    @GetMapping("history/{id}")
    fun getHistory(@PathVariable("id") id: Long?): ResponseEntity<List<DisplayHistoryEntry>> {
        if (id == null) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val item = baseDao.find(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val historyDTOs = historyFormatService.selectAsDisplayEntries(baseDao, item)
        return ResponseEntity(historyDTOs, HttpStatus.OK)
    }

    /**
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
     */
    @PostMapping(RestPaths.CLONE)
    fun clone(request: HttpServletRequest, @Valid @RequestBody postData: PostData<DTO>)
            : ResponseEntity<ResponseAction> {
        val clone = prepareClone(postData.data)
        if (cloneSupport == CloneSupport.AUTOSAVE) {
            // If cloneSupport is of type AUTOSAVE and no validation error exist: clone, save and close.
            postData.data = clone
            val result = saveOrUpdate(request, postData)
            if (result.statusCode == HttpStatus.OK) {
                return result
            }
            // Validation errors or other errors occurred, doesn't save. Proceed with editing.
        }
        val formLayoutData = getItemAndLayout(request, clone, UILayout.UserAccess(history = false, insert = true))
        return ResponseEntity(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", formLayoutData.data)
                .addVariable("ui", formLayoutData.ui)
                .addVariable("variables", formLayoutData.variables),
            HttpStatus.OK
        )
    }

    /**
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
     */
    @PostMapping(RestPaths.SET_COLUMN_STATES)
    fun updateColumnStates(@Valid @RequestBody columnStates: List<AGColumnState>): String {
        agGridSupport.storeColumnState(category, columnStates)
        return "OK"
    }

    protected open fun autoSaveOnClone(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<DTO>,
        clone: DTO
    ): Boolean {
        return true
    }

    /**
     * Might be modified e. g. for edit pages handled in modals (timesheets and calendar events).
     */
    protected open fun getRestEditPath(): String {
        return PagesResolver.getEditPageUrl(this::class.java)
    }

    /**
     * Will be called by clone service. Override this method for more complex clone functionality.
     * @return The object itself with id set to null if of type BaseDO and deleted to false and lastUpdate and created
     * to null if ExtendecBaseDO.
     */
    open fun prepareClone(dto: DTO): DTO {
        if (dto is BaseDO<*>) {
            dto.id = null
            if (dto is ExtendedBaseDO<*>) {
                dto.deleted = false
                dto.lastUpdate = null
                dto.created = null
            }
        } else if (dto is BaseDTO<*>) {
            dto.id = null
            dto.deleted = false
            dto.lastUpdate = null
            dto.created = null
        }
        return dto
    }

    //class PostData<DTO: Any>(data: DTO, watchFieldsTriggered: Array<String>?)

    /**
     * Will be called for watched fields from client, if any of the watched fields was modified.
     * This method may be used for updating model after modification of any watch field.
     * You may define watch fields in layout.
     */
    @PostMapping(RestPaths.WATCH_FIELDS)
    fun watchFields(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<DTO>
    ): ResponseEntity<ResponseAction> {
        return onWatchFieldsUpdate(request, postData.data, postData.watchFieldsTriggered)
    }

    protected open fun onWatchFieldsUpdate(
        request: HttpServletRequest,
        dto: DTO,
        watchFieldsTriggered: Array<String>?
    ): ResponseEntity<ResponseAction> {
        return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
    }

    /**
     * Use this service for adding new items as well as updating existing items (id isn't null).
     */
    @PutMapping(RestPaths.SAVE_OR_UDATE)
    fun saveOrUpdate(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<DTO>
    ): ResponseEntity<ResponseAction> {
        sessionCsrfService.validateCsrfToken(request, postData, "Upsert")?.let { return it }
        val dbObj = transformForDB(postData.data)
        return saveOrUpdate(request, baseDao, dbObj, postData, this, validate(dbObj, postData))
    }

    /**
     * The given object (marked as deleted before) will be undeleted.
     */
    @PutMapping(RestPaths.UNDELETE)
    fun undelete(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<DTO>
    ): ResponseEntity<ResponseAction> {
        sessionCsrfService.validateCsrfToken(request, postData, "Undelete")?.let { return it }
        val dbObj = transformForDB(postData.data)
        return undelete(request, baseDao, dbObj, postData, this, validate(dbObj, postData))
    }

    /**
     * The given object will be deleted.
     * Please note, if you try to delete a historizable data base object, an exception will be thrown.
     */
    @DeleteMapping(RestPaths.MARK_AS_DELETED)
    fun markAsDeleted(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<DTO>
    ): ResponseEntity<ResponseAction> {
        sessionCsrfService.validateCsrfToken(request, postData, "Mark as deleted")?.let { return it }
        val dbObj = transformForDB(postData.data)
        return markAsDeleted(request, baseDao, dbObj, postData, this, validate(dbObj, postData))
    }

    /**
     * The given object will be deleted, if supported by the [BaseDao] (forced, including any history entries due to privacy protection).
     * No undo is possible!
     */
    @DeleteMapping(RestPaths.FORCE_DELETE)
    fun forceDelete(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<DTO>
    ): ResponseEntity<ResponseAction> {
        val dbObj = transformForDB(postData.data)
        return forceDelete(request, baseDao, dbObj, postData, this)
    }

    /**
     * The given object is marked as deleted.
     * Please note, if you try to mark a non-historizable data base object, an exception will be thrown.
     */
    @DeleteMapping(RestPaths.DELETE)
    fun delete(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<DTO>
    ): ResponseEntity<ResponseAction> {
        sessionCsrfService.validateCsrfToken(request, postData, "Delete")?.let { return it }
        val dbObj = transformForDB(postData.data)
        return delete(request, baseDao, dbObj, postData, this, validate(dbObj, postData))
    }

    /**
     * Use this service for cancelling editing. The purpose of this method is only, to tell the client where
     * to redirect after cancellation.
     * @return ResponseAction
     */
    @PostMapping(RestPaths.CANCEL)
    fun onCancelEdit(request: HttpServletRequest, @Valid @RequestBody postData: PostData<DTO>): ResponseAction {
        val dbObj = transformForDB(postData.data)
        return onCancelEdit(request, dbObj, postData, getRestPath())
    }

    /**
     * This rest service will be called on multi selection list pages, if the user wants to cancel the multi selection.
     * @return redirect url
     */
    @GetMapping(RestPaths.CANCEL_MULTI_SELECTION)
    fun handleCancelUrl(request: HttpServletRequest): String {
        val callerUrl = MultiSelectionSupport.clear(request, this)
            ?: PagesResolver.getListPageUrl(this::class.java, absolute = true)
        return callerUrl
    }

    /**
     * The current filter will be reset and returned.
     */
    @GetMapping(RestPaths.FILTER_RESET)
    fun filterReset(): MagicFilter {
        val filter = getCurrentFilter()
        filter.reset()
        return filter
    }

    /**
     * Called before save, update, delete, markAsDeleted and undelete.
     */
    internal open fun onBeforeDatabaseAction(
        request: HttpServletRequest,
        obj: O,
        postData: PostData<DTO>,
        operation: OperationType
    ) {
    }

    /**
     * Called before save and update.
     */
    internal open fun onBeforeSaveOrUpdate(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Called before save.
     */
    internal open fun onBeforeSave(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Called before update.
     */
    internal open fun onBeforeUpdate(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Called after save and update.
     */
    open fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Here you may change the redirect url after the operation is completed.
     */
    open fun afterOperationRedirectTo(obj: O, postData: PostData<DTO>, event: RestButtonEvent): String? {
        return null
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterSave(obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(obj, postData, RestButtonEvent.SAVE)
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterUpdate(obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(obj, postData, RestButtonEvent.UPDATE)
    }

    /**
     * Called before delete (not markAsDeleted!).
     */
    internal open fun onBeforeDelete(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterDelete(obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(obj, postData, RestButtonEvent.DELETE)
    }

    /**
     * Called before markAsDeleted.
     */
    internal open fun onBeforeMarkAsDeleted(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterMarkAsDeleted(obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(obj, postData, RestButtonEvent.DELETE)
    }

    /**
     * Called before undelete.
     */
    internal open fun onBeforeUndelete(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterUndelete(obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(obj, postData, RestButtonEvent.UN_DELETE)
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onCancelEdit(
        request: HttpServletRequest,
        obj: O,
        postData: PostData<DTO>,
        restPath: String
    ): ResponseAction {
        return onAfterEdit(obj, postData, RestButtonEvent.CANCEL)
    }

    /**
     * Will be called after create, update, delete, markAsDeleted, undelete and cancel.
     * @return ResponseAction with the url of the standard list page.
     */
    internal open fun onAfterEdit(obj: O, postData: PostData<DTO>, event: RestButtonEvent): ResponseAction {
        obj.id?.let {
            userPrefService.putEntry(category, USER_PREF_PARAM_HIGHLIGHT_ROW, it, false)
        }
        var returnToCaller = postData.serverData?.returnToCaller
        if (!returnToCaller.isNullOrBlank()) {
            // ReturnToCaller was defined:
            val responseAction = createReturnToCallerResponseAction(returnToCaller)
            // Add any caller params if available:
            postData.serverData?.returnToCallerParams?.forEach {
                responseAction.addVariable(it.key, it.value)
            }
            return responseAction
        }
        returnToCaller = afterOperationRedirectTo(obj, postData, event)
                // Workaround to force reload to restore the AG Grid state (forceAGGridReload = true):
            ?: PagesResolver.getListPageUrl(
                this::class.java,
                absolute = true,
                // Force new hash for getting initialList (including ui on actions/list/index.js
                forceAGGridReload = true,
            )
        return ResponseAction(returnToCaller)
            .addVariable("id", obj.id ?: -1)
    }

    /**
     * Overwrite this method to replace returnToCaller by URL. At default the given returnToCaller will be used
     * unmodified as URL.
     */
    protected open fun createReturnToCallerResponseAction(returnToCaller: String): ResponseAction {
        return ResponseAction(returnToCaller)
    }

    internal open fun filterList(resultSet: MutableList<O>, filter: MagicFilter): List<O> {
        //  if (filter.maxRows > 0 && resultSet.size > filter.maxRows) {
        //      return resultSet.take(filter.maxRows)
        //  }
        return resultSet
    }

    /**
     * A unique id which is used as parent node for all attachments. Use [enableJcr] for creating unique nodes.
     * @return unique jcr path if attachments are supported or null, if no attachment support is given (download, upload and list).
     * @see [org.projectforge.rest.orga.ContractPagesRest] as an example.
     */
    open var jcrPath: String? = null  // open needed by Wicket's SpringBean for proxying.
        protected set

    /**
     * Call this method for enabling jcr support.
     * jcr part will be set to '$prefix.${baseDao.identifier}', must be unique.
     * @param prefix Define a prefix for having uniqueness. At default 'org.projectforge' is used.
     * @param identifier Uses [BaseDao.identifier] at default value.
     * @param supportedListIds Each entity may support multiple lists of attachments. This specifies the available lists in
     * *addition* to [AttachmentsDaoAccessChecker.DEFAULT_LIST_OF_ATTACHMENTS].
     */
    @JvmOverloads
    fun enableJcr(
        supportedListIds: Array<String>? = null,
        identifier: String? = null,
        attachmentsAccessChecker: AttachmentsAccessChecker? = null,
        /**
         * For creating FileSizeStandardChecker. Works only, if no accessChecker is given.
         */
        maxFileSize: Long = attachmentsService.maxDefaultFileSize.toBytes(),
        maxFileSizeSpringProperty: String = AttachmentsService.MAX_DEFAULT_FILE_SIZE_SPRING_PROPERTY
    ) {
        jcrPath = if (identifier != null) {
            getJcrPath(identifier)
        } else {
            baseDao.identifier?.let {
                getJcrPath(it)
            }
        }
        this.attachmentsAccessChecker =
            attachmentsAccessChecker ?: AttachmentsDaoAccessChecker(
                baseDao, jcrPath, supportedListIds, FileSizeStandardChecker(maxFileSize, maxFileSizeSpringProperty)
            )
    }

    /**
     * Convenience method for getting the list page response entity.
     * @param params Additional parameters for the list page.
     * @param absolute If true, the absolute URL will be returned.
     * @param highlightedObjectId If given, the row with this id will be highlighted. The id will be stored in the user preferences as well.
     * @param forceAGGridReload If true, the AG Grid will be reloaded (workaround).
     * @return ResponseEntity for the list page.
     */
    fun getListPageResponseEntity(
        params: Map<String, Any?>? = null,
        absolute: Boolean = false,
        highlightedObjectId: Long? = null,
        forceAGGridReload: Boolean = false,
    ): ResponseEntity<ResponseAction> {
        return ResponseEntity.ok()
            .body(
                getListPageResponseAction(
                    params,
                    absolute = absolute,
                    highlightedObjectId = highlightedObjectId,
                    forceAGGridReload = forceAGGridReload,
                )
            )
    }

    /**
     * Convenience method for getting the list page response action.
     * @param params Additional parameters for the list page.
     * @param absolute If true, the absolute URL will be returned.
     * @param highlightedObjectId If given, the row with this id will be highlighted. The id will be stored in the user preferences as well.
     * @param forceAGGridReload If true, the AG Grid will be reloaded (workaround).
     * @return ResponseAction for the list page.
     */
    fun getListPageResponseAction(
        params: Map<String, Any?>? = null,
        absolute: Boolean = false,
        highlightedObjectId: Long? = null,
        forceAGGridReload: Boolean = false,
    ): ResponseAction {
        val url = PagesResolver.getListPageUrl(
            this::class.java,
            params,
            absolute = absolute,
            forceAGGridReload = forceAGGridReload
        )
        val action = ResponseAction(url)
        if (highlightedObjectId != null) {
            action.addVariable("id", highlightedObjectId)
            if (highlightedObjectId >= 0) {
                userPrefService.putEntry(category, USER_PREF_PARAM_HIGHLIGHT_ROW, highlightedObjectId, false)
            }
        }
        return action
    }


    /**
     * Might be initialized by [enableJcr] with default dao access checker.
     */
    open lateinit var attachmentsAccessChecker: AttachmentsAccessChecker // open needed by Wicket's SpringBean for proxying.
        protected set

    /**
     * Implement on how to transform dto objects to data base objects (ExtendedBaseDO).
     */
    abstract fun transformForDB(dto: DTO): O

    /**
     * Implement on how to transform objects from the database (of type O, ExtendedBaseDO) to dto objects.
     * @param obj The object to transform.
     * @param editMode If true, this object will be prepared for editing by the user. (Used e. g. by [org.projectforge.rest.TeamCalPagesRest]. EditMode
     * may also be used for transforming from data base for list views (with minimal set of data) or edit mode with more data.
     */
    abstract fun transformFromDB(obj: O, editMode: Boolean = false): DTO

    abstract fun getId(dto: Any): Long?

    abstract fun isDeleted(dto: Any): Boolean

    abstract fun isHistorizable(): Boolean
}
