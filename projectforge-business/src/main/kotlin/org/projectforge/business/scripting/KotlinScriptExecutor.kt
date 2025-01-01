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
import org.projectforge.business.scripting.kotlin.CustomScriptingHost
import org.projectforge.business.scripting.kotlin.JarExtractor
import org.projectforge.business.scripting.kotlin.KotlinScriptUtils
import org.projectforge.framework.i18n.translate
import java.net.URLClassLoader
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.*

private val log = KotlinLogging.logger {}

/**
 * For checking new algorithm and strategies, refer https://github.com/micromata/SpringBoot-KotlinScripting
 */
class KotlinScriptExecutor(scriptLogger: ScriptLogger) : ScriptExecutor(scriptLogger) {
    override fun execute(): ScriptExecutionResult {
        log.debug { "Updated classpathFiles: ${JarExtractor.classpathFiles?.joinToString()}" }
        log.debug { "Updated classpath URLs: ${JarExtractor.classpathUrls?.joinToString()}" }
        val classLoader = if (JarExtractor.runningInFatJar) {
            URLClassLoader(JarExtractor.classpathUrls, Thread.currentThread().contextClassLoader).also {
                Thread.currentThread().contextClassLoader = it
            }
        } else {
            Thread.currentThread().contextClassLoader
        }
        val scriptingHost = CustomScriptingHost()
        val compilationConfiguration = ScriptCompilationConfiguration {
            jvm {
                if (JarExtractor.classpathFiles != null) {
                    dependenciesFromClassloader(classLoader = classLoader, wholeClasspath = true)
                    updateClasspath(JarExtractor.classpathFiles)
                } else {
                    dependenciesFromCurrentContext(wholeClasspath = true)
                }
            }
            providedProperties("context" to KotlinScriptContext::class)
            compilerOptions.append("-nowarn")
        }
        val context = KotlinScriptContext()
        variables.forEach {
            context.setProperty(it.key, it.value)
        }
        scriptParameterList?.forEach {
            context.setProperty(createValidIdentifier(it.parameterName), it.value)
        }
        val evaluationConfiguration = ScriptEvaluationConfiguration {
            jvm {
                baseClassLoader(classLoader)
            }
            providedProperties("context" to context)
        }
        val scriptSource = effectiveScript.trimIndent().toScriptSource()
        val result = execute(scriptingHost, scriptSource, compilationConfiguration, evaluationConfiguration)
        KotlinScriptUtils.handleResult(scriptExecutionResult, result, effectiveScript)
        return scriptExecutionResult
    }

    private fun execute(
        scriptingHost: CustomScriptingHost,
        scriptSource: SourceCode,
        compilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration,
    ): ResultWithDiagnostics<EvaluationResult>? {
        val executor = Executors.newSingleThreadExecutor()
        var future: Future<ResultWithDiagnostics<EvaluationResult>>? = null
        try {
            future = executor.submit<ResultWithDiagnostics<EvaluationResult>> {
                scriptingHost.eval(scriptSource, compilationConfiguration, evaluationConfiguration)
            }
            return future.get(300, TimeUnit.SECONDS)  // Timeout
        } catch (ex: TimeoutException) {
            log.info("Script execution was cancelled due to timeout.")
            future?.cancel(true)  // Attempt to cancel
            scriptExecutionResult.exception = ex
            scriptExecutionResult.scriptLogger.error(translate("scripting.error.timeout"))
        } catch (ex: Exception) {
            log.info("Exception on Kotlin script execution: ${ex.message}", ex)
            scriptExecutionResult.exception = ex
            scriptExecutionResult.scriptLogger.error("Exception on Kotlin script execution: ${ex.message}")
        } finally {
            executor.shutdownNow()
        }
        return null
    }

    override fun standardImports(): List<String> {
        val kotlinImports = STANDARD_IMPORTS.map { it.replace("import static", "import") } + kotlinImports
        return kotlinImports.filter { !it.startsWith("import java.io") }
    }

    override fun appendBlockAfterImports(sb: StringBuilder) {
        KotlinScriptUtils.appendBlockAfterImports(sb, this)
    }

    companion object {
        private val kotlinImports = listOf(
            "import org.projectforge.framework.i18n.translate",
            "import org.projectforge.framework.i18n.translateMsg",
            "import org.projectforge.common.extensions.format",
            "import org.projectforge.common.extensions.format2Digits",
            "import org.projectforge.common.extensions.format3Digits",
            "import org.projectforge.common.extensions.formatBytes",
            "import org.projectforge.common.extensions.formatBytesForUser",
            "import org.projectforge.common.extensions.formatCurrency",
            "import org.projectforge.common.extensions.formatForUser",
            "import org.projectforge.business.PfCaches.Companion.initialize"
        )
    }
}
