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

import javax.script.ScriptException

object KotlinScriptExceptionAnalyzer {
    fun parseScriptException(exception: ScriptException, scriptLogger: ScriptLogger): List<ScriptDiagnostic> {
        val diagnostics = mutableListOf<ScriptDiagnostic>()
        exception.message?.lines()?.forEach { line ->
            when {
                line.startsWith("WARNING") -> {
                    diagnostics.add(ScriptDiagnostic("WARNING", extractMessage(line), extractLocation(line)))
                }
                line.startsWith("ERROR") -> {
                    diagnostics.add(ScriptDiagnostic("ERROR", extractMessage(line), extractLocation(line)))
                }
            }
        }
        diagnostics.forEach { entry ->
            scriptLogger.error("Script diagnostic: ${entry.severity} - ${entry.message} - ${entry.location}")
        }
        return diagnostics
    }

    fun extractMessage(line: String): String {
        return line.substringAfter(": ").substringBefore(" (")
    }

    fun extractLocation(line: String): String {
        return line.substringAfter("(").substringBefore(")")
    }

    data class ScriptDiagnostic(
        val severity: String,
        val message: String,
        val location: String

    )
}
