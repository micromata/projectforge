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

import org.projectforge.business.scripting.ScriptExecutor.Companion.createValidIdentifier
import org.projectforge.framework.i18n.translate
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.valueOrNull

internal object KotlinScriptUtils {
    fun handleResult(
        scriptExecutionResult: ScriptExecutionResult,
        result: ResultWithDiagnostics<EvaluationResult>?,
        script: String,
        logSeverity: ScriptDiagnostic.Severity = ScriptDiagnostic.Severity.ERROR
    ) {
        if (result == null) {
            scriptExecutionResult.result = translate("scripting.error.timeout")
            return
        }
        val scriptLines = script.lines()
        val logger = scriptExecutionResult.scriptLogger
        result.reports.forEach { report ->
            val severity = report.severity
            if (severity < logSeverity) {
                return@forEach
            }
            val message = report.message
            val location = report.location
            var line1 = "[$severity] $message"
            var line2: String? = null
            location?.let {
                // line1 = "[$severity] $message: ${it.start.line}:${it.start.col} to ${it.end?.line}:${it.end?.col}"
                line1 = "[$severity] $message: line ${it.start.line} to ${it.end?.line}"
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
            logger.add(line1, severity)
            line2?.let { logger.add(it, severity) }
        }
        scriptExecutionResult.result = result.valueOrNull()
    }

    fun appendBlockAfterImports(
        sb: StringBuilder,
        executor: KotlinScriptExecutor,
    ) {
        sb.appendLine("// Auto generated bindings:")
        // Prepend bindings now before proceeding
        val bindingsEntries = mutableListOf<String>()
        val script = executor.resolvedScript ?: executor.source
        executor.variables.forEach { (name, value) ->
            if (!(executor.resolvedScript
                    ?: executor.source).contains(createContextGet(name))
            ) { // Don't add binding twice
                addBinding(bindingsEntries, name, value)
            }
        }
        executor.scriptParameterList?.forEach { param ->
            if (!script.contains(createContextGet(param.parameterName))) { // Don't add binding twice
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

    fun createContextGet(name: String): String {
        return "context.getProperty(\"$name\")"
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
        bindingsEntries.add("val $identifier = ${createContextGet(identifier)} as $clsName$nullable")
    }

    private val bindingsClassReplacements = mapOf(
        "java.lang.String" to "String",
        "java.lang.Integer" to "Int",
        "java.lang.Boolean" to "Boolean",
        "java.util.HashMap" to "MutableMap<*, *>",
    )
}
