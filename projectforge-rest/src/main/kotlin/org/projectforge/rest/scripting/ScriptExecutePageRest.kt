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

package org.projectforge.rest.scripting

import mu.KotlinLogging
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.business.scripting.ScriptParameterType
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogSubscription
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Script
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/scriptExecute")
class ScriptExecutePageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var scriptDao: ScriptDao

  @Autowired
  private lateinit var scriptExecution: ScriptExecution

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    var scriptDO: ScriptDO? = null
    val script = Script()
    val id = NumberHelper.parseInteger(idString)
    if (id != null) {
      scriptDO = scriptDao.getById(id) ?: throw IllegalArgumentException("Script not found.")
      script.copyFrom(scriptDO)
    } else {
      script.availableVariables = scriptDao.getScriptVariableNames(ScriptDO(), emptyMap<String, Any>()).joinToString()
    }
    val variables = mutableMapOf<String, Any>()
    val layout = getLayout(request, script, variables, scriptDO)
    return FormLayoutData(script, layout, createServerData(request), variables)
  }

  private fun getLayout(
    request: HttpServletRequest,
    script: Script,
    variables: MutableMap<String, Any>,
    scriptDO: ScriptDO?,
    executionResults: String? = null
  ): UILayout {
    val layout = UILayout("scripting.script.execute")
    if (scriptDO != null) {
      // DB-Script execution
      scriptExecution.updateFromRecentCall(script)?.let { task ->
        // Task must be added to variables for displaying it:
        variables["task"] = task
      }
      // Update param names
      script.parameter1?.name = scriptDO.parameter1Name
      script.parameter2?.name = scriptDO.parameter2Name
      script.parameter3?.name = scriptDO.parameter3Name
      script.parameter4?.name = scriptDO.parameter4Name
      script.parameter5?.name = scriptDO.parameter5Name
      script.parameter6?.name = scriptDO.parameter6Name
      layout.add(UIReadOnlyField("name", label = "scripting.script.name"))
      layout.add(UIReadOnlyField("description", label = "description"))
      addParameterInput(layout, script.parameter1, 1)
      addParameterInput(layout, script.parameter2, 2)
      addParameterInput(layout, script.parameter3, 3)
      addParameterInput(layout, script.parameter4, 4)
      addParameterInput(layout, script.parameter5, 5)
      addParameterInput(layout, script.parameter6, 6)
    } else {
      // Editing and executing ad-hoc script
      layout.add(UIEditor("script"))
        .add(UIReadOnlyField("availableVariables", label = "scripting.script.availableVariables"))
    }

    layout.add(
      UIButton(
        "back",
        translate("back"),
        UIColor.SUCCESS,
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(
            ScriptPagesRest::
            class.java, absolute = true
          ),
          targetType = TargetType.REDIRECT
        ),
      )
    )

    layout.add(
      UIButton(
        "execute",
        translate("execute"),
        UIColor.DANGER,
        responseAction = ResponseAction(
          url = "${getRestPath()}/execute",
          targetType = TargetType.POST
        ),
        default = true
      )
    )

    if (!executionResults.isNullOrBlank()) {
      layout.add(UIAlert(executionResults, title = "scripting.script.result", markdown = true, color = UIColor.INFO))
    }

    scriptExecution.getDownloadFile(request)?.let { download ->
      script.scriptDownload = Script.ScriptDownload(download.filename, download.sizeHumanReadable)
      val availableUntil = translateMsg("scripting.download.filename.additional", download.availableUntil)
      layout.add(
        UIRow().add(
          UIFieldset(title = "scripting.download.filename.info").add(
            UIReadOnlyField(
              "scriptDownload.filenameAndSize",
              label = "scripting.download.filename",
              additionalLabel = "'$availableUntil",
            )
          )
            .add(
              UIButton(
                id = "download", translate("download"),
                UIColor.SECONDARY,
                responseAction = ResponseAction(
                  url = "${getRestPath()}/download",
                  targetType = TargetType.DOWNLOAD
                )
              )
            )
        )
      )
    }

    if (scriptDO != null) {
      layout.add(
        MenuItem(
          "EDIT",
          i18nKey = "scripting.title.edit",
          url = PagesResolver.getEditPageUrl(
            ScriptPagesRest::
            class.java, script.id
          ),
          type = MenuItemTargetType.REDIRECT
        )
      )
    }
    layout.add(
      MenuItem(
        "logViewer",
        i18nKey = "system.admin.logViewer.title",
        url = PagesResolver.getDynamicPageUrl(
          LogViewerPageRest::
          class.java, id = ensureUserLogSubscription().id
        ),
        type = MenuItemTargetType.REDIRECT,
      )
    )

    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return layout
  }

  @PostMapping("execute")
  fun execute(
    request: HttpServletRequest,
    @Valid @RequestBody postData: PostData<Script>
  ): ResponseEntity<ResponseAction> {
    validateCsrfToken(request, postData)?.let { return it }
    val variables = mutableMapOf<String, Any>()
    val script = postData.data

    val parameters = script.getParameters()
    parameters.forEach { scriptParameter ->
      if (scriptParameter.type === ScriptParameterType.TASK) {
        scriptParameter.intValue?.let { taskId ->
          TaskServicesRest.createTask(taskId)?.let { task ->
            // Task must be added to variables for displaying it:
            variables["task"] = task
          }
        }
      }
    }
    val result = scriptExecution.execute(request, script, parameters)
    val output = StringBuilder()
    output.append("'") // ProjectForge shouldn't try to find i18n-key.
    result.exception?.let { ex ->
      output.appendLine("---") // Horizontal rule
      output.appendLine("${ex::class.java.name}:")
      output.appendLine("```") // Code
      output.appendLine(ex.message)
      output.appendLine("```") // Code
    }
    result.scriptLogger.messages.forEach { msg ->
      if (msg.level == LogLevel.ERROR) {
        msg.message?.lines()?.forEach { line ->
          if (line.isNotBlank()) {
            output.appendLine("** ${line}")
            output.appendLine()
          }
        }
      } else {
        output.appendLine(msg.message)
        output.appendLine()
      }
    }
    result.result?.let {
      if (it is String) {
        output.appendLine(it)
      }
    }
    val executionResults = output.toString()
    val scriptDO = scriptDao.getById(script.id) // null, if script.id is null.
    return ResponseEntity.ok(
      ResponseAction(targetType = TargetType.UPDATE, merge = true)
        .addVariable("data", script)
        .addVariable("ui", getLayout(request, script, variables, scriptDO, executionResults = executionResults))
        .addVariable("variables", variables)
    )
  }

  @GetMapping("download")
  fun download(request: HttpServletRequest): ResponseEntity<*> {
    val downloadFile = scriptExecution.getDownloadFile(request)
      ?: return RestUtils.badRequest(translate("scripting.download.expired"))
    log.info("Downloading '${downloadFile.filename}' of size ${downloadFile.sizeHumanReadable}.")
    return RestUtils.downloadFile(downloadFile.filename, downloadFile.bytes)
  }

  private fun addParameterInput(layout: UILayout, parameter: Script.Param?, index: Int) {
    parameter?.type ?: return
    val label = "'${parameter.name}"
    layout.add(
      when (parameter.type!!) {
        ScriptParameterType.STRING -> UIInput("parameter$index.stringValue", label = label)
        ScriptParameterType.INTEGER -> UIInput(
          "parameter$index.intValue",
          label = label, dataType = UIDataType.INT
        )
        ScriptParameterType.DECIMAL -> UIInput(
          "parameter$index.decimalValue",
          label = label, dataType = UIDataType.DECIMAL
        )
        ScriptParameterType.BOOLEAN -> UICheckbox("parameter$index.booleanValue", label = label)
        ScriptParameterType.TASK -> UIInput(
          "parameter$index.taskValue",
          label = label, dataType = UIDataType.TASK
        )
        ScriptParameterType.USER -> UISelect.createUserSelect(
          id = "parameter$index.userValue",
          label = label
        )
        ScriptParameterType.TIME_PERIOD -> UIRow()
          .add(
            UICol(4).add(
              UIInput(
                "parameter$index.dateValue",
                label = label,
                additionalLabel = "date.begin",
                dataType = UIDataType.DATE
              )
            )
          ).add(
            UICol(4).add(
              UIInput(
                "parameter$index.toDateValue",
                label = label,
                additionalLabel = "date.end",
                dataType = UIDataType.DATE
              )
            )
          )
        ScriptParameterType.DATE -> UIInput("parameter$index.dateValue", label = label, dataType = UIDataType.DATE)
      }
    )
  }

  private fun ensureUserLogSubscription(): LogSubscription {
    val username = ThreadLocalUserContext.getUser().username ?: throw InternalError("User not given")
    return LogSubscription.ensureSubscription(
      title = "Scripting",
      user = username,
      create = { title, user ->
        LogSubscription(
          title,
          user,
          LogEventLoggerNameMatcher("org.projectforge.rest.scripting", "org.projectforge.business.scripting"),
          maxSize = 10000,
        )
      })
  }
}
