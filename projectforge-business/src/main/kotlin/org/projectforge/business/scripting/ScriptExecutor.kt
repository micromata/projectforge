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

import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.projectforge.ProjectForgeVersion
import org.projectforge.business.fibu.kost.reporting.ReportGeneratorList
import org.projectforge.business.task.ScriptingTaskTree
import org.projectforge.business.task.TaskTree
import org.projectforge.registry.Registry

private val log = KotlinLogging.logger {}

abstract class ScriptExecutor(
  /**
   * Script input (before pre-processing). #INCLUDE statements are not yet resolved and
   * auto-imports and bindings (Kotlin) are not included.
   */
  var source: String = ""
) {
  /**
   * List of standard variables, such as daos, appId etc.
   */
  var variables = mutableMapOf<String, Any?>()

  val scriptParameterValues = mutableMapOf<String, Any?>()

  val allVariables: Map<String, Any?>
    get() = variables + scriptParameterValues

  /**
   * script with resolved #INCLUDEs.
   */
  var resolvedScript: String? = null
    private set

  private var _effectiveScript: String? = null

  /**
   * Script including additional content, such as auto imports, variable bindings (Kotlin) and
   * resolved #INPUT statements. This is the script, that will be executed by the scripting engine.
   */
  val effectiveScript: String
    get() = _effectiveScript ?: resolvedScript ?: source

  val scriptLogger = ScriptLogger()

  val scriptExecutionResult = ScriptExecutionResult(scriptLogger)

  private lateinit var scriptDao: ScriptDao

  /**
   * Adds all registered dao's and other variables, such as appId, appVersion and task-tree. These variables are
   * available in Groovy and Kotlin scripts
   */
  init {
    variables["appId"] = ProjectForgeVersion.APP_ID
    variables["appVersion"] = ProjectForgeVersion.VERSION_NUMBER
    variables["appRelease"] = ProjectForgeVersion.BUILD_DATE
    variables["taskTree"] = ScriptingTaskTree(TaskTree.getInstance())
    variables["log"] = scriptLogger
    variables["reportList"] = ReportGeneratorList()
    variables["i18n"] = I18n()
    for (entry in Registry.getInstance().orderedList) {
      val scriptingDao = entry.scriptingDao
      if (scriptingDao != null) {
        val varName = StringUtils.uncapitalize(entry.id)
        variables[varName + "Dao"] = scriptingDao
      }
    }
  }

  /**
   * @param scripDao Needed for resolving #INPUT statements for loading requested sniplets.
   */
  fun init(
    scriptDO: ScriptDO,
    scripDao: ScriptDao,
    additionalVariables: Map<String, Any?>,
    /**
     * List of script parameter values, given by user form.
     */
    scriptParameters: List<ScriptParameter>? = null,
  ) {
    this.scriptDao = scripDao
    source = scriptDO.scriptAsString ?: ""
    if (scriptDO.filename != null || scriptDO.file != null) {
      variables["file"] = scriptDO.file
      variables["filename"] = scriptDO.filename
      // For files directly stored in db entry DB_Script, the file will be accessible via script.file and script.filename
      // as well as via file and filename.
      variables["script"] = mutableMapOf("file" to scriptDO.file, "filename" to scriptDO.filename)
    }
    variables.putAll(additionalVariables)
    putScriptParameters(scriptDO.getParameterList())
    putScriptParameters(scriptParameters)
    resolvedScript = resolveInputs(scriptLogger, scriptDO)
    buildEffectiveScript()
  }

  private fun putScriptParameters(params: List<ScriptParameter>?) {
    params?.filter { it.parameterName != null }?.forEach { param ->
      val paramName = param.parameterName.replaceFirstChar { it.lowercase() } // decapitalize script params.
      val paramValue = param.value
      if (paramValue == null && param.type == ScriptParameterType.BOOLEAN) {
        // Put false instead of null value for boolean values:
        scriptParameterValues[paramName] = false
      } else {
        scriptParameterValues[paramName] = param.value
      }
    }
  }

  /**
   * Resolve #INCLUDE "<Name of snippet or DB id of snippet>" by replacing it by snippet.
   * Snippets may also include other snippets (recursive).
   */
  private fun resolveInputs(
    scriptLogger: ScriptLogger,
    script: ScriptDO,
    callers: List<ScriptDO> = emptyList(),
  ): String {
    if (callers.any { it.id == script.id }) {
      val error = "Endless recursion detected: ${callers.joinToString(" -> ") { it.name ?: "untitled" }}"
      scriptLogger.error(error)
      throw IllegalArgumentException(error)
    }
    val scriptContent = script.scriptAsString ?: return ""
    return regex.replace(scriptContent) { m ->
      val snippetNameOrId = m.groupValues[1]
      val snippet = scriptDao.loadByNameOrId(snippetNameOrId)
      if (snippet == null) {
        scriptLogger.error("Can't load snippet und name/id '$snippetNameOrId'.")
        ""
      } else {
        log.info { "Including script '${snippet.name}'" }
        resolveInputs(scriptLogger, snippet, callers + script)
      }
    }
  }

  abstract fun execute(): ScriptExecutionResult

  private fun buildEffectiveScript() {
    val sb = StringBuilder()
    (resolvedScript ?: source).let { src ->
      var importBlock = true
      src.lines().forEach { line ->
        if (importBlock) {
          if (line.startsWith("import ") || line.isBlank() || line.startsWith("//")) {
            // Inside import block
            sb.appendLine(line)
          } else {
            importBlock = false // End of import block.
            sb.appendLine("// Auto generated imports:")
            autoImports().sorted().forEach { importLine ->
              if (!src.contains(importLine)) { // Don't add import twice
                sb.appendLine(importLine)
              }
            }
            sb.appendLine()
            appendBlockAfterImports(sb)
          }
        }
        if (!importBlock) { // Don't use else! (see importBlock = false)
          sb.appendLine(line)
        }
      }
    }
    _effectiveScript = sb.toString()
  }

  /**
   * Does nothing at default. The Kotlin executor adds binding variables.
   */
  protected open fun appendBlockAfterImports(sb: StringBuilder) {
  }

  protected open fun autoImports(): List<String> {
    return autoImports
  }

  companion object {
    private val VAL_REGEX = """^\s*val """.toRegex(RegexOption.MULTILINE)
    private val VAR_REGEX = """^\s*var """.toRegex(RegexOption.MULTILINE)

    fun createScriptExecutor(scriptDO: ScriptDO): ScriptExecutor {
      if (scriptDO.type == ScriptDO.ScriptType.KOTLIN) {
        return KotlinScriptExecutor()
      } else if (scriptDO.type == ScriptDO.ScriptType.GROOVY) {
        return GroovyScriptExecutor()
      }
      val script = scriptDO.scriptAsString ?: ""
      return if (script.contains(VAL_REGEX) || script.contains(VAR_REGEX)) {
        KotlinScriptExecutor()
      } else {
        GroovyScriptExecutor()
      }
    }

    val autoImports = listOf(
      "import java.io.ByteArrayInputStream",
      "import java.math.BigDecimal",
      "import java.math.RoundingMode",
      "import java.time.format.DateTimeFormatter",
      "import java.time.LocalDate",
      "import java.time.Month",
      "import org.apache.poi.ss.usermodel.IndexedColors",
      "import de.micromata.merlin.I18n",
      "import de.micromata.merlin.excel.ExcelCell",
      "import de.micromata.merlin.excel.ExcelRow",
      "import de.micromata.merlin.excel.ExcelSheet",
      "import de.micromata.merlin.excel.ExcelWorkbook",
      "import de.micromata.merlin.excel.ExcelWriterContext",
      "import org.projectforge.framework.calendar.*",
      "import org.projectforge.framework.time.*",
      "import org.projectforge.framework.utils.*",
      "import org.projectforge.business.fibu.*",
      "import org.projectforge.business.fibu.kost.*",
      "import org.projectforge.business.task.*",
      "import org.projectforge.business.timesheet.*",
      "import org.projectforge.business.scripting.ExportZipArchive",
      "import org.projectforge.business.scripting.ExportJson",
      "import org.projectforge.business.scripting.ScriptDO",
      "import org.projectforge.business.scripting.ScriptingDao",
      "import org.projectforge.business.scripting.support.*",
      "import org.projectforge.common.*",
      "import org.projectforge.excel.ExcelUtils",
    )

    private val regex = """#INCLUDE\s*"(.+)"""".toRegex() // #INCLUDE "<Name of Snippet or DB-ID>"
  }
}
