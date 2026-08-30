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

package org.projectforge.rest

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.Constants
import org.projectforge.business.PfCaches
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.scripting.ScriptParameterType
import org.projectforge.business.system.SystemInfoCache
import org.projectforge.business.task.TaskTree
import org.projectforge.business.teamcal.service.CalendarFeedService
import org.projectforge.business.timesheet.*
import org.projectforge.business.user.service.UserService
import org.projectforge.favorites.Favorites
import org.projectforge.framework.configuration.ApplicationContextProvider
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.*
import org.projectforge.framework.utils.MarkdownBuilder
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.jira.JiraUtils
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.calendar.CalendarServicesRest
import org.projectforge.rest.calendar.TeamEventPagesRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.RestButtonEvent
import org.projectforge.rest.core.RestHelper
import org.projectforge.rest.core.ResultSet
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.dto.*
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.projectforge.ui.filter.LayoutListFilterUtils
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterObjectElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("${Rest.URL}/timesheet")
class TimesheetPagesRest : AbstractDTOPagesRest<TimesheetDO, Timesheet, TimesheetDao>(
    TimesheetDao::class.java, "timesheet.title",
    cloneSupport = CloneSupport.AUTOSAVE
) {
    private val dateTimeFormatter = DateTimeFormatter.instance()

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var teamEventRest: TeamEventPagesRest

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetFavoritesService: TimesheetFavoritesService

    @Autowired
    private lateinit var timesheetRecentService: TimesheetRecentService

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var timesheetExport: TimesheetExport

    @Autowired
    private lateinit var timesheetListPdfExport: TimesheetListPdfExport

    @Autowired
    private lateinit var calendarFeedService: CalendarFeedService

    /**
     * For exporting list of timesheets.
     */
    @Suppress("unused")
    private class Timesheet4ListExport(
        val timesheet: Timesheet,
        val id: Long, // Needed for history Service
        val weekOfYear: String,
        val dayName: String,
        val timePeriod: String,
        val duration: String,
        /** Duration in millis, used by the client only for sorting the [duration] column. */
        val durationMillis: Long,
        val aiTimeSavings: String,
        val deleted: Boolean? = null,
    )

    /**
     * For exporting recent timesheets for copying for new time sheets.
     */
    @Suppress("unused")
    class RecentTimesheets(
        val timesheets: List<Timesheet>,
        val cost2Visible: Boolean
    )

    /**
     * The aggregates of the whole timesheet list for a hand-built page that formats nothing itself: the summed
     * duration (already formatted in the user's locale, taken as-is), its raw millis for a client that wants to
     * add it up itself, and — only where the installation tracks it — the share of time saved by AI. The typed
     * counterpart of the [resultInfo] markdown the legacy React list reads (see [ResultSet.statistics]).
     */
    @Suppress("unused")
    class TimesheetListStatistics(
        val totalDurationMillis: Long,
        val totalDuration: String,
        val aiEnabled: Boolean,
        val aiPercentage: String?,
    )

    /**
     * Returning a non-null DTO opts the next list into the lean row: [createListRow] then fills only the
     * list's columns via [Timesheet.copyFrom4ListRow] instead of the whole [transformFromDB] DTO (see
     * [org.projectforge.rest.core.AbstractDTOPagesRest.createListRow]). The React list still gets the full
     * DTO against the kept [createListLayout], as [postProcessResultSet] keeps its own row shape for it.
     */
    override fun newDTO(): Timesheet {
        return Timesheet()
    }

    override fun transformFromDB(obj: TimesheetDO, editMode: Boolean): Timesheet {
        val timesheet = Timesheet()
        caches.initialize(obj)
        timesheet.copyFrom(obj)
        timesheet.timeSavingsByAIEnabled = baseDao.timeSavingsByAIEnabled
        timesheet.timeSavingsByAINote = timeSavingsByAINote()
        timesheet.tags = timesheetDao.getTags(timesheet.tag)
        // PFDay.fromOrNull(timesheet.startTime)
        return timesheet
    }

    override fun transformForDB(dto: Timesheet): TimesheetDO {
        val timesheetDO = TimesheetDO()
        dto.copyTo(timesheetDO)
        if (timesheetDO.kost2 != null && baseDao.getKost2List(timesheetDO).isNullOrEmpty()) {
            // Work around: if kost 2 was selected in client before new task without kost2 assignments was chosen,
            // the former kost2 selection will be sent by the client.
            timesheetDO.kost2 = null
        }
        return timesheetDO
    }


    override fun getInitialList(request: HttpServletRequest): InitialListData {
        val taskId = NumberHelper.parseLong(request.getParameter("taskId")) ?: return super.getInitialList(request)
        val filter = MagicFilter()
        filter.entries.add(MagicFilterEntry("task", "$taskId"))
        return super.getInitialList(request, filter)
    }

    override fun newBaseDTO(request: HttpServletRequest?): Timesheet {
        val sheet = Timesheet()
        val startTimeEpochSeconds = RestHelper.parseLong(request, "start")
        val endTimeEpochSeconds = RestHelper.parseLong(request, "end")
        if (startTimeEpochSeconds != null) {
            val start = PFDateTime.fromOrNow(startTimeEpochSeconds)
            sheet.startTime = start.sqlTimestamp
        }
        if (endTimeEpochSeconds != null) {
            val stop = PFDateTime.fromOrNow(endTimeEpochSeconds)
            sheet.stopTime = stop.sqlTimestamp
        }
        val userId = RestHelper.parseLong(request, "userId") // Optional parameter given to edit page
        sheet.user = User.getUser(userId)
        val recentEntry = timesheetRecentService.getRecentTimesheet()
        if (recentEntry != null) {
            if (recentEntry.taskId != null) {
                sheet.task = Task.getTask(recentEntry.taskId)
                if (recentEntry.kost2Id != null) {
                    sheet.kost2 = Kost2.getkost2(recentEntry.kost2Id)
                }
            }
            sheet.location = recentEntry.location
            sheet.reference = recentEntry.reference
            sheet.tag = recentEntry.tag
            sheet.description = recentEntry.description
            recentEntry.timeSavedByAIUnit?.let {
                sheet.timeSavedByAIUnit = it
            }
            if (sheet.user == null && recentEntry.userId != null) {
                sheet.user = User.getUser(recentEntry.userId)
            }
        }
        if (sheet.user == null) {
            sheet.user = User.getUser(ThreadLocalUserContext.loggedInUserId) // Use current user.
        }
        sheet.timeSavingsByAIEnabled = baseDao.timeSavingsByAIEnabled
        sheet.timeSavingsByAINote = timeSavingsByAINote()
        sheet.tags = timesheetDao.getTags(sheet.tag)
        // The hand-built page reaches this preset through newEntry, which — unlike the UILayout edit
        // endpoint — never runs onGetItemAndLayout. Apply the same start/stop preset here so a timesheet
        // created from the calendar is snapped, defaulted to firstHour and rolled to the day's last sheet
        // exactly as before (see presetStartStopTime). Idempotent, so the edit endpoint applying it again
        // in onGetItemAndLayout does no harm.
        request?.let { presetStartStopTime(it, sheet) }
        return sheet
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: Timesheet) {
        if (baseDao.timeSavingsByAIEnabled) {
            timesheetDao.validateTimeSavingsByAI(dto.timeSavedByAI, dto.timeSavedByAIUnit)?.let {
                validationErrors.add(ValidationError(translate(it), fieldId = "timeSavedByAI"))
            }
        }
    }

    override fun onAfterEdit(request: HttpServletRequest, obj: TimesheetDO, postData: PostData<Timesheet>, event: RestButtonEvent): ResponseAction {
        // Save time sheet as recent time sheet
        val timesheet = postData.data
        timesheetRecentService.addRecentTimesheet(transformForDB(timesheet))
        return CalendarServicesRest.redirectToCalendarWithDate(obj.startTime, event)
    }

    override fun postProcessResultSet(
        resultSet: ResultSet<TimesheetDO>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        // Two clients, two row shapes: the hand-built next list reads a flat row (TimesheetListRow, with
        // top-level task/user/times/texts), while the legacy React list reads the nested Timesheet4ListExport
        // with its pre-formatted week/day/period/duration columns. useListRow is the framework's switch (next
        // client of a migrated entity vs. the rest, see AbstractEntityRest.useListRow).
        val leanRows = useListRow(request)
        val list: List<Any> = resultSet.resultSet.map {
            if (leanRows) {
                // The flat row the hand-built next list reads: its plain columns via the shared lean-row
                // path (newDTO + Timesheet.copyFrom4ListRow, task/user/kost2 from the caches, no N+1), plus
                // the same pre-formatted week/day/period/duration/AI columns the legacy export below builds —
                // here on the flat row instead of a nested one, formatted in the user's locale and working-day
                // config so the hand-built page renders them as-is.
                createListRow(it).also { row ->
                    val day = PFDay.fromOrNull(it.startTime)
                    row.weekOfYear = DateTimeFormatter.formatWeekOfYear(it.startTime)
                    row.dayName = day?.dayOfWeekAsShortString ?: "??"
                    row.formattedTimePeriod = dateTimeFormatter.getFormattedTimePeriodOfDay(it.timePeriod)
                    row.formattedDuration = dateTimeFormatter.getFormattedDuration(it.timePeriod)
                    row.durationMillis = it.duration
                    if (baseDao.timeSavingsByAIEnabled) {
                        row.aiTimeSavings = AITimeSavings.getFormattedTimeSavedByAI(it)
                    }
                }
            } else {
                // Populate task, user and kost2 from the in-memory caches by their FK ids (as transformFromDB
                // does for the single entity) before copyFrom dereferences them: otherwise each row would lazy
                // load its task from the DB, an N+1 over the page (getListByIds loads by IN(...), see getListPage).
                caches.initialize(it)
                val timesheet = Timesheet()
                timesheet.copyFrom(it)
                val day = PFDay.fromOrNull(it.startTime)
                Timesheet4ListExport(
                    timesheet,
                    id = it.id!!,
                    weekOfYear = DateTimeFormatter.formatWeekOfYear(it.startTime),
                    dayName = day?.dayOfWeekAsShortString ?: "??",
                    timePeriod = dateTimeFormatter.getFormattedTimePeriodOfDay(it.timePeriod),
                    duration = dateTimeFormatter.getFormattedDuration(it.timePeriod),
                    durationMillis = it.duration,
                    aiTimeSavings = if (baseDao.timeSavingsByAIEnabled) {
                        AITimeSavings.getFormattedTimeSavedByAI(it)
                    } else "",
                    deleted = timesheet.deleted,
                )
            }
        }
        // Carry the paging fields through: for a server-side paged result (POST listPage) totalSize is the
        // size of the whole result, not of this page, and offset being set is what tells the client it holds
        // one page (see ResultSet, AbstractDTOPagesRest.postProcessResultSet). For the non-paged POST list
        // offset stays null and totalSize is this page, which is the whole result.
        val myResultSet = ResultSet(
            list,
            resultSet,
            totalSize = resultSet.totalSize ?: list.size,
            magicFilter = magicFilter,
            offset = resultSet.offset,
            limit = resultSet.limit,
            totalSizeExact = resultSet.totalSizeExact,
        )
        if (resultSet.offset == null) {
            // Non-paged POST list (the legacy React list and the exports): the result set is the whole result,
            // so its statistics are the whole result's, computed here in one pass.
            val stats = buildStatistics(resultSet.resultSet)
            myResultSet.statistics = stats
            // The markdown footer the legacy React list reads, beside the typed statistics the next page reads.
            val md = MarkdownBuilder()
            md.appendPipedValue("timesheet.totalDuration", stats.totalDuration, MarkdownBuilder.Color.BLUE)
            if (stats.aiEnabled) {
                md.appendPipedValue("timesheet.ai.timeSavedByAI", stats.aiPercentage ?: "", MarkdownBuilder.Color.BLUE)
            }
            myResultSet.addResultInfo(md.toString())
        } else {
            // Server-side paged (the next client only): resultSet.resultSet is one page, so the whole-result
            // statistics were computed over the full id list in aggregate() and are carried through here.
            myResultSet.statistics = resultSet.statistics
        }
        return myResultSet
    }

    /**
     * The whole-result statistics of a server-side paged list (see [getListPage]): the summed duration and the
     * AI share over the full id list, not over the single page [postProcessResultSet] returns. The paging
     * counterpart of computing them there over the whole non-paged result.
     */
    override fun aggregate(ids: LongArray, filter: MagicFilter): Any {
        // A lean four-column projection, not getListByIds: the whole id list can be thousands of sheets, and
        // buildStatistics reads only the duration and the AI fields, so hydrating the entities (all columns, in
        // IN batches) just to sum them is pure waste (see TimesheetDao.selectStatisticsData). The result is
        // cached with the id list, so this runs once per filter, not per page (see getListPage).
        return buildStatistics(timesheetDao.selectStatisticsData(ids.toList()))
    }

    /**
     * The summed duration and — where the installation tracks it — the AI share over the given time sheets, in
     * one pass so the footer's two numbers can never disagree (see [AITimeSavings.buildStats]). Reads only the
     * duration and AI fields of each sheet, so it needs no cache-populated task.
     */
    private fun buildStatistics(list: List<TimesheetDO>): TimesheetListStatistics {
        val stats = AITimeSavings.buildStats(list)
        val aiEnabled = baseDao.timeSavingsByAIEnabled
        return TimesheetListStatistics(
            totalDurationMillis = stats.totalDurationMillis,
            totalDuration = dateTimeFormatter.getPrettyFormattedDuration(stats.totalDurationMillis),
            aiEnabled = aiEnabled,
            aiPercentage = if (aiEnabled) stats.percentageString else null,
        )
    }

    override fun isAutocompletionPropertyEnabled(property: String): Boolean {
        return property == "location"
    }

    override fun getAutoCompletionForProperty(
        @RequestParam("property") property: String,
        @RequestParam("search") searchString: String?
    )
            : List<String> {
        if (property == "location") {
            val toLowerSearch = searchString?.lowercase()?.trim()
            val recentLocations = timesheetRecentService.getRecentLocations()
            if (toLowerSearch.isNullOrBlank()) {
                // No search string given, so show all recent entries, or:
                return recentLocations
            }
            val exactMatch = recentLocations.find { it.trim().equals(toLowerSearch, ignoreCase = true) }
            if (!exactMatch.isNullOrEmpty()) {
                // Exact match (so show also other recent locations as well for showing recent if location is prefilled, work-around
                // for convenience):
                val result = recentLocations.toMutableList()
                if (!exactMatch.trim()
                        .equals(searchString.trim(), ignoreCase = false)
                ) { // ignoreCase = false: Strings differs.
                    result.add(0, exactMatch) // Prepend exact match
                }
                return result
            }
            return recentLocations.filter { it.lowercase().contains(toLowerSearch) }
        }
        return super.getAutoCompletionForProperty(property, searchString)
    }

    @GetMapping("acReference")
    fun getReferences(@RequestParam("search") search: String?, @RequestParam("taskId") taskId: Long?): List<String> {
        taskId ?: return emptyList()
        return timesheetDao.getUsedReferences(taskId, search)
    }


    /**
     * The gates of the hand-built next list's optional columns, read by their `visible` callbacks (see
     * `timesheet.page.tsx`): the cost unit only where cost accounting is configured, the AI time-savings
     * only where the installation tracks it, the tag only where any tag is configured — the same three
     * conditions the [createListLayout] UILayout guards its columns with, so the two clients cannot
     * disagree about which columns the list has.
     */
    override fun addVariablesForListPage(): Map<String, Any> {
        return mapOf(
            "kost2Configured" to Configuration.instance.isCostConfigured,
            "timeSavingsByAIEnabled" to baseDao.timeSavingsByAIEnabled,
            "tagsConfigured" to !baseDao.getTags().isNullOrEmpty(),
        )
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        lc.idPrefix = "timesheet."
        val table = agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            TimesheetMultiSelectedPageRest::class.java,
            userAccess,
        )
            .add(lc, "user")
        //.add(lc, "kost2.project.customer", lcField = "kost2.projekt.kunde")
        //.add(lc, "kost2.project", lcField = "kost2.projekt")
        if (Configuration.instance.isCostConfigured) {
            table.add(lc, "kost2")
        }
        table.add(lc, "task")
            .add("weekOfYear", headerName = "calendar.weekOfYearShortLabel", width = 30)
            .add("dayName", headerName = "calendar.dayOfWeekShortLabel", width = 30)
            .add("timePeriod", headerName = "timePeriod", width = 140, sortField = "timesheet.startTime")
            .add("duration", headerName = "timesheet.duration", width = 50, sortField = "durationMillis")
        if (baseDao.timeSavingsByAIEnabled) {
            table.add("aiTimeSavings", headerName = "timesheet.ai.timeSavedByAI", width = 50)
        }
        table.add(lc, "location", "reference")
            .withMultiRowSelection(request, magicFilter)
        if (!baseDao.getTags().isNullOrEmpty()) {
            table.add(lc, "tag", width = 100)
        }
        table.add(lc, "description", width = 1000)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Timesheet, userAccess: UILayout.UserAccess): UILayout {
        val dayRange = UICustomized("dayRange")
        dayRange.add("startDateId", "startTime")
        dayRange.add("endDateId", "stopTime")
        dayRange.add("label", translate("timePeriod"))
        val descriptionArea = UITextArea("description", lc, rows = 5)
        val referenceField = UIInput(
            "reference", lc,
            label = "timesheet.reference",
            tooltip = "timesheet.reference.info"
        ).setAutoCompletion("timesheet/acReference?search=:search", mapOf("taskId" to "task.id"))
        val layout = super.createEditLayout(dto, userAccess)
            .add(UICustomized("timesheet.edit.templatesAndRecent"))
            .add(UICustomized("timesheet.edit.taskAndKost2", values = mutableMapOf("id" to "kost2")))
            .add(lc, "user")
            .add(dayRange)
            .add(
                UIRow()
                    .add(
                        UICol(xs = 6)
                            .add(UICustomized("task.consumption"))
                    )
            )
            .add(UIInput("location", lc).enableAutoCompletion(this))
        val row = UIRow()
        layout.add(row)
        createTagUISelect(dto)?.let { select ->
            row.add(UICol(md = 6).add(select))
        }
        row.add(UICol(md = 6).add(referenceField))
        layout.add(descriptionArea)
        if (baseDao.timeSavingsByAIEnabled) {
            layout.add(
                UIRow()
                    .add(UICol(md = 3).add(lc, TimesheetDO::timeSavedByAI))
                    .add(
                        UICol(md = 3).add(
                            UISelect<ScriptParameterType>(
                                "timeSavedByAIUnit",
                                required = true,
                                label = "timesheet.ai.timeSavedByAIUnit",
                                tooltip = "timesheet.ai.timeSavedByAIUnit.info",
                            ).buildValues(
                                TimesheetDO.TimeSavedByAIUnit::class.java
                            )
                        )
                    )
                    .add(UICol(md = 6).add(lc, TimesheetDO::timeSavedByAIDescription))
            )
        }
        timeSavingsByAINote()?.let { hint ->
            layout.layoutBelowActions.add(
                UIAlert(hint, title = "timesheet.ai.timeSavedByAI", color = UIColor.SECONDARY, markdown = true)
            )
        }

        JiraSupport.createJiraElement(dto.description, descriptionArea)
            ?.let { layout.add(UIRow().add(UICol().add(it))) }
        Favorites.addTranslations(layout.translations)
        layout.addAction(
            UIButton.createSecondaryButton(
                id = "switch",
                title = "plugins.teamcal.switchToTeamEventButton",
                responseAction = ResponseAction(getRestRootPath("switch2CalendarEvent"), targetType = TargetType.POST)
            )
        )
        layout.addTranslations(
            "search.search",
            "fibu.kost2",
            "fibu.kunde",
            "fibu.projekt",
            "timesheet.description",
            "timesheet.location",
            "timesheet.reference",
            "timesheet.recent",
            "timesheet.tag",
            "timesheet.templates",
            "timesheet.templates.migrationOfLegacy.button",
            "timesheet.templates.migrationOfLegacy.confirmationMessage",
            "timesheet.templates.migrationOfLegacy.tooltip",
            "timesheet.templates.new",
            "timesheet.templates.new.tooltip",
            "until",
            "yes", "cancel", // Confirmation message
        )
        LayoutUtils.addTranslations4TaskSelection(layout)
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    /**
     * The configured note to show below the edit form, or null when AI time-savings tracking is off or
     * no note is configured. The single source both the UILayout ([createEditLayout], as a
     * [UIAlert] in `layoutBelowActions`) and the hand-built page (via [Timesheet.timeSavingsByAINote])
     * read, so the two can never drift.
     */
    private fun timeSavingsByAINote(): String? {
        if (!baseDao.timeSavingsByAIEnabled) {
            return null
        }
        return configurationService.timesheetNoteSavingsByAI?.takeIf { it.isNotBlank() }
    }

    /**
     * @return The list of recent edited time sheets of the current logged-in user.
     */
    @GetMapping("recentList")
    fun getRecentList(): RecentTimesheets {
        val recentTimesheets = timesheetRecentService.getRecentTimesheets()
        var counter = 1
        val timesheets = recentTimesheets.map {
            val ts = Timesheet()
            ts.location = it.location
            ts.tag = it.tag
            ts.reference = it.reference
            ts.description = it.description
            val task = taskTree.getTaskById(it.taskId)
            if (task != null) {
                ts.task = Task()
                ts.task!!.copyFromMinimal(task)
            }
            it.timeSavedByAIUnit?.let {
                ts.timeSavedByAIUnit = it
            }
            // Don't copy these values to the timesheet. The user should enter them manually.
            // ts.timeSavedByAI = it.timeSavedByAI
            // ts.timeSavedByAIDescription = it.timeSavedByAIDescription
            val user = userService.getUser(it.userId)
            if (user != null) {
                ts.user = User()
                ts.user!!.copyFromMinimal(user)
            }
            if (it.kost2Id != null) {
                val kost2DO = caches.getAndPopulateKost2(it.kost2Id)
                if (kost2DO != null) {
                    val kost2 = Kost2()
                    ts.kost2 = kost2
                    kost2.copyFromMinimal(kost2DO)
                    kost2.formattedNumber = kost2DO.formattedNumber
                    kost2DO.projekt?.let { projektDO ->
                        val projekt = Project(projektDO.id, name = projektDO.name)
                        kost2.project = projekt
                        projektDO.kunde?.let { kundeDO ->
                            val kunde = Customer(kundeDO.id, name = kundeDO.name)
                            projekt.customer = kunde
                        }
                    }
                }
            }
            ts.counter = counter++
            ts
        }
        return RecentTimesheets(timesheets, SystemInfoCache.instance().isCost2EntriesExists())
    }

    @PostMapping("selectRecent")
    fun selectRecent(@RequestBody timesheet: Timesheet): ResponseAction {
        val task = TaskServicesRest.createTask(timesheet.task?.id)
        timesheet.tag = timesheet.tag ?: "" // "" Needed for overwriting clients data.tag if already defined.
        return ResponseAction(targetType = TargetType.UPDATE)
            .addVariable("task", task)
            .addVariable("data", timesheet)
    }

    /**
     * Will be called by clone button. Sets the id of the form data object to null and deleted to false.
     * @return ResponseAction with [TargetType.UPDATE] and variable "initial" with all the initial data of [getItemAndLayout] as given for new objects.
     */
    @RequestMapping("switch2CalendarEvent")
    fun switch2CalendarEvent(request: HttpServletRequest, @Valid @RequestBody postData: PostData<Timesheet>)
            : ResponseAction {
        return teamEventRest.cloneFromTimesheet(request, postData.data)
    }

    override fun getRestEditPath(): String {
        return "calendar/${super.getRestEditPath()}"
    }

    @Deprecated("Will be replaced by cloneFromCalendarEvent(request, calendarEvent).")
    fun cloneFromTeamEvent(request: HttpServletRequest, teamEvent: TeamEvent): ResponseAction {
        val calendarEvent = TeamEvent(
            startDate = teamEvent.startDate,
            endDate = teamEvent.endDate,
            location = teamEvent.location,
            subject = teamEvent.subject
        )
        return cloneFromCalendarEvent(request, calendarEvent)
    }

    fun cloneFromCalendarEvent(request: HttpServletRequest, calendarEvent: TeamEvent): ResponseAction {
        val timesheet = newBaseDTO(request)
        timesheet.startTime = calendarEvent.startDate
        timesheet.stopTime = calendarEvent.endDate
        if (!calendarEvent.location.isNullOrBlank())
            timesheet.location = calendarEvent.location
        if (!calendarEvent.subject.isNullOrBlank() || !calendarEvent.note.isNullOrBlank())
            timesheet.description = "${calendarEvent.subject ?: ""} ${calendarEvent.note ?: ""}"
        val editLayoutData = getItemAndLayout(request, timesheet, UILayout.UserAccess(false, true))
        return ResponseAction(
            url = "/${Constants.REACT_APP_PATH}calendar/${getRestPath(RestPaths.EDIT)}",
            targetType = TargetType.UPDATE
        )
            .addVariable("data", editLayoutData.data)
            .addVariable("ui", editLayoutData.ui)
            .addVariable("serverData", editLayoutData.serverData)
            .addVariable("variables", editLayoutData.variables)
    }

    /**
     * Supports request parameters startDate and endDate for creating new time sheet entries.
     *
     * Supports different date formats: long number of epoch seconds
     * or iso date time including any time zone offset.
     * @see PFDateTimeUtils.parse for supported date formats.
     */
    override fun onGetItemAndLayout(request: HttpServletRequest, dto: Timesheet, formLayoutData: FormLayoutData) {
        presetStartStopTime(request, dto)
        super.onGetItemAndLayout(request, dto, formLayoutData)
    }

    /**
     * Presets [Timesheet.startTime]/[Timesheet.stopTime] from the request parameters `startDate`/`endDate`,
     * shared by the UILayout edit page ([onGetItemAndLayout]) and the hand-built page's preset ([newBaseDTO]).
     *
     * Both parameters accept an epoch-seconds number or an ISO date-time including any zone offset
     * (see [PFDateTimeUtils.parse]). When both fall on the begin of a day — a length-less sheet dropped
     * from a month/agenda grid — the start rolls to `firstHour` (default 8) and, if the user already has
     * sheets that day, to the end of the day's last one. Both ends are then snapped to five minutes. Does
     * nothing when neither parameter is present, so a plain add is untouched.
     */
    private fun presetStartStopTime(request: HttpServletRequest, dto: Timesheet) {
        var startTime = PFDateTimeUtils.parseAndCreateDateTime(
            request.getParameter("startDate"),
            numberFormat = PFDateTime.NumberFormat.EPOCH_SECONDS
        )
        var stopTime = PFDateTimeUtils.parseAndCreateDateTime(
            request.getParameter("endDate"),
            numberFormat = PFDateTime.NumberFormat.EPOCH_SECONDS
        )
        if (startTime == null && stopTime == null) {
            return
        }
        if (startTime != null && startTime.isBeginOfDay && stopTime != null && stopTime.isBeginOfDay) {
            // Time sheet has no length (generated from grid view like month, agenda or overview).
            // Try to find a better startTime
            val firstHour = request.getParameter("firstHour")?.toIntOrNull() ?: 8
            startTime = startTime.withHour(firstHour)
            stopTime = stopTime.withHour(firstHour)
            val userId = dto.user?.id
            if (userId != null) {
                val filter = TimesheetFilter()
                filter.userId = userId
                filter.startTime = startTime.utilDate
                filter.stopTime = startTime.endOfDay.utilDate
                val timesheetsOfDay = timesheetDao.select(filter)
                var maxStopDate: Date? = null
                timesheetsOfDay.forEach {
                    if (maxStopDate == null ||
                        (it.stopTime != null && it.stopTime!!.after(maxStopDate))
                    ) {
                        maxStopDate = it.stopTime
                    }
                }
                maxStopDate?.let {
                    startTime = PFDateTime.from(maxStopDate!!)
                    stopTime = startTime
                }
            }
        }
        startTime?.let {
            dto.startTime = it.withPrecision(DatePrecision.MINUTE_5).sqlTimestamp
        }
        stopTime?.let {
            dto.stopTime = it.withPrecision(DatePrecision.MINUTE_5).sqlTimestamp
        }
    }

    /**
     * Puts the task information such as path, consumption etc. as additional variable for the client, because the
     * origin task of the timesheet is of type TaskDO and doesn't contain such data.
     */
    override fun addVariablesForEditPage(dto: Timesheet): MutableMap<String, Any>? {
        val task = TaskServicesRest.createTask(dto.task?.id) ?: return null
        return mutableMapOf(
            "task" to task,
            "timesheetFavorites" to timesheetFavoritesService.getList(),
            "hasLegacyFavoritesToMigrate" to timesheetFavoritesService.hasLegacyFavoritesToMigrate(),
        )
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        val element = UIFilterElement("kost2.nummer")
        element.label = element.id // Default label if no translation will be found below.
        element.label = LayoutListFilterUtils.getLabel(
            ElementInfo(
                "nummer",
                i18nKey = "fibu.kost2.nummer",
                parent = ElementInfo("kost2", i18nKey = "fibu.kost2")
            )
        )
        elements.add(element)
        // The three settings the legacy list form keeps always open (TimesheetListForm): the period the
        // sheets fall into, the user they belong to and the task they were booked on. All `defaultFilter`,
        // so they show without being added — the pills the user narrows a time sheet list by first.
        //
        // `startTime` and `stopTime` are both `@GenericField`, so each is auto-generated above as a
        // TIMESTAMP filter ("Beginn"/"Stopp", down to the time of day) — the two literal-bound filters a
        // user can add to pin an exact start or end. The list's own default period is a *third*, distinct
        // filter: a sticky date range whose overlap semantics differ from either bound (it catches a sheet
        // that began before the window but runs into it, see preProcessMagicFilter). It gets its own id so
        // it never collides with the `startTime`/`stopTime` pills — a shared id would open two at once.
        elements.add(
            // A synthetic field (no `period` property on TimesheetDO): consumed in preProcessMagicFilter,
            // where its DATE from/to become the overlap predicate. DATE, so the picker sends day-only bounds.
            UIFilterElement("period", filterType = UIFilterElement.FilterType.DATE, label = translate("timePeriod"))
                .also { it.defaultFilter = true }
        )
        elements.add(
            // Consumed in preProcessMagicFilter, because the object picker sends the picked user as `value.id`
            // and the generic BaseDO predicate reads `value.value` instead (see there).
            UIFilterObjectElement(
                "user",
                label = translate("timesheet.user"),
                autoCompletion = AutoCompletion.getAutoCompletion4Users(),
            ).also { it.defaultFilter = true }
        )
        elements.add(
            // The task's own type-ahead — `TaskServicesRest.autosearch` under `task/tree`, not the inherited
            // `task/autosearch` of this class (which has no search fields and would error). Consumed in
            // preProcessMagicFilter, both for the recursive toggle and for the `value.id` the picker sends.
            UIFilterObjectElement(
                "task",
                label = translate("task"),
                autoCompletion = AutoCompletion<Long>(url = AutoCompletion.getAutoCompletionUrl("task/tree")),
            ).also { it.defaultFilter = true }
        )
        // The two options of the legacy list form (TimesheetListForm): search the picked task including its
        // sub-tasks (on by default, see MagicFilterProcessor, and consumed in preProcessMagicFilter so the
        // toggle can switch it off), and keep only sheets booked on a billable cost unit.
        elements.add(UIFilterBooleanElement("recursive", label = translate("task.recursive"), defaultFilter = true))
        elements.add(UIFilterBooleanElement("onlyBillable", label = translate("task.onlyBillable")))
        // Keep only sheets whose description or reference names a JIRA issue — the fields the next list
        // renders as JIRA links. Offered only where JIRA is configured (as IncompleteInvoiceFilter.isOffered
        // gates its pill), so the option doesn't appear on an instance that has no JIRA to link to.
        if (JiraUtils.isJiraConfigured) {
            elements.add(UIFilterBooleanElement("hasJiraIssues", label = translate("timesheet.filter.hasJiraIssues")))
        }
    }

    /**
     * Consumes the list's sticky settings ([addMagicFilterElements]) before the generic processor turns the
     * remaining entries into the query:
     * - `period` (a `defaultFilter` date range): a synthetic field, turned into the overlap predicate
     *   `stopTime >= from AND startTime <= to` — so a sheet that began before the window but runs into it is
     *   kept (as `TimesheetDao.buildQueryFilter` does). The literal `startTime`/`stopTime` pills a user may
     *   add stay with the generic processor as plain bounds on their columns.
     * - `task` (a `defaultFilter` object picker): taken over here rather than left to the generic processor,
     *   because the picker sends the task as `value.id` while the generic `TaskDO` predicate reads
     *   `value.value` (null then, which would match sheets *without* a task). The `recursive` toggle decides
     *   whether the sub-tasks are searched too (default true, as the legacy list).
     * - `recursive` (default true): see above; only consumed so it doesn't fall through as an unknown field.
     * - `user` (a `defaultFilter` object picker): same `value.id` reason, filtered by `user.id` as
     *   `TimesheetDao.getList` does it.
     * - `onlyBillable`: a post filter over the result, as `TimesheetDao.internalGetList` does it.
     */
    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<TimesheetDO>> {
        val filters = mutableListOf<CustomResultFilter<TimesheetDO>>()
        source.entries.find { it.field == "period" }?.let { periodEntry ->
            periodEntry.synthetic = true
            // Overlap, not containment: a sheet counts as inside the window if it *touches* it, so one that
            // began before the window but runs into it is kept — the same predicate TimesheetDao.buildQueryFilter
            // builds. The DATE picker sends day-only bounds; widen them to the whole day in the user's zone
            // (begin of the from-day, end of the to-day) so both edges are inclusive.
            val periodStart = PFDateTimeUtils.parseAndCreateDateTime(periodEntry.value.fromValue)?.beginOfDay?.utilDate
            val periodEnd = PFDateTimeUtils.parseAndCreateDateTime(periodEntry.value.toValue)?.endOfDay?.utilDate
            if (periodStart != null) {
                target.add(QueryFilter.ge("stopTime", periodStart))
            }
            if (periodEnd != null) {
                target.add(QueryFilter.le("startTime", periodEnd))
            }
        }
        val recursiveEntry = source.entries.find { it.field == "recursive" }
        recursiveEntry?.synthetic = true
        val recursive = recursiveEntry?.value?.value != "false" // Default true, as the legacy list.
        source.entries.find { it.field == "task" }?.let { taskEntry ->
            taskEntry.synthetic = true
            val taskId = taskEntry.value.id ?: taskEntry.value.value?.toLongOrNull()
            if (taskId != null) {
                // On `task.id`, not the `task` association: the recursive case is an `isIn` of the descendant
                // ids, and comparing the TaskDO association to a list of Longs is a type error (as TimesheetDao
                // does it too).
                target.add(QueryFilter.taskSearch("task.id", taskId, recursive))
            }
        }
        source.entries.find { it.field == "user" }?.let { userEntry ->
            userEntry.synthetic = true
            val userId = userEntry.value.id ?: userEntry.value.value?.toLongOrNull()
            if (userId != null) {
                target.add(QueryFilter.eq("user.id", userId))
            }
        }
        source.entries.find { it.field == "onlyBillable" }?.let { entry ->
            entry.synthetic = true
            if (entry.value.value == "true") {
                filters.add(TimesheetBillableFilter())
            }
        }
        source.entries.find { it.field == "hasJiraIssues" }?.let { entry ->
            entry.synthetic = true
            if (entry.value.value == "true") {
                filters.add(TimesheetJiraFilter())
            }
        }
        return filters
    }

    /**
     * Exports the filtered timesheets as an Excel file, the "Excel export" of the legacy list
     * (`TimesheetListPage.exportExcel` → [TimesheetExport]).
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        // The list endpoints (getList/listPage) normalize the client filter before querying; the export has to
        // do the same, or its full-text search behaves differently and returns nothing where the list showed rows.
        filter.autoWildcardSearch = true
        fixMagicFilterFromClient(filter)
        // Always a workbook, header row included even for an empty result (TimesheetExport.export) — so the
        // download never yields an empty file that would read as a broken export.
        val xls = timesheetExport.export(getObjectList(this, baseDao, filter))
        val filename = "ProjectForge-TimesheetExport_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx"
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
            .body(ByteArrayResource(xls))
    }

    /**
     * Exports the filtered timesheets as a PDF, the "PDF export" of the legacy list — now built with OpenPDF
     * in the business layer ([TimesheetListPdfExport]) rather than the wicket-bound FOP path.
     */
    @PostMapping(RestPaths.REST_PDF_SUB_PATH)
    fun exportAsPdf(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        // The list endpoints (getList/listPage) normalize the client filter before querying; the export has to
        // do the same, or its full-text search behaves differently and returns nothing where the list showed rows.
        filter.autoWildcardSearch = true
        fixMagicFilterFromClient(filter)
        // The filter summary shown on the PDF's first page: the sticky pills the list was narrowed by
        // (see addMagicFilterElements) — the period, the free-text search and the picked user.
        val periodEntry = filter.entries.find { it.field == "period" }
        val userEntry = filter.entries.find { it.field == "user" }
        val context = TimesheetListPdfExport.Context(
            periodFrom = periodEntry?.value?.fromValue,
            periodTo = periodEntry?.value?.toValue,
            searchString = filter.searchString,
            userName = userEntry?.value?.displayName,
        )
        // Always a valid PDF, header row included even for an empty result (TimesheetListPdfExport.export).
        val pdf = timesheetListPdfExport.export(getObjectList(this, baseDao, filter), context)
        val filename = "ProjectForge-TimesheetExport_${DateHelper.getDateAsFilenameSuffix(Date())}.pdf"
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
            .body(ByteArrayResource(pdf))
    }

    /**
     * The subscription URL of the timesheet calendar feed for the given user (the current one by default), the
     * "ics export" of the legacy list. Returns the URL rather than a stream: the client shows it for the user
     * to subscribe to in their calendar (see [CalendarFeedService.getUrl4Timesheets]).
     */
    @GetMapping("icsExportUrl")
    fun getIcsExportUrl(@RequestParam("userId", required = false) userId: Long?): Map<String, String> {
        val id = userId ?: ThreadLocalUserContext.loggedInUserId
        return mapOf("url" to calendarFeedService.getUrl4Timesheets(id))
    }

    /**
     * @param timesheet Only needed, if the tag of the given timesheet should be added to the tag list and is not
     * configured (after changing configuration of tag list).
     * @param id Field (id) is "tag" as default.
     * @return UISelect or null, if no tags exist (neither configured nor given in timesheet).
     */
    fun createTagUISelect(timesheet: Timesheet? = null, id: String = "tag"): UISelect<String>? {
        val tags = timesheetDao.getTags(timesheet?.tag)
        if (tags.isNullOrEmpty()) {
            return null
        }
        return UISelect(id, label = "timesheet.tag", required = false, values = tags.map { UISelectValue(it, it) })
    }

    /**
     * Keeps only time sheets booked on a billable cost unit, the `onlyBillable` option of the legacy list
     * (`TimesheetDao.internalGetList`). The cost unit and its type come from the cache, not from a lazy
     * association on the detached result row.
     */
    private class TimesheetBillableFilter : CustomResultFilter<TimesheetDO> {
        override fun match(list: MutableList<TimesheetDO>, element: TimesheetDO): Boolean {
            val kost2Id = element.kost2?.id ?: return false
            return caches.getKost2(kost2Id)?.kost2Art?.fakturiert == true
        }

        companion object {
            private val caches =
                ApplicationContextProvider.getApplicationContext().getBean(PfCaches::class.java)
        }
    }

    /**
     * Keeps only time sheets whose own text names a JIRA issue — its description or reference, the two
     * fields the next list renders as JIRA links. The task's fields (title, short description, description)
     * are deliberately not scanned: a key in a task title would otherwise match every sheet booked on it,
     * which is not what „time sheets with JIRA issues" means. A pure text check (`JiraUtils.hasJiraIssues`)
     * needs no cache and no DB — the availability of JIRA is already gated at the filter's registration.
     */
    internal class TimesheetJiraFilter : CustomResultFilter<TimesheetDO> {
        override fun match(list: MutableList<TimesheetDO>, element: TimesheetDO): Boolean =
            JiraUtils.hasJiraIssues(element.description) || JiraUtils.hasJiraIssues(element.reference)
    }
}
