/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.NextMigration
import org.projectforge.business.user.service.UserPrefService
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
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.framework.persistence.api.*
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.database.DatabaseDao
import org.projectforge.framework.persistence.history.DisplayHistoryEntry
import org.projectforge.framework.persistence.history.HistoryBaseDaoAdapter
import org.projectforge.framework.persistence.history.HistoryFormatService
import org.projectforge.framework.persistence.search.SearchStringTokenizer
import org.projectforge.jcr.FileSizeStandardChecker
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.core.aggrid.GridState
import org.projectforge.rest.dto.*
import org.projectforge.rest.dto.datatable.DataTableStateRequest
import org.projectforge.rest.jobs.ReindexJob
import org.projectforge.rest.multiselect.MultiSelectNavigation
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.web.rest.RestAuthenticationUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.io.Serializable

private val log = KotlinLogging.logger {}

/**
 * The layout free base class of all entity rest services: query, autocompletion, history, filter
 * favorites, the writes and the attachment support of one entity - everything a client needs that
 * builds its pages itself.
 *
 * The counterpart is [AbstractPagesRest], which adds the server side generated `UILayout` on top for
 * the legacy React app (and for the pages of projectforge-next that are still rendered from it). A
 * page migrated to a hand built projectforge-next page extends this class instead and stops
 * producing that layout altogether: for such a page the layout was built, serialized and dropped by
 * the client.
 *
 * "Layout free" doesn't mean free of [org.projectforge.ui]: the filter fields of a list page are
 * derived from the DAO's search fields ([LayoutListFilterUtils.createNamedSearchFilterContainer]) and
 * travel as [UILabelledElement]s, because that serialization *is* the contract of a filter field with
 * both frontends. What is gone is the page layout: grid columns, edit form, page menu and the ~50
 * translations that come with them.
 *
 * @author Kai Reinhard
 */
abstract class AbstractEntityRest<
        O : ExtendedBaseDO<Long>,
        DTO : Any, // DTO may be equals to O if no special data transfer objects are used.
        B : BaseDao<O>>
@JvmOverloads
constructor(
    private val baseDaoClazz: Class<B>,
    protected val i18nKeyPrefix: String,
    val cloneSupport: CloneSupport = CloneSupport.NONE,
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

    companion object {
        const val USER_PREF_PARAM_HIGHLIGHT_ROW = "highlightedRow"
        private const val JCR_PATH_PREFIX: String = "org.projectforge"

        fun getJcrPath(identifier: String): String {
            return "$JCR_PATH_PREFIX.$identifier"
        }

        // For caching the historizable flag:
        private val historizableMap = mutableMapOf<Class<*>, Boolean>()

        internal fun isHistorizable(clazz: Class<out ExtendedBaseDO<*>>): Boolean {
            historizableMap[clazz]?.let { return it }
            val result = HistoryBaseDaoAdapter.isHistorizable(clazz)
            historizableMap[clazz] = result
            return result
        }
    }

    class DisplayObject(val id: Any?, override val displayName: String?) : DisplayNameCapable

    /**
     * Category should be unique and is e. g. used as react path. At default, it's the dir of the url defined in class annotation [RequestMapping].
     */
    open val category: String by lazy { getRestPath().removePrefix("${Rest.URL}/") } // open needed by Wicket's SpringBean for proxying.

    /**
     * The layout context is needed to examine the data objects for maxLength, nullable, dataType etc.
     */
    protected lateinit var lc: LayoutContext

    val baseDao: B by lazy { applicationContext.getBean(baseDaoClazz) }

    @Autowired
    protected lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    protected lateinit var attachmentsService: AttachmentsService

    @Autowired
    lateinit var agGridSupport: AGGridSupport

    @Autowired
    private lateinit var databaseDao: DatabaseDao

    @Autowired
    private lateinit var historyFormatService: HistoryFormatService

    @Autowired
    private lateinit var jobHandler: JobHandler

    @Autowired
    protected lateinit var sessionCsrfService: SessionCsrfService

    @Autowired
    protected lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var listPageCache: ListPageCache

    @PostConstruct
    private fun postConstruct() {
        this.lc = LayoutContext(baseDao.doClass)
        PagesResolver.register(category, this)
    }

    /**
     * If [getAutoCompleteObjects] is called without a special property to search for, all properties will be searched for,
     * given by this attribute. If null, an exception is thrown, if [getAutoCompleteObjects] is called without a property.
     */
    open val autoCompleteSearchFields: Array<String>? = null

    open val addNewEntryUrl: String by lazy { NextMigration.newEntryUrl(category) }

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

    /**
     * The url template of the page a row of the list page leads to, with [NextMigration.ID_PLACEHOLDER]
     * for the id. Without leading slash. Overridden by pages whose rows don't open an edit form (the
     * address view page, the script execution page, the Wicket project edit page ...).
     *
     * Not `protected`, because [org.projectforge.rest.core.aggrid.AGGridSupport] builds the row click
     * url of the grid from it.
     */
    open fun getStandardEditPage(): String {
        return NextMigration.standardEditPage(category)
    }

    // ------------------------------------------------------------------------------------------
    // New objects
    // ------------------------------------------------------------------------------------------

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

    /**
     * The preset a client starts an "add" from, e. g. today's date of entry and the logged-in project
     * manager for a new order.
     *
     * The layout free counterpart of `{entity}/edit` without an id: a hand built page has no use for
     * the edit layout beside the data, and no use for the server data either (it takes the CSRF token
     * from `userStatus`).
     */
    @GetMapping("newEntry")
    fun getNewEntry(request: HttpServletRequest): ResponseEntity<Any> {
        return ResponseEntity(newBaseDTO(request), HttpStatus.OK)
    }

    // ------------------------------------------------------------------------------------------
    // List meta data and the magic filter
    // ------------------------------------------------------------------------------------------

    /**
     * Everything a hand built list page needs beside its rows: the filter fields of this entity, the
     * filter the user left the page with, and their saved filters.
     *
     * The layout free counterpart of `{entity}/initialList`, which additionally builds the whole page
     * layout and - for a client that fetches the rows from `list` itself - the whole result set.
     */
    @GetMapping("listMeta")
    fun requestListMeta(request: HttpServletRequest): ListMetaData {
        return getListMeta(request, getCurrentFilter())
    }

    protected fun getListMeta(request: HttpServletRequest, filter: MagicFilter): ListMetaData {
        val userAccess = UILayout.UserAccess()
        checkUserAccess(null, userAccess)
        // Assumed rather than checked, because it is not a question about the *list*: whether an entry
        // may be written is asked per entry and travels on its DTO (EntityAccessSupport, filled in
        // getById from the same DAO the write goes through - see Group for an entity every user may read
        // but only an administrator may change).
        // The flags checkUserAccess did fill are read with throwException = false for the same reason:
        // userAccess describes what the UI should offer, it does not authorize anything. The DAO decides
        // that when the call arrives (see ListMetaData.userAccess).
        //
        // The exception is `read`: a user without select access has no list to be shown at all, so
        // projectforge-next keeps the page from rendering rather than offering an empty one. Still not
        // the authorization - `list` is refused by the DAO either way, and answers 403 for a next client
        // (see GlobalDefaultExceptionHandler).
        //
        // Assumed for every entity that has no entity wide answer, and asked where one exists: a list
        // whose rows nobody may open must not offer the click that opens them (see listUpdateAccess).
        userAccess.update = listUpdateAccess()
        // Whether a write may carry a comment for the history entry it produces, which is a property of
        // the entity and not of the user: the same answer AbstractPagesRest puts into the layout's
        // userAccess, from where the server laid out edit page adds its comment field
        // (LayoutUtils.processEditPage). A hand built form has no layout to read it from, so it takes it
        // from here (see useHistoryCommentSupport).
        userAccess.editHistoryComments = baseDao.supportsHistoryUserComments
        val searchFilterContainer = LayoutListFilterUtils.createNamedSearchFilterContainer(this, lc)
        val elements = searchFilterContainer.content.filterIsInstance<UILabelledElement>()
        removeUnknownFilterEntries(filter, elements.filterIsInstance<UIFilterElement>().map { it.id }.toSet())
        val favorites = getFilterFavorites()
        return ListMetaData(
            filter = filter,
            filterFavorites = favorites.idTitleList,
            // The stored values of the favorite this filter came from, so a client that was just loaded
            // knows whether there is anything to save (see ListMetaData.filterFavorite).
            filterFavorite = favorites.get(filter.id),
            filterElements = elements,
            standardEditPage = getStandardEditPage(),
            // Escape hatches ("way back" to the legacy page), marked so OrphanedLinkFilter lets them
            // reach it instead of bending them straight back to next (see NextMigration.withEscapeHatchMarker).
            legacyListPage = NextMigration.withEscapeHatchMarker(NextMigration.legacyListUrl(category)),
            legacyEditPage = NextMigration.withEscapeHatchMarker(NextMigration.legacyEditPage(category)),
            legacyNewEntryPage = NextMigration.withEscapeHatchMarker(NextMigration.legacyNewEntryUrl(category)),
            userAccess = userAccess,
            // What this user last ticked for a mass update, so a reload (or a detour through the legacy
            // app) restores it. Read under this rest class, the identifier startSelection registers and
            // {page}/select narrows under - see ListMetaData.selectedIds.
            selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, this::class.java),
            variables = addVariablesForListPage(),
        )
    }

    /**
     * Add customized magic filter element in addition to the automatically detected elements.
     */
    open fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
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

    /**
     * Use this method to add customized variables for your list page for the initial call.
     */
    protected open fun addVariablesForListPage(): Map<String, Any>? {
        return null
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
     * The filter a user who has none starts with, and the one a reset returns to.
     *
     * Empty for most pages, but not for every: Wicket's task list hides closed tasks until they are asked
     * for ([org.projectforge.business.task.TaskFilter] defaults `closed` to false), and a preset entry is
     * the only way to say so *and* show it - the client renders the filter row from the stored filter, so a
     * restriction applied silently while querying would filter the list without appearing anywhere in it.
     *
     * Only preset entries whose field is offered as a filter element (see [addMagicFilterElements]):
     * [removeUnknownFilterEntries] drops the rest.
     */
    open fun newMagicFilter(): MagicFilter {
        return MagicFilter()
    }

    fun getCurrentFilter(): MagicFilter {
        var currentFilter = userPrefService.getEntry(category, Favorites.PREF_NAME_CURRENT, MagicFilter::class.java)
        if (currentFilter == null) {
            currentFilter = newMagicFilter()
            saveCurrentFilter(currentFilter)
        } else {
            currentFilter.init()
        }
        // Fixing the maxRows to the default value. Max rows was mis-used as paginationPageSize in the past.
        currentFilter.maxRows = QueryFilter.QUERY_FILTER_MAX_ROWS
        return currentFilter
    }

    protected fun saveCurrentFilter(currentFilter: MagicFilter) {
        userPrefService.putEntry(category, Favorites.PREF_NAME_CURRENT, currentFilter)
    }

    protected fun getFilterFavorites(): Favorites<MagicFilter> {
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
    protected fun fixMagicFilterFromClient(magicFilter: MagicFilter) {
        if (magicFilter.entries.isNullOrEmpty()) {
            magicFilter.deleted = false
            return
        }
        magicFilter.entries.removeIf { it.isEmpty }
    }

    /**
     * Makes the given favorite the user's current filter.
     *
     * @return The applied filter, or the unchanged current one if the favorite is gone.
     */
    protected fun applyFavoriteFilter(id: Long): MagicFilter {
        val favorites = getFilterFavorites()
        val currentFilter = favorites.get(id)
        if (currentFilter == null) {
            log.warn("Can't select filter $id, because it's not found in favorites list.")
            return getCurrentFilter()
        }
        // Puts a deep copy of the current filter. Without copying, the favorite filter of the list will
        // be synchronized with the current filter.
        saveCurrentFilter(currentFilter.clone())
        currentFilter.init()
        return currentFilter
    }

    /**
     * Applies a saved filter and returns the state of the list page for it.
     *
     * @return [ListMetaData]. [AbstractPagesRest] answers with its `InitialListData` instead, which is
     * why the return type is [Any]: the two clients need a different payload for the same action.
     */
    @GetMapping("filter/select")
    open fun selectFavoriteFilter(
        request: HttpServletRequest,
        @RequestParam("id", required = true) id: Long
    ): Any {
        return getListMeta(request, applyFavoriteFilter(id))
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
        saveCurrentFilter(newMagicFilter())
        agGridSupport.resetGridState(category)
        return ResponseAction(targetType = TargetType.RELOAD)
            .addVariable("filter", newMagicFilter())
    }

    /**
     * The current filter will be reset and returned.
     */
    @GetMapping(RestPaths.FILTER_RESET)
    fun filterReset(): MagicFilter {
        val filter = getCurrentFilter()
        filter.reset()
        // The defaults of this page again ([newMagicFilter]): reset only clears, so a page that hides
        // something until it is asked for (task: closed tasks) would come back showing it. The instance
        // itself is kept, because it may be a named favorite.
        filter.entries.addAll(newMagicFilter().entries)
        return filter
    }

    // ------------------------------------------------------------------------------------------
    // The list itself
    // ------------------------------------------------------------------------------------------

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
     * The envelope of a [listPage] request: the filter plus the slice, kept out of [MagicFilter] itself.
     *
     * `offset`/`limit` must not live in [MagicFilter] — it is the persisted favorite and the argument of
     * [MagicFilter.isModified], so a page flip would mark the favorite modified (the `paginationPageSize`
     * as-entry mess is the cautionary tale). `refresh` forces a rebuild of the cached id list after the
     * client's own write.
     */
    class ListPageRequest(
        var filter: MagicFilter = MagicFilter(),
        var offset: Int = 0,
        var limit: Int = 50,
        var refresh: Boolean = false,
        /**
         * Do not remember this filter as the user's current one ([saveCurrentFilter]). For a transient jump
         * into a pre-filtered list — the consumption bar linking to a task's time sheets, Wicket's
         * `storeFilter=false` — so opening the list from the menu afterwards does not still show that filter.
         */
        var doNotStore: Boolean = false,
    )

    /**
     * One page of the list, served server-side: the ordered id list is materialized once per (session,
     * filter) and cached, and this returns the 50-row slice at `offset` (see [getListPage] and
     * `MIGRATION-list-paging.md`). A new path beside [getList], not an extension of it: `POST list` stays
     * byte-identical for the legacy React app, the multi-selection start and the Excel exports, all of which
     * need every row.
     *
     * Opt in per page with `serverPaging` on the next `PageDef`; the frontend calls this only then.
     */
    @PostMapping(RestPaths.LIST_PAGE)
    fun listPage(request: HttpServletRequest, @RequestBody body: ListPageRequest): ResultSet<*> {
        val filter = body.filter
        filter.autoWildcardSearch = true
        fixMagicFilterFromClient(filter)
        val list = getListPage(request, this, baseDao, listPageCache, filter, body.offset, body.limit, body.refresh)
        // A transient jump (doNotStore) must not leave its filter behind as the user's remembered one.
        if (!body.doNotStore) {
            saveCurrentFilter(filter)
        }
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
     * The computed/transient list columns of this page — the ones no SQL `ORDER BY` can express (sums,
     * person days, a `#count`, `kunde.displayName`, an invoice's translated status). Keyed by the sort
     * property the Next.js client sends (the column `id` declared in `*.page.tsx`), each mapped to the
     * value a *loaded* entity sorts by.
     *
     * Declaring a column here is all a page needs: the query drops these properties before it runs (see
     * `AbstractPagesRestUtils.buildQueryFilter`, where `addOrder` would otherwise swallow them and ship an
     * unordered query), and the generic [filterList]/[sortIds] sort by them afterwards. The empty default
     * means no computed column, so both generic paths are no-ops — every page without one is untouched.
     */
    open val computedSortProperties: Map<String, (O) -> Comparable<*>?> get() = emptyMap()

    /**
     * The stable last criterion [filterList] and [sortIds] append after the [computedSortProperties], so
     * equal computed values (0.00 is the most common of all sums, a customer has many orders) keep a
     * deterministic order between two requests over the same data — and the paged and non-paged orderings
     * agree at ties. Default: primary key descending, which reflection resolves on any entity. Consulted
     * only when a computed column is actually sorted on.
     */
    open val computedSortTieBreak: SortProperty get() = SortProperty.desc("id")

    /**
     * Opt-in cheap id path for [sortIds]: true if the page can resolve every computed sort value (and the
     * [computedSortTieBreak] value) straight from an id via a cache, without loading the entity — worth it
     * for a list of thousands (the order book reads [computedSortValueById] from `AuftragsCache`). Default
     * false: [sortIds] loads the matching entities and reuses [filterList], which the invoice lists do
     * because their customer/project `displayName` is in no cache.
     */
    open val hasComputedSortById: Boolean get() = false

    /**
     * The value an id sorts by for [property] on the cheap id path — consulted only when
     * [hasComputedSortById]. Must resolve both the [computedSortProperties] keys and the
     * [computedSortTieBreak] property. `null` sorts the id as blank (which ranks lowest), the same
     * fallback as an id not (yet) in the cache.
     */
    open fun computedSortValueById(id: Long, property: String): Comparable<*>? = null

    /**
     * Orders the materialized id list of a server-side paged list (see [getListPage]) by the
     * [computedSortProperties] a database `ORDER BY` cannot express, so a page is a slice of an already
     * ordered list. Sorts once per (session, filter), not once per page, and — sharing the same computed
     * selection, [computedSortTieBreak] and comparator as [filterList] — yields byte-for-byte the order the
     * non-paged `POST list` returns. Keeps the database order when no computed column is sorted on (the ids
     * then already came pre-ordered from the query).
     *
     * Two paths: the cheap one reads each id's value from a cache ([computedSortValueById], the order book);
     * the default one loads the matching entities and reuses [filterList] (the invoice lists). A page opts
     * into the cheap one via [hasComputedSortById].
     */
    open fun sortIds(ids: LongArray, filter: MagicFilter): LongArray {
        val computed = filter.sortProperties.filter { computedSortProperties.containsKey(it.property) }
        if (computed.isEmpty()) {
            return ids
        }
        return if (hasComputedSortById) {
            val sortProperties = computed + computedSortTieBreak
            SortPropertyComparator.sort(ids.toList(), sortProperties) { id, property ->
                computedSortValueById(id, property)
            }.toLongArray()
        } else {
            filterList(getListByIds(ids.toList()).toMutableList(), filter).mapNotNull { it.id }.toLongArray()
        }
    }

    /**
     * Aggregates of the whole result of a server-side paged list — the value assigned to
     * [ResultSet.statistics] (see [getListPage]). The default is none; a page overrides this to compute its
     * totals over the full id list rather than over the single page it returns (the order book's sums).
     *
     * Given the whole ordered id list. It aggregates from a cache where the data allows (the order book), or
     * by loading the matching entities where the totals need more than a cache holds (the invoice statistics,
     * which convert foreign currencies from the loaded `RechnungDO`). The paging counterpart of computing
     * statistics in [postProcessResultSet] over `resultSet.resultSet`.
     */
    open fun aggregate(ids: LongArray, filter: MagicFilter): Any? {
        return null
    }

    /**
     * The given entries as list rows, for a page that holds a selection as ids only.
     *
     * Lives here rather than at the caller because `O` is only in scope here: [AbstractMultiSelectedPage]
     * knows its list rest as `AbstractEntityRest<*, *, *>` and could not build the [ResultSet] of `O`
     * that [postProcessResultSet] takes. And going through that method is the point of this one - it is
     * where [AbstractDTOEntityRest] turns the database objects into the lean rows of a hand built next
     * page (see [createListRow]), so the answer has the same shape as the rows of the entity's list and
     * needs no columns of its own.
     *
     * Read only, and access checked where every list is: `BaseDao.select(idList)`.
     */
    fun getResultSetByIds(request: HttpServletRequest, entityIds: Collection<Serializable>?): ResultSet<*> {
        // Not the user's current filter: nothing here is filtered, and the result set only reads it to
        // tell whether it was truncated (see ResultSet). Passing the stored one would also mean this
        // read could change what the list page shows next.
        val filter = MagicFilter()
        val resultSet = ResultSet(getListByIds(entityIds), null, magicFilter = filter)
        return postProcessResultSet(resultSet, request, filter)
    }

    /**
     * Please note: filter.deleted is ignored (entries.field == "deleted" is used instead).
     */
    fun getResultList(filter: MagicFilter): List<O> {
        filter.autoWildcardSearch = true
        fixMagicFilterFromClient(filter)
        return getObjectList(this, baseDao, filter)
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
     * Whether the rows of this response should be the lean ones of [createListRow].
     *
     * Only a hand built projectforge-next page knows which columns it renders, so only for that page is
     * a partly filled DTO the complete answer. Every other client renders from `UILayout`, whose
     * columns bind to fields a lean row leaves empty.
     *
     * A page that offers no lean row is unaffected - [createListRow] falls back to the full DTO.
     */
    protected fun useListRow(request: HttpServletRequest): Boolean {
        return RestAuthenticationUtils.isNextClient(request) && NextMigration.isMigrated(category)
    }

    /**
     * The row of a list as a hand built projectforge-next page renders it: the same DTO, with only the
     * fields its columns show.
     *
     * Answered by the DTO rather than by the page — `BaseDTO.copyFrom4ListRow` is where an entity says what
     * a row of it consists of, so [AbstractDTOEntityRest] implements this once for every DTO page. Override
     * here only if building the row needs something the DTO cannot reach (as `AddressPagesRest` needs its
     * image cache).
     *
     * The default is the full DTO, so this costs a page that overrides neither nothing.
     */
    protected open fun createListRow(obj: O): DTO {
        return transformFromDB(obj, false)
    }

    /**
     * Sorts the loaded result set by the [computedSortProperties] a database `ORDER BY` cannot express (see
     * there), and leaves the order the query produced alone when none of them is sorted on: a stable sort by
     * nothing is not the same as no sort. The [computedSortTieBreak] is appended last for a deterministic
     * order at equal computed values. The paging counterpart is [sortIds], which returns the same order.
     */
    internal open fun filterList(resultSet: MutableList<O>, filter: MagicFilter): List<O> {
        val props = computedSortProperties
        val computed = filter.sortProperties.filter { props.containsKey(it.property) }
        if (computed.isEmpty()) {
            return resultSet
        }
        val sortProperties = computed + computedSortTieBreak
        return SortPropertyComparator.sort(resultSet, sortProperties) { obj, property -> props[property]?.invoke(obj) }
    }

    /**
     * Is this list page currently in multi selection mode?
     */
    fun isMultiSelectionMode(request: HttpServletRequest, magicFilter: MagicFilter): Boolean {
        return MultiSelectionSupport.isMultiSelection(request, magicFilter)
    }

    /**
     * Starts multi selection by registering current result list.
     */
    @PostMapping(RestPaths.REST_START_MULTI_SELECTION)
    fun startMultiSelections(request: HttpServletRequest, @RequestBody filter: MagicFilter): ResponseAction {
        val count = registerForSelection(request, filter)
        log.info("User wants to start multiselection of $count entries.")
        return ResponseAction(url = PagesResolver.getMultiSelectionPageUrl(this::class.java, absolute = true))
    }

    /**
     * Starts multi selection and answers how many entries the filter matched.
     *
     * The layout free counterpart of [startMultiSelections], whose answer is a redirect to the mass
     * update page of the legacy frontend - a hand built page routes itself and only needs to know that
     * the session now holds the ids (see `MultiSelectMetaData`).
     */
    @PostMapping("startSelection")
    fun startSelection(
        request: HttpServletRequest,
        @RequestBody filter: MagicFilter,
    ): MultiSelectNavigation {
        val count = registerForSelection(request, filter)
        log.info("User wants to start multiselection of $count entries.")
        return MultiSelectNavigation(
            url = PagesResolver.getMultiSelectionPageUrl(this::class.java, absolute = true),
            selectedCount = count,
        )
    }

    /** Registers every id the filter matches as selectable and returns how many those are. */
    private fun registerForSelection(request: HttpServletRequest, filter: MagicFilter): Int {
        val ids = getList(request, filter).resultSet.mapNotNull { getId(it) }
        MultiSelectionSupport.registerEntityIdsForSelection(request, this::class.java, ids)
        return ids.size
    }

    /**
     * This rest service will be called on multi selection list pages, if the user wants to cancel the multi selection.
     * @return redirect url
     */
    @GetMapping(RestPaths.CANCEL_MULTI_SELECTION)
    fun handleCancelUrl(request: HttpServletRequest): String {
        return MultiSelectionSupport.clear(request, this)
            ?: PagesResolver.getListPageUrl(this::class.java, absolute = true)
    }

    // ------------------------------------------------------------------------------------------
    // Column states of the grid
    // ------------------------------------------------------------------------------------------

    /**
     * Will be called when grid state changes (column order, width, visibility, sorting, filters).
     * @return "OK" string response
     */
    @PostMapping(RestPaths.SET_COLUMN_STATES)
    fun updateColumnStates(@Valid @RequestBody request: DataTableStateRequest): String {
        agGridSupport.storeGridState(category, request)
        return "OK"
    }

    /**
     * The user's stored grid state (column order, width, visibility, pinning, sorting), or an empty
     * state if there is none yet.
     *
     * Counterpart to [updateColumnStates] for pages that aren't built from a `UILayout`: those get
     * the state folded into their column definitions instead (see
     * AGGridSupport.restoreColumnsFromUserPref), which a hand-built page has no equivalent of.
     * The format is TanStack Table's own state, so the frontend can apply it as is.
     */
    @GetMapping(RestPaths.COLUMN_STATES)
    fun getColumnStates(): GridState {
        return agGridSupport.getGridState(category) ?: GridState()
    }

    // ------------------------------------------------------------------------------------------
    // Single items
    // ------------------------------------------------------------------------------------------

    /**
     * Gets the item from the database.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     */
    // Digits only: a non-numeric single segment (e.g. a removed legacy endpoint like the former
    // `/rs/order/edit`, or a stale bookmark) must miss this handler and fall through to a 404, not
    // reach it and 500 while Spring tries to parse the word as a Long.
    @GetMapping("{id:\\d+}")
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
        if (editMode && result is EntityAccessSupport) {
            // What the hand built next form may offer, asked here and not per rest class: the DAO calls
            // are the same ones checkUserAccess makes for the layout driven frontends, so a second set
            // in a transformFromDB override would only ask the DAO twice. Edit mode only - a list row
            // has no save button, and every row would cost these two calls.
            result.writeAccess = userAccess?.update ?: baseDao.hasLoggedInUserUpdateAccess(item, item, false)
            result.deleteAccess = userAccess?.delete ?: baseDao.hasLoggedInUserDeleteAccess(item, item, false)
        }
        jcrPath?.let {
            if (result is AttachmentsSupport) {
                result.attachments = attachmentsService.getAttachments(it, id, attachmentsAccessChecker)
            }
        }
        return result
    }

    /**
     * Whether this user may write entries of this entity at all - the question a *list* asks, as opposed
     * to the write access of a single entry.
     *
     * True by default, because for most entities there is no entity wide answer: whether an entry may be
     * written depends on the entry (its task, its status, its owner) and travels on its DTO
     * (EntityAccessSupport, filled in [getById]). A few DAOs do answer it for the entity as a whole
     * though - only an administrator may change a group, whoever may read one - and their pages override
     * this. Wicket asks the same question in the same place (`GroupListPage` renders the name as a plain
     * label instead of a link without it), and both list pages of this app use the flag for it: the
     * server laid out grid makes its rows clickable with it (`AGGridSupport`), a hand built list wires
     * its row click with it (see useEditTargets in projectforge-next).
     *
     * Not authorization, like the rest of [UILayout.UserAccess]: the DAO refuses the write regardless.
     */
    protected open fun listUpdateAccess(): Boolean = true

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
            // Whether this user may see the entity's entries at all - the one flag here that isn't a mere
            // UI hint (see UILayout.UserAccess.read).
            //
            // Caught, because this must not be the thing that breaks a layout or the list meta data: a DAO
            // with neither a userRightId nor an override throws UnsupportedOperationException here
            // (BaseDao.hasAccess). Such an entity has no readable list either way, so null - "not known" -
            // is the honest answer, and the client then falls back to what the read itself reports.
            userAccess.read = runCatching {
                if (obj != null) baseDao.hasLoggedInUserSelectAccess(obj, false)
                else baseDao.hasLoggedInUserSelectAccess(false)
            }.getOrNull()
        }
    }

    /**
     * The clone of an entity for a page built by hand: prepared by [prepareClone] and answered as it is,
     * without saving it.
     *
     * The layout free counterpart of `AbstractPagesRest.clone`, and deliberately a different path
     * ([RestPaths.CLONE_DATA]): that one is mapped by a subclass of this one, so the same path here would
     * be ambiguous for every legacy page. It also answers something else - a `ResponseAction` carrying
     * data *and* a rebuilt layout - which a hand built page has no use for. What travels here is the DTO,
     * and where it is edited is the client's business.
     *
     * The posted entity is **not** validated: it is the form as the user has it in front of them, errors
     * and all, and nothing is written (Wicket clones from an invalid form too, see
     * `RechnungEditForm.ignoreErrorOnClone`). [CloneSupport.AUTOSAVE] is not honoured either - no
     * layout free entity asks for it, so anything but [CloneSupport.NONE] means "prepare and return".
     *
     * @return The prepared clone, or HTTP 501 if this entity has no clone support.
     */
    @PostMapping(RestPaths.CLONE_DATA)
    fun cloneData(@RequestBody postData: PostData<DTO>): ResponseEntity<Any> {
        if (cloneSupport == CloneSupport.NONE) {
            return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)
        }
        // Throws an AccessException, which the exception handler answers as HTTP 406 with a validation
        // error - the client shouldn't have offered the button (see ListMetaData.userAccess.insert).
        baseDao.hasLoggedInUserInsertAccess()
        return ResponseEntity(prepareClone(postData.data) as Any, HttpStatus.OK)
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

    // ------------------------------------------------------------------------------------------
    // Autocompletion
    // ------------------------------------------------------------------------------------------

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
        // Every word required, and each of them matched either as typed or as the terms the index holds for it:
        // 'dhl-pop' is two terms in the index, and 'dhl-pop*' - a wildcard term, which Lucene doesn't tokenize -
        // used to match nothing at all (see [SearchStringTokenizer]). A word the caller already marked as
        // required carries a syntax of its own and stays untouched, as before.
        val modifiedSearchString = searchString
            ?.split(' ', '\t', '\n')
            ?.filter { it.isNotBlank() }
            ?.joinToString(" ") {
                if (it.startsWith("+")) it else SearchStringTokenizer.expandWord(it, required = true)
            }
            ?.takeIf { it.isNotBlank() }
        filter.searchString = modifiedSearchString
        filter.searchFields = autoCompleteSearchFields!!
        maxResults?.let { filter.maxRows = it }
        val list = queryAutocompleteObjects(request, filter)
        return list.map { DisplayObject(it.id, if (it is DisplayNameCapable) it.displayName else it.toString()) }
    }

    /**
     * Will create a new BaseSearchFilter. If you want to use a DO specific filter, override this method.
     */
    open fun createAutoCompleteObjectsFilter(request: HttpServletRequest): BaseSearchFilter {
        return BaseSearchFilter()
    }

    protected open fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<O> {
        return baseDao.select(filter)
    }

    // ------------------------------------------------------------------------------------------
    // History
    // ------------------------------------------------------------------------------------------

    /**
     * The history items of the given entity, along with the capabilities a client needs to render them.
     *
     * [supportsUserComments] is also part of the edit layout (`UILayout.UserAccess.editHistoryComments`),
     * but the hand built pages of projectforge-next have no layout to read it from, so the history
     * answers it as well.
     */
    class HistoryInfo(
        val entries: List<DisplayHistoryEntry>,
        /** [BaseDao.supportsHistoryUserComments]: only then may the client append comments. */
        val supportsUserComments: Boolean,
    )

    /**
     * Gets the history items of the given entity.
     * @param id Id of the item to get the history entries for.
     */
    @GetMapping("history/{id}")
    fun getHistory(@PathVariable("id") id: Long?): ResponseEntity<HistoryInfo> {
        if (id == null) {
            return ResponseEntity(HttpStatus.BAD_REQUEST)
        }
        val item = baseDao.find(id) ?: return ResponseEntity(HttpStatus.BAD_REQUEST)
        val entries = historyFormatService.selectAsDisplayEntries(baseDao, item)
        return ResponseEntity(
            HistoryInfo(entries, supportsUserComments = baseDao.supportsHistoryUserComments),
            HttpStatus.OK,
        )
    }

    // ------------------------------------------------------------------------------------------
    // Re-indexing
    // ------------------------------------------------------------------------------------------

    /**
     * Rebuilds the index by the search engine for the newest entries.
     * @see [BaseDao.rebuildDatabaseIndex4NewestEntries]
     */
    @GetMapping("reindexNewest")
    fun reindexNewest(request: HttpServletRequest): ResponseAction {
        startReindexOrRunIt(request, full = false)?.let { return it }
        baseDao.rebuildDatabaseIndex4NewestEntries()
        return UIToast.createToast(translate("administration.reindexNewest.successful"), color = UIColor.SUCCESS)
    }

    /**
     * Rebuilds the index by the search engine for all entries. Admins only: the run discards the index of the entity
     * and builds it from scratch, which costs minutes and system performance on a large table. The classic frontend
     * simply hides the menu entry, but projectforge-next builds its list pages itself, so the check has to be here.
     * @see [BaseDao.rebuildDatabaseIndex]
     */
    @GetMapping("reindexFull")
    fun reindexFull(request: HttpServletRequest): ResponseAction {
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()
        startReindexOrRunIt(request, full = true)?.let { return it }
        baseDao.rebuildDatabaseIndex()
        return UIToast.createToast(translate("administration.reindexFull.successful"), color = UIColor.SUCCESS)
    }

    /**
     * projectforge-next shows a progress toast, so it gets the id of a job to poll (see JobsMonitorPageRest).
     * The classic clients have no such display and keep waiting for the toast of the finished run.
     *
     * @return The response for projectforge-next, or null if the caller has to do the run synchronously.
     */
    private fun startReindexOrRunIt(request: HttpServletRequest, full: Boolean): ResponseAction? {
        if (!RestAuthenticationUtils.isNextClient(request)) {
            return null
        }
        val i18nKey = if (full) "administration.reindexFull.job.title" else "administration.reindexNewest.job.title"
        val job = jobHandler.addJob(
            ReindexJob(
                databaseDao = databaseDao,
                classes = if (full) baseDao.reindexClasses else baseDao.reindexClasses4NewestEntries,
                // The entity names restrict the change history of the partial run to the rows of this page (the
                // entity and the children whose history it shows).
                settings = DatabaseDao.createReindexSettings(!full, baseDao.historyEntityNames),
                adminRequired = full,
                // The same key the list layout uses as its title — every entity has it.
                title = translateMsg(i18nKey, translate("$i18nKeyPrefix.list")),
            )
        )
        return ResponseAction(targetType = TargetType.NOTHING).addVariable("jobId", job.id)
    }

    // ------------------------------------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------------------------------------

    open fun validate(validationErrors: MutableList<ValidationError>, dto: DTO) {
    }

    fun validate(dbObj: O): MutableList<ValidationError> {
        return ValidationUtils.validateFields(dbObj)
    }

    fun validate(dbObj: O, postData: PostData<DTO>): List<ValidationError>? {
        val validationErrors = validate(dbObj)
        val dto = postData.data
        validate(validationErrors, dto)
        if (validationErrors.isEmpty()) return null
        return validationErrors
    }

    // ------------------------------------------------------------------------------------------
    // Writes
    // ------------------------------------------------------------------------------------------

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
    internal open fun onAfterSave(request: HttpServletRequest, obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(request, obj, postData, RestButtonEvent.SAVE)
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterUpdate(request: HttpServletRequest, obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(request, obj, postData, RestButtonEvent.UPDATE)
    }

    /**
     * Called before delete (not markAsDeleted!).
     */
    internal open fun onBeforeDelete(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterDelete(request: HttpServletRequest, obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(request, obj, postData, RestButtonEvent.DELETE)
    }

    /**
     * Called before markAsDeleted.
     */
    internal open fun onBeforeMarkAsDeleted(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterMarkAsDeleted(
        request: HttpServletRequest,
        obj: O,
        postData: PostData<DTO>
    ): ResponseAction {
        return onAfterEdit(request, obj, postData, RestButtonEvent.DELETE)
    }

    /**
     * Called before undelete.
     */
    internal open fun onBeforeUndelete(request: HttpServletRequest, obj: O, postData: PostData<DTO>) {
    }

    /**
     * Will only be called on success. Simply call [onAfterEdit].
     */
    internal open fun onAfterUndelete(request: HttpServletRequest, obj: O, postData: PostData<DTO>): ResponseAction {
        return onAfterEdit(request, obj, postData, RestButtonEvent.UN_DELETE)
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
        return onAfterEdit(request, obj, postData, RestButtonEvent.CANCEL)
    }

    /**
     * Will be called after create, update, delete, markAsDeleted, undelete and cancel.
     * @return ResponseAction with the url of the standard list page.
     */
    internal open fun onAfterEdit(
        request: HttpServletRequest,
        obj: O,
        postData: PostData<DTO>,
        event: RestButtonEvent
    ): ResponseAction {
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
            ?: getListPageUrlAfterEdit(request)
        return ResponseAction(returnToCaller)
            .addVariable("id", obj.id ?: -1)
    }

    /**
     * The list page to return to after an edit. Overridden by [AbstractPagesRest], whose page exists in
     * the legacy React app as well and has to return the caller to the frontend they came from.
     */
    protected open fun getListPageUrlAfterEdit(request: HttpServletRequest): String {
        return PagesResolver.getListPageUrl(
            this::class.java,
            absolute = true,
            // Force new hash for getting initialList (including ui on actions/list/index.js
            forceAGGridReload = true,
        )
    }

    /**
     * Overwrite this method to replace returnToCaller by URL. At default the given returnToCaller will be used
     * unmodified as URL.
     */
    protected open fun createReturnToCallerResponseAction(returnToCaller: String): ResponseAction {
        return ResponseAction(returnToCaller)
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

    // ------------------------------------------------------------------------------------------
    // Attachments
    // ------------------------------------------------------------------------------------------

    /**
     * A unique id which is used as parent node for all attachments. Use [enableJcr] for creating unique nodes.
     * @return unique jcr path if attachments are supported or null, if no attachment support is given (download, upload and list).
     * @see [org.projectforge.rest.orga.ContractPagesRest] as an example.
     */
    open var jcrPath: String? = null  // open needed by Wicket's SpringBean for proxying.
        protected set

    /**
     * Might be initialized by [enableJcr] with default dao access checker.
     */
    open lateinit var attachmentsAccessChecker: AttachmentsAccessChecker // open needed by Wicket's SpringBean for proxying.
        protected set

    protected fun getMaxFileSizeKB(): Int {
        return this.attachmentsAccessChecker.fileSizeChecker.maxFileSizeKB
    }

    /**
     * Call this method for enabling jcr support.
     * jcr part will be set to '$prefix.${baseDao.identifier}', must be unique.
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

    // ------------------------------------------------------------------------------------------
    // DO <-> DTO
    // ------------------------------------------------------------------------------------------

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
