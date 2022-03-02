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
import javax.script.ScriptEngineManager

private val log = KotlinLogging.logger {}

object KotlinScriptExecutor {

  val autoImports = listOf(
    "import java.io.ByteArrayInputStream",
    "import java.math.BigDecimal",
    "import java.math.RoundingMode",
    "import java.time.format.DateTimeFormatter",
    "import de.micromata.merlin.I18n",
    "import de.micromata.merlin.excel.ExcelCell",
    "import de.micromata.merlin.excel.ExcelRow",
    "import de.micromata.merlin.excel.ExcelSheet",
    "import de.micromata.merlin.excel.ExcelWorkbook",
    "import de.micromata.merlin.excel.ExcelWriterContext",
    "import org.projectforge.framework.calendar.*",
    "import org.projectforge.framework.i18n.translate",
    "import org.projectforge.framework.i18n.translateMsg",
    "import org.projectforge.framework.time.*",
    "import org.projectforge.framework.utils.*",
    "import org.projectforge.business.fibu.*",
    "import org.projectforge.business.task.*",
    "import org.projectforge.business.timesheet.*",
    "import org.projectforge.business.scripting.ExportZipArchive",
    "import org.projectforge.business.scripting.ExportJson",
    "import org.projectforge.business.scripting.ScriptDO",
    "import org.projectforge.business.scripting.ScriptingDao",
    "import org.projectforge.common.*",
    "import org.projectforge.excel.ExcelUtils",
  )

  /**
   * @param script Common imports will be prepended.
   * @param variables Variables to bind. Variables are usable via binding["key"] or directly, if #autobind# is part of script.
   * @see GroovyExecutor.executeTemplate
   */
  @JvmStatic
  @JvmOverloads
  fun execute(
    script: String,
    variables: Map<String, Any?>,
    file: ByteArray? = null,
    filename: String? = null,
    /**
     * Required for defining bindings of null values.
     */
    inputParameters: List<ScriptParameter>? = null,
  ): ScriptExecutionResult {
    val engine = MyKotlinScriptEngineFactory().scriptEngine
    val bindings = engine.createBindings()
    variables.forEach {
      bindings[it.key] = it.value
    }
    if (file != null) {
      bindings["file"] = file
      bindings["filename"] = filename
    }
    val sb = StringBuilder()
    var importBlock = true
    script.lines().forEach { line ->
      if (importBlock) {
        if (line.startsWith("import ") || line.isBlank() || line.startsWith("//")) {
          // Inside import block
          sb.appendLine(line)
        } else {
          importBlock = false // End of import block.
          sb.appendLine("// Auto generated imports:")
          sb.appendLine(autoImports.joinToString("\n"))
          sb.appendLine()
          sb.appendLine("// Auto generated bindings:")
          // Prepend bindings now before proceeding
          val bindingsEntries = mutableListOf<String>()
          variables.forEach { name, value ->
            addBinding(bindingsEntries, name, value)
          }
          inputParameters?.forEach { param ->
            if (variables[param.parameterName] == null) {
              // OK, null value wasn't added to variables. So we had to add them here:
              addBinding(bindingsEntries, param.parameterName, param)
            }
          }
          bindingsEntries.sortedBy { it.lowercase() }.forEach {
            sb.appendLine(it)
          }
          sb.appendLine()
          sb.appendLine()
        }
      }
      if (!importBlock) { // Don't use else! (see importBlock = false)
        sb.appendLine(line)
      }
    }
    val effectiveScript = sb.toString()
    val result = ScriptExecutionResult(ScriptDao.getScriptLogger(variables))
    try {
      result.script = effectiveScript
      result.result = engine.eval(effectiveScript, bindings)
    } catch (ex: Exception) {
      log.info("Exception on Kotlin script execution: ${ex.message}", ex)
      result.exception = ex
    }
    return result
  }

  private val bindingsClassReplacements = mapOf(
    "java.lang.String" to "String",
    "java.lang.Integer" to "Int",
    "java.util.HashMap" to "MutableMap<*, *>",
  )

  private fun addBinding(bindingsEntries: MutableList<String>, name: String, value: Any?) {
    if (name.isBlank()) {
      return // Don't add blank variables (shouldn't occur).
    }
    var nullable = ""
    val clazz = if (value != null) {
      if (value is ScriptParameter) {
        nullable = "?" // Script parameter not found as variable -> is null!
        value.valueClass
      } else {
        value::class.java
      }
    } else {
      Any::class.java
    }
    val clsName = if (bindingsClassReplacements.containsKey(clazz.name)) {
      bindingsClassReplacements[clazz.name]
    } else if (value is ScriptingDao<*>) {
      "ScriptingDao<${value.doClass.name}>"
    } else {
      clazz.name
    }
    bindingsEntries.add("val $name = bindings[\"$name\"] as $clsName$nullable")
    if (name[0].isUpperCase()) {
      bindingsEntries.add("val ${name.replaceFirstChar { it.lowercase() }} = $name // Set alias")
    }
  }
}

fun main() {
  val engineManager = ScriptEngineManager()
  for (factory in engineManager.engineFactories) {
    println(factory.engineName)
    println("\t" + factory.languageName)
  }
  val engine = engineManager.getEngineByExtension("kts")
  engine.eval("import org.projectforge.*\nprintln(\"\${ProjectForgeVersion.APP_ID} \${ProjectForgeVersion.VERSION_STRING}\")")
}
