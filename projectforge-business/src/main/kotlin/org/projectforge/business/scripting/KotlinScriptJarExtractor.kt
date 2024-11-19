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
import org.projectforge.framework.configuration.ConfigXml
import java.io.File
import java.util.jar.JarFile

private val log = KotlinLogging.logger {}

/**
 * Kotlin scripts don't run out of the box with spring boot. This workaround is needed.
 */
internal object KotlinScriptJarExtractor { // : KotlinJsr223JvmScriptEngineFactoryBase() {
    /**
     * The classpath to be used for the script engine.
     * It contains the fat jar as well as the extracted jars.
     * It's null if not running from a jar file.
     */
    var combinedClasspath: List<File>? = null
        private set
    private val libDir = File(ConfigXml.getInstance().tempDirectory, "scriptClassPath")
    private val extractedFiles = mutableListOf<File>()
    private val whiteListJars = listOf(
        //"merlin-core",
        //"org.projectforge",
        //"projectforge",
        //"kotlin-stdlib",
        "kotlin-compiler-embeddable",
        "kotlin-scripting-",
        "kotlin-std"
        //"kotlin-script-util",
        //"poi"
    )

    init {
        val classpath = System.getProperty("java.class.path")
        if (!classpath.endsWith(".jar") || classpath.contains("projectforge-business")) {
            // We're not running in a jar file.
            log.info { "KotlinScriptJarExtractor is not needed. We're not running from a jar file (from IDE?)." }
        } else {
            val jarPath = File(classpath) // Fat JAR or classpath
            if (!jarPath.exists() || !jarPath.name.endsWith(".jar")) {
                log.info { "Given file doesn't exists or isn't jar: ${jarPath.path}" }
            } else {
                extract(jarPath)
            }
        }
    }

    private fun extract(jarFile: File) {
        val uriString = KotlinScriptJarExtractor::class.java.protectionDomain.codeSource.location.toString()
        if (uriString.startsWith("file:")) {
            // We're not running in a jar file.
        } else {
            if (libDir.exists()) {
                log.info { "Deleting existing tmp dir '$libDir'." }
                libDir.deleteRecursively()
            }
            log.info { "Detecting jar file: ${jarFile.absolutePath}" }
            log.info { "Creating new tmp dir '$libDir'." }
            libDir.mkdirs()
            JarFile(jarFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        if (entry.isDirectory) {
                            // Do nothing (only jars required)
                            // file.mkdirs()
                        } else {
                            val origFile = File(entry.name)
                            if (origFile.extension == "jar" && whiteListJars.any { origFile.name.startsWith(it) }) {
                                val file = File(libDir, origFile.name)
                                extractedFiles.add(file)
                                file.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
            }
            combinedClasspath = extractedFiles.filter { it.isDirectory || it.name.endsWith(".jar") } + jarFile
            combinedClasspath?.find {
                it.name.matches(Regex(""".*kotlin-stdlib-\d+(\.\d+)*\.jar""")) // kotlin-stdlib-2.0.21.jar
            }.let { kotlinStdlibJar ->
                if (kotlinStdlibJar != null) {
                    log.info { "Setting system property Kotlin kotlin.java.stdlib.jar=${kotlinStdlibJar.absolutePath}" }
                    System.setProperty("kotlin.java.stdlib.jar", kotlinStdlibJar.absolutePath)
                } else {
                    log.error { "Kotlin stdlib not found in classpath!" }
                }
            }
            log.info { "Setting script classpath: ${combinedClasspath?.joinToString(":") { it.absolutePath }}" }
        }
    }
}
