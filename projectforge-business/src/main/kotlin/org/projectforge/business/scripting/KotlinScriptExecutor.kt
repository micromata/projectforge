/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.slf4j.LoggerFactory
import javax.script.ScriptEngineManager

class KotlinEngine {

    /**
     * @param template
     * @see GroovyExecutor.executeTemplate
     */
     fun execute(template: String?, variables: Map<String, Any>): ScriptExecutionResult {
        val engineManager = ScriptEngineManager()
        val engine = engineManager.getEngineByExtension("kts")
        variables.forEach {
            engine.put(it.key, it.value)
        }
        try {
            val result = ScriptExecutionResult()
            result.result = engine.eval(template)
            return result
        } catch (ex: Exception) {
            log.info("Exception on Kotlin script execution: ${ex.message}", ex)
            return ScriptExecutionResult(ex)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(KotlinEngine::class.java)
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
