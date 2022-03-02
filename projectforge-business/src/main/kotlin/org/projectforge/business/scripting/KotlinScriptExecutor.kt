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
    filename: String? = null
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
    sb.appendLine(autoImports.joinToString("\n"))
    sb.append(script)
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
