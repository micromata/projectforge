/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.framework.files

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectforge.framework.files.LegacyJcrGuard.Result
import java.io.File
import java.util.Date

class LegacyJcrGuardTest {
    @Test
    fun `no JCR data, start`() {
        Assertions.assertEquals(Result.NO_JCR, evaluate(jcrHasData = false))
        Assertions.assertEquals(Result.NO_JCR, evaluate(jcrHasData = false, report = REPORT_ERRORS))
        Assertions.assertEquals(Result.NO_JCR, evaluate(jcrHasData = false, fileStoreMode = "jcr"))
    }

    @Test
    fun `JCR data without a successful migration, refuse the start`() {
        Assertions.assertEquals(Result.NOT_MIGRATED, evaluate())
        Assertions.assertEquals(Result.NOT_MIGRATED, evaluate(oldFiles = 42))
        Assertions.assertEquals(Result.NOT_MIGRATED, evaluate(report = REPORT_ERRORS, oldFiles = 42))
    }

    @Test
    fun `JCR data with a successful migration, start`() {
        Assertions.assertEquals(Result.MIGRATED, evaluate(report = report(total = 3), oldFiles = 3))
        Assertions.assertEquals(Result.MIGRATED, evaluate(report = report(total = 3), fileStoreMode = "db", oldFiles = 1))
        // An empty JCR was migrated (e. g. a new installation of 9.1 without any attachment):
        Assertions.assertEquals(Result.MIGRATED, evaluate(report = report(total = 0)))
    }

    @Test
    fun `migrated JCR, but no migrated files in the file store (data base restored before the migration), refuse`() {
        Assertions.assertEquals(Result.NOT_MIGRATED, evaluate(report = report(total = 3), oldFiles = 0))
    }

    @Test
    fun `migrated files are recognized by their date of creation before the migration`() {
        val reportTime = 1_000_000_000_000L
        var queried: Date? = null
        val result = LegacyJcrGuard.evaluate(
            jcrHasData = true,
            migrationReport = report(total = 3, durationSeconds = 60),
            reportLastModified = reportTime,
            fileStoreMode = null,
            ignoreLegacyJcr = false,
            filesCreatedBefore = { queried = it; 1 },
        )
        Assertions.assertEquals(Result.MIGRATED, result)
        Assertions.assertEquals(
            reportTime - 60_000 - LegacyJcrGuard.MIGRATION_START_MARGIN_MILLIS,
            queried!!.time,
            "Start of the migration minus the margin.",
        )
    }

    @Test
    fun `file store isn't queried without JCR data`() {
        LegacyJcrGuard.evaluate(false, null, 0, null, false) { Assertions.fail("Queried.") }
        LegacyJcrGuard.evaluate(true, report(total = 0), 0, null, false) { Assertions.fail("Queried.") }
    }

    @Test
    fun `switched back to the JCR after the migration, refuse the start`() {
        Assertions.assertEquals(Result.STORED_IN_JCR, evaluate(report = report(total = 3), fileStoreMode = "jcr", oldFiles = 3))
        Assertions.assertEquals(Result.STORED_IN_JCR, evaluate(fileStoreMode = " JCR "))
        Assertions.assertEquals(Result.IGNORED, evaluate(fileStoreMode = "jcr", ignore = true))
    }

    @Test
    fun `refused start may be forced`() {
        Assertions.assertEquals(Result.IGNORED, evaluate(ignore = true))
        Assertions.assertEquals(Result.NO_JCR, evaluate(jcrHasData = false, ignore = true))
    }

    @Test
    fun `JCR directory with data`(@TempDir tempDir: File) {
        val dir = File(tempDir, "jcr")
        Assertions.assertFalse(LegacyJcrGuard.hasData(dir), "Missing directory.")
        dir.mkdirs()
        Assertions.assertFalse(LegacyJcrGuard.hasData(dir), "Empty directory.")
        File(dir, "segmentstore").mkdirs()
        Assertions.assertTrue(LegacyJcrGuard.hasData(dir))
    }

    @Test
    fun `JCR directories of the property, config xml and the default`(@TempDir tempDir: File) {
        val home = tempDir.absolutePath
        Assertions.assertEquals(
            listOf(File(tempDir, "jcr").absolutePath),
            LegacyJcrGuard.jcrDirs(home, " ", emptyList()).map { it.absolutePath },
        )
        Assertions.assertEquals(
            listOf("/data/pf-jcr", "/old/jcr", File(tempDir, "jcr").absolutePath),
            LegacyJcrGuard.jcrDirs(home, "/data/pf-jcr", listOf("/old/jcr", "/data/pf-jcr")).map { it.absolutePath },
        )
        Assertions.assertTrue(LegacyJcrGuard.configXmlJcrDirs(tempDir).isEmpty())
        File(tempDir, "config.xml.migrated").writeText(configXml("/old/jcr"))
        File(tempDir, "config.xml").writeText(configXml("repo"))
        File(tempDir, "config.xml.bak").writeText(configXml("/ignored"))
        Assertions.assertEquals(listOf("/old/jcr", "repo"), LegacyJcrGuard.configXmlJcrDirs(tempDir).sorted())
    }

    private fun configXml(jcrDirectory: String): String {
        return "<config>\n  <jcrDirectory>$jcrDirectory</jcrDirectory>\n</config>\n"
    }

    private fun evaluate(
        jcrHasData: Boolean = true,
        report: String? = null,
        fileStoreMode: String? = null,
        oldFiles: Long = 0,
        ignore: Boolean = false,
    ): Result {
        return LegacyJcrGuard.evaluate(jcrHasData, report, System.currentTimeMillis(), fileStoreMode, ignore) { oldFiles }
    }

    /** As written by the migration of 9.1.x. */
    private fun report(total: Int, result: String = "OK", durationSeconds: Int = 0): String {
        return """
            Migration of the files from the JCR to the file store
            Result: $result
            Files in the JCR: $total
            Copied (kept in the JCR): 0
              into the data base: 0
              into the file system: 0
            Size of migrated files: 0 bytes
            Already migrated before: $total
            Duration: $durationSeconds s

            Errors (0):
        """.trimIndent() + "\n"
    }

    private val REPORT_ERRORS = report(total = 3, result = "ERRORS")
}
