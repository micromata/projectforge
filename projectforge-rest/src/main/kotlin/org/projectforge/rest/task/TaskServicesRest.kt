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

package org.projectforge.rest.task

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.NextMigration
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.KostHelper
import org.projectforge.business.task.*
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.i18n.Priority
import org.projectforge.common.task.TaskStatus
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.core.ListFilterService
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.core.aggrid.SortModelEntry
import org.projectforge.rest.dto.datatable.DataTableStateRequest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal

/**
 * For serving the task tree as tree or table..
 */
@RestController
@RequestMapping("${Rest.URL}/task")
class TaskServicesRest {
    class Kost2(val id: Long, val title: String)

    /**
     * The project a task's cost units come from, resolved by walking up the tree
     * ([TaskTree.getProjekt]) — so a task without a project of its own reports its ancestor's, which is
     * the one its kost2 list is built from.
     */
    class Projekt(
        val id: Long?,
        val name: String?,
        /**
         * The cost number of the project without the two Kost2Art digits, e. g. `5.123.45`
         * ([ProjektDO.kost]). What the Wicket form shows as `<kost>.*` next to the black/white list.
         */
        val kost: String?,
    )

    /**
     * The state of the kost2 block of the task form, for a list that is not saved yet: everything derived
     * from `kost2BlackWhiteList` + `kost2IsBlackList` the client cannot derive itself.
     *
     * Not reimplemented in TypeScript on purpose. [TaskTree.getKost2List] matches the entries of the list
     * as suffixes against the active cost units of the project (`KostCache`), and [TaskHelper.addKost2]
     * abbreviates a picked unit to its two Kost2Art digits — except for a task that has no id but a parent,
     * where it appends the full number. A copy of that would have to duplicate the number format, the
     * Kost2Art ids and the project resolution.
     */
    class Kost2Preview(
        /** The black/white list, normalized and sorted ([TaskHelper.normalizeKost2BlackWhiteList]). */
        val kost2BlackWhiteList: String?,
        /** [Projekt.kost] of the resolved project, or null if the task has none. */
        val projektKost: String?,
        /** The resulting cost units in wild card form, e. g. `5.123.45.*`. */
        val kost2WildCard: String?,
        /** The resulting cost units, one formatted number per line — the Wicket tooltip's content. */
        val kost2ListAsLines: String?,
    )

    /**
     * What the client asks a [Kost2Preview] for: the black/white list as the user has it in the form,
     * plus the task it belongs to.
     */
    class Kost2PreviewRequest(
        /** Id of the task being edited, null while it is being added. */
        var id: Long? = null,
        /**
         * Id of the parent task. The only way to resolve the project of a task that has no id yet, and
         * what `TaskHelper.addKost2` and `TaskDao.hasAccessForKost2AndTimesheetBookingStatus` fall back
         * on (Wicket passes the parent for the same reason, see `TaskEditForm.onBeforeRender`).
         */
        var parentTaskId: Long? = null,
        var kost2BlackWhiteList: String? = null,
        var kost2IsBlackList: Boolean = false,
        /**
         * A cost unit just picked from the list, to be appended before the preview is computed — one round
         * trip instead of two, because after a pick the client needs the new preview anyway.
         */
        var addKost2Id: Long? = null,
    )

    /**
     * One order having at least one position assigned to a task, as the tree's `Aufträge` column shows
     * it: the number as a link to the order, the rest as its tooltip.
     */
    class Order(
        /** The order number, e. g. `7242` — the link's label. */
        val number: String,
        /** Title of the order, its person days on this task and its status — the tooltip's first line. */
        val title: String,
        /** One line per position of this order on the task — the tooltip's body. */
        val text: String,
        /** Url of the order's page, ready to use (see [NextMigration.standardEditPage]). */
        val url: String,
    )

    enum class TreeStatus { LEAF, OPENED, CLOSED }
    class Task(
        val id: Long,
        /**
         * Indent is only given for table view.
         */
        var indent: Int? = null,
        /**
         * All (opened) sub notes for table view or direct child notes for tree view
         */
        var children: MutableList<Task>? = null,
        var treeStatus: TreeStatus? = null,
        val title: String? = null,
        val shortDescription: String? = null,
        val protectTimesheetsUntil: PFDay? = null,
        val reference: String? = null,
        val priority: Priority? = null,
        val status: TaskStatus? = null,
        val responsibleUser: PFUserDO? = null,
        /**
         * References used in time-sheets for this task, or any ancestor or descendant task.
         */
        var timesheetReferenceList: List<String>? = null,
        var kost2List: List<Kost2>? = null,
        /**
         * Kost2List as formatted numbers (separated in each line) for displaying in tooltip.
         */
        var kost2ListAsLines: String? = null,
        /**
         * Wild card form of kost2List, e. g. 5.123.456.*
         */
        var kost2WildCard: String? = null,
        /**
         * The project the cost units come from, resolved through the ancestors. Only filled by
         * [createTask] (`info/{id}`), not per tree node: the edit form needs it to show `<kost>.*` and to
         * prefilter the cost unit picker, the tree shows the units themselves.
         */
        var projekt: Projekt? = null,
        /**
         * Whether cost units are configured at all ([Configuration.isCostConfigured]) — the gate for
         * showing the kost2 block of the form in the first place, as in `TaskEditForm.init`. Not
         * derivable from an empty `projekt`: a task simply without a project looks the same.
         */
        var costConfigured: Boolean? = null,
        var path: List<Task>? = null,
        var consumption: Consumption? = null,
        var orderList: MutableList<Order>? = null,
        /**
         * True for the tree's root node, which is only ever sent to admins and financial staff and only
         * for display (see `showRootForAdmins`). The client cannot derive this: `parentTask` is not part
         * of the tree's answer, and the root's id is 1 only by convention.
         *
         * It matters because the root may not be *selected* for anything but a task itself — booking a
         * timesheet or an order position against it is meaningless.
         */
        var root: Boolean? = null,
        /**
         * Whether the task is marked as deleted. On the wire because the tree's filter may include
         * deleted tasks ([TaskFilter.deleted]) and this class is no `BaseDTO`, which would carry the
         * flag itself. The client marks such a row (`row-deleted`).
         */
        val deleted: Boolean = false,
    ) {
        /**
         * The status as the user reads it. `deleted` wins over the status itself: a deleted task is
         * gone, and its last status says nothing worth reading (Wicket shows the status struck
         * through instead, which reads as if the task were still closed).
         */
        val statusAsString: String? =
            if (deleted) translate("deleted") else status?.i18nKey?.let { translate(it) }

        val priorityAsString: String? = priority?.i18nKey?.let { translate(it) }

        constructor(node: TaskNode) : this(
            id = node.task.id!!,
            title = node.task.title,
            shortDescription = node.task.shortDescription,
            protectTimesheetsUntil = PFDay.fromOrNull(node.task.protectTimesheetsUntil),
            reference = node.task.reference,
            priority = node.task.priority,
            status = node.task.status,
            responsibleUser = node.task.responsibleUser,
            deleted = node.task.deleted,
        )

        /**
         * Only for creating a pseudo empty task.
         */
        constructor(title: String) : this(
            id = -1, title = title
        )
    }

    class Result(
        val nodes: MutableList<Task> = mutableListOf(),
        var initFilter: TaskFilter? = null,
        var translations: MutableMap<String, String>? = null,
        var sortModel: List<SortModelEntry>? = null,
        var filterModel: Map<String, Any>? = null,
        var onColumnStatesChangedUrl: String? = null,
        var resetGridStateUrl: String? = null
    ) {
        var columnDefs: MutableList<UIAgGridColumnDef> = mutableListOf()
    }

    companion object {
        private const val PREF_ARA = "task"

        /**
         * The two modes get their own stored column state, because they get their own columns: the page
         * shows every column the data justifies, the select popover only the narrow ones it has room
         * for. Sharing the category would let hiding a column in the popover change the page's layout.
         */
        private const val GRID_CATEGORY = "taskTree"
        private const val GRID_CATEGORY_SELECT = "taskTreeSelect"

        private fun gridCategory(select: Boolean) = if (select) GRID_CATEGORY_SELECT else GRID_CATEGORY

        /**
         * Suffix of the select popover's stored search filter, so it filters independently of the page:
         * a search typed while picking a task is about that one pick, not about the tree page the user
         * left open behind it (and vice versa).
         */
        private const val FILTER_SUFFIX_SELECT = "select"

        private fun filterKeySuffix(select: Boolean) = if (select) FILTER_SUFFIX_SELECT else null

        /** REST category of the order book (`OrderEntityRest`), whose page an order link leads to. */
        private const val ORDER_CATEGORY = "order"

        /**
         * The fields the type-ahead searches, as `TaskSelectAutoCompleteFormComponent.SEARCH_FIELDS`:
         * the title and `taskpath`, the class bridge holding the titles of all ancestors
         * ([org.projectforge.business.task.HibernateSearchTaskPathBridge]).
         */
        private val AUTOCOMPLETE_SEARCH_FIELDS = arrayOf("title", "taskpath")

        /**
         * The path of a task as one line, `Structure | Customer | Development` — the label of a type-ahead
         * hit, built as Wicket's `createPath` builds it. [TaskTree.getPathToRoot] ends at the task itself
         * and leaves the root out, so the root task alone has an empty path and is named instead.
         */
        private fun formatPath(taskId: Long?): String {
            val path = TaskTree.instance.getPathToRoot(taskId)
            if (path.isEmpty()) {
                return translate("task.path.rootTask")
            }
            return path.joinToString(" | ") { it.task.title ?: "" }
        }

        fun createTask(id: Long?): Task? {
            if (id == null)
                return null
            val taskTree = TaskTree.instance
            val taskNode = taskTree.getTaskNodeById(id) ?: return null
            val task = Task(taskNode)
            addKost2List(task)
            addTimesheetReferenceList(task)
            task.costConfigured = Configuration.instance.isCostConfigured
            task.projekt = taskTree.getProjekt(id)?.let { Projekt(it.id, it.name, it.kost) }
            task.consumption = Consumption.create(taskNode)
            val pathToRoot = taskTree.getPathToRoot(taskNode.parentId)
            val pathArray = mutableListOf<Task>()
            pathToRoot.forEach {
                val ancestor = Task(id = it.task.id!!, title = it.task.title)
                pathArray.add(ancestor)
            }
            task.path = pathArray
            return task
        }

        /**
         * @param recursive Whether inherited cost units count. `false` for the tree/list display, where a
         *   task shows its cost units only where they are *specifically* defined — through its own project
         *   assignment or its own black/white list — never the ones it merely inherits from an ancestor
         *   (matching Wicket's list column, `TaskListPage: getKost2List(id, false)`). `true` (the default)
         *   for `info/{id}`, whose `kost2List` is the *bookable* list a timesheet on this task may charge,
         *   which must include the inherited cost units (see the webapp's TimesheetEditTaskAndKost2).
         */
        fun addKost2List(task: Task, includeKost2ObjectList: Boolean = true, recursive: Boolean = true) {
            val kost2DOList = TaskTree.instance.getKost2List(task.id, recursive)
            if (!kost2DOList.isNullOrEmpty()) {
                if (includeKost2ObjectList) {  // Only if needed in tree, save bandwidth...
                    val kost2List: List<Kost2> = kost2DOList.map {
                        Kost2(
                            it.id!!,
                            KostFormatter.instance.formatKost2(it, formatType = KostFormatter.FormatType.TEXT, 80),
                        )
                    }
                    task.kost2List = kost2List
                }
                task.kost2WildCard = KostHelper.getWildCardString(kost2DOList, "*")
                task.kost2ListAsLines = KostHelper.getFormattedNumberLines(kost2DOList)
            }
        }

        /**
         * The orders having a position assigned to this task, one entry per order, each with the tooltip
         * the Wicket column shows (see `OrderPositionsPanel`).
         *
         * Only the task's own positions, as that column does (`getOrderPositionEntries`, not
         * `getOrderPositionsUpwards`): an ancestor's order is not this task's. The tree caches the
         * positions by task id, so this costs no query per node.
         */
        fun addOrderList(task: Task) {
            val positions = TaskTree.instance.getOrderPositionEntries(task.id)
            if (positions.isNullOrEmpty()) {
                return
            }
            val personDaysUnit = translate("projectmanagement.personDays.short")
            // Grouped by order, because a task may have several positions of the same one and the cell
            // names the order once. Sorted by number, the cached set has no order of its own.
            val orderList = positions.groupBy { it.auftragNummer }
                .toSortedMap(nullsLast(naturalOrder()))
                .map { (number, orderPositions) ->
                    val order = orderPositions.first().auftrag
                    val personDays = orderPositions.mapNotNull { it.personDays }
                        .fold(BigDecimal.ZERO, BigDecimal::add)
                    val status = order?.status?.let { ", ${translate(it.i18nKey)}" } ?: ""
                    val title = "${order?.titel ?: ""} (${NumberFormatter.format(personDays)} " +
                            "$personDaysUnit)$status"
                    val text = orderPositions.sortedBy { it.number }.joinToString("\n") { pos ->
                        val days = pos.personDays?.let { NumberFormatter.format(it) } ?: "??"
                        "#${pos.number} ($days $personDaysUnit): ${pos.titel ?: ""}" +
                                ", ${translate(pos.status.i18nKey)}"
                    }
                    Order(
                        number = "$number",
                        title = title,
                        text = text,
                        // The order's own page, wherever it is served: the url is a per-page decision the
                        // client can't make (see NextMigration).
                        url = NextMigration.standardEditPage(ORDER_CATEGORY)
                            .replace(NextMigration.ID_PLACEHOLDER, "${orderPositions.first().auftragId}"),
                    )
                }
            task.orderList = orderList.toMutableList()
        }

        fun addTimesheetReferenceList(task: Task) {
            val timesheetReferenceList = listOf("Uni Kassel", "Uni Göttingen")
            task.timesheetReferenceList =
                timesheetReferenceList//Registry.getInstance().getDao(TimesheetDao::class.java).getUsedReferences(task.id)
        }
    }

    private class BuildContext(
        val result: Result,
        val user: PFUserDO,
        val taskFilter: TaskFilter,
        val openedNodes: MutableSet<Long>,
        /** Whether the answer carries each node's orders, i. e. whether the user may see them at all. */
        val withOrders: Boolean = false,
    )

    private val log = org.slf4j.LoggerFactory.getLogger(TaskServicesRest::class.java)

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var kostCache: KostCache

    @Autowired
    private lateinit var listFilterService: ListFilterService

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var agGridSupport: AGGridSupport

    /**
     * The columns of the task tree, in the order the Wicket page shows them.
     *
     * @param columns Which optional columns have data. All off for the select mode, whose popover only
     * has room for the narrow ones anyway.
     * @return MutableList of UIAgGridColumnDef with all default columns
     */
    private fun createDefaultColumnDefs(
        columns: TaskColumnVisibility = TaskColumnVisibility()
    ): MutableList<UIAgGridColumnDef> {
        val lc = LayoutContext(TaskDO::class.java)
        val kost2Visible = Configuration.instance.isCostConfigured
        val columnDefs = mutableListOf<UIAgGridColumnDef>()

        columnDefs.add(
            UIAgGridColumnDef.createCol(
                "title",
                headerName = translate("task"),
                valueFormatter = UIAgGridColumnDef.Formatter.TREE_NAVIGATION,
                sortable = false,
                width = UIAgGridColumnDef.DESCRIPTION_WIDTH,
                filter = false,
                pinnedAndLocked = UIAgGridColumnDef.Orientation.LEFT,
            )
        )
        columnDefs.add(
            UIAgGridColumnDef.createCol(
                "consumption",
                headerName = translate("task.consumption"),
                valueFormatter = UIAgGridColumnDef.Formatter.CONSUMPTION,
                sortable = false,
                filter = false,
            )
        )
        if (kost2Visible) {
            columnDefs.add(
                UIAgGridColumnDef.createCol(
                    "kost2WildCard",
                    headerName = translate("fibu.kost2"),
                    sortable = false,
                    width = 80,
                    filter = false,
                )
                    .withTooltipField("kost2ListAsLines")
            )
        }
        if (columns.orders) {
            columnDefs.add(
                UIAgGridColumnDef.createCol(
                    "orderList",
                    headerName = translate("fibu.auftrag.auftraege"),
                    valueFormatter = UIAgGridColumnDef.Formatter.ORDERS,
                    sortable = false,
                    width = 100,
                    filter = false,
                )
            )
        }
        columnDefs.add(
            UIAgGridColumnDef.createCol(lc, "shortDescription", sortable = false, filter = false)
        )
        if (columns.protectTimesheetsUntil) {
            columnDefs.add(
                UIAgGridColumnDef.createCol(
                    "protectTimesheetsUntil",
                    headerName = translate("task.protectTimesheetsUntil.short"),
                    valueFormatter = UIAgGridColumnDef.Formatter.DATE,
                    sortable = false,
                    width = 100,
                    filter = false,
                )
            )
        }
        if (columns.reference) {
            columnDefs.add(
                UIAgGridColumnDef.createCol(lc, "reference", sortable = false, filter = false)
            )
        }
        if (columns.priority) {
            columnDefs.add(
                UIAgGridColumnDef.createCol(
                    lc,
                    "priorityAsString",
                    lcField = "priority",
                    sortable = false,
                    width = 100,
                    filter = false,
                )
            )
        }
        columnDefs.add(
            UIAgGridColumnDef.createCol(
                lc,
                "statusAsString",
                lcField = "status",
                // Coloured like the Wicket page shows it, and the colour follows the status rather than
                // the text, so it survives translation (see TaskStatusCell in projectforge-next).
                formatter = UIAgGridColumnDef.Formatter.TASK_STATUS,
                sortable = false,
                width = 100,
                filter = false,
            )
        )
        // Always shown, in both modes: it is one of the six the tree had from the start.
        columnDefs.add(
            UIAgGridColumnDef.createCol(lc, "responsibleUser", sortable = false, filter = false)
        )

        return columnDefs
    }

    /**
     * Gets the user's task tree as tree matching the filter. The open task nodes will be restored from the user's prefs.
     * @param initial If true, the layout info and translations are also returned. Default is to return only the tree data.
     * @param open Optional task to open in the tree (if a descendent child of closed tasks, all ancestor tasks will be opened as well).
     * @param close Optional task to close.
     * @param table If true, the result will be returned flat with indent counter of each task node, otherwise a tree object is returned.
     * @param opened Show opened tasks. For initial = true, this value is ignored.
     * @param notOpened Show un-opened tasks. For initial = true, this value is ignored.
     * @param closed Show closed tasks. For initial = true, this value is ignored.
     * @param deleted Show deleted tasks. For initial = true, this value is ignored.
     * @return json
     */
    @GetMapping("tree")
    fun getTree(
        request: HttpServletRequest,
        @RequestParam("initial") initial: Boolean?,
        @RequestParam("open") open: Long?,
        @RequestParam("close") close: Long?,
        @RequestParam("highlightedTaskId") highlightedTaskId: Long?,
        @RequestParam("table") table: Boolean?,
        @RequestParam("searchString") searchString: String?,
        @RequestParam("opened") opened: Boolean?,
        @RequestParam("notOpened") notOpened: Boolean?,
        @RequestParam("closed") closed: Boolean?,
        @RequestParam("deleted") deleted: Boolean?,
        @RequestParam("showRootForAdmins") showRootForAdmins: Boolean?,
        @RequestParam("select") select: Boolean?,
        @RequestParam("rootTaskId") rootTaskId: Long?,
    )
            : Result {
        val selectMode = select == true
        val openNodes = userPrefService.ensureEntry(PREF_ARA, TaskTree.USER_PREFS_KEY_OPEN_TASKS, mutableSetOf<Long>())
        // Its own stored filter per mode (see filterKeySuffix), as with the column state.
        val filter = listFilterService.getSearchFilter(
            request.getSession(false), TaskFilter::class.java, filterKeySuffix(selectMode)
        ) as TaskFilter

        if (initial != true) {
            // User filter settings not on initial call.
            // On initial calls the stored filter will be used and returned for restoring in the client.
            if (opened != null) filter.isOpened = opened
            if (notOpened != null) filter.isNotOpened = notOpened
            if (closed != null) filter.isClosed = closed
            if (deleted != null) filter.deleted = deleted
            filter.searchString = searchString
        }
        if (!filter.isStatusSet) {
            // Nothing will be found, so avoid no result by user's mistake:
            filter.isOpened = true
            filter.isNotOpened = true
        }
        val result = Result()
        val ctx = BuildContext(
            result, ThreadLocalUserContext.loggedInUser!!, filter, openNodes,
            // Only where the column exists at all: the select popover doesn't show the orders, so the
            // answer needn't carry them (see createDefaultColumnDefs). Access as the Wicket page has it
            // (TaskTreePage.onInitialize): orders for project staff and above.
            withOrders = !selectMode && taskTree.hasOrderPositionsEntries() &&
                    accessChecker.isLoggedInUserMemberOfGroup(*TaskColumnVisibility.ORDER_GROUPS),
        )
        openTask(ctx, open)
        closeTask(ctx, close)
        if (initial == true) {
            openTask(ctx, highlightedTaskId) // Only open on initial call.
        }
        //UserPreferencesHelper.putEntry(TaskTree.USER_PREFS_KEY_OPEN_TASKS, expansion.getIds(), true)
        filter.resetMatch() // taskFilter caches visibility, reset needed first.
        val indent = if (table == true) 0 else null
        // Re-rooting (the breadcrumb navigation of the select panel): seed the walk at a chosen node so the
        // answer is only its subtree, with that node's direct children starting at indent 0 exactly as the
        // real root's would. buildTree only recurses into the real root or an opened node, so the pseudo-root
        // is opened first (openTask). A node the user may not select is ignored, falling back to the full tree.
        val rerootNode = rootTaskId?.let { taskTree.getTaskNodeById(it) }
            ?.takeIf { taskDao.hasUserSelectAccess(ctx.user, it.getTask(), false) }
        val rootNode = rerootNode ?: taskTree.rootTaskNode
        if (rerootNode != null) {
            openTask(ctx, rerootNode.taskId)
        }
        val root = Task(rootNode)
        addKost2List(root, recursive = false)
        buildTree(ctx, root, rootNode, indent)
        if (rerootNode == null && showRootForAdmins == true && table == true &&
            (accessChecker.isLoggedInUserMemberOfAdminGroup() ||
                    accessChecker.isLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP))
        ) {
            // Append root node for admins and financial staff only in table view for displaying purposes.
            // Last position, as the Wicket page shows it (TaskTreeProvider's comparator).
            result.nodes.add(Task(rootNode).also { it.root = true })
        }
        if (initial == true) {
            // The select popover keeps the narrow set of columns, no matter what the tree has data for:
            // the extra ones (orders, reference, priority, protection) don't fit and aren't what the user
            // picks a task by.
            result.columnDefs.addAll(
                createDefaultColumnDefs(
                    if (selectMode) TaskColumnVisibility()
                    else TaskColumnVisibility.of(accessChecker, taskTree)
                )
            )

            // Set grid state URLs (with tree/ prefix to avoid conflict with TaskPagesRest). The mode is
            // part of them, because each mode stores its own column state (see gridCategory).
            val modeParam = if (selectMode) "?select=true" else ""
            result.onColumnStatesChangedUrl =
                RestResolver.getRestUrl(this::class.java, "tree/${RestPaths.SET_COLUMN_STATES}$modeParam")
            result.resetGridStateUrl = RestResolver.getRestUrl(this::class.java, "tree/resetGridState$modeParam")

            // Create temporary UIAgGrid to restore user preferences
            val agGrid = UIAgGrid("taskTree")
            result.columnDefs.forEach { agGrid.add(it) }
            agGridSupport.restoreColumnsFromUserPref(gridCategory(selectMode), agGrid)

            // Copy restored state back to result
            result.columnDefs = agGrid.columnDefs
            result.sortModel = agGrid.sortModel
            result.filterModel = agGrid.filterModel

            result.initFilter = filter
            result.translations = addTranslations(
                "deleted",
                "search",
                "task.selectPanel.info", // Alert box at the end.
                "task.status.closed",
                "task.status.notOpened",
                "task.status.opened",
            )
        }
        return result
    }

    /**
     * Gets the task data including kost2 information if any and its path.
     * @param id Task id.
     * @return json
     */
    @GetMapping("info/{id}")
    fun getTaskInfo(@PathVariable("id") id: Long?): ResponseEntity<Task> {
        val task = createTask(id) ?: return ResponseEntity(HttpStatus.NOT_FOUND)
        return ResponseEntity(task, HttpStatus.OK)
    }

    /**
     * The tasks a typed term matches, as `{id, displayName}` — the type-ahead beside the tree of the task
     * select field (Wicket's `TaskSelectAutoCompleteFormComponent`).
     *
     * Everything about it is that component's: the two search fields (`title` and the indexed `taskpath`,
     * so a term matches a task's ancestors as well), the search itself ([TaskDao.select], which sorts by
     * title, drops what the user may not see and — through the [TaskFilter] it wraps a plain filter in —
     * leaves closed tasks out), and the label, which is the whole path rather than the bare title: in a
     * deep tree two tasks called "Development" are told apart by nothing else.
     *
     * Not `task/autosearch`: that name belongs to [org.projectforge.rest.task.TaskPagesRest], which
     * inherits it without declaring `autoCompleteSearchFields` — hence the `tree/` prefix, as for the grid
     * state above. The answer is a `DisplayObject` all the same, so the client's shared picker
     * (`EntitySearchList`) needs nothing of its own.
     *
     * An empty term is answered with the head of the list, because a picker asks that way as soon as it is
     * opened (see `useEntityLookup`); Wicket's field, which only ever searches on two typed characters,
     * never sees that case.
     */
    @GetMapping("tree/autosearch")
    fun autosearch(
        @RequestParam("search") search: String?,
        @RequestParam("maxResults") maxResults: Int?,
    ): List<AbstractEntityRest.DisplayObject> {
        val filter = BaseSearchFilter()
        filter.searchFields = AUTOCOMPLETE_SEARCH_FIELDS
        filter.searchString = search
        maxResults?.let { filter.maxRows = it }
        return taskDao.select(filter).map { AbstractEntityRest.DisplayObject(it.id, formatPath(it.id)) }
    }

    /**
     * The root of the structure tree, as `{id, displayName}`.
     *
     * Every task needs a parent — `TaskDao.checkConstraintVioloation` refuses one without it — and a caller
     * that wants to add a top level element has to name the root, which only the server knows. Wicket's
     * structure wizard does exactly that for its "create structure element" link
     * (`TaskWizardForm`, `TaskTree.getRootTaskNode`); the client here needs the id to build the same url.
     *
     * Nothing to protect: the root's id is in every tree answer an admin sees, and its name is the
     * installation's own (`task.path.rootTask`). Whether a user may add below it is the DAO's decision, not
     * this endpoint's.
     */
    @GetMapping("tree/root")
    fun getRoot(): AbstractEntityRest.DisplayObject {
        val rootId = TaskTree.instance.rootTaskNode.id
        return AbstractEntityRest.DisplayObject(rootId, formatPath(rootId))
    }

    /**
     * What the kost2 block of the task form would resolve to for a black/white list the user has typed but
     * not saved — and, with [Kost2PreviewRequest.addKost2Id], the list with a picked cost unit appended.
     *
     * Wicket recomputes this locally on every render from the unsaved form model
     * (`TaskEditForm`, the tooltip of `projektKostLabel`); a hand built page has to ask, because the three
     * calls behind it need the cost cache, the project of the task and the number format.
     *
     * Read only, but a POST: the black/white list is form content, and a GET would carry it in the url and
     * into every log.
     */
    @PostMapping("kost2Preview")
    fun getKost2Preview(@RequestBody request: Kost2PreviewRequest): ResponseEntity<Kost2Preview> {
        // The task as the form has it, not as the database has it: the preview is about the unsaved list.
        // Access is not checked - nothing is written, and what may be seen is the cost units of a project,
        // which the tree shows to everybody who may see the task. A task id that doesn't exist resolves no
        // project and answers an empty preview.
        val task = TaskDO()
        task.id = request.id
        request.parentTaskId?.let { taskDao.setParentTask(task, it) }
        task.kost2IsBlackList = request.kost2IsBlackList
        task.kost2BlackWhiteList = request.kost2BlackWhiteList
        request.addKost2Id?.let { kost2Id ->
            // Appends the two Kost2Art digits, or the whole number - see TaskHelper.addKost2, whose
            // branches are the reason this is not done in the client.
            task.kost2BlackWhiteList = TaskHelper.addKost2(taskTree, task, kostCache.getKost2(kost2Id))
        }
        val projekt = taskTree.getProjekt(task.id ?: task.parentTaskId)
        val kost2List = taskTree.getKost2List(projekt, task, task.kost2BlackWhiteItems, task.kost2IsBlackList)
        return ResponseEntity(
            Kost2Preview(
                kost2BlackWhiteList = TaskHelper.normalizeKost2BlackWhiteList(task),
                projektKost = projekt?.kost,
                kost2WildCard = kost2List?.let { KostHelper.getWildCardString(it, "*") },
                kost2ListAsLines = kost2List?.let { KostHelper.getFormattedNumberLines(it) },
            ),
            HttpStatus.OK,
        )
    }

    /**
     * @param indent null for tree view, int for table view.
     */
    private fun buildTree(ctx: BuildContext, task: Task, taskNode: TaskNode, indent: Int? = null) {
        if (!taskNode.hasChildren()) {
            task.treeStatus = TreeStatus.LEAF
            return
        }
        if (taskNode.isRootNode || ctx.openedNodes.contains(taskNode.taskId)) {
            task.treeStatus = TreeStatus.OPENED
            val children = taskNode.children.toMutableList()
            children.sortBy { it.task.title }
            children.forEach { node ->
                if (ctx.taskFilter.match(node, taskDao, ctx.user) &&
                    taskDao.hasUserSelectAccess(ctx.user, node.getTask(), false)
                ) {
                    val child = Task(node)
                    addKost2List(child, includeKost2ObjectList = false, recursive = false)
                    child.consumption = Consumption.create(node)
                    if (indent != null) {
                        // Flat (table) view: every visible node is a row of its own, carrying its depth as
                        // `indent`. The whole tree is returned down to the opened nodes — the select popover
                        // opens the current task's ancestors (see `openTask` on the initial call) and marks
                        // the row client-side, rather than hiding the rest of the tree around it.
                        ctx.result.nodes.add(child)
                        child.indent = indent
                        completeVisible(ctx, child)
                        buildTree(ctx, child, node, indent + 1)
                    } else {
                        // TaskNode has children and is opened:
                        if (task.children == null)
                            task.children = mutableListOf()
                        task.children!!.add(child)
                        completeVisible(ctx, child)
                        buildTree(ctx, child, node, null) // Build as tree
                    }
                }
            }
        } else {
            task.treeStatus = TreeStatus.CLOSED
        }
    }

    /**
     * What only a node the user actually gets to see is worth doing: its orders.
     *
     * Called for a node that made it into the answer, so the cost of resolving its orders is only paid
     * for rows the client is actually sent.
     */
    private fun completeVisible(ctx: BuildContext, task: Task) {
        if (ctx.withOrders) {
            addOrderList(task)
        }
    }

    private fun openTask(ctx: BuildContext, taskId: Long?) {
        if (taskId == null)
            return
        val taskNode = taskTree.getTaskNodeById(taskId)
        if (taskNode == null) {
            log.warn("Task with id $taskId not found to open.")
            return
        }
        ctx.openedNodes.add(taskId)
        var parent = taskNode.parent
        while (parent != null) {
            ctx.openedNodes.add(parent.taskId)
            parent = parent.parent
        }
    }

    private fun closeTask(ctx: BuildContext, taskId: Long?) {
        if (taskId == null)
            return
        val taskNode = taskTree.getTaskNodeById(taskId)
        if (taskNode == null) {
            log.warn("Task with id $taskId not found to close.")
            return
        }
        ctx.openedNodes.remove(taskId)
    }

    /**
     * Saves grid state (column order, width, visibility, filters, etc.) for task tree.
     */
    @PostMapping("tree/${RestPaths.SET_COLUMN_STATES}")
    fun updateColumnStates(
        @Valid @RequestBody request: DataTableStateRequest,
        @RequestParam("select") select: Boolean?,
    ): String {
        agGridSupport.storeGridState(gridCategory(select == true), request)
        return "OK"
    }

    /**
     * Resets the AG Grid state to defaults and returns fresh column definitions for task tree.
     */
    @GetMapping("tree/resetGridState")
    fun resetGridState(@RequestParam("select") select: Boolean?): ResponseAction {
        val selectMode = select == true
        agGridSupport.resetGridState(gridCategory(selectMode))

        // The same set the initial call would send, so "reset" restores what the user started with.
        val agGrid = UIAgGrid("taskTree")
        createDefaultColumnDefs(
            if (selectMode) TaskColumnVisibility()
            else TaskColumnVisibility.of(accessChecker, taskTree)
        ).forEach { agGrid.add(it) }

        // Create ResponseAction using AGGridSupport helper
        return agGridSupport.createResetGridStateResponse(agGrid)
    }
}
