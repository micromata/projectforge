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

import org.projectforge.business.PfCaches
import org.projectforge.business.gantt.GanttAccess
import org.projectforge.business.gantt.GanttChartDO
import org.projectforge.business.gantt.GanttChartSettings
import org.projectforge.business.gantt.GanttChartStyle
import org.projectforge.business.task.TaskTree
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.EntityAccessSupport
import org.projectforge.rest.dto.Task
import org.projectforge.rest.dto.User
import java.time.LocalDate

/**
 * A Gantt chart as the projectforge-next page edits it ([GanttChartDO] with its XML-backed style and
 * settings flattened to the fields the editor offers, as Wicket's `GanttChartEditForm` does).
 *
 * Copied by hand, not by the reflective [BaseDTO.copyFrom]: the DO's access fields are [GanttAccess]
 * values named `readAccess`/`writeAccess`, which would collide with [EntityAccessSupport.writeAccess]
 * (the Boolean "may the user save this"). They travel as [readAccessType]/[writeAccessType] instead.
 */
class GanttDiagram(
    var name: String? = null,
    var task: Task? = null,
    var owner: User? = null,
    var readAccessType: GanttAccess? = null,
    var writeAccessType: GanttAccess? = null,
    // GanttChartSettings:
    var title: String? = null,
    var fromDate: LocalDate? = null,
    var toDate: LocalDate? = null,
    var showOnlyVisibles: Boolean = false,
    var openNodes: List<Long>? = null,
    // GanttChartStyle:
    var width: Int? = null,
    var totalLabelWidth: Double? = null,
    var relativeTimeValues: Boolean = false,
    var showToday: Boolean = false,
    var showCompletion: Boolean = false,
    /**
     * The Gantt object tree (root = the chart's task). Filled for the edit form only, and written back on
     * save (only the values differing from the task tree are stored, see `GanttChartDao.writeGanttObjects`).
     */
    var root: GanttObject? = null,
) : BaseDTO<GanttChartDO>(), EntityAccessSupport {
    override var writeAccess: Boolean? = null
    override var deleteAccess: Boolean? = null

    override fun copyFrom(src: GanttChartDO) {
        copyFrom4ListRow(src)
        deactivated = src.deactivated
        readAccessType = src.readAccess
        writeAccessType = src.writeAccess
        src.settings?.let { settings ->
            title = settings.title
            fromDate = settings.fromDate
            toDate = settings.toDate
            showOnlyVisibles = settings.isShowOnlyVisibles
            openNodes = settings.openNodes?.mapNotNull { (it as? Number)?.toLong() }
        }
        src.style?.let { style ->
            width = style.width
            totalLabelWidth = style.totalLabelWidth
            relativeTimeValues = style.isRelativeTimeValues
            showToday = style.isShowToday
            showCompletion = style.isShowCompletion
        }
    }

    override fun copyFrom4ListRow(src: GanttChartDO) {
        id = src.id
        deleted = src.deleted
        copyAuditFieldsFrom(src)
        name = src.name
        // From the caches by the FK ids: the DO's task and owner are lazy.
        task = TaskTree.instance.getTaskById(src.taskId)?.let {
            Task(id = it.id, displayName = it.displayName, title = it.title).also { dto -> dto.deleted = it.deleted }
        }
        owner = PfCaches.instance.getUser(src.ownerId)?.let {
            User(id = it.id, displayName = it.displayName).also { dto ->
                dto.deleted = it.deleted
                dto.deactivated = it.deactivated
            }
        }
    }

    /**
     * Copies everything but task, owner and the Gantt object tree, which need the DAOs (see
     * [GanttChartEntityRest.transformForDB]). [style] and [settings] are the stored ones of an existing
     * chart, so the style values this form doesn't offer (x unit, scales, ...) survive.
     */
    fun copyTo(dest: GanttChartDO, style: GanttChartStyle, settings: GanttChartSettings) {
        dest.id = id
        dest.deleted = deleted
        dest.name = name
        dest.readAccess = readAccessType
        dest.writeAccess = writeAccessType
        // Explicit setters: the Java ones are fluent (return this), so Kotlin offers no property syntax.
        settings.setTitle(title)
        settings.setFromDate(fromDate)
        settings.setToDate(toDate)
        settings.setShowOnlyVisibles(showOnlyVisibles)
        settings.setOpenNodes(openNodes?.toMutableSet<java.io.Serializable>() ?: mutableSetOf())
        width?.let { style.setWidth(it) }
        totalLabelWidth?.let { style.setTotalLabelWidth(it) }
        style.setRelativeTimeValues(relativeTimeValues)
        style.setShowToday(showToday)
        style.setShowCompletion(showCompletion)
        dest.style = style
        dest.settings = settings
    }
}
