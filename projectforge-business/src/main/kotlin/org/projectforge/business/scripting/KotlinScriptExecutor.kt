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

package org.projectforge.business.scripting

import mu.KotlinLogging
import javax.script.ScriptEngineManager
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.StringScriptSource
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

private val log = KotlinLogging.logger {}

class KotlinScriptExecutor : ScriptExecutor() {
    companion object {
        private val bindingsClassReplacements = mapOf(
            "java.lang.String" to "String",
            "java.lang.Integer" to "Int",
            "java.lang.Long" to "Long",
            "java.lang.Boolean" to "Boolean",
            "java.util.HashMap" to "MutableMap<*, *>",
        )

        private val kotlinImports = listOf(
            "import org.projectforge.framework.i18n.translate",
            "import org.projectforge.framework.i18n.translateMsg",
        )
    }

    /**
     * @param script Common imports will be prepended.
     * @param variables Variables to bind. Variables are usable via binding["key"] or directly, if #autobind# is part of script.
     * @see GroovyExecutor.executeTemplate
     */
    override fun execute(): ScriptExecutionResult {
        val scriptingHost = BasicJvmScriptingHost()

        val compilationConfiguration = ScriptCompilationConfiguration {
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
            compilerOptions.append("-nowarn")
        }
        val bindings = mutableMapOf<String, Any?>()
        variables.forEach {
            bindings[it.key] = it.value
        }
        scriptParameterList?.forEach {
            bindings[createValidIdentifier(it.parameterName)] = it.value
        }
        val evaluationConfiguration = ScriptEvaluationConfiguration {
            // Alle Bindings hinzufügen
            bindings.forEach { (key, value) ->
                providedProperties(key to value)
            }
        }
        val scriptSource = StringScriptSource(effectiveScript)
        var result: ResultWithDiagnostics<EvaluationResult>? = null
        try {
            result = scriptingHost.eval(scriptSource, compilationConfiguration, evaluationConfiguration)
        } catch (ex: Exception) {
            log.info("Exception on Kotlin script execution: ${ex.message}", ex)
            scriptExecutionResult.exception = ex
        }
        val scriptLines = effectiveScript.lines()
        val logger = scriptExecutionResult.scriptLogger
        val messages = mutableListOf<String>()
        result?.reports?.forEach { report ->
            val severity = report.severity
            val message = report.message
            val location = report.location
            var line1 = "[$severity] $message"
            var line2: String? = null
            location?.let {
                line1 = "[$severity] $message: ${it.start.line}:${it.start.col} to ${it.end?.line}:${it.end?.col}"
                val lineIndex = it.start.line - 1 // Zeilenindex anpassen
                if (lineIndex in scriptLines.indices) {
                    val line = scriptLines[lineIndex]
                    val startCol = it.start.col - 1
                    val endCol = (it.end?.col ?: line.length) - 1

                    // Teile die Zeile auf und füge die Marker ein
                    val markedLine = buildString {
                        append(">")
                        append(line.substring(0, startCol))
                        append(">>>")  // Markierung für Startposition
                        append(line.substring(startCol, endCol))
                        append("<<<")  // Markierung für Endposition
                        append(line.substring(endCol))
                    }
                    line2 = markedLine
                }
            }
            if (severity == ScriptDiagnostic.Severity.ERROR) {
                logger.error(line1)
                line2?.let { logger.error(it) }
            } else {
                logger.info(line1)
                line2?.let { logger.info(it) }
            }
        }
        scriptExecutionResult.result = result?.valueOrNull()
        return scriptExecutionResult
    }

    override fun standardImports(): List<String> {
        val kotlinImports = STANDARD_IMPORTS.map { it.replace("import static", "import") } + kotlinImports
        return kotlinImports.filter { !it.startsWith("import java.io") }
    }

    override fun appendBlockAfterImports(sb: StringBuilder) {
        sb.appendLine("// Auto generated bindings:")
        // Prepend bindings now before proceeding
        val bindingsEntries = mutableListOf<String>()
        val script = resolvedScript ?: source
        /*variables.forEach { (name, value) ->
            if (!(resolvedScript ?: source).contains("bindings[\"$name\"]")) { // Don't add binding twice
                addBinding(bindingsEntries, name, value)
            }
        }
        scriptParameterList?.forEach { param ->
            if (!script.contains("bindings[\"${param.parameterName}\"]")) { // Don't add binding twice
                // OK, null value wasn't added to variables. So we had to add them here:
                addBinding(bindingsEntries, param.parameterName, param)
            }
        }*/
        bindingsEntries.sortedBy { it.lowercase() }.forEach {
            sb.appendLine(it)
        }
        sb.appendLine()
        sb.appendLine()
    }

    private fun addBinding(bindingsEntries: MutableList<String>, name: String, value: Any?) {
        if (name.isBlank()) {
            return // Don't add blank variables (shouldn't occur).
        }
        var nullable = ""
        val clazz = if (value != null) {
            if (value is ScriptParameter) {
                if (value.type != ScriptParameterType.BOOLEAN) {
                    nullable = "?" // Script parameter not found as variable -> is null!
                }
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
            "ScriptingDao<${value.dOClass.name}>"
        } else {
            clazz.name
        }
        val identifier = createValidIdentifier(name)
        bindingsEntries.add("val $identifier = bindings[\"$identifier\"] as $clsName$nullable")
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
