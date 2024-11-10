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

package org.projectforge.commons.test

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.slf4j.LoggerFactory
import java.io.File
import java.math.BigDecimal
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

/**
 * Utility class for testing purposes.
 * It provides methods to create test files and directories and to suppress error log entries.
 * The base directory is determined by the module name.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class TestUtils(modulName: String) {
    val baseDir: File = File(".").absoluteFile.parentFile
    val outputDir: File

    init {
        check(baseDir.name == modulName) {
            "Check of working test directory failed. Base dir isn't module directory: ${baseDir.absolutePath}"
        }
        val testDir = File(baseDir, "test")
        outputDir = File(testDir, "out")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
    }

    /**
     * Creates a test file with the given [name] in the output directory.
     * If the file already exists, the content of the directory will be deleted.
     */
    fun deleteAndCreateTestFile(name: String): File {
        val file = File(outputDir, name)
        file.deleteRecursively()
        return file
    }

    fun determineBaseDirFile(vararg path: String): File {
        return Paths.get(baseDir.absolutePath, *path).toFile()
    }

    fun determineOutputDirFile(vararg path: String): File {
        return Paths.get(baseDir.absolutePath, *path).toFile()
    }

    /*
    Automatic detection:
            var uriString = this::class.java.protectionDomain.codeSource.location.toString().removeSuffix("/src/main/kotlin/org/projectforge/jcr")
          Assertions.assertTrue(uriString.startsWith("file:"), "We're not running in a normal file system. Can't proceed with tests.")
          uriString = uriString.removePrefix("file:")
          Assertions.assertTrue(uriString.contains("/$MODULE_NAME"), "Where we're running? '/$MODULE_NAME' expected in path to find sources for testing, but not found '$uriString'.")
          // Determine location of module projectforge-jcr
          while (uriString.contains("/$MODULE_NAME/")) {
              uriString = uriString.substring(0, uriString.lastIndexOf('/'))
          }

     */

    companion object {
        /**
         * Suppresses ERROR log entries during the execution of the given action.
         *
         * @param action The action to be executed
         */
        @JvmStatic
        fun suppressErrorLogs(action: () -> Unit) {
            // Access the LoggerContext of the current application
            val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
            // Save the original log levels of all loggers
            val originalLevels = loggerContext.loggerList.associateWith { it.level }
            try {
                // Set the log level of all loggers to OFF (suppress ERROR log entries)
                loggerContext.loggerList.forEach { logger -> logger.level = Level.OFF }
                action()
            } finally {
                // Restore the original log levels of all loggers
                originalLevels.forEach { (logger, originalLevel) ->
                    logger.level = originalLevel
                }
            }
        }

        /**
         * Asserts that the given [expected] value is equal to the given [actual] value.
         * The values are considered equal if their values are equals, independent of the scale.
         */
        fun assertEquals(expected: BigDecimal, actual: BigDecimal?, message: String? = null) {
            Assertions.assertNotNull(actual, message)
            Assertions.assertTrue(expected.compareTo(actual) == 0, message)
        }
    }
}
