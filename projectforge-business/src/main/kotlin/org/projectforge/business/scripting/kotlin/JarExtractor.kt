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

package org.projectforge.business.scripting.kotlin

import mu.KotlinLogging
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

private val log = KotlinLogging.logger {}

/**
 * Kotlin scripts don't run out of the box with spring boot. This workaround is needed.
 */
internal object JarExtractor {
    const val TEMP_DIR = "ProjectForge-extracted-jar"

    val extractedDir = createFixedTempDirectory().toFile()

    val runningInFatJar = JarExtractor::class.java.protectionDomain.codeSource.location.toString().startsWith("jar:")

    /**
     * The classpath to be used for the script engine.
     * It contains the copied jars.
     */
    var classpathFiles: List<File>? = null
        private set
    var classpathUrls: Array<URL>? = null
        private set

    /**
     * The jars to be copied to the classpath.
     * The regex is used to match the jar files.
     * projectforge-business-8.0.0-SNAPSHOT.jar, projectforge-common-8.0.0.jar
     */
    private val copyJars = listOf(
        "merlin-core",
        "org.projectforge.plugins.banking",
        "org.projectforge.plugins.datatransfer",
        "org.projectforge.plugins.licensemanagement",
        "org.projectforge.plugins.liquidityplanning",
        "org.projectforge.plugins.marketing",
        "org.projectforge.plugins.merlin",
        "org.projectforge.plugins.skillmatrix",
        "poi",
        "poi-ooxml",
        "projectforge-business",
        "projectforge-common",
        "projectforge-rest",
    ).map { Regex("""$it-\d+(\.\d+)*(-[A-Za-z0-9]+)?\.jar${'$'}""") } // """commons-\d+(\.\d+)*\.jar$""",

    init {
        log.info { "Source code location: ${JarExtractor::class.java.protectionDomain.codeSource.location}" }
        if (runningInFatJar) {
            log.info { "We're running in a fat jar: ${JarExtractor::class.java.protectionDomain.codeSource.location}" }
            val classpath = System.getProperty("java.class.path")
            val jarPath = File(classpath) // Fat JAR or classpath
            extract(jarPath)
        } else {
            log.info { "We aren't running in a fat jar: ${JarExtractor::class.java.protectionDomain.codeSource.location}" }
        }
    }

    private fun extract(springBootJarFile: File) {
        log.info { "Detecting jar file: ${springBootJarFile.absolutePath}" }
        val files = mutableListOf<File>()
        files.add(springBootJarFile.absoluteFile) // Add the spring boot jar file itself. But the test COPY_MIX_FAIL will fail, why?
        JarFile(springBootJarFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    if (!entry.isDirectory) {
                        val origFile = File(entry.name)
                        if (origFile.extension == "jar" && copyJars.any { origFile.name.matches(it) }) {
                            val jarFile = File(extractedDir, origFile.name)
                            log.debug { "Copying jar file: ${origFile.name} -> ${jarFile.absolutePath}" }
                            // Extract JAR file in destination directory
                            jarFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                            files.add(jarFile.absoluteFile)
                        }
                    }
                }
            }
        }
        classpathFiles = files
        classpathUrls = files.map { it.toURI().toURL() }.toTypedArray()
    }


    fun createFixedTempDirectory(): Path {
        val systemTempDir = Paths.get(System.getProperty("java.io.tmpdir"))
        val tempDir = systemTempDir.resolve(TEMP_DIR)
        if (tempDir.exists()) {
            log.info { "Deleting temp directory: ${tempDir.absolutePathString()}" }
            tempDir.toFile().deleteRecursively()
        }
        log.info { "Creating temp directory: ${tempDir.absolutePathString()}" }
        Files.createDirectories(tempDir)
        return tempDir
    }
}
