/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseDao

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
abstract class AbstractScriptDao : BaseDao<ScriptDO>(ScriptDO::class.java) {
    override fun newInstance(): ScriptDO {
        return ScriptDO()
    }

    /**
     * @param name of script (case insensitive)
     */
    open fun loadByNameOrId(name: String): ScriptDO? {
        name.toIntOrNull()?.let { id ->
            return find(id, checkAccess = false)
        }
        return persistenceService.selectNamedSingleResult(
            ScriptDO.SELECT_BY_NAME,
            ScriptDO::class.java,
            Pair("name", "%${name.trim().lowercase()}%"),
        )
    }

    open fun getScriptVariableNames(
        script: ScriptDO,
        parameters: List<ScriptParameter>,
        additionalVariables: Map<String, Any?>,
        myImports: List<String>? = null,
    ): List<String> {
        val scriptExecutor = createScriptExecutor(script, additionalVariables, parameters, myImports, ScriptLogger())
        return scriptExecutor.allVariables.keys.filter { it.isNotBlank() }.sortedBy { it.lowercase() }
    }

    open fun getEffectiveScript(
        script: ScriptDO,
        parameters: List<ScriptParameter>,
        additionalVariables: Map<String, Any?>,
        imports: List<String>? = null,
    ): String {
        val scriptExecutor = createScriptExecutor(script, additionalVariables, parameters, imports, ScriptLogger())
        return scriptExecutor.effectiveScript
    }

    /**
     * @param imports Additional imports (only package/class name, such as "org.projectforge.rest.scripting.ExecuteAsUser".)
     */
    fun execute(
        script: ScriptDO,
        parameters: List<ScriptParameter>,
        additionalVariables: Map<String, Any>,
        imports: List<String>? = null,
        scriptLogger: ScriptLogger,
    ): ScriptExecutionResult {
        hasLoggedInUserSelectAccess(script, true)
        scriptLogger.info(translate("scripting.script.execution.log.preparing"))
        val executor = createScriptExecutor(script, additionalVariables, parameters, imports, scriptLogger)
        return executor.execute()
    }

    protected fun createScriptExecutor(
        script: ScriptDO,
        additionalVariables: Map<String, Any?>,
        scriptParameters: List<ScriptParameter>? = null,
        imports: List<String>? = null,
        scriptLogger: ScriptLogger,
    ): ScriptExecutor {
        val scriptExecutor = ScriptExecutor.createScriptExecutor(script, scriptLogger)
        scriptExecutor.init(script, this, additionalVariables, scriptParameters, imports)
        return scriptExecutor
    }
}
