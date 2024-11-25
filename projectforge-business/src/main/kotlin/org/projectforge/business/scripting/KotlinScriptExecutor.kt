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
import org.projectforge.business.scripting.KotlinClassLoaderWorkarround.classLoader
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

private val log = KotlinLogging.logger {}

/**
 * Kotlin script executor.
 * -cp ~/ProjectForge/resources/kotlin-scripting/kotlin-compiler-embeddable-2.0.21.jar:~/ProjectForge/resources/kotlin-scripting/
 */
class KotlinScriptExecutor : ScriptExecutor() {

    class MyScriptingHost :
        BasicJvmScriptingHost(evaluator = CustomScriptEvaluator()) {
        val loggedInUser = ThreadLocalUserContext.loggedInUser
        override fun <T> runInCoroutineContext(block: suspend () -> T): T {
            try {
                log.debug { "MyScriptingHost: Setting user context: $loggedInUser" }
                ThreadLocalUserContext.setUser(loggedInUser)
                checkResource()
                return super.runInCoroutineContext(block)
            } finally {
                ThreadLocalUserContext.clear()
            }
        }

        fun checkResource() {
            log.debug {
                "MyScriptingHost: Search for META-INF/extensions/compiler.xml: ${
                    Thread.currentThread().contextClassLoader.getResource(
                        "META-INF/extensions/compiler.xml"
                    )
                }"
            }
        }
    }

    class CustomScriptEvaluator() : BasicJvmScriptEvaluator() {
        override suspend fun invoke(
            compiledScript: CompiledScript,
            scriptEvaluationConfiguration: ScriptEvaluationConfiguration
        ): ResultWithDiagnostics<EvaluationResult> {
            return super.invoke(compiledScript, scriptEvaluationConfiguration)
        }
    }

    override fun execute(): ScriptExecutionResult {
        log.debug { "Classpath of thread: ${KotlinScriptJarExtractor.finalClasspathURLs.joinToString()}" }
        log.debug { "ClassLoader of thread: ${Thread.currentThread().getContextClassLoader()}" }
        log.debug { "ClassLoader of BasicJvmScriptingHost: ${BasicJvmScriptingHost::class.java.classLoader}" }
        val scriptingHost = MyScriptingHost()
        val compilationConfiguration = ScriptCompilationConfiguration {
            jvm {
                //dependenciesFromClassloader(*finalClasspath.map { it.name }.toTypedArray(), wholeClasspath = true)
                //CompilerConfiguration.addJvmClasspathRoots(classpathUrls)
                //dependenciesFromClassloader(classLoader = customClassLoader, wholeClasspath = true)
                //updateClasspath(finalClasspath)
                //dependenciesFromCurrentContext(wholeClasspath = true)
                dependenciesFromClassloader(classLoader = classLoader, wholeClasspath = true)
                dependencies(JvmDependencyFromClassLoader { classLoader })
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
        result?.let { useResult ->
            KotlinScriptUtils.handleResult(scriptExecutionResult, result, effectiveScript)
            if (useResult is ResultWithDiagnostics.Success) {
                val returnValue = useResult.value.returnValue
                if (returnValue is ResultValue.Value) {
                    val output = returnValue.value
                    scriptExecutionResult.result = output
                }
            }
        }
        return scriptExecutionResult
    }

    private fun execute(
        scriptingHost: MyScriptingHost,
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
            "import org.projectforge.business.PfCaches.Companion.initialize"
        )
        val classpath = listOf(
            File("BOOT-INF/classes/lib"), // Directory of unpacked classes.
            // File("path/to/other/jars/specific-library.jar") // Additional dependencies if required.
        )
    }
}
