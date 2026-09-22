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

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.task.TaskNode
import org.projectforge.business.task.TaskTree
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.timesheet.TimesheetDao
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.*
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable

/**
 * Mass update after selection.
 */
@RestController
@RequestMapping("${Rest.URL}/timesheet${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class TimesheetMultiSelectedPageRest : AbstractMultiSelectedPage<TimesheetDO>() {
    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var taskTree: TaskTree

    @Autowired
    private lateinit var timesheetDao: TimesheetDao

    @Autowired
    private lateinit var timesheetPagesRest: TimesheetPagesRest

    override val layoutContext: LayoutContext = LayoutContext(TimesheetDO::class.java)

    override fun getTitleKey(): String {
        return "timesheet.multiselected.title"
    }

    override val listPageUrl: String = "/${MenuItemDefId.TIMESHEET_LIST.url}"

    @PostConstruct
    private fun postConstruct() {
        pagesRest = timesheetPagesRest
    }

    /**
     * The layout-free field set for a client (the next frontend) that renders the form itself - the
     * counterpart of the `createAndAddFields` calls in [fillForm].
     *
     * The task/kost2 picker is a custom component ([UICustomized]) the hand built page renders on its own;
     * only the plain fields are declared here. The tag is declared as a select of the configured tags
     * (built at runtime, hence [MassUpdateFieldDeclaration.values]), so the layout free frontend renders
     * it as a combobox with a delete option - the same [UISelect] the [fillForm] path builds. The AI
     * fields are only offered when the feature is enabled, exactly as [fillForm] adds them.
     */
    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        val declarations = mutableListOf(
            // reference has length 1.000 and description 4.000, see [fillForm].
            MassUpdateFieldDeclaration("location", minLengthOfTextArea = 1001),
            MassUpdateFieldDeclaration("reference", minLengthOfTextArea = 1001),
            MassUpdateFieldDeclaration("description", minLengthOfTextArea = 1001),
            // The task/cost-unit picker the next page renders itself (TaskKost2MassUpdateField); declared
            // here only for its position - right below the activity report (description) - in the field
            // order the client shows and the preview sorts by.
            MassUpdateFieldDeclaration("taskAndKost2", custom = true),
        )
        // Only where tags are configured at all - no timesheet is known here, so the current tag of one
        // cannot be added (see [TimesheetDao.getTags]); an empty list means the field is left out entirely,
        // exactly as [TimesheetPagesRest.createTagUISelect] returns null.
        timesheetDao.getTags(null)?.takeIf { it.isNotEmpty() }?.let { tags ->
            declarations.add(
                MassUpdateFieldDeclaration(
                    "tag",
                    showDeleteOption = true,
                    values = tags.map { UISelectValue(it, it) },
                )
            )
        }
        if (timesheetDao.timeSavingsByAIEnabled) {
            declarations.add(MassUpdateFieldDeclaration("timeSavedByAI"))
            declarations.add(MassUpdateFieldDeclaration("timeSavedByAIUnit"))
            declarations.add(MassUpdateFieldDeclaration("timeSavedByAIDescription"))
        }
        return declarations
    }

    override fun infoMessageKey(): String? {
        return if (Configuration.instance.isCostConfigured) "timesheet.massupdate.kost.info" else null
    }

    /**
     * The selection's statistics as pre-rendered markdown for the legacy UILayout form (see [fillForm]): the
     * same summed duration / AI-savings line the list footer shows, over the selected time sheets.
     */
    override fun getStatistics(selectedIds: Collection<Serializable>?): String {
        return timesheetPagesRest.buildStatisticsMarkdown(buildStatistics(selectedIds))
    }

    /**
     * The selection's statistics as typed values for the hand-built next page, which renders them with the
     * same [org.projectforge.rest.dto.Timesheet] statistics line the list uses (reusing
     * [TimesheetPagesRest.TimesheetListStatistics] so both pages show the identical line).
     */
    override fun getStatisticsData(selectedIds: Collection<Serializable>?): Any {
        return buildStatistics(selectedIds)
    }

    private fun buildStatistics(selectedIds: Collection<Serializable>?): TimesheetPagesRest.TimesheetListStatistics {
        // Lean four-column projection, not a full select: this runs live on every debounced selection
        // change, and buildStatistics reads only duration and the AI fields (see TimesheetPagesRest
        // .aggregate, which sums the whole list the same way).
        val ids = selectedIds?.mapNotNull { (it as? Number)?.toLong() }.orEmpty()
        return timesheetPagesRest.buildStatistics(timesheetDao.selectStatisticsData(ids))
    }

    /**
     * Start values for the hand built next page: the task and cost unit the selected time sheets have in
     * common, so the picker opens on them and a change against them is what the run acts on (see
     * [sharedTaskAndKost2]). The counterpart of what [fillForm] pre-computes for the legacy form.
     *
     * The cost unit is offered only when it is reachable from the shared task ([TaskTree.getKost2List]);
     * otherwise the client would drop it on load (the picker keeps a value only while the task allows it),
     * which would read as a change the user never made.
     */
    override fun initialParams(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>?,
    ): Map<String, MassUpdateParameter> {
        val (taskId, kost2Id) = sharedTaskAndKost2(timesheetDao.select(selectedIds))
        val params = mutableMapOf<String, MassUpdateParameter>()
        taskId?.let { params["task"] = MassUpdateParameter().also { p -> p.id = it } }
        kost2Id?.takeIf { taskTree.getKost2List(taskId)?.any { k -> k.id == it } == true }?.let {
            params["kost2"] = MassUpdateParameter().also { p -> p.id = it }
        }
        return params
    }

    /**
     * The task and cost unit the given time sheets share, or null where they do not: the task is the
     * deepest common ancestor of all their tasks, the cost unit the one they all book on (none if it
     * differs). Extracted from [fillForm] so the legacy form and the layout-free [initialParams] compute
     * the same preset the same way.
     */
    internal fun sharedTaskAndKost2(timesheets: List<TimesheetDO>?): Pair<Long?, Long?> {
        if (timesheets.isNullOrEmpty()) {
            return null to null
        }
        var taskNode: TaskNode? = null
        loop@ for (timesheet in timesheets) {
            val node = taskTree.getTaskNodeById(timesheet.taskId) ?: continue
            val current = taskNode
            if (current == null) {
                taskNode = node // First node
            } else if (node.isParentOf(current)) {
                taskNode = node
            } else if (current == node || current.isParentOf(node)) {
                // OK, node is on the same path.
            } else {
                // current and node aren't on the same path; climb to a shared ancestor.
                var ancestor = current.parent
                for (i in 0..1000) { // Paranoia loop for avoiding endless loops (instead of while(true))
                    if (ancestor == node || ancestor.isParentOf(node)) {
                        taskNode = ancestor
                        continue@loop
                    }
                    ancestor = ancestor.parent
                }
                taskNode = null
                break
            }
        }
        // The cost unit only if all time sheets book on the very same one.
        var kost2Id: Long? = null
        for (timesheet in timesheets) {
            if (timesheet.kost2Id == null) {
                // No kost2Id found.
                break
            }
            if (kost2Id == null) {
                kost2Id = timesheet.kost2Id
            } else if (kost2Id != timesheet.kost2Id) {
                // Kost2-id differs, so terminate.
                kost2Id = null
                break
            }
        }
        return taskNode?.id to kost2Id
    }

    override fun fillForm(
        request: HttpServletRequest,
        layout: UILayout,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        selectedIds: Collection<Serializable>?,
        variables: MutableMap<String, Any>,
    ) {
        var taskNode: TaskNode? = taskTree.getTaskNodeById(massUpdateData["task"]?.id)
        var kost2Id: Long? = massUpdateData["kost2"]?.id
        val timesheets = timesheetDao.select(selectedIds)
        if (taskNode == null && timesheets != null) {
            val (sharedTaskId, sharedKost2Id) = sharedTaskAndKost2(timesheets)
            taskNode = taskTree.getTaskNodeById(sharedTaskId)
            if (kost2Id == null) {
                kost2Id = sharedKost2Id
            }
        }
        // The same duration / AI-savings summary the next page and the list footer show, as markdown.
        layout.add(UIAlert("'${getStatistics(selectedIds)}", color = UIColor.LIGHT, markdown = true))

        kost2Id?.let {
            ensureMassUpdateParam(massUpdateData, "kost2", "fibu.kost2").id = it
        }
        taskNode?.id?.let { taskId ->
            TaskServicesRest.createTask(taskId)?.let { task ->
                ensureMassUpdateParam(massUpdateData, "task", "task").id = taskId
                variables["task"] = if (taskNode.isRootNode) {
                    // Don't show. If task is null, the React page will not be updated from time to time (workaround)
                    TaskServicesRest.Task("")
                } else {
                    task
                }
            }
        }
        val myOptions = mutableListOf<UIElement>(
            UICheckbox(
                "taskAndKost2.change",
                label = "update",
                tooltip = "timesheet.massupdate.updateTask",
            )
        )
        layout.add(
            createInputFieldRow(
                "taskAndKost2",
                UICustomized("timesheet.edit.taskAndKost2", values = mutableMapOf("id" to "kost2.id")),
                massUpdateData,
                myOptions = myOptions,
                displayName = "task"
            )
        )
        timesheetPagesRest.createTagUISelect(id = "tag.textValue")?.let { select ->
            layout.add(createInputFieldRow("tag", select, massUpdateData, showDeleteOption = true))
        }
        createAndAddFields(
            layoutContext,
            massUpdateData,
            layout,
            "location",
            "reference",
            "description",
            minLengthOfTextArea = 1001, // reference has length 1.000 and description 4.000
        )
        if (timesheetDao.timeSavingsByAIEnabled) {
            createAndAddFields(
                layoutContext,
                massUpdateData,
                layout,
                "timeSavedByAI",
                "timeSavedByAIUnit",
                "timeSavedByAIDescription",
            )
        }
        if (Configuration.instance.isCostConfigured) {
            layout.add(UIAlert(message = "timesheet.massupdate.kost.info", color = UIColor.INFO))
        }
    }

    override fun checkParamHasAction(
        params: Map<String, MassUpdateParameter>,
        param: MassUpdateParameter,
        field: String,
    ): Boolean {
        if (field == "kost2" || field == "task") {
            // No check here, action is checked on field taskAndKost2.
            return false
        }
        if (field == "taskAndKost2") {
            return param.change == true && (params["task"]?.id != null || params["kost2"]?.id != null)
        }
        return super.checkParamHasAction(params, param, field)
    }

    override fun handleClientMassUpdateCall(
        request: HttpServletRequest,
        massUpdateContext: MassUpdateContext<TimesheetDO>
    ) {
        val params = massUpdateContext.massUpdateParams
        val kost2Id = params["kost2"]?.id
        val taskId = params["task"]?.id
        if (taskId != null) {
            // Only when the task is being changed: a cost unit reachable from the *old* task may not be
            // reachable from the new one, so drop a now-invalid one (proceedMassUpdate then remaps by type).
            // Without a task change the cost unit is applied as picked (it was offered from the shared task's
            // list), so it must not be validated against a task that is not part of this update - doing so
            // (getKost2List(null)) would wrongly null it and turn a cost-unit-only change into "nothing to do".
            val availableKost2s = taskTree.getKost2List(taskId)
            if (kost2Id != null && availableKost2s?.any { it.id == kost2Id } != true) {
                params["kost2"]?.id = null
            }
        }
        // The synthetic taskAndKost2 field carries only the `change` flag, so the confirmation dialog would
        // read "set <task> to <empty>". Give it the picked task (and cost unit, if still valid) as its value,
        // which the preview shows as-is (proceedMassUpdate ignores it, it changes on task/kost2 instead).
        params["taskAndKost2"]?.takeIf { it.change == true }?.let { param ->
            val parts = mutableListOf<String>()
            taskId?.let { id -> taskTree.getTaskById(id)?.title?.let { parts.add(it) } }
            params["kost2"]?.id?.let { id ->
                kost2Dao.find(id, checkAccess = false)?.formattedNumber?.let { parts.add(it) }
            }
            param.textValue = parts.joinToString(" / ").takeIf { it.isNotBlank() }
        }
    }

    /**
     * The synthetic taskAndKost2 field is no property of the entity, so the registry has no label for it and
     * the default would capitalize the field name. Translate it as the task field, exactly as the [fillForm]
     * path labels its row (`displayName = "task"`).
     */
    override fun getFieldTranslation(field: String): String {
        if (field == "taskAndKost2") {
            // Both may change, and either alone, so the label names both rather than only the task.
            return "${translate("task")} / ${translate("fibu.kost2")}"
        }
        return super.getFieldTranslation(field)
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<TimesheetDO>,
    ): ResponseEntity<*>? {
        val timesheets = timesheetDao.select(selectedIds)
        if (timesheets.isNullOrEmpty()) {
            return null
        }
        val params = massUpdateContext.massUpdateParams
        val taskId = params["task"]?.id
        val project = taskTree.getProjekt(taskId)
        val availableKost2s = taskTree.getKost2List(taskId)
        val kost2Id = params["kost2"]?.id
        massUpdateContext.ignoreFieldsForModificationCheck = listOf("taskAndKost2")
        timesheets.forEach { timesheet ->
            massUpdateContext.startUpdate(timesheet)
            TextFieldModification.processTextParameter(timesheet, "bemerkung", params)
            TextFieldModification.processTextParameter(timesheet, "reference", params)
            TextFieldModification.processTextParameter(timesheet, "description", params)
            TextFieldModification.processTextParameter(timesheet, "location", params)
            TextFieldModification.processTextParameter(timesheet, "tag", params)
            params["timeSavedByAI"]?.let { param ->
                if (param.delete == true) {
                    timesheet.timeSavedByAI = null
                } else {
                    param.decimalValue?.let { timesheet.timeSavedByAI = it }
                }
            }
            params["timeSavedByAIUnit"]?.let { param ->
                if (param.delete == true) {
                    timesheet.timeSavedByAIUnit = null
                } else {
                    param.textValue?.let { textValue ->
                        timesheet.timeSavedByAIUnit = TimesheetDO.TimeSavedByAIUnit.valueOf(textValue)
                    }
                }
            }
            TextFieldModification.processTextParameter(timesheet, "timeSavedByAIDescription", params)
            params["taskAndKost2"]?.let { param ->
                if (param.change == true) {
                    if (taskId != null) {
                        taskTree.getTaskById(taskId)?.let { task ->
                            timesheet.task = task
                        }
                        if (!availableKost2s.isNullOrEmpty() && timesheet.kost2?.projekt != project) {
                            // Try to find kost2 with same type (last 2 digits of projects)
                            availableKost2s.find { it.kost2Art?.id == timesheet.kost2?.kost2Art?.id }?.let { newKost2 ->
                                timesheet.kost2 = newKost2
                            }
                        }
                    }
                    if (kost2Id != null) {
                        kost2Dao.find(kost2Id, checkAccess = false)?.let { kost2 ->
                            timesheet.kost2 = kost2
                        }
                    }
                }
            }
            massUpdateContext.commitUpdate(
                identifier4Message = "${timesheet.user?.getFullname()} ${timesheet.timePeriod.formattedString}",
                timesheet,
                update = { timesheetDao.update(timesheet) },
            )
        }
        return null
    }

    override fun ensureUserLogSubscription(): LogSubscription {
        val username = ThreadLocalUserContext.loggedInUser!!.username ?: throw InternalError("User not given")
        val displayTitle = translate("fibu.timesheet.multiselected.title")
        return LogSubscription.ensureSubscription(
            title = "Timesheets",
            displayTitle = displayTitle,
            user = username,
            create = { title, user ->
                LogSubscription(
                    title,
                    user,
                    LogEventLoggerNameMatcher(
                        "de.micromata.fibu.TimesheetDao",
                        "org.projectforge.framework.persistence.api.BaseDaoSupport|TimesheetDO"
                    ),
                    maxSize = 10000,
                    displayTitle = displayTitle
                )
            })
    }

    override fun customizeExcelIdentifierHeadCells(): Array<String> {
        return arrayOf("${translate("user")}|20", "${translate("timePeriod")}|25")
    }

    override fun getExcelIdentifierCells(massUpdateObject: MassUpdateObject<TimesheetDO>): List<Any?> {
        val timesheet = massUpdateObject.modifiedObj
        return listOf(timesheet!!.user?.getFullname(), timesheet.timePeriod.formattedString)
    }
}
