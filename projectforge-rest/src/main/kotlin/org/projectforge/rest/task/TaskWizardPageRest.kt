/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.Task
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import jakarta.servlet.http.HttpServletRequest

@RestController
@RequestMapping("${Rest.URL}/taskWizard")
class TaskWizardPageRest : AbstractDynamicPageRest() {
  class Data(
    var task: Task? = null,
    var group: Group? = null,
    var managerGroup: Group? = null,
  )

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("taskId") taskId: Long?): FormLayoutData {
    val layout = UILayout("task.wizard.pageTitle")
    layout.add(UIAlert("To-do: watchfields, create new entities, show no action", color = UIColor.DANGER))
    layout.add(UIAlert("task.wizard.intro", title = "wizard", color = UIColor.INFO))
    UIFieldset().let { fieldset ->
      layout.add(fieldset)
      fieldset.add(UIAlert("task.wizard.task.intro", color = UIColor.LIGHT))
      fieldset.add(UIInput("task", label = "task", dataType = UIDataType.TASK))
    }
    layout.add(
      UICol()
        .add(
          UIRow()
            .add(
              UIFieldset(title = "task.wizard.managerGroup", md = 6)
                .add(UIAlert("task.wizard.managerGroup.intro", color = UIColor.LIGHT))
                .add(UIInput("managerGroup", label = "fibu.projekt.projektManagerGroup", dataType = UIDataType.GROUP))
            )
            .add(
              UIFieldset(title = "task.wizard.team", md = 6)
                .add(UIAlert("task.wizard.team.intro", color = UIColor.LIGHT))
                .add(UIInput("group", label = "group", dataType = UIDataType.GROUP))

            )
        )
    )
    layout.add(UIAlert("task.wizard.action.noactionRequired", title = "task.wizard.action", color = UIColor.WARNING))
    layout.add(
      UIButton.createCancelButton(responseAction = ResponseAction("/wa/taskTree", targetType = TargetType.REDIRECT))
    ).add(
      UIButton.createDefaultButton(
        "execute",
        title = "task.wizard.finish",
        responseAction = ResponseAction(
          RestResolver.getRestUrl(
            this::class.java,
            subPath = "execute"
          ), targetType = TargetType.POST
        ),
      )
    )
    val data = Data(task = Task.getTask(taskId))
    LayoutUtils.process(layout)
    return FormLayoutData(data, layout, createServerData(request))
  }

}
