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

import org.projectforge.business.gantt.GanttChartData
import org.projectforge.business.gantt.GanttTask
import org.projectforge.business.gantt.GanttTaskImpl
import org.projectforge.business.gantt.GanttUtils
import org.projectforge.business.task.TaskDO
import org.projectforge.business.task.TaskDao
import org.projectforge.business.task.TaskTree

/**
 * Converts between the Gantt object tree of the business layer ([GanttTask]) and the [GanttObject]
 * tree of the projectforge-next editor.
 */
object GanttObjectConverter {
    /**
     * @param taskDao For the per task update access (the reject/save buttons), null to skip it.
     * @param withCalculatedDates Whether to add the calculated dates (call after a recalculation).
     */
    fun toDTO(node: GanttTask, taskDao: TaskDao?, withCalculatedDates: Boolean = false): GanttObject {
        val dto = GanttObject(
            id = node.id as? Long,
            title = node.title,
            startDate = node.startDate,
            endDate = node.endDate,
            duration = node.duration,
            progress = node.progress,
            predecessorId = node.predecessorId as? Long,
            predecessorOffset = node.predecessorOffset,
            relationType = node.relationType,
            type = node.type,
            workpackageCode = node.workpackageCode,
            visible = node.isVisible,
        )
        dto.predecessorTitle = node.predecessor?.title
        dto.task = getTask(dto.id)?.let { task -> toTaskValues(task, taskDao) }
        if (withCalculatedDates) {
            dto.calculatedStartDate = GanttUtils.getCalculatedStartDate(node)
            dto.calculatedEndDate = GanttUtils.getCalculatedEndDate(node)
        }
        node.children?.let { children ->
            dto.children = children.map { toDTO(it, taskDao, withCalculatedDates) }.toMutableList()
        }
        return dto
    }

    fun toTaskValues(task: TaskDO, taskDao: TaskDao?): GanttObject.TaskValues {
        return GanttObject.TaskValues(
            title = task.title,
            startDate = task.startDate,
            endDate = task.endDate,
            duration = task.duration,
            progress = task.progress,
            predecessorId = task.ganttPredecessorId,
            predecessorTitle = getTask(task.ganttPredecessorId)?.title,
            predecessorOffset = task.ganttPredecessorOffset,
            relationType = task.ganttRelationType,
            type = task.ganttObjectType,
            updateAccess = taskDao?.hasLoggedInUserUpdateAccess(task, task, false) == true,
        )
    }

    /**
     * Rebuilds the business tree. A predecessor is looked up in the tree first and else, as
     * `GanttChartDao.readGanttObjects` does, as an external object of the ProjectForge task tree.
     *
     * @return The chart data with the root object (null for a null root) and the external predecessors.
     */
    fun fromDTO(root: GanttObject?): GanttChartData {
        val data = GanttChartData()
        root ?: return data
        val nodes = mutableMapOf<Long, GanttTaskImpl>()
        val rootNode = build(root, nodes)
        data.rootObject = rootNode
        resolvePredecessors(root, nodes, data)
        return data
    }

    private fun build(dto: GanttObject, nodes: MutableMap<Long, GanttTaskImpl>): GanttTaskImpl {
        val node = GanttTaskImpl(dto.id)
        node.setTitle(dto.title)
            .setStartDate(dto.startDate)
            .setEndDate(dto.endDate)
            .setDuration(dto.duration)
            .setProgress(dto.progress)
            .setPredecessorOffset(dto.predecessorOffset)
            .setRelationType(dto.relationType)
            .setType(dto.type)
            .setWorkpackageCode(dto.workpackageCode)
        node.setVisible(dto.visible)
        // Not edited here, but needed by the MS Project export:
        getTask(dto.id)?.let { node.setDescription(it.description) }
        dto.id?.let { nodes[it] = node }
        dto.children?.forEach { node.addChild(build(it, nodes)) }
        return node
    }

    private fun resolvePredecessors(dto: GanttObject, nodes: Map<Long, GanttTaskImpl>, data: GanttChartData) {
        dto.predecessorId?.let { predecessorId ->
            val predecessor = nodes[predecessorId]
                ?: data.ensureAndGetExternalGanttObject(getTask(predecessorId))
            dto.id?.let { nodes[it] }?.setPredecessor(predecessor)
        }
        dto.children?.forEach { resolvePredecessors(it, nodes, data) }
    }

    private fun getTask(id: Long?): TaskDO? {
        if (id == null || id <= 0) {
            return null
        }
        return TaskTree.instance.getTaskById(id)
    }
}
