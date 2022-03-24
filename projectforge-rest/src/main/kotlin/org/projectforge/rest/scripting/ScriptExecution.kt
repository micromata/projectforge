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
import org.projectforge.common.DateFormatType
import org.projectforge.export.ExportJFreeChart
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.dto.Script
import org.projectforge.rest.task.TaskServicesRest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.time.temporal.ChronoUnit
import java.util.*
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger {}

@Controller
class ScriptExecution {
  @Autowired
  private lateinit var userPrefService: UserPrefService

  @Autowired
  private lateinit var attachmentsService: AttachmentsService

  /**
   * @return task, if some task of recent call was found (for updating ui variables)
   */
  internal fun updateFromRecentCall(script: Script): TaskServicesRest.Task? {
    val scriptCallData = userPrefService.getEntry(USER_PREF_AREA, USER_PREF_KEY, RecentScriptCalls::class.java)
      ?.getScriptCallData("${script.id}")
    var task: TaskServicesRest.Task? = null
    scriptCallData?.let { scriptCallData ->
      scriptCallData.scriptParameter?.forEachIndexed { index, scriptParameter ->
        script.updateParameter(index, scriptParameter)
        if (scriptParameter.type == ScriptParameterType.TASK) {
          scriptParameter.intValue?.let { taskId ->
            // Task must be added to variables for displaying it:
            task = TaskServicesRest.createTask(taskId)
          }
        }
      }
    }
    return task
  }

  internal fun getEffectiveScript(
    script: Script,
    parameters: List<ScriptParameter>,
    scriptDao: AbstractScriptDao,
    scriptPagesRest: AbstractPagesRest<*, *, *>,
  ): String {
    val initData = prepareScriptInit(script, scriptDao, scriptPagesRest)
    return scriptDao.getEffectiveScript(initData.scriptDO, parameters, initData.additionalVariables, initData.myImports)
  }

  internal fun getVariableNames(
    script: Script,
    parameters: List<ScriptParameter>,
    scriptDao: AbstractScriptDao,
    scriptPagesRest: AbstractPagesRest<*, *, *>,
  ): List<String> {
    val initData = prepareScriptInit(script, scriptDao, scriptPagesRest)
    return scriptDao.getScriptVariableNames(
      initData.scriptDO,
      parameters,
      initData.additionalVariables,
      initData.myImports
    )
  }

  internal fun execute(
    request: HttpServletRequest,
    script: Script,
    parameters: List<ScriptParameter>,
    scriptDao: AbstractScriptDao,
    scriptPagesRest: AbstractPagesRest<*, *, *>,
  ): ScriptExecutionResult {
    log.info {
      "Execute script '${script.name}' with params: ${
        parameters.filter { it.parameterName != null }.joinToString { it.asString }
      }"
    }
    // Store as recent script call params:
    val recentScriptCalls = userPrefService.ensureEntry(USER_PREF_AREA, USER_PREF_KEY, RecentScriptCalls())
    val scriptCallData = ScriptCallData("${script.id}", parameters)
    recentScriptCalls.append(scriptCallData)

    val initData = prepareScriptInit(script, scriptDao, scriptPagesRest)
    val saveUserContext = ThreadLocalUserContext.getUserContext()
    val scriptExecutionResult = try {
      scriptDao.execute(initData.scriptDO, parameters, initData.additionalVariables, initData.myImports)
    } finally {
      ThreadLocalUserContext.setUserContext(saveUserContext) // If script was executed as.
    }
    if (scriptExecutionResult.hasException()) {
      scriptExecutionResult.scriptLogger.error(scriptExecutionResult.exception.toString())
      return scriptExecutionResult
    }
    scriptExecutionResult.result?.let { result ->
      when (result) {
        is ExportWorkbook -> {
          exportExcel(request, result, scriptExecutionResult)
        }
        is ExcelWorkbook -> {
          exportExcel(request, result, scriptExecutionResult)
        }
        is ExportJFreeChart -> {
          exportJFreeChart(request, result, scriptExecutionResult)
        }
        is ExportZipArchive -> {
          exportZipArchive(request, result, scriptExecutionResult)
        }
        is ExportJson -> {
          exportJson(request, result, scriptExecutionResult)
        }
      }
    }
    return scriptExecutionResult
  }

  private fun prepareScriptInit(
    script: Script,
    scriptDao: AbstractScriptDao,
    scriptPagesRest: AbstractPagesRest<*, *, *>,
  ): ScriptInitData {
    val scriptDO: ScriptDO
    if (script.id != null) {
      // Exceuting db script:
      scriptDO = scriptDao.getById(script.id)
    } else {
      // Executing ad-hoc script (from editor instead of data base).
      scriptDO = ScriptDO()
      script.copyTo(scriptDO)
      scriptDO.scriptAsString = script.script
    }
    val additionalVariables = mutableMapOf<String, Any>()
    if (script.id != null) {
      additionalVariables[SCRIPT_VAR_NAME_FILES] = ScriptFileAccessor(attachmentsService, scriptPagesRest, scriptDO)
    }
    var myImports: List<String>? = null
    scriptDO.executeAsUser?.let { executeAsUser ->
      additionalVariables[SCRIPT_VAR_NAME_EXECUTE_USER] = ExecuteAsUser(executeAsUser, scriptDO)
      myImports = listOf("import org.projectforge.rest.scripting.ExecuteAsUser")
    }
    return ScriptInitData(scriptDO, additionalVariables, myImports)
  }

  private fun exportExcel(
    request: HttpServletRequest,
    workbook: ExportWorkbook,
    scriptExecutionResult: ScriptExecutionResult
  ) {
    val filename = createDownloadFilename(workbook.filename, "xls")
    try {
      val xls = workbook.asByteArray
      if (xls == null || xls.size == 0) {
        scriptExecutionResult.scriptLogger.error("Oups, xls has zero size. Filename: $filename")
        return
      }
      storeDownloadFile(request, filename, xls, scriptExecutionResult)
    } finally {
      workbook.poiWorkbook.close()
    }
  }

  private fun exportExcel(
    request: HttpServletRequest,
    workbook: ExcelWorkbook,
    scriptExecutionResult: ScriptExecutionResult
  ) {
    workbook.use {
      val filename = createDownloadFilename(workbook.filename, workbook.filenameExtension)
      val xls = workbook.asByteArrayOutputStream.toByteArray()
      if (xls == null || xls.size == 0) {
        scriptExecutionResult.scriptLogger.error("Oups, xls has zero size. Filename: $filename")
        return
      }
      storeDownloadFile(request, filename, xls, scriptExecutionResult)
    }
  }

  private fun exportJFreeChart(
    request: HttpServletRequest,
    exportJFreeChart: ExportJFreeChart,
    scriptExecutionResult: ScriptExecutionResult
  ) {
    val out = ByteArrayOutputStream()
    val extension = exportJFreeChart.write(out)
    val filename = createDownloadFilename("pf_chart", extension)
    storeDownloadFile(request, filename, out.toByteArray(), scriptExecutionResult)
  }

  private fun exportZipArchive(
    request: HttpServletRequest,
    exportZipArchive: ExportZipArchive,
    scriptExecutionResult: ScriptExecutionResult
  ) {
    try {
      val filename = createDownloadFilename(exportZipArchive.filename, "zip")
      ByteArrayOutputStream().use { out ->
        exportZipArchive.write(out)
        storeDownloadFile(request, filename, out.toByteArray(), scriptExecutionResult)
      }
    } catch (ex: Exception) {
      scriptExecutionResult.exception = ex
      log.error(ex.message, ex)
    }
  }

  private fun exportJson(
    request: HttpServletRequest,
    exportJson: ExportJson,
    scriptExecutionResult: ScriptExecutionResult
  ) {
    try {
      val filename = createDownloadFilename(exportJson.jsonName, "json")
      storeDownloadFile(
        request,
        filename,
        JsonUtils.toJson(exportJson.result).toByteArray(StandardCharsets.UTF_8),
        scriptExecutionResult
      )
    } catch (ex: Exception) {
      scriptExecutionResult.exception = ex
      log.error(ex.message, ex)
    }
  }

  internal fun storeDownloadFile(
    request: HttpServletRequest,
    filename: String,
    bytes: ByteArray,
    scriptExecutionResult: ScriptExecutionResult
  ) {
    val expires = PFDateTime.now().plus(DOWNLOAD_EXPIRY_MINUTES.toLong(), ChronoUnit.MINUTES)
    val expiresTime = expires.format(DateFormatType.TIME_OF_DAY_MINUTES)
    val downloadFile = DownloadFile(filename, bytes, expiresTime)
    ExpiringSessionAttributes.setAttribute(request, EXPIRING_SESSION_ATTRIBUTE, downloadFile, DOWNLOAD_EXPIRY_MINUTES)
    scriptExecutionResult.scriptLogger.info("File '$filename' prepared for download (up-to $DOWNLOAD_EXPIRY_MINUTES minutes available).")
  }

  internal fun getDownloadFile(request: HttpServletRequest): DownloadFile? {
    return ExpiringSessionAttributes.getAttribute(request, EXPIRING_SESSION_ATTRIBUTE, DownloadFile::class.java)
  }

  internal fun createDownloadFilename(filename: String?, extension: String): String {
    val suffix = "${DateHelper.getTimestampAsFilenameSuffix(Date())}.$extension"
    return if (filename.isNullOrBlank()) {
      "pf_scriptresult_$suffix"
    } else {
      "${filename.removeSuffix(".$extension")}_$suffix"
    }
  }

  data class ScriptInitData(
    val scriptDO: ScriptDO,
    val additionalVariables: MutableMap<String, Any>,
    val myImports: List<String>?
  )

  data class DownloadFile(val filename: String, val bytes: ByteArray, val availableUntil: String) {
    val sizeHumanReadable
      get() = NumberHelper.formatBytes(bytes.size)
  }

  companion object {
    private val USER_PREF_AREA = "ScriptExecution:"
    private val USER_PREF_KEY = "recentCalls"

    private val SCRIPT_VAR_NAME_EXECUTE_USER = "executeAsUser"
    private val SCRIPT_VAR_NAME_FILES = "files"

    private val DOWNLOAD_EXPIRY_MINUTES = 5

    private val EXPIRING_SESSION_ATTRIBUTE = "${this::class.java.name}.downloadFile"
  }
}
