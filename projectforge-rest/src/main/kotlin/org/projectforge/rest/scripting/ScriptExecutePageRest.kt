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

import de.micromata.merlin.excel.ExcelWorkbook
import mu.KotlinLogging
import org.projectforge.business.excel.ExportWorkbook
import org.projectforge.business.scripting.*
import org.projectforge.business.scripting.xstream.RecentScriptCalls
import org.projectforge.business.scripting.xstream.ScriptCallData
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.logging.LogEventLoggerNameMatcher
import org.projectforge.common.logging.LogLevel
import org.projectforge.common.logging.LogSubscription
import org.projectforge.export.ExportJFreeChart
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Script
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.*
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/scriptExecute")
class ScriptExecutePageRest : AbstractDynamicPageRest() {
  @Autowired
  private lateinit var scriptDao: ScriptDao

  @Autowired
  private lateinit var userPrefService: UserPrefService

  @GetMapping("dynamic")
  fun getForm(request: HttpServletRequest, @RequestParam("id") idString: String?): FormLayoutData {
    val id = NumberHelper.parseInteger(idString) ?: throw IllegalArgumentException("id not given.")
    val scriptDO = scriptDao.getById(id) ?: throw IllegalArgumentException("Script not found.")
    val script = Script()
    script.copyFrom(scriptDO)

    val variables = mutableMapOf<String, Any>()
    userPrefService.getEntry(USER_PREF_AREA, USER_PREF_KEY, RecentScriptCalls::class.java)
      ?.getScriptCallData("${script.id}")?.let { scriptCallData ->
        scriptCallData.scriptParameter?.forEachIndexed { index, scriptParameter ->
          script.updateParameter(index, scriptParameter)
          if (scriptParameter.type === ScriptParameterType.TASK) {
            scriptParameter.intValue?.let { taskId ->
              TaskServicesRest.createTask(taskId)?.let { task ->
                // Task must be added to variables for displaying it:
                variables["task"] = task
              }
            }
          }
        }
      }
    // Update param names
    script.parameter1?.name = scriptDO.parameter1Name
    script.parameter2?.name = scriptDO.parameter2Name
    script.parameter3?.name = scriptDO.parameter3Name
    script.parameter4?.name = scriptDO.parameter4Name
    script.parameter5?.name = scriptDO.parameter5Name
    script.parameter6?.name = scriptDO.parameter6Name

    val layout = UILayout("scripting.script.execute")
    layout.add(UIReadOnlyField("name", label = "scripting.script.name"))
    layout.add(UIReadOnlyField("description", label = "description"))
    addParameterInput(layout, script.parameter1, 1)
    addParameterInput(layout, script.parameter2, 2)
    addParameterInput(layout, script.parameter3, 3)
    addParameterInput(layout, script.parameter4, 4)
    addParameterInput(layout, script.parameter5, 5)
    addParameterInput(layout, script.parameter6, 6)
    layout.add(
      UIButton(
        "back",
        translate("back"),
        UIColor.SUCCESS,
        responseAction = ResponseAction(
          PagesResolver.getListPageUrl(ScriptPagesRest::class.java, absolute = true),
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

    layout.add(UIAlert(id = "results", title = "scripting.script.result", markdown = true, color = UIColor.INFO))
    script.results = "---"

    layout.add(
      MenuItem(
        "EDIT",
        i18nKey = "scripting.title.edit",
        url = PagesResolver.getEditPageUrl(ScriptPagesRest::class.java, id),
        type = MenuItemTargetType.REDIRECT
      )
    )
      .add(
        MenuItem(
          "logViewer",
          i18nKey = "plugins.merlin.viewLogs",
          url = PagesResolver.getDynamicPageUrl(LogViewerPageRest::class.java, id = ensureUserLogSubscription().id),
          type = MenuItemTargetType.REDIRECT,
        )
      )

    LayoutUtils.process(layout)
    layout.postProcessPageMenu()
    return FormLayoutData(script, layout, createServerData(request), variables)
  }

  @PostMapping("execute")
  fun execute(@Valid @RequestBody postData: PostData<Script>): ResponseAction {
    val variables = mutableMapOf<String, Any?>()
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
    val result = execute(script, parameters)
    val output = StringBuilder()
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
    script.results = output.toString()
    return ResponseAction(targetType = TargetType.UPDATE, merge = true)
      .addVariable("data", script)
      .addVariable("variables", variables)
  }

  private fun execute(script: Script, parameters: List<ScriptParameter>): ScriptExecutionResult {
    log.info {
      "Execute script '${script.name}' with params: ${
        parameters.filter { it.parameterName != null }.joinToString { it.asString }
      }"
    }

    // Store as recent script call params:
    val recentScriptCalls = userPrefService.ensureEntry(USER_PREF_AREA, USER_PREF_KEY, RecentScriptCalls())
    val scriptCallData = ScriptCallData("${script.id}", parameters)
    recentScriptCalls.append(scriptCallData)

    val scriptDO = scriptDao.getById(script.id)
    val scriptExecutionResult = scriptDao.execute(scriptDO, parameters)
    if (scriptExecutionResult.hasException()) {
      scriptExecutionResult.scriptLogger.error(scriptExecutionResult.exception.toString())
      return scriptExecutionResult
    }
    scriptExecutionResult.result?.let { result ->
      when (result) {
        is ExportWorkbook -> {
          exportExcel(result, scriptExecutionResult)
        }
        is ExcelWorkbook -> {
          exportExcel(result, scriptExecutionResult)
        }
        is ExportJFreeChart -> {
          exportJFreeChart(result, scriptExecutionResult)
        }
        is ExportZipArchive -> {
          exportZipArchive(result, scriptExecutionResult)
        }
        is ExportJson -> {
          exportJson(result, scriptExecutionResult)
        }
      }
    }
    return scriptExecutionResult
  }

  private fun exportExcel(workbook: ExportWorkbook, scriptExecutionResult: ScriptExecutionResult) {
    val buf = StringBuffer()
    if (workbook.filename != null) {
      buf.append(workbook.filename).append("_")
    } else {
      buf.append("pf_scriptresult_")
    }
    buf.append(DateHelper.getTimestampAsFilenameSuffix(Date())).append(".xls")
    val filename = buf.toString()
    val xls = workbook.asByteArray
    if (xls == null || xls.size == 0) {
      scriptExecutionResult.scriptLogger.error("Oups, xls has zero size. Filename: $filename")
      return
    }
    //DownloadUtils.setDownloadTarget(xls, filename)
  }

  private fun exportExcel(workbook: ExcelWorkbook, scriptExecutionResult: ScriptExecutionResult) {
    try {
      val buf = StringBuffer()
      if (workbook.filename != null) {
        buf.append(workbook.filenameWithoutExtension).append("_")
      } else {
        buf.append("pf_scriptresult_")
      }
      buf.append(DateHelper.getTimestampAsFilenameSuffix(Date())).append(".").append(workbook.filenameExtension)
      val filename = buf.toString()
      val xls = workbook.asByteArrayOutputStream.toByteArray()
      if (xls == null || xls.size == 0) {
        scriptExecutionResult.scriptLogger.error("Oups, xls has zero size. Filename: $filename")
        return
      }
      //DownloadUtils.setDownloadTarget(xls, filename)
    } finally {
      workbook.close()
    }
  }

  private fun exportJFreeChart(exportJFreeChart: ExportJFreeChart, scriptExecutionResult: ScriptExecutionResult) {
    val sb = StringBuilder()
    sb.append("pf_chart_")
    sb.append(DateHelper.getTimestampAsFilenameSuffix(Date()))
    val out = ByteArrayOutputStream()
    val extension = exportJFreeChart.write(out)
    sb.append('.').append(extension)
    //DownloadUtils.setDownloadTarget(out.toByteArray(), sb.toString())
  }

  private fun exportZipArchive(exportZipArchive: ExportZipArchive, scriptExecutionResult: ScriptExecutionResult) {
    try {
      val filename = "${exportZipArchive.filename}_${DateHelper.getTimestampAsFilenameSuffix(Date())}.zip"
      val file = File(filename)
      file.outputStream().use { out ->
        exportZipArchive.write(out)
      }
      log.info { "File ${file.absolutePath} written." }
      //DownloadUtils.setDownloadTarget(filename, ScriptingHelper.createResourceStreamWriter(exportZipArchive))
    } catch (ex: Exception) {
      scriptExecutionResult.exception = ex
      log.error(ex.message, ex)
    }
  }

  private fun exportJson(exportJson: ExportJson, scriptExecutionResult: ScriptExecutionResult) {
    /*try {
      val sb = StringBuilder()
      sb.append(exportJson.jsonName).append("_")
      sb.append(DateHelper.getTimestampAsFilenameSuffix(Date())).append(".json")
      val filename = sb.toString()
      DownloadUtils.setDownloadTarget(filename, ScriptingHelper.createResourceStreamWriter(exportJson))
    } catch (ex: java.lang.Exception) {
      error(getLocalizedMessage("error", ex.message))
      log.error(ex.message, ex)
    }*/
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

  private val USER_PREF_AREA = "ScriptExecution:"
  private val USER_PREF_KEY = "recentCalls"
}
