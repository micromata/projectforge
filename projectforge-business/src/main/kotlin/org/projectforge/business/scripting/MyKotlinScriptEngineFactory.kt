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

import org.jetbrains.kotlin.cli.common.repl.KotlinJsr223JvmScriptEngineFactoryBase
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.jetbrains.kotlin.script.jsr223.KotlinStandardJsr223ScriptTemplate
import org.projectforge.framework.configuration.ConfigXml
import org.slf4j.LoggerFactory
import java.io.File
import java.util.jar.JarFile
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine

/**
 * Kotlin scripts don't run out of the box with spring boot. This workarround is needed.
 */
// https://stackoverflow.com/questions/44781462/kotlin-jsr-223-scriptenginefactory-within-the-fat-jar-cannot-find-kotlin-compi
class MyKotlinScriptEngineFactory : KotlinJsr223JvmScriptEngineFactoryBase() {
    override fun getScriptEngine(): ScriptEngine =
            KotlinJsr223JvmLocalScriptEngine(
                    //Disposer.newDisposable(),
                    this,
                    classPath,
                    KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
                    { ctx, types ->
                        ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray())
                    },
                    arrayOf(Bindings::class)
            )

    companion object {
        private val log = LoggerFactory.getLogger(MyKotlinScriptEngineFactory::class.java)
        private lateinit var jarFile: File
        private val libDir = File(ConfigXml.getInstance().tempDirectory, "scriptClassPath")
        private val classPath = mutableListOf<File>()

        init {
            if (libDir.exists()) {
                log.info("Deleting existing tmp dir '$libDir'.")
                libDir.deleteRecursively()
            }
            val uriString = MyKotlinScriptEngineFactory::class.java.protectionDomain.codeSource.location.toString()
            val filename = uriString.substring(0, uriString.indexOf('!')).removePrefix("jar:file:")
            jarFile = File(filename)
            log.info("Detecting jar file: ${jarFile.absolutePath}")
            log.info("Creating new tmp dir '$libDir'.")
            libDir.mkdirs()
            JarFile(jarFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        val file = File(libDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            if (file.extension == "jar") {
                                classPath.add(file)
                            }
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            //classPath.add(jarFile)
            log.info("Setting script classPath: ${classPath.joinToString(";") { it.absolutePath }}")
        }
    }
}

// org.jetbrains.kotlin.com.intellij.openapi.util.UserDataHolderBase (kotlin-compiler-embeddable)

// https://discuss.kotlinlang.org/t/kotlin-compiler-embeddable-exception-on-kotlin-script-evaluation/6547
/**
 * Special Kotlin-script engine factory that adds script's dependencies to the script evaluator classpath.
 */
/*
class SpringBootKotlinScriptEngineFactory  : KotlinJsr223JvmScriptEngineFactoryBase() {
    override fun getScriptEngine(): ScriptEngine {
        val extractedJarLocation = <some of the classes referenced by the script>::class.java.protectionDomain.codeSource.location.toURI()
        val jarDirectory = try {
            Paths.get(extractedJarLocation).parent
        } catch (e: FileSystemNotFoundException) {
            log.error("Script engine creation error: can't get JAR directory for $extractedJarLocation", ex)
            throw e
        }

        val classpath = Files.list(jarDirectory)
                .filter { it.fileName.endsWith(".jar") }
                .map { it.toFile() }
                .collect(Collectors.toCollection { mutableListOf<File>() })

        classpath += scriptCompilationClasspathFromContext("kotlin-script-util.jar", wholeClasspath = true)

        return KotlinJsr223JvmLocalScriptEngine(
                this,
                classpath,
                KotlinStandardJsr223ScriptTemplate::class.qualifiedName!!,
                { ctx, types ->
                    ScriptArgsWithTypes(arrayOf(ctx.getBindings(ScriptContext.ENGINE_SCOPE)), types ?: emptyArray())
                },
                arrayOf(Bindings::class)
        )
    }

    private val log = LoggerFactory.getLogger(KotlinScriptExecutor::class.java)

}*/
