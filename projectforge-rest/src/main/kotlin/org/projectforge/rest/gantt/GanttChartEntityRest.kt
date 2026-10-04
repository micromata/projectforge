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

import jakarta.servlet.http.HttpServletRequest
import java.math.BigDecimal
import org.projectforge.business.gantt.GanttAccess
import org.projectforge.business.gantt.GanttChartDO
import org.projectforge.business.gantt.GanttChartDao
import org.projectforge.business.gantt.GanttChartSettings
import org.projectforge.business.gantt.GanttChartStyle
import org.projectforge.business.task.TaskDao
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AbstractEntityRest.CloneSupport
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the Gantt charts, serving the hand-built projectforge-next page (see
 * components/features/gantt). Replaces Wicket's `GanttChartListPage`/`GanttChartEditPage`; the chart
 * preview, the exports and the task mutations of the tree editor live in [GanttServicesRest].
 *
 * The edit DTO carries the whole Gantt object tree ([GanttDiagram.root]), so the editor works on the
 * unsaved state and saving writes the tree like Wicket's `GanttChartEditPage.onSaveOrUpdate`.
 */
@RestController
@RequestMapping("${Rest.URL}/gantt")
class GanttChartEntityRest :
    AbstractDTOEntityRest<GanttChartDO, GanttDiagram, GanttChartDao>(
        GanttChartDao::class.java,
        "gantt.title",
        cloneSupport = CloneSupport.CLONE,
    ) {
    @Autowired
    private lateinit var taskDao: TaskDao

    companion object {
        /** The name of a new chart (Wicket's `GanttChartEditForm`). */
        const val DEFAULT_NAME = "MyChart"

        const val TITLE_MAX_LENGTH = 100

        /** The bound of an activity's duration (Wicket's `TaskEditForm.MAX_DURATION_DAYS`). */
        const val MAX_DURATION_DAYS = 10000
    }

    override fun newDTO(): GanttDiagram = GanttDiagram()

    override fun transformFromDB(obj: GanttChartDO, editMode: Boolean): GanttDiagram {
        val dto = GanttDiagram()
        dto.copyFrom(obj)
        if (editMode) {
            dto.root = readTree(obj, isNew = obj.id == null)
        }
        return dto
    }

    override fun transformForDB(dto: GanttDiagram): GanttChartDO {
        val obj = GanttChartDO()
        // The stored style and settings of an existing chart, so the values this form doesn't offer survive.
        val stored = dto.id?.let { baseDao.find(it, checkAccess = false) }
        dto.copyTo(obj, stored?.style ?: GanttChartStyle(), stored?.settings ?: GanttChartSettings())
        dto.task?.id?.let { setCheckedTask(obj, it) }
        ownerToSet(dto.owner?.id, stored)?.let { baseDao.setOwner(obj, it) }
        val root = dto.root
        if (root != null && obj.task != null) {
            baseDao.writeGanttObjects(obj, GanttObjectConverter.fromDTO(root).rootObject)
        } else {
            obj.ganttObjectsAsXml = stored?.ganttObjectsAsXml
        }
        return obj
    }

    /**
     * A new chart is owned by the logged-in user and readable/writable by them only, for the task given by
     * the `task` parameter (the task page's "Gantt" link), and named "MyChart", as in Wicket.
     */
    override fun newBaseDO(request: HttpServletRequest?): GanttChartDO {
        val obj = baseDao.newInstance()
        obj.name = DEFAULT_NAME
        obj.readAccess = GanttAccess.OWNER
        obj.writeAccess = GanttAccess.OWNER
        baseDao.setOwner(obj, ThreadLocalUserContext.loggedInUserId!!)
        NumberHelper.parseLong(request?.getParameter("task"))?.let { setCheckedTask(obj, it) }
        return obj
    }

    /**
     * Sets the task of [obj] if the logged-in user may see it: the chart's tree is read from the task's
     * subtree (titles, dates), which must not be readable for an arbitrary task id.
     */
    private fun setCheckedTask(obj: GanttChartDO, taskId: Long) {
        val task = taskDao.find(taskId) ?: return
        taskDao.hasLoggedInUserSelectAccess(task, true)
        baseDao.setTask(obj, taskId)
    }

    /**
     * The owner a saved chart gets: only the current owner (the logged-in user for a new chart) or an admin
     * may hand a chart to someone else; otherwise the stored owner (or the logged-in user) is kept.
     */
    private fun ownerToSet(requestedOwnerId: Long?, stored: GanttChartDO?): Long? {
        val loggedInUserId = ThreadLocalUserContext.loggedInUserId
        val currentOwnerId = if (stored != null) stored.ownerId else loggedInUserId
        if (requestedOwnerId == null || requestedOwnerId == currentOwnerId) {
            return requestedOwnerId ?: currentOwnerId
        }
        return if (currentOwnerId == loggedInUserId || accessChecker.isLoggedInUserMemberOfAdminGroup) {
            requestedOwnerId
        } else {
            currentOwnerId
        }
    }

    override fun newBaseDTO(request: HttpServletRequest?): GanttDiagram {
        return transformFromDB(newBaseDO(request), true)
    }

    override fun prepareClone(dto: GanttDiagram): GanttDiagram {
        super.prepareClone(dto)
        dto.writeAccess = null
        dto.deleteAccess = null
        return dto
    }

    /**
     * The rules of Wicket's `GanttChartEditForm` (task, name and title required, the title at most
     * [TITLE_MAX_LENGTH] characters) and of its tree table (`GanttChartEditTreeTablePanel`: every activity
     * titled, duration and progress bounded).
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: GanttDiagram) {
        super.validate(validationErrors, dto)
        if (dto.task?.id == null) {
            validationErrors.add(required("task", "task"))
        }
        if (dto.name.isNullOrBlank()) {
            validationErrors.add(required("name", "gantt.name"))
        }
        if (dto.title.isNullOrBlank()) {
            validationErrors.add(required("title", "title"))
        } else if (dto.title!!.length > TITLE_MAX_LENGTH) {
            validationErrors.add(
                ValidationError(
                    translateMsg("validation.error.maxLength", translate("title"), TITLE_MAX_LENGTH),
                    fieldId = "title",
                )
            )
        }
        dto.root?.children?.forEach { validateTree(it, validationErrors) }
    }

    private fun required(fieldId: String, labelKey: String) =
        ValidationError(translateMsg("validation.error.fieldRequired", translate(labelKey)), fieldId = fieldId)

    /**
     * The values of the tree have no form field of their own, so their errors are field-less (shown as a
     * toast), each naming the activity and the column.
     */
    private fun validateTree(node: GanttObject, validationErrors: MutableList<ValidationError>) {
        val activity = node.title?.takeIf { it.isNotBlank() }
        if (activity == null) {
            validationErrors.add(ValidationError(translateMsg("validation.error.fieldRequired", translate("title"))))
        }
        fun outOfRange(labelKey: String, min: Int, max: Int) {
            val message = translateMsg("validation.error.range.integerOutOfRange", min, max)
            validationErrors.add(ValidationError("${activity ?: "?"}, ${translate(labelKey)}: $message"))
        }
        node.duration?.let {
            if (it < BigDecimal.ZERO || it > BigDecimal(MAX_DURATION_DAYS)) {
                outOfRange("gantt.duration", 0, MAX_DURATION_DAYS)
            }
        }
        node.progress?.let {
            if (it < 0 || it > 100) {
                outOfRange("task.progress", 0, 100)
            }
        }
        node.children?.forEach { validateTree(it, validationErrors) }
    }

    /**
     * The Gantt object tree of [obj]'s task, overlaid with the chart's stored deviations. Of a new chart the
     * first level is made visible, as Wicket's `GanttChartEditPage.refresh` does.
     */
    internal fun readTree(obj: GanttChartDO, isNew: Boolean): GanttObject? {
        obj.task ?: return null
        val root = baseDao.readGanttObjects(obj).rootObject ?: return null
        if (isNew) {
            root.children?.forEach { it.setVisible(true) }
        }
        return GanttObjectConverter.toDTO(root, taskDao)
    }
}
