/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common.development

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * Different source file checks.
 * @author Kai Reinhard
 */
class SourceFileCheckerTest {
    /**
     * Checks all Kotlin and Java sources for correct declaration of LoggerFactory.getLogger(SourceFileCheckerTest::class.java). If any
     * Logger declaration differs from the source file name, the test fails.
     * Example: Foo.java: private final Logger log = LoggerFactory.getLogger(Bar.class); // Bar.class should bee Foo.class.
     */
    @Test
    @Throws(IOException::class)
    fun checkLoggerDeclarations() {
        var baseDir = File(System.getProperty("user.dir"))
        if (!baseDir.exists() || !baseDir.isDirectory) return
        if (baseDir.name == "projectforge-common") {
            // Check parent directory for checking all other ProjectForge modules too.
            baseDir = baseDir.parentFile
        }
        log.info("Checking directory '${baseDir.absolutePath}...")
        var processedKotlinFiles = 0
        var processedJavaFiles = 0
        val errorFiles = mutableListOf<String>()
        baseDir.walkTopDown().filter { supportedExtensions.contains(it.extension) }.forEach { file ->
            if (file.extension == "kt") {
                ++processedKotlinFiles
            } else {
                ++processedJavaFiles
            }
            val content = file.readText(StandardCharsets.UTF_8)
            val matchResult = loggerRegex.find(content)
            val loggerClass = matchResult?.groups?.get(1)?.value?.trim()
            if (loggerClass != null && file.nameWithoutExtension != loggerClass) {
                errorFiles.add(file.path)
                log.warn("$file declares foreign logger class: '$loggerClass'")
            }
        }
        log.info("Checked $processedKotlinFiles Kotlin and $processedJavaFiles Java source files for Logger declaration.")
        Assertions.assertTrue( errorFiles.isEmpty(), "Source files with mismatching logger classes found: ${errorFiles.joinToString { it }}")
    }

    companion object {
        private val loggerRegex = """LoggerFactory\s*.\s*getLogger\s*\(\s*(.*)\s*(\.class|:{2}class)""".toRegex()
        private val supportedExtensions = arrayOf("kt", "java")

        // Kotlin:
        private val log = LoggerFactory.getLogger(SourceFileCheckerTest::class.java)
        // Java: private static final Logger log = LoggerFactory.getLogger(SourceFileCheckerTest.class);
    }
}
