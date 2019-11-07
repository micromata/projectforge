/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.beanutils.NestedNullException
import org.apache.commons.beanutils.PropertyUtils
import org.projectforge.Const
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.*
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

/**
 * This is the base class for all fronted functionality regarding query, editing etc. It also serves layout
 * data for the frontend.
 * <br>
 * For each entity type such as users, addresses, time sheets etc. an own class is inherited for doing customizations.
 * It's recommended for the frontend to develop generic list and edit pages by using the layout information served
 * by these rest services.
 */
abstract class AbstractBaseRest<
        O : ExtendedBaseDO<Int>,
        DTO : Any, // DTO may be equals to O if no special data transfer objects are used.
        B : BaseDao<O>>(
        private val baseDaoClazz: Class<B>,
        private val i18nKeyPrefix: String,
        val cloneSupported: Boolean = false) {
    /**
     * If [getAutoCompletionObjects] is called without a special property to search for, all properties will be searched for,
     * given by this attribute. If null, an exception is thrown, if [getAutoCompletionObjects] is called without a property.
     */
    protected open val autoCompleteSearchFields: Array<String>? = null

    @PostConstruct
    private fun postConstruct() {
        this.lc = LayoutContext(baseDao.doClass)
    }

    companion object {
        const val GEAR_MENU = "GEAR"
        const val CREATE_MENU = "CREATE"
    }

    /**
     * Contains the layout data returned for the frontend regarding edit pages.
     * @param variables Additional variables / data provided for the edit page.
     */
    class EditLayoutData(val data: Any?, val ui: UILayout?, var variables: Map<String, Any>? = null)

    /**
     * Contains the data, layout and filter settings served by [getInitialList].
     */
    class InitialListData(
            val ui: UILayout?,
            val data: ResultSet<*>,
            val filterFavorites: List<Favorites.FavoriteIdTitle>,
            val filter: MagicFilter)

    private var initialized = false

    private var _baseDao: B? = null

    /**
     * e. g. /rs/address (/rs/{category}
     */
    private var restPath: String? = null

    private var category: String? = null

    private val userPrefArea = getCategory()

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
    private lateinit var historyService: HistoryService

    @Autowired
    private lateinit var userPrefService: UserPrefService

    /**
     * Override this method for initializing fields for new objects.
     * @return new instance of class ExtendedDO.
     */
    open fun newBaseDO(request: HttpServletRequest? = null): O {
        return baseDao.doClass.newInstance()
    }

    /**
     * Override this method for initializing fields for new objects.
     * Creates a new dto by calling [newBaseDO] and [transformFromDB].
     */
    open fun newBaseDTO(request: HttpServletRequest? = null): DTO {
        return transformFromDB(newBaseDO())
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

    /**
     * Relative rest path (without leading /rs
     */
    fun getRestPath(subPath: String? = null): String {
        if (restPath == null) {
            val requestMapping = this::class.annotations.find { it is RequestMapping } as? RequestMapping
            val url = requestMapping?.value?.joinToString("/") { it } ?: "/"
            restPath = url.substringAfter("${Rest.URL}/")
        }
        return if (subPath != null) "${restPath!!}/$subPath" else restPath!!
    }

    /**
     * Relative rest path (without leading /rs
     */
    fun getRestRootPath(subPath: String? = null): String {
        return "/${getRestPath(subPath)}"
    }

    private fun getCategory(): String {
        if (category == null) {
            category = getRestPath().removePrefix("${Rest.URL}/")
        }
        return category!!
    }

    open fun createEditLayout(dto: DTO, userAccess: UILayout.UserAccess): UILayout {
        val titleKey = if (getId(dto) != null) "$i18nKeyPrefix.edit" else "$i18nKeyPrefix.add"
        val layout = UILayout(titleKey)
        layout.userAccess.copyFrom(userAccess)
        return layout
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
                    validationErrors.add(ValidationError(translateMsg("validation.error.fieldRequired", translate(elementInfo.i18nKey)),
                            fieldId = property))
            }
        }
        return validationErrors
    }

    fun validate(dbObj: O, dto: DTO): List<ValidationError>? {
        val validationErrors = validate(dbObj)
        validate(validationErrors, dto)
        if (validationErrors.isEmpty()) return null
        return validationErrors
    }

    /**
     * Get the current filter from the server, all matching items and the layout of the list page.
     */
    @GetMapping("initialList")
    fun requestInitialList(request: HttpServletRequest): InitialListData {
        return getInitialList(request)
    }

    protected open fun getInitialList(request: HttpServletRequest): InitialListData {
        return getInitialList(getCurrentFilter())
    }

    protected fun getInitialList(filter: MagicFilter): InitialListData {
        val favorites = getFilterFavorites()
        val resultSet = processResultSetBeforeExport(getList(this, baseDao, filter))
        val layout = createListLayout()
                .addTranslations("table.showing")
        layout.add(LayoutListFilterUtils.createNamedContainer(this, lc))
        layout.postProcessPageMenu()
        layout.add(MenuItem(CREATE_MENU, title = "+", url = "${Const.REACT_APP_PATH}${getCategory()}/edit"), 0)
        return InitialListData(ui = layout,
                data = resultSet,
                filter = filter,
                filterFavorites = favorites.idTitleList)
    }

    /**
     * Add customized magic filter element in addition to the automatically detected elements.
     */
    open fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
    }

    /**
     * Get the list of all items matching the given filter.
     */
    @RequestMapping(RestPaths.LIST)
    fun getList(@RequestBody filter: MagicFilter): ResultSet<*> {
        filter.autoWildcardSearch = true
        fixMagicFilterFromClient(filter)
        val list = getList(this, baseDao, filter)
        saveCurrentFilter(filter)
        val resultSet = processResultSetBeforeExport(list)
        return resultSet
    }

    private fun getFilterFavorites(): Favorites<MagicFilter> {
        var favorites: Favorites<MagicFilter>? = null
        try {
            @Suppress("UNCHECKED_CAST", "USELESS_ELVIS")
            favorites = userPrefService.getEntry(userPrefArea, Favorites.PREF_NAME_LIST, Favorites::class.java) as? Favorites<MagicFilter>
        } catch (ex: Exception) {
            log.error("Exception while getting user preferred favorites: ${ex.message}. This might be OK for new releases. Ignoring filter.")
        }
        if (favorites == null) {
            // Creating empty filter list (user has no filter list yet):
            favorites = Favorites()
            userPrefService.putEntry(userPrefArea, Favorites.PREF_NAME_LIST, favorites)
        }
        return favorites
    }

    /**
     * Workaround of parsing effects, e. g. from and to is given as json to value.
     */
    private fun fixMagicFilterFromClient(magicFilter: MagicFilter) {
        if (magicFilter.entries.isNullOrEmpty())
            return
        magicFilter.entries.removeIf { it.isEmpty }
    }

    private fun getCurrentFilter(): MagicFilter {
        var currentFilter = userPrefService.getEntry(userPrefArea, Favorites.PREF_NAME_CURRENT, MagicFilter::class.java)
        if (currentFilter == null) {
            currentFilter = MagicFilter()
            saveCurrentFilter(currentFilter)
        } else {
            currentFilter.init()
        }
        @Suppress("UNCHECKED_CAST")
        return currentFilter
    }

    private fun saveCurrentFilter(currentFilter: MagicFilter) {
        userPrefService.putEntry(userPrefArea, Favorites.PREF_NAME_CURRENT, currentFilter)
    }

    @GetMapping("filter/select")
    fun selectFavoriteFilter(@RequestParam("id", required = true) id: Int): InitialListData {
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
        return getInitialList(currentFilter ?: getCurrentFilter())
    }

    /**
     * @return currentFilter, new filterFavorites and isFilterModified=false.
     */
    @RequestMapping("filter/create")
    fun createFavoriteFilter(@RequestBody newFilter: MagicFilter): Map<String, Any> {
        fixMagicFilterFromClient(newFilter)
        val favorites = getFilterFavorites()
        favorites.add(newFilter)
        val currentFilter = newFilter.clone() // A clone is needed, otherwise current and favorite of list are the same object.
        saveCurrentFilter(currentFilter)
        return mapOf(
                "filter" to currentFilter,
                "filterFavorites" to favorites.idTitleList)
    }

    /**
     * @return new filterFavorites
     */
    @GetMapping("filter/rename")
    fun renameFavoriteFilter(@RequestParam("id", required = true) id: Int, @RequestParam("newName", required = true) newName: String): Map<String, Any> {
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
                "filterFavorites" to favorites.idTitleList)
    }

    /**
     * Updates the named Filter with the given values.
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
    fun deleteFavoriteFilter(@RequestParam("id", required = true) id: Int): Map<String, Any> {
        val favorites = getFilterFavorites()
        favorites.remove(id)
        return mapOf("filterFavorites" to getFilterFavorites().idTitleList)
    }

    /**
     * Rebuilds the index by the search engine for the newest entries.
     * @see [BaseDao.rebuildDatabaseIndex4NewestEntries]
     */
    @GetMapping("reindexNewest")
    fun reindexNewest(): ResponseAction {
        baseDao.rebuildDatabaseIndex4NewestEntries()
        return ResponseAction(message = ResponseAction.Message("administration.reindexNewest.successful", color = UIColor.SUCCESS))
    }

    /**
     * Rebuilds the index by the search engine for all entries.
     * @see [BaseDao.rebuildDatabaseIndex]
     */
    @GetMapping("reindexFull")
    fun reindexFull(): ResponseAction {
        baseDao.rebuildDatabaseIndex()
        return ResponseAction(message = ResponseAction.Message("administration.reindexFull.successful", color = UIColor.SUCCESS))
    }

    abstract fun processResultSetBeforeExport(resultSet: ResultSet<O>): ResultSet<*>

    /**
     * Gets the item from the database.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * layout will be also included if the id is not given.
     */
    @GetMapping("{id}")
    fun getItem(@PathVariable("id") id: Int?): ResponseEntity<Any> {
        val item = getById(id, true) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity(item, HttpStatus.OK)
    }

    protected open fun getById(idString: String?, editMode: Boolean = false, userAccess: UILayout.UserAccess? = null): DTO? {
        if (idString == null) return null
        return getById(idString.toInt(), editMode, userAccess)
    }

    protected fun getById(id: Int?, editMode: Boolean = false, userAccess: UILayout.UserAccess? = null): DTO? {
        val item = baseDao.getById(id) ?: return null
        checkUserAccess(item, userAccess)
        return transformFromDB(item, editMode)
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
     */
    @GetMapping(RestPaths.EDIT)
    fun getItemAndLayout(request: HttpServletRequest, @RequestParam("id") id: String?)
            : ResponseEntity<EditLayoutData> {
        val userAccess = UILayout.UserAccess()
        val item = (if (null != id) {
            getById(id, true, userAccess)
        } else {
            checkUserAccess(null, userAccess)
            newBaseDTO(request)
        })
                ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        onBeforeGetItemAndLayout(request, item, userAccess)
        val layout = getItemAndLayout(request, item, userAccess)
        return ResponseEntity(layout, HttpStatus.OK)
    }

    /**
     * Will be called after getting the item from the database before calling [onGetItemAndLayout]. No initial layout
     * is available.
     */
    open protected fun onBeforeGetItemAndLayout(request: HttpServletRequest, dto: DTO, userAccess: UILayout.UserAccess) {
    }

    protected fun getItemAndLayout(request: HttpServletRequest, dto: DTO, userAccess: UILayout.UserAccess): EditLayoutData {
        val layout = createEditLayout(dto, userAccess)
        layout.addTranslations("changes", "tooltip.selectMe")
        layout.postProcessPageMenu()
        val result = EditLayoutData(dto, layout)
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
    protected open fun onGetItemAndLayout(request: HttpServletRequest, dto: DTO, editLayoutData: EditLayoutData) {
    }

    /**
     * Use this method to add customized variables for your edit page for the initial call.
     */
    protected open fun addVariablesForEditPage(dto: DTO): Map<String, Any>? {
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
    @GetMapping("ac")
    open fun getAutoCompletionForProperty(@RequestParam("property") property: String, @RequestParam("search") searchString: String?)
            : List<String> {
        return baseDao.getAutocompletion(property, searchString)
    }

    /**
     * Gets the autocompletion list for the given search string by searching in all properties defined by [autoCompleteSearchFields].
     * If [autoCompleteSearchFields] is not given an [InternalErrorException] will be thrown.
     * @param searchString
     * @return list of found objects.
     */
    @GetMapping("aco")
    open fun getAutoCompletionObjects(@RequestParam("search") searchString: String?): MutableList<O> {
        if (autoCompleteSearchFields.isNullOrEmpty()) {
            throw RuntimeException("Can't call getAutoCompletion without property, because no autoCompleteSearchFields are configured by the developers for this entity.")
        }
        val filter = BaseSearchFilter()
        filter.searchString = searchString
        filter.setSearchFields(*autoCompleteSearchFields!!)
        return baseDao.getList(filter)
    }

    /**
     * Gets the history items of the given entity.
     * @param id Id of the item to get the history entries for.
     */
    @GetMapping("history/{id}")
    fun getHistory(@PathVariable("id") id: Int?): ResponseEntity<List<HistoryService.DisplayHistoryEntry>> {
        if (id == null) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val item = baseDao.getById(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val historyEntries = baseDao.getHistoryEntries(item)
        return ResponseEntity(historyService.format(historyEntries), HttpStatus.OK)
    }

    /**
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
     */
    @RequestMapping(RestPaths.CLONE)
    fun clone(request: HttpServletRequest, @RequestBody dto: DTO)
            : ResponseAction {
        val item = prepareClone(dto)
        val editLayoutData = getItemAndLayout(request, item, UILayout.UserAccess(false, true))
        return ResponseAction(url = getRestEditPath(), targetType = TargetType.UPDATE)
                .addVariable("data", editLayoutData.data)
                .addVariable("ui", editLayoutData.ui)
                .addVariable("variables", editLayoutData.variables)
    }

    /**
     * Might be modified e. g. for edit pages handled in modals (timesheets and calendar events).
     */
    open protected fun getRestEditPath(): String {
        return getRestRootPath(RestPaths.EDIT)
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
                dto.isDeleted = false
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

    /**
     * Use this service for adding new items as well as updating existing items (id isn't null).
     */
    @PutMapping(RestPaths.SAVE_OR_UDATE)
    fun saveOrUpdate(request: HttpServletRequest, @Valid @RequestBody dto: DTO): ResponseEntity<ResponseAction> {
        val dbObj = transformForDB(dto)
        return saveOrUpdate(request, baseDao, dbObj, dto, this, validate(dbObj, dto))
    }

    /**
     * The given object (marked as deleted before) will be undeleted.
     */
    @PutMapping(RestPaths.UNDELETE)
    fun undelete(request: HttpServletRequest, @Valid @RequestBody dto: DTO): ResponseEntity<ResponseAction> {
        val dbObj = transformForDB(dto)
        return undelete(request, baseDao, dbObj, dto, this, validate(dbObj, dto))
    }

    /**
     * The given object will be deleted.
     * Please note, if you try to delete a historizable data base object, an exception will be thrown.
     */
    @DeleteMapping(RestPaths.MARK_AS_DELETED)
    fun markAsDeleted(request: HttpServletRequest, @Valid @RequestBody dto: DTO): ResponseEntity<ResponseAction> {
        val dbObj = transformForDB(dto)
        return markAsDeleted(request, baseDao, dbObj, dto, this, validate(dbObj, dto))
    }

    /**
     * The given object is marked as deleted.
     * Please note, if you try to mark a non-historizable data base object, an exception will be thrown.
     */
    @DeleteMapping(RestPaths.DELETE)
    fun delete(request: HttpServletRequest, @Valid @RequestBody dto: DTO): ResponseEntity<ResponseAction> {
        val dbObj = transformForDB(dto)
        return delete(request, baseDao, dbObj, dto, this, validate(dbObj, dto))
    }

    /**
     * Use this service for cancelling editing. The purpose of this method is only, to tell the client where
     * to redirect after cancellation.
     * @return ResponseAction
     */
    @PostMapping(RestPaths.CANCEL)
    fun cancelEdit(request: HttpServletRequest, @RequestBody dto: DTO): ResponseAction {
        val dbObj = transformForDB(dto)
        return cancelEdit(request, dbObj, dto, getRestPath())
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
    internal open fun beforeDatabaseAction(request: HttpServletRequest, obj: O, dto: DTO, operation: OperationType) {
    }

    /**
     * Called before save and update.
     */
    internal open fun beforeSaveOrUpdate(request: HttpServletRequest, obj: O, dto: DTO) {
    }

    /**
     * Called after save and update.
     */
    internal open fun afterSaveOrUpdate(obj: O, dto: DTO) {
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterSave(obj: O, dto: DTO): ResponseAction {
        return afterEdit(obj, dto)
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterUpdate(obj: O, dto: DTO): ResponseAction {
        return afterEdit(obj, dto)
    }

    /**
     * Called before delete (not markAsDeleted!).
     */
    internal open fun beforeDelete(request: HttpServletRequest, obj: O, dto: DTO) {
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterDelete(obj: O, dto: DTO): ResponseAction {
        return afterEdit(obj, dto)
    }

    /**
     * Called before markAsDeleted.
     */
    internal open fun beforeMarkAsDeleted(request: HttpServletRequest, obj: O, dto: DTO) {
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterMarkAsDeleted(obj: O, dto: DTO): ResponseAction {
        return afterEdit(obj, dto)
    }

    /**
     * Called before undelete.
     */
    internal open fun beforeUndelete(request: HttpServletRequest, obj: O, dto: DTO) {
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterUndelete(obj: O, dto: DTO): ResponseAction {
        return afterEdit(obj, dto)
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun cancelEdit(request: HttpServletRequest, obj: O, dto: DTO, restPath: String): ResponseAction {
        return afterEdit(obj, dto)
    }

    /**
     * Will be called after create, update, delete, markAsDeleted, undelete and cancel.
     * @return ResponseAction with the url of the standard list page.
     */
    internal open fun afterEdit(obj: O, dto: DTO): ResponseAction {
        return ResponseAction("/${Const.REACT_APP_PATH}${getCategory()}").addVariable("id", obj.id ?: -1)
    }

    internal open fun filterList(resultSet: MutableList<O>, filter: MagicFilter): List<O> {
        //  if (filter.maxRows > 0 && resultSet.size > filter.maxRows) {
        //      return resultSet.take(filter.maxRows)
        //  }
        return resultSet
    }

    /**
     * Implement on how to transform dto objects to data base objects (ExtendedBaseDO).
     */
    abstract fun transformForDB(dto: DTO): O

    /**
     * Implement on how to transform objects from the data base (of type O, ExtendedBaseDO) to dto objects.
     * @param obj The object to transform.
     * @param editMode If true, this object will be prepared for editing by the user. (Used e. g. by [org.projectforge.rest.TeamCalRest].
     */
    abstract fun transformFromDB(obj: O, editMode: Boolean = false): DTO

    abstract fun getId(dto: Any): Int?

    abstract fun isDeleted(dto: Any): Boolean

    abstract fun isHistorizable(): Boolean

    private class MagicFilterEntries(var entries: MutableList<MagicFilterEntry> = mutableListOf(),
                                     var name: String? = null,
                                     var id: Int? = null)
}
