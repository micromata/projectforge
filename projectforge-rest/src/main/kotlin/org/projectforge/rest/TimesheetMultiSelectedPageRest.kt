/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.rest.multiselect.TextFieldModification
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import javax.servlet.http.HttpServletRequest

/**
 * Mass update after selection.
 */
@RestController
@RequestMapping("${Rest.URL}/timesheet${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class TimesheetMultiSelectedPageRest : AbstractMultiSelectedPage<TimesheetDO>() {
  @Autowired
  private lateinit var dateTimeFormatter: DateTimeFormatter

  @Autowired
  private lateinit var kost2Dao: Kost2Dao

  @Autowired
  private lateinit var taskTree: TaskTree

  @Autowired
  private lateinit var timesheetDao: TimesheetDao

  @Autowired
  private lateinit var timesheetPagesRest: TimesheetPagesRest

  override fun getTitleKey(): String {
    return "timesheet.multiselected.title"
  }

  override val listPageUrl: String = "/${MenuItemDefId.TIMESHEET_LIST.url}"

  override val pagesRestClass: Class<out AbstractPagesRest<*, *, *>>
    get() = TimesheetPagesRest::class.java

  override fun fillForm(
    request: HttpServletRequest,
    layout: UILayout,
    massUpdateData: MutableMap<String, MassUpdateParameter>,
    selectedIds: Collection<Serializable>?,
    variables: MutableMap<String, Any>,
  ) {
    var taskNode: TaskNode? = taskTree.getTaskNodeById(massUpdateData["task"]?.id)
    var kost2Id: Int? = massUpdateData["kost2"]?.id
    val timesheets = timesheetDao.getListByIds(selectedIds)
    if (taskNode == null && timesheets != null) {
      // Try to get a shared task of all time sheets.
      loop@ for (timesheet in timesheets) {
        val node = taskTree.getTaskNodeById(timesheet.taskId) ?: continue
        if (taskNode == null) {
          taskNode = node // First node
        } else if (node.isParentOf(taskNode)) {
          taskNode = node
        } else if (taskNode == node || taskNode.isParentOf(node)) {
          // OK
        } else {
          // taskNode and node aren't in same path.
          // Try to check shared ancestor:
          var ancestor = taskNode.parent
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
      // Check if all time sheets uses the same kost2:
      for (timesheet in timesheets) {
        if (timesheet.kost2Id == null) {
          // No kost2Id found
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
    }
    val lc = LayoutContext(TimesheetDO::class.java)
    val duration = timesheetDao.getListByIds(selectedIds)?.sumOf { it.getDuration() }
    val durationAsString = dateTimeFormatter.getPrettyFormattedDuration(duration ?: 0)
    layout.add(
      UIAlert(
        "'${translate("timesheet.totalDuration")}: $durationAsString",
        color = UIColor.LIGHT,
        markdown = true
      )
    )

    kost2Id?.let { kost2Id ->
      ensureMassUpdateParam(massUpdateData, "kost2").id = kost2Id
    }
    taskNode?.id?.let { taskId ->
      TaskServicesRest.createTask(taskId)?.let { task ->
        ensureMassUpdateParam(massUpdateData, "task").id = taskId
        variables["task"] = if (taskNode.isRootNode) {
          // Don't show. If task is null, the React page will not be updated from time to time (workarround)
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
      )
    )
    timesheetPagesRest.createTagUISelect(id = "tag.textValue")?.let { select ->
      layout.add(createInputFieldRow("tag", select, massUpdateData, showDeleteOption = true))
    }
    createAndAddFields(
      lc,
      massUpdateData,
      layout,
      "location",
      "reference",
      "description",
      minLengthOfTextArea = 1001, // reference has length 1.000 and description 4.000
    )
    if (Configuration.instance.isCostConfigured) {
      layout.add(UIAlert(message = "timesheet.massupdate.kost.info", color = UIColor.INFO))
    }
  }

  override fun checkParamHasAction(
    params: Map<String, MassUpdateParameter>,
    param: MassUpdateParameter,
    field: String,
    validationErrors: MutableList<ValidationError>
  ): Boolean {
    if (field == "kost2" || field == "task") {
      // No check here, action is checked on field taskAndKost2.
      return false
    }
    if (field == "taskAndKost2") {
      return param.change == true && (params["task"]?.id != null || params["kost2"]?.id != null)
    }
    return super.checkParamHasAction(params, param, field, validationErrors)
  }

  override fun handleClientMassUpdateCall(
    request: HttpServletRequest,
    massUpdateContext: MassUpdateContext<TimesheetDO>
  ) {
    val params = massUpdateContext.massUpdateData
    val kost2Id = params["kost2"]?.id
    val taskId = params["task"]?.id
    val availableKost2s = taskTree.getKost2List(taskId)
    if (kost2Id != null && availableKost2s?.any { it.id == kost2Id } != true) {
      // Due to a client bug, the kost2 id of the old project is sent, delete it, because, the project
      // was changed and kost2Id is invalid:
      params["kost2"]?.id = null
    }
  }

  override fun proceedMassUpdate(
    selectedIds: Collection<Serializable>,
    massUpdateContext: MassUpdateContext<TimesheetDO>,
  ): ResponseEntity<*>? {
    val timesheets = timesheetDao.getListByIds(selectedIds)
    if (timesheets.isNullOrEmpty()) {
      return null
    }
    val params = massUpdateContext.massUpdateData
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
      params["taskAndKost2"]?.let { param ->
        if (param.change == true) {
          if (taskId != null) {
            taskTree.getTaskById(taskId)?.let { task ->
              timesheet.task = task
            }
            if (!availableKost2s.isNullOrEmpty() && timesheet.kost2?.projekt != project) {
              // Try to find kost2 with same type (last 2 digits of projects)
              availableKost2s.find { it.kost2ArtId == timesheet.kost2?.kost2ArtId }?.let { newKost2 ->
                timesheet.kost2 = newKost2
              }
            }
          }
          if (kost2Id != null) {
            kost2Dao.internalGetById(kost2Id)?.let { kost2 ->
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
    val username = ThreadLocalUserContext.getUser().username ?: throw InternalError("User not given")
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
}
