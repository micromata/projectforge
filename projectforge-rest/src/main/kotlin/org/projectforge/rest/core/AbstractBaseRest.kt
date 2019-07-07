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

import org.apache.commons.beanutils.PropertyUtils
import org.projectforge.business.common.MagicFilter
import org.projectforge.business.common.MagicFilterEntry
import org.projectforge.business.user.UserPrefCache
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.MessageType
import org.projectforge.rest.ResponseData
import org.projectforge.rest.config.Rest
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession
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
        B : BaseDao<O>,
        F : BaseSearchFilter>(
        private val baseDaoClazz: Class<B>,
        private val filterClazz: Class<F>,
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
    }

    /**
     * Contains the layout data returned for the frontend regarding edit pages.
     * @param variables Additional variables / data provided for the edit page.
     */
    class EditLayoutData(val data: Any?, val ui: UILayout?, var variables: Map<String, Any>? = null)

    /**
     * Contains the data, layout and filter settings served by [getInitialList].
     */
    class InitialListData<F : BaseSearchFilter>(
            val ui: UILayout?,
            val data: ResultSet<*>,
            val filterFavorites: List<MagicFilter<F>>,
            val filter: MagicFilter<F>)

    private var initialized = false

    private var _baseDao: B? = null

    /**
     * e. g. /rs/address (/rs/{category}
     */
    private var restPath: String? = null

    private var category: String? = null

    protected val favoriteFiltersUserPrefArea = "${getCategory()}.filterFavorites"

    protected val currentFilterUserPrefArea = "${getCategory()}.filter"

    protected val currentFilterUserPrefName = "current"

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
    private lateinit var listFilterService: ListFilterService

    @Autowired
    private lateinit var userPrefCache: UserPrefCache

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
    fun getRestPath(): String {
        if (restPath == null) {
            val requestMapping = this::class.annotations.find { it is RequestMapping } as? RequestMapping
            val url = requestMapping?.value?.joinToString("/") { it } ?: "/"
            restPath = url.substringAfter("${Rest.URL}/")
        }
        return restPath!!
    }

    private fun getCategory(): String {
        if (category == null) {
            category = getRestPath().removePrefix("${Rest.URL}/")
        }
        return category!!
    }

    open fun createEditLayout(dto: DTO): UILayout {
        val titleKey = if (getId(dto) != null) "$i18nKeyPrefix.edit" else "$i18nKeyPrefix.add"
        return UILayout(titleKey)
    }

    open fun validate(validationErrors: MutableList<ValidationError>, dto: DTO) {
    }

    fun validate(dbObj: O): MutableList<ValidationError> {
        val validationErrors = mutableListOf<ValidationError>()
        val propertiesMap = ElementsRegistry.getProperties(dbObj::class.java)!!
        propertiesMap.forEach {
            val property = it.key
            val elementInfo = it.value
            val value = PropertyUtils.getProperty(dbObj, property)
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
    open fun getInitialList(session: HttpSession): InitialListData<F> {
        //val test = providers.getContextResolver(MyObjectMapper::class.java,  MediaType.WILDCARD_TYPE)
        @Suppress("UNCHECKED_CAST")
        val currentFilter = getCurrentFilter()
        val resultSet = processResultSetBeforeExport(getList(this, baseDao, currentFilter, filterClazz))
        val layout = createListLayout()
                .addTranslations("table.showing")
        layout.add(LayoutListFilterUtils.createNamedContainer(baseDao, lc))
        layout.postProcessPageMenu()
        return InitialListData<F>(ui = layout, data = resultSet, filter = currentFilter, filterFavorites = getFavoriteFilterList())
    }

    /**
     * Rebuilds the index by the search engine for the newest entries.
     * @see [BaseDao.rebuildDatabaseIndex4NewestEntries]
     */
    @GetMapping("reindexNewest")
    fun reindexNewest(): ResponseData {
        baseDao.rebuildDatabaseIndex4NewestEntries()
        return ResponseData("administration.reindexNewest.successful", messageType = MessageType.TOAST, style = UIStyle.SUCCESS)
    }

    /**
     * Rebuilds the index by the search engine for all entries.
     * @see [BaseDao.rebuildDatabaseIndex]
     */
    @GetMapping("reindexFull")
    fun reindexFull(): ResponseData {
        baseDao.rebuildDatabaseIndex()
        return ResponseData("administration.reindexFull.successful", messageType = MessageType.TOAST, style = UIStyle.SUCCESS)
    }

    /**
     * Get the list of all items matching the given filter.
     */
    @RequestMapping(RestPaths.LIST)
    fun getList(request: HttpServletRequest, @RequestBody filter: MagicFilter<F>): ResultSet<*> {
        val list = getList(this, baseDao, filter, filterClazz)
        saveCurrentFilter(filter)
        val resultSet = processResultSetBeforeExport(list)
        val storedFilter = listFilterService.getSearchFilter(request.session, filterClazz)
        BeanUtils.copyProperties(filter, storedFilter)
        return resultSet
    }

    fun getFavoriteFilterList(): List<MagicFilter<F>> {
        val userPrefs = userPrefCache.getEntries(favoriteFiltersUserPrefArea)
        return userPrefs.map {
            @Suppress("UNCHECKED_CAST")
            it.valueObject as MagicFilter<F>
        }
    }

    fun getCurrentFilter(): MagicFilter<F> {
        var currentFilter = userPrefCache.getEntry(currentFilterUserPrefArea, currentFilterUserPrefName, MagicFilter::class.java)
        if (currentFilter == null) {
            currentFilter = MagicFilter<F>()
            saveCurrentFilter(currentFilter)
        }
        return currentFilter as MagicFilter<F>
    }

    private fun saveCurrentFilter(currentFilter: MagicFilter<F>) {
        if (currentFilter.searchFilter == null) {
            currentFilter.searchFilter = filterClazz.newInstance()
        }
        userPrefCache.putEntry(currentFilterUserPrefArea, currentFilterUserPrefName, currentFilter)
    }

    @GetMapping("filter/select")
    fun selectFavoriteFilter(@RequestParam("id", required = true) id: Int): Any {
        //val userPref = userPrefDao.getUserPref(favoriteFiltersUserPrefArea, id) ?: return ""
        //@Suppress("UNCHECKED_CAST")
        //return userPref.valueObject as MagicFilter<F>
        return MagicFilter<F>()
    }

    /**
     * Get the list of all items matching the given filter.
     */
    @RequestMapping("filter/create")
    fun createFavoriteFilter(request: HttpServletRequest, @RequestBody newFilter: MagicFilter<F>): List<MagicFilter<F>> {
        //val favorites = Favorites(getFavoriteFilterList(), favoriteFiltersUserPrefArea)
        //favorites.createUserPref(userPrefDao, newFilter)
        return getFavoriteFilterList()
    }

    /**
     * Updates the named Filter with the values of the current filter.
     * @return The current filter with flag modified=false.
     */
    @GetMapping("filter/update")
    fun updateFavoriteFilter(@RequestParam("id", required = true) id: Int): Map<String, Any> {
        log.info("updateFilter has to be implemented.")
        return mapOf("isFilterModified" to false)
    }

    /**
     * @return The new list of filterFavorites (id's with titles) without the deleted filter.
     */
    @GetMapping("filter/delete")
    fun removeFavoriteFilter(@RequestParam("id", required = true) id: Int): Map<String, Any> {
        log.info("deleteFilter has to be implemented.")
        return mapOf()//"filterFavorites" to getFilterFavorites().idTitleList)
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

    protected open fun getById(idString: String?, editMode: Boolean = false): DTO? {
        if (idString == null) return null
        return getById(idString.toInt(), editMode)
    }

    protected fun getById(id: Int?, editMode: Boolean = false): DTO? {
        val item = baseDao.getById(id) ?: return null
        return transformFromDB(item, editMode)
    }

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * a group with a separate label and input field will be generated.
     * layout will be also included if the id is not given.
     */
    @GetMapping("edit")
    fun getItemAndLayout(request: HttpServletRequest, @RequestParam("id") id: String?)
            : ResponseEntity<EditLayoutData> {
        val item = (if (null != id) getById(id, true) else newBaseDTO(request))
                ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity(getItemAndLayout(request, item), HttpStatus.OK)
    }

    private fun getItemAndLayout(request: HttpServletRequest, dto: DTO): EditLayoutData {
        val layout = createEditLayout(dto)
        layout.addTranslations("changes", "tooltip.selectMe")
        layout.postProcessPageMenu()
        val result = EditLayoutData(dto, layout)
        onGetItemAndLayout(request, dto, result)
        val additionalVariables = addVariablesForEditPage(dto)
        if (additionalVariables != null)
            result.variables = additionalVariables
        return result
    }

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
    @RequestMapping("clone")
    fun clone(request: HttpServletRequest, @RequestBody dto: DTO)
            : ResponseAction {
        val item = prepareClone(dto)
        val editLayoutData = getItemAndLayout(request, item)
        return ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", editLayoutData.data)
                .addVariable("ui", editLayoutData.ui)
                .addVariable("variables", editLayoutData.variables)
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
    fun undelete(@RequestBody dto: DTO): ResponseEntity<ResponseAction> {
        val dbObj = transformForDB(dto)
        return undelete(baseDao, dbObj, dto, this, validate(dbObj, dto))
    }

    /**
     * The given object will be deleted.
     * Please note, if you try to delete a historizable data base object, an exception will be thrown.
     */
    @DeleteMapping(RestPaths.MARK_AS_DELETED)
    fun markAsDeleted(@RequestBody dto: DTO): ResponseEntity<ResponseAction> {
        val dbObj = transformForDB(dto)
        return markAsDeleted(baseDao, dbObj, dto, this, validate(dbObj, dto))
    }

    /**
     * The given object is marked as deleted.
     * Please note, if you try to mark a non-historizable data base object, an exception will be thrown.
     */
    @DeleteMapping(RestPaths.DELETE)
    fun delete(@RequestBody dto: DTO): ResponseEntity<ResponseAction> {
        val dbObj = transformForDB(dto)
        return delete(baseDao, dbObj, dto, this, validate(dbObj, dto))
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
     * The filters are reset and the default returned.
     */
    @GetMapping(RestPaths.FILTER_RESET)
    fun filterReset(request: HttpServletRequest): BaseSearchFilter {
        return listFilterService.getSearchFilter(request.session, filterClazz).reset()
    }

    internal open fun beforeSaveOrUpdate(request: HttpServletRequest, obj: O, dto: DTO) {
    }

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
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterDelete(obj: O, dto: DTO): ResponseAction {
        return afterEdit(obj, dto)
    }

    /**
     * Will only be called on success. Simply call [afterEdit].
     */
    internal open fun afterMarkAsDeleted(obj: O, dto: DTO): ResponseAction {
        return afterEdit(obj, dto)
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
        return ResponseAction("/${getCategory()}").addVariable("id", obj.id ?: -1)
    }

    internal open fun filterList(resultSet: MutableList<O>, filter: F): List<O> {
        if (filter.maxRows > 0 && resultSet.size > filter.maxRows) {
            return resultSet.take(filter.maxRows)
        }
        return resultSet
    }

    /**
     * Implement on how to transform dto objects to data base objects (ExtendedBaseDO).
     */
    abstract fun transformForDB(dto: DTO): O

    /**
     * Implement on how to transform objects from the data base (of type O, ExtendedBaseDO) to dto objects.
     */
    abstract fun transformFromDB(obj: O, editMode: Boolean = false): DTO

    abstract fun getId(dto: Any): Int?

    abstract fun isDeleted(dto: Any): Boolean

    abstract fun isHistorizable(): Boolean

    private class MagicFilterEntries(var entries: MutableList<MagicFilterEntry> = mutableListOf(),
                                     var name: String? = null,
                                     var id: Int? = null)
}
