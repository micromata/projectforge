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
import org.projectforge.common.FileUtils
import org.projectforge.framework.configuration.ConfigXml
import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
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
    var combinedClasspathFiles: MutableList<File>? = null
        private set
    var finalClasspathURLs = emptyArray<URL>()
    private set
    var finalClasspathFiles: List<File>? = null
        private set
    //File(ConfigXml.getInstance().tempDirectory, "scriptClassPath")
    val libDir = FileUtils.createFile(ConfigXml.getInstance().applicationHomeDir, "resources", "kotlin-scripting")
    private val extractedFiles = mutableListOf<File>()
    private val extractJars = listOf(
        //"none-to-extract",
        "kotlin-compiler-embeddable",
    ).map { Regex("""$it-\d+(\.\d+)*\.jar${'$'}""") }
    private val copyJars = listOf(
        //"merlin-core",
        //"org.projectforge",
        //"projectforge",
        //"kotlin-stdlib",
        "kotlin-stdlib",
        "kotlin-compiler-embeddable",
        "kotlin-scripting-compiler-embeddable",
        "kotlin-scripting-jsr223",
        //"kotlin-script-util",
        //"poi"
    ).map { Regex("""$it-\d+(\.\d+)*\.jar${'$'}""") } // """kotlin-stdlib-\d+(\.\d+)*\.jar$""",
    private val kotlinStdLibMatcher = Regex("""kotlin-stdlib-\d+(\.\d+)*\.jar$""")
    private val kotlinScriptSystemProperty = "kotlin.java.stdlib.jar"
    private val handleJars = extractJars + copyJars

    init {
        /*
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
        }*/
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
            val classesDir = File(libDir, "classes").also { it.mkdirs() }
            combinedClasspathFiles = mutableListOf()
            JarFile(jarFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    zip.getInputStream(entry).use { input ->
                        if (!entry.isDirectory) {
                            val origFile = File(entry.name)
                            if (origFile.extension == "jar" && handleJars.any { origFile.name.matches(it) }) {
                                log.debug { "Processing file: ${origFile.name}: ${origFile.absolutePath}" }
                                log.debug { "Copying JAR file: ${jarFile.absolutePath}" }
                                val jarFile = File(libDir, origFile.name)
                                // Extract JAR file in destination directory
                                jarFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                                if (extractJars.any { origFile.name.matches(it) }) {
                                    log.debug { "Extracting JAR file: ${jarFile.absolutePath}" }
                                    // Extract inner JAR file
                                    JarFile(jarFile).use { innerJar ->
                                        innerJar.entries().asSequence().forEach { innerEntry ->
                                            innerJar.getInputStream(innerEntry).use { innerInput ->
                                                if (!innerEntry.isDirectory) {
                                                    val extractedFile = File(classesDir, innerEntry.name)
                                                    extractedFile.parentFile.mkdirs() // Create parent directories
                                                    extractedFile.outputStream().use { output ->
                                                        innerInput.copyTo(output)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // Delete temp jar file
                                    jarFile.delete()
                                    if (!combinedClasspathFiles!!.contains(classesDir)) {
                                        combinedClasspathFiles!!.add(classesDir)
                                    }
                                    if (origFile.name.matches(kotlinStdLibMatcher)) {
                                        System.setProperty(kotlinScriptSystemProperty, libDir.absolutePath)
                                    }
                                } else {
                                    combinedClasspathFiles!!.add(jarFile.absoluteFile)
                                    if (origFile.name.matches(kotlinStdLibMatcher)) {
                                        System.setProperty(kotlinScriptSystemProperty, jarFile.absolutePath)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // copyResourceFile("/kotlin-compiler-embeddable-2.0.21.jar", libDir)?.also {
            //     combinedClasspathFiles!!.add(it)
            // }
            //combinedClasspath = extractedFiles.filter { it.isDirectory || it.name.endsWith(".jar") } + jarFile
            finalClasspathFiles = combinedClasspathFiles //+ combinedClasspath!!.filter { it.isDirectory }
            finalClasspathFiles?.map { it.toURI().toURL() }?.toTypedArray()?.also { uRLS ->
                finalClasspathURLs = uRLS
            }
            log.info {
                "Settings:  kotlin.java.stdlib.jar=${System.getProperty(kotlinScriptSystemProperty)}, classpath=${
                    finalClasspathFiles?.joinToString(
                        ":"
                    ) { it.absolutePath }
                }"
            }
        }
    }

    /**
     * @param filename The name of the file in resource to copy (must start with '/').
     */
    private fun copyResourceFile(filename: String, destDir: File): File? {
        val file = File(destDir, filename)
        if (file.exists()) {
            log.info { "File already exists: ${file.absolutePath}" }
            return file
        }
        val resourceStream: InputStream? = object {}.javaClass.getResourceAsStream(filename)
        if (resourceStream == null) {
            log.error { "Resource file not found: $filename" }
            return null
        }
        resourceStream.use { input ->
            log.debug { "Copying JAR file: $filename" }
            Files.copy(input, file.toPath())
            return File(destDir, filename)
        }
    }
}
