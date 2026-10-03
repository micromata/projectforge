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

package org.projectforge.rest.gantt

import org.projectforge.business.gantt.ExportMSProject
import org.projectforge.business.gantt.GanttChart
import org.projectforge.business.gantt.GanttChartDO
import org.projectforge.business.gantt.GanttChartDao
import org.projectforge.business.gantt.GanttChartSettings
import org.projectforge.business.gantt.GanttChartStyle
import org.projectforge.business.gantt.GanttTask
import org.projectforge.business.gantt.GanttUtils
import org.projectforge.business.gantt.Task2GanttTaskConverter
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree
import org.projectforge.common.FilenameUtils
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.renderer.BatikImageRenderer
import org.projectforge.framework.renderer.ImageFormat
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AccessChecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

/**
 * The services of the projectforge-next Gantt editor beside the CRUD of [GanttChartEntityRest]: the chart
 * (rendered by Batik on the server, of the unsaved state), the exports and the task mutations of the tree
 * editor, ported from Wicket's `GanttChartEditPage` and `GanttChartEditTreeTablePanel`.
 */
@RestController
@RequestMapping("${Rest.URL}/ganttServices")
class GanttServicesRest {
    @Autowired
    private lateinit var ganttChartDao: GanttChartDao

    @Autowired
    private lateinit var taskDao: TaskDao

    @Autowired
    private lateinit var taskTree: TaskTree

    class CalculatedDates(val start: LocalDate?, val end: LocalDate?)

    /**
     * @param svg The chart as SVG, null if there is nothing to draw (no activities).
     * @param dates The calculated start and end date of every node by its id.
     */
    class Preview(val svg: String?, val dates: Map<Long, CalculatedDates>)

    enum class ExportFormat(val suffix: String, val imageFormat: ImageFormat? = null) {
        PDF(".pdf", ImageFormat.PDF),
        PNG(".png", ImageFormat.PNG),
        JPG(".jpg", ImageFormat.JPEG),
        SVG(".svg", ImageFormat.SVG),
        MS_PROJECT_MPX(".mpx"),
        MS_PROJECT_XML(".xml"),
        PROJECTFORGE(".xml"),
    }

    /** A field of a Gantt object that the editor may write back to its task. */
    enum class TaskField { TITLE, START_DATE, END_DATE, DURATION, PROGRESS, PREDECESSOR, PREDECESSOR_OFFSET, RELATION_TYPE, TYPE }

    class SaveToTaskData(var node: GanttObject? = null, var field: TaskField? = null)

    class MoveTaskData(var taskId: Long? = null, var parentTaskId: Long? = null)

    class SaveAsTaskData(var node: GanttObject? = null, var parentTaskId: Long? = null)

    class SaveAsTaskResult(val id: Long, val task: GanttObject.TaskValues)

    /**
     * The plain Gantt object tree of a task, for a chart whose task was just chosen (the first level visible,
     * as Wicket shows a new chart).
     */
    @AccessChecked("Task select access (TaskDao)")
    @GetMapping("objects")
    fun objects(@RequestParam("taskId") taskId: Long): GanttObject? {
        val task = taskTree.getTaskById(taskId) ?: return null
        taskDao.hasLoggedInUserSelectAccess(task, true)
        val root = Task2GanttTaskConverter.convertToGanttObjectTree(taskTree, task).rootObject ?: return null
        root.children?.forEach { it.setVisible(true) }
        return GanttObjectConverter.toDTO(root, taskDao)
    }

    @AccessChecked("Task select access per node + GanttChartDao.find")
    @PostMapping("preview")
    fun preview(@RequestBody dto: GanttDiagram): Preview {
        val chart = createChart(dto) ?: return Preview(null, emptyMap())
        val document = chart.create() // Recalculates all dates.
        val svg = document?.let { String(BatikImageRenderer.getByteArray(it, chart.width, ImageFormat.SVG), Charsets.UTF_8) }
        val dates = mutableMapOf<Long, CalculatedDates>()
        collectDates(chart.rootNode, dates)
        return Preview(svg, dates)
    }

    /**
     * The posted chart as a file of the given format. 404 if there is nothing to draw, which the frontend
     * reports as such (the same convention as the list exports).
     */
    @AccessChecked("Task select access per node + GanttChartDao.find")
    @PostMapping("export")
    fun export(@RequestParam("format") format: ExportFormat, @RequestBody dto: GanttDiagram): ResponseEntity<Resource> {
        val chart = createChart(dto) ?: return ResponseEntity.notFound().build()
        val filename = FilenameUtils.createSafeFilename(dto.name ?: "gantt", format.suffix, 50, true)
        val content = format.imageFormat?.let { imageFormat ->
            val document = chart.create() ?: return ResponseEntity.notFound().build()
            BatikImageRenderer.getByteArray(document, chart.width, imageFormat)
        } ?: when (format) {
            ExportFormat.MS_PROJECT_MPX -> ExportMSProject.exportMpx(chart)
            ExportFormat.MS_PROJECT_XML -> ExportMSProject.exportXml(chart)
            else -> ganttChartDao.exportAsXml(chart, true).toByteArray()
        }
        return RestUtils.downloadFile(filename, content)
    }

    /**
     * Writes one value of a Gantt object back to its task (the "save" button beside a differing value).
     * @return The task's values after the update.
     */
    @AccessChecked("DAO: TaskDao find/insert/update access")
    @PostMapping("saveToTask")
    fun saveToTask(@RequestBody data: SaveToTaskData): GanttObject.TaskValues {
        val node = requireNotNull(data.node) { "node required" }
        val task = requireNotNull(taskDao.find(node.id)) { "No task with id ${node.id}" }
        when (requireNotNull(data.field) { "field required" }) {
            TaskField.TITLE -> task.title = node.title
            TaskField.START_DATE -> task.startDate = node.startDate
            TaskField.END_DATE -> task.endDate = node.endDate
            TaskField.DURATION -> task.duration = node.duration
            TaskField.PROGRESS -> task.progress = node.progress
            TaskField.PREDECESSOR -> {
                val predecessorId = node.predecessorId
                if (predecessorId != null && predecessorId > 0) {
                    taskDao.setGanttPredecessor(task, predecessorId)
                } else {
                    task.ganttPredecessor = null
                }
            }
            TaskField.PREDECESSOR_OFFSET -> task.ganttPredecessorOffset = node.predecessorOffset
            TaskField.RELATION_TYPE -> task.ganttRelationType = node.relationType
            TaskField.TYPE -> task.ganttObjectType = node.type
        }
        taskDao.update(task)
        return GanttObjectConverter.toTaskValues(task, taskDao)
    }

    /**
     * Moves a task below another one ("move here", or "move to top" with the chart's task as parent).
     */
    @AccessChecked("DAO: TaskDao find/insert/update access")
    @PostMapping("moveTask")
    fun moveTask(@RequestBody data: MoveTaskData) {
        val task = requireNotNull(taskDao.find(data.taskId)) { "No task with id ${data.taskId}" }
        taskDao.setParentTask(task, requireNotNull(data.parentTaskId) { "parentTaskId required" })
        taskDao.update(task)
    }

    /**
     * Saves a Gantt-only activity as a new task below [SaveAsTaskData.parentTaskId].
     * @return The id of the new task, which becomes the activity's id.
     */
    @AccessChecked("DAO: TaskDao find/insert/update access")
    @PostMapping("saveAsTask")
    fun saveAsTask(@RequestBody data: SaveAsTaskData): SaveAsTaskResult {
        val node = requireNotNull(data.node) { "node required" }
        val parentTask = data.parentTaskId?.takeIf { it > 0 }?.let { taskTree.getTaskById(it) }
            ?: throw UserException("gantt.error.parentObjectIsNotAPFTask")
        val ganttObject = GanttObjectConverter.fromDTO(node.also { it.children = null }).rootObject!!
        val task = Task2GanttTaskConverter.convertToTask(ganttObject)
        task.parentTask = parentTask
        node.predecessorId?.takeIf { it > 0 }?.let { taskTree.getTaskById(it) }?.let { task.ganttPredecessor = it }
        val id = taskDao.insert(task)
        return SaveAsTaskResult(id, GanttObjectConverter.toTaskValues(task, taskDao))
    }

    /**
     * The chart of the posted, unsaved state. Every task the tree names has to be readable by the user, so
     * a hand-made request can't render (or export) tasks the user may not see.
     */
    private fun createChart(dto: GanttDiagram): GanttChart? {
        checkTaskAccess(dto.root)
        val root = GanttObjectConverter.fromDTO(dto.root).rootObject ?: return null
        root.sortChildren()
        val stored = dto.id?.let { ganttChartDao.find(it) }
        val obj = GanttChartDO()
        dto.copyTo(obj, stored?.style ?: GanttChartStyle(), stored?.settings ?: GanttChartSettings())
        return GanttChart(root, obj.style, obj.settings, dto.name)
    }

    private fun checkTaskAccess(node: GanttObject?) {
        node ?: return
        listOf(node.id, node.predecessorId).forEach { id ->
            if (id != null && id > 0) {
                taskTree.getTaskById(id)?.let { taskDao.hasLoggedInUserSelectAccess(it, true) }
            }
        }
        node.children?.forEach { checkTaskAccess(it) }
    }

    private fun collectDates(node: GanttTask, dates: MutableMap<Long, CalculatedDates>) {
        (node.id as? Long)?.let {
            dates[it] = CalculatedDates(GanttUtils.getCalculatedStartDate(node), GanttUtils.getCalculatedEndDate(node))
        }
        node.children?.forEach { collectDates(it, dates) }
    }
}
