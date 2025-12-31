/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting.kotlin

import org.jetbrains.kotlin.com.intellij.ide.plugins.PluginManagerCore.logger
import org.projectforge.business.scripting.ScriptLogger
import org.projectforge.business.scripting.ThreadLocalScriptingContext
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.util.*
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.util.PropertiesCollection

class CustomScriptingHost(val logger: ScriptLogger) : BasicJvmScriptingHost() {
    private val loggedInUser = ThreadLocalUserContext.requiredLoggedInUser

    override fun eval(
        script: SourceCode,
        compilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration?,
    ): ResultWithDiagnostics<EvaluationResult> {
        try {
            ThreadLocalUserContext.setUser(loggedInUser)
            ThreadLocalScriptingContext.setLogger(logger)
            return super.eval(script, compilationConfiguration, evaluationConfiguration)
        } finally {
            ThreadLocalUserContext.clear()
            ThreadLocalScriptingContext.clear()
        }
    }
}
