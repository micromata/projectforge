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

package org.projectforge.business.scripting

import de.micromata.merlin.utils.ReplaceUtils.encodeFilename
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.ProjectForgeVersion
import org.projectforge.business.fibu.kost.reporting.ReportGeneratorList
import org.projectforge.business.scripting.KotlinScriptExecutor.execute
import org.projectforge.business.task.ScriptingTaskTree
import org.projectforge.business.task.TaskTree
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime.Companion.now
import org.projectforge.registry.Registry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.io.File
import java.io.IOException
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
open class ScriptDao : BaseDao<ScriptDO>(ScriptDO::class.java) {
  @Autowired
  private lateinit var groovyExecutor: GroovyExecutor

  @Autowired
  private lateinit var taskTree: TaskTree

  /**
   * Copy old script as script backup if modified.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao.onChange
   */
  override fun onChange(obj: ScriptDO, dbObj: ScriptDO) {
    if (!Arrays.equals(dbObj.script, obj.script)) {
      obj.scriptBackup = dbObj.script
      val suffix = getScriptSuffix(obj)
      val filename = encodeFilename("${dbObj.name}_${now().isoStringSeconds}.$suffix", true)
      val backupDir = File(ConfigXml.getInstance().backupDirectory, "scripts")
      ConfigXml.ensureDir(backupDir)
      val file = File(backupDir, filename)
      try {
        log.info("Writing backup of script to: " + file.absolutePath)
        file.writeText(dbObj.scriptAsString ?: "")
      } catch (ex: IOException) {
        log.error("Error while trying to save backup file of script '" + file.absolutePath + "': " + ex.message, ex)
      }
    }
  }

  fun getScriptSuffix(obj: ScriptDO): String {
    return if (obj.type == ScriptDO.ScriptType.KOTLIN) "kts" else "groovy"
  }

  /**
   * User must be member of group controlling or finance.
   *
   * @see org.projectforge.framework.persistence.api.BaseDao.hasDeleteAccess
   */
  override fun hasAccess(
    user: PFUserDO, obj: ScriptDO?, oldObj: ScriptDO?,
    operationType: OperationType,
    throwException: Boolean
  ): Boolean {
    return accessChecker.isUserMemberOfGroup(
      user, throwException, ProjectForgeGroup.CONTROLLING_GROUP,
      ProjectForgeGroup.FINANCE_GROUP
    )
  }

  override fun newInstance(): ScriptDO {
    return ScriptDO()
  }

  open fun execute(script: ScriptDO, parameters: List<ScriptParameter>?): ScriptExecutionResult {
    hasLoggedInUserSelectAccess(script, true)
    val scriptVariables = getScriptVariables(script, parameters)
    var scriptContent = script.scriptAsString ?: ""
    if (script.type === ScriptDO.ScriptType.KOTLIN) {
      return execute(scriptContent, scriptVariables, script.file, script.filename)
    }
    if (scriptContent.contains("import org.projectforge.export")) {
      // Package was renamed in version 5.2 and 6.13:
      scriptContent = scriptContent.replace(
        "import org.projectforge.export",
        "import org.projectforge.export.*\nimport org.projectforge.business.excel"
      )
    }
    return groovyExecutor.execute(
      ScriptExecutionResult(getScriptLogger(scriptVariables)),
      scriptContent,
      scriptVariables
    )
  }

  open fun getScriptVariableNames(script: ScriptDO): List<String> {
    val scriptVariables = getScriptVariables(script, null)
    val result = mutableListOf<String>()
    scriptVariables.forEach { variable ->
      val key = variable.key
      val value = variable.value
      if (value is Map<*, *>) {
        // E. G. script.file, script.filename
        value.keys.forEach {
          result.add("$key.$it")
        }
      } else {
        result.add(key)
      }
    }
    script.parameter1Name?.let { result.add(it) }
    script.parameter2Name?.let { result.add(it) }
    script.parameter3Name?.let { result.add(it) }
    script.parameter4Name?.let { result.add(it) }
    script.parameter5Name?.let { result.add(it) }
    script.parameter6Name?.let { result.add(it) }
    return result.filter { it.isNotBlank() }.sortedBy { it.lowercase() }
  }

  open fun getScriptVariables(script: ScriptDO, parameters: List<ScriptParameter>?): Map<String, Any?> {
    val scriptVariables = mutableMapOf<String, Any?>()
    addScriptVariables(scriptVariables)
    scriptVariables["reportList"] = ReportGeneratorList()
    parameters?.filter { it.parameterName != null && it.value != null }?.forEach { param ->
      scriptVariables[param.getParameterName()] = param.value
    }
    if (script.file != null) {
      val scriptVars: MutableMap<String, Any?> = HashMap()
      scriptVariables["script"] = scriptVars
      scriptVars["file"] = script.file
      scriptVars["filename"] = script.filename
    }
    scriptVariables["i18n"] = I18n()
    return scriptVariables
  }

  /**
   * Adds all registered dao's and other variables, such as appId, appVersion and task-tree. These variables are
   * available in Groovy scripts
   */
  open fun addScriptVariables(scriptVariables: MutableMap<String, Any?>) {
    scriptVariables["appId"] = ProjectForgeVersion.APP_ID
    scriptVariables["appVersion"] = ProjectForgeVersion.VERSION_NUMBER
    scriptVariables["appRelease"] = ProjectForgeVersion.BUILD_DATE
    scriptVariables["taskTree"] = ScriptingTaskTree(taskTree)
    scriptVariables["log"] = ScriptLogger()
    scriptVariables["reportList"] = null
    for (entry in Registry.getInstance().orderedList) {
      val scriptingDao = entry.scriptingDao
      if (scriptingDao != null) {
        val varName = StringUtils.uncapitalize(entry.id)
        scriptVariables[varName + "Dao"] = scriptingDao
      }
    }
  }

  companion object {
    @JvmStatic
    fun getScriptLogger(variables: Map<String, Any?>): ScriptLogger {
      val scriptLogger = variables["log"]
      if (scriptLogger != null && scriptLogger is ScriptLogger) {
        return scriptLogger
      } else {
        log.warn { "Oups, can't find scriptLogger ('log') in script variables!" }
        return ScriptLogger()
      }
    }
  }
}
