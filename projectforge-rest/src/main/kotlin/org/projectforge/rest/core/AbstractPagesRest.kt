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

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import mu.KotlinLogging
import org.projectforge.NextMigration
import org.projectforge.favorites.Favorites
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.dto.*
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.web.rest.RestAuthenticationUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger {}

/**
 * The `UILayout` half of an entity rest service: it serves the list page and the edit form of an entity
 * as data, so that a generic renderer can build the page from it. That renderer is the legacy React app
 * (`projectforge-webapp`), and projectforge-next brings its own for the entities not yet migrated
 * (`components/dynamic/`).
 *
 * Everything that isn't page layout lives in [AbstractEntityRest]. A page that is hand built in
 * projectforge-next extends that class directly and stops producing a layout no client renders.
 *
 * @see AbstractEntityRest
 */
@Deprecated(
    "Serves the UILayout of the generic list/edit pages. A page hand built in projectforge-next " +
            "extends AbstractEntityRest instead, which serves data only."
)
abstract class AbstractPagesRest<
        O : ExtendedBaseDO<Long>,
        DTO : Any, // DTO may be equals to O if no special data transfer objects are used.
        B : BaseDao<O>>
@JvmOverloads
constructor(
    baseDaoClazz: Class<B>,
    i18nKeyPrefix: String,
    cloneSupport: CloneSupport = CloneSupport.NONE,
) : AbstractEntityRest<O, DTO, B>(baseDaoClazz, i18nKeyPrefix, cloneSupport) {

    companion object {
        const val GEAR_MENU = "GEAR"
        const val CLASSIC_VERSION_MENU = "CLASSIC"
        const val CREATE_MENU = "CREATE"
    }

    /**
     * If true, edit pages will open in modal dialogs instead of full page navigation.
     * This affects both the "Add New" button and row clicks in the grid.
     */
    open val useModalEditDialog = false

    /**
     * If given, a link to this url is shown on the list page. This is used for accessing the classical Wicket-version
     * of the current page during migration phase.
     */
    open val classicsLinkListUrl: String? = null

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
     * Contains the data, layout and filter settings served by [getInitialList].
     */
    class InitialListData(
        val ui: UILayout?,
        val standardEditPage: String,
        /**
         * The legacy edit page with [NextMigration.ID_PLACEHOLDER] for the id, e.g.
         * `react/book/edit/:id` or `wa/cost1Edit?id=:id`.
         *
         * Needed by the hand built pages of projectforge-next: their edit page doesn't call
         * `{entity}/edit`, so it can't read [UILayout.legacyUrl] from there and takes the template
         * from the list response instead.
         */
        val legacyEditPage: String?,
        /**
         * The legacy page for adding an entry, e.g. `react/book/edit` or `wa/cost1Edit`. Served next
         * to [legacyEditPage], because it isn't derivable from it: the Wicket edit page carries the
         * id as a query parameter, so dropping the placeholder is a per-app rule, not a suffix cut.
         */
        val legacyNewEntryPage: String?,
        val data: ResultSet<*>,
        val filterFavorites: List<Favorites.FavoriteIdTitle>,
        val filter: MagicFilter,
        /**
         * Quickselect url for searching entries while typing search string. If given, the user may click on
         * the autocompletion results for direct editing of the object.
         */
        val quickSelectUrl: String? = null,
        /**
         * If true, quick select and edit pages will open in modal dialogs instead of full page navigation.
         */
        val useModalEditDialog: Boolean = false,
        var variables: Map<String, Any>? = null
    )

    // ------------------------------------------------------------------------------------------
    // The list page
    // ------------------------------------------------------------------------------------------

    internal fun createListLayout(request: HttpServletRequest, magicFilter: MagicFilter): UILayout {
        val userAccess = UILayout.UserAccess()
        checkUserAccess(null, userAccess)
        // Assume that the user has general update access (override listUpdateAccess, see GroupPagesRest)
        userAccess.update = listUpdateAccess()
        val layout = UILayout("$i18nKeyPrefix.list")
        if (!isMultiSelectionMode(request, magicFilter)) {
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
        }
        layout.addTranslations(
            "reset", "datatable.no-records-found", "date.begin", "date.end", "exportAsXls",
            "search.lastMinute", "search.lastHour", "calendar.today", "search.sinceYesterday",
            "multiselection.button",
            "columns", "columns.manage", "columns.pin", "columns.reset", "columns.unpin", "columns.dragToSort",
            "filter.apply", "filter.reset", "filter.close", "filter.search", "filter.selectAll", "filter.selection",
            "filter.equals", "filter.notEqual", "filter.greaterThan", "filter.lessThan",
            "filter.between", "filter.blank", "filter.notBlank", "filter.before", "filter.after",
            "filter.contains", "filter.notContains", "filter.startsWith", "filter.endsWith",
            "filter.value", "filter.valueTo",
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

    abstract fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    )

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
     * The placeholder [InitialListData.data] carries for a hand built projectforge-next page, which reads
     * the rows from `list` instead. Not nullable, because the field isn't: making it so would touch every
     * legacy caller for a value only one client ever reads.
     */
    private fun emptyResultSet(filter: MagicFilter): ResultSet<DTO> {
        return ResultSet(resultSet = emptyList(), origResultSet = null, totalSize = 0, magicFilter = filter)
    }

    /**
     * Whether this response may leave the rows out, i.e. whether the client asking fetches them itself.
     *
     * Being a next client is not enough: projectforge-next serves the pages of the not yet migrated
     * entities from the very same `UILayout` the React app gets, and that renderer
     * (`components/dynamic/components/grid/`) has nothing but this response to render from - it never
     * calls `list`. Only a page listed in [NextMigration] is hand built and does.
     */
    private fun skipResultSet(request: HttpServletRequest): Boolean {
        return RestAuthenticationUtils.isNextClient(request) && NextMigration.isMigrated(category)
    }

    protected fun getInitialList(request: HttpServletRequest, filter: MagicFilter): InitialListData {
        val favorites = getFilterFavorites()
        // The React app renders the rows out of this response, a hand built next page fetches them from
        // `list` right afterwards and never looks at [InitialListData.data] - so for such a page the
        // whole result set would be queried, mapped and serialized only to be dropped. That is the
        // entire list twice per page load: on the order book, two full table scans over some 7000 rows.
        val resultSet = if (skipResultSet(request)) {
            emptyResultSet(filter)
        } else {
            val list = getList(request, this, baseDao, filter)
            postProcessResultSet(list, request, filter).also {
                it.highlightRowId = userPrefService.getEntry(category, USER_PREF_PARAM_HIGHLIGHT_ROW, Long::class.java)
            }
        }
        val ui = createListLayout(request, filter)
            .addTranslations(
                "table.showing",
                "searchFilter",
                "nothingFound"
            )
        // The way back to the legacy list page, shown by projectforge-next next to the page title. The
        // marker keeps OrphanedLinkFilter from bouncing this escape hatch straight back to next.
        ui.legacyUrl = NextMigration.withEscapeHatchMarker(NextMigration.legacyListUrl(category))
        if (isMultiSelectionMode(request, filter)) {
            // Don't show search filter in multi selection mode (it isn't supported). The user
            // should use the AG-Grid filter instead.
            ui.hideSearchFilter = true
        } else {
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
                ui.add(
                    MenuItem(
                        CREATE_MENU,
                        title = translate("add"),
                        url = getAddNewEntryUrl(request),
                        type = if (useModalEditDialog) MenuItemTargetType.MODAL else null
                    )
                )
            }
        }
        return InitialListData(
            ui = ui,
            standardEditPage = getEditPage(request),
            // Escape hatches, marked so OrphanedLinkFilter lets them reach the legacy page (see above).
            legacyEditPage = NextMigration.withEscapeHatchMarker(NextMigration.legacyEditPage(category)),
            legacyNewEntryPage = NextMigration.withEscapeHatchMarker(NextMigration.legacyNewEntryUrl(category)),
            quickSelectUrl = quickSelectUrl,
            useModalEditDialog = useModalEditDialog,
            data = resultSet,
            filter = filter,
            filterFavorites = favorites.idTitleList
        )
    }

    /**
     * Answers with the whole list page state, because the generic list page of the React app rebuilds
     * itself from it (including the layout, whose grid columns carry the sort order of the filter).
     */
    override fun selectFavoriteFilter(
        request: HttpServletRequest,
        @RequestParam("id", required = true) id: Long
    ): InitialListData {
        return getInitialList(request, applyFavoriteFilter(id))
    }

    /**
     * Resets the AG Grid column states (position, width, pinning) from the server.
     * This will clear all user preferences for the grid and update the UI with default column definitions.
     */
    @GetMapping("resetGridState")
    fun resetGridState(request: HttpServletRequest): ResponseAction {
        agGridSupport.resetGridState(category)
        val initialList = getInitialList(request, getCurrentFilter())

        // Extract AG Grid element and its column definitions using AGGridSupport helper
        val agGridElement = agGridSupport.findAgGridElement(initialList.ui)

        // Create ResponseAction using AGGridSupport helper
        return agGridSupport.createResetGridStateResponse(agGridElement)
    }

    // ------------------------------------------------------------------------------------------
    // The edit page
    // ------------------------------------------------------------------------------------------

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

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * a group with a separate label and input field will be generated.
     * layout will be also included if the id is not given.
     * @param returnToCaller This optional parameter defines the caller page of this service to put in server data. After processing this page
     * the user will be redirected to this given returnToCaller.
     */
    @GetMapping(RestPaths.EDIT)
    open fun getItemAndLayout(
        request: HttpServletRequest,
        @RequestParam("id") id: String?,
        @RequestParam("returnToCaller") returnToCaller: String?
    )
            : ResponseEntity<FormLayoutData> {
        val userAccess = UILayout.UserAccess()
        // The frontend may send "undefined" as id for new items:
        val effectiveId = if (id == "undefined") null else id
        val item = (if (null != effectiveId) {
            getById(id, true, userAccess)
        } else {
            checkUserAccess(null, userAccess)
            newBaseDTO(request)
        })
            ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        userAccess.editHistoryComments = baseDao.supportsHistoryUserComments
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
        ui.addTranslations("changes", "history.userComment.edit", "tooltip.selectMe")
        // The way back to the legacy edit page of this very entry, shown by projectforge-next next to
        // the page title. A new entry has no id yet, so it leads to the legacy add page instead. The
        // marker keeps OrphanedLinkFilter from bouncing this escape hatch straight back to next.
        ui.legacyUrl = NextMigration.withEscapeHatchMarker(getId(dto).let { id ->
            if (id != null) {
                NextMigration.legacyEditPage(category)?.replace(NextMigration.ID_PLACEHOLDER, "$id")
            } else {
                NextMigration.legacyNewEntryUrl(category)
            }
        })
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
     * e.g. parsing the request and preset the item values.
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

    // ------------------------------------------------------------------------------------------
    // The urls of the page, per calling frontend
    // ------------------------------------------------------------------------------------------

    /**
     * The edit page for the frontend that asked for this list: [getStandardEditPage] for
     * projectforge-next, the generic React page for the legacy React app.
     *
     * The layout of a migrated page is still served to the React app - it is reachable under
     * `react/<category>` through a bookmark or the browser history, and for a page migrated from
     * Wicket it may never have had a menu entry at all. A user who is looking at that list must not
     * be thrown into projectforge-next by a click on a row; they are in the React app.
     *
     * Deliberately not [NextMigration.legacyEditPage]: that names the page the way back leads to,
     * which may be a Wicket page (`cost1`). Wicket renders server side and never asks here for a
     * layout, so the only non-next caller is the React app.
     */
    open fun getEditPage(request: HttpServletRequest): String {
        return if (servedByReactApp(request)) {
            NextMigration.reactEditPage(category)
        } else {
            getStandardEditPage()
        }
    }

    /**
     * The url of the "add" menu item for the frontend that asked, see [getEditPage] for why it
     * depends on the caller.
     */
    protected open fun getAddNewEntryUrl(request: HttpServletRequest): String {
        return if (servedByReactApp(request)) {
            NextMigration.reactNewEntryUrl(category)
        } else {
            addNewEntryUrl
        }
    }

    /**
     * The list page to return to after an edit, for the frontend that asked: a user who saves in the
     * React edit form of a migrated page returns to the React list, not to projectforge-next.
     *
     * @see getEditPage
     */
    override fun getListPageUrlAfterEdit(request: HttpServletRequest): String {
        if (servedByReactApp(request)) {
            // The hash is the same workaround as below: a new url makes the React app fetch initialList
            // again, which restores the AG Grid state.
            return "/${NextMigration.reactListUrl(category)}?hash=${NumberHelper.getSecureRandomAlphanumeric(4)}"
        }
        return super.getListPageUrlAfterEdit(request)
    }

    /**
     * @return true, if this page is migrated to projectforge-next, but the request came from the
     * legacy React app. Nothing is gated on this - it only picks which of the two urls of the same
     * page is handed out (see [RestAuthenticationUtils.isNextClient]).
     */
    private fun servedByReactApp(request: HttpServletRequest): Boolean {
        return NextMigration.isMigrated(category) && !RestAuthenticationUtils.isNextClient(request)
    }
}
