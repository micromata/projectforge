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

package org.projectforge.framework.support.logfile

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectforge.common.logging.LogLevel
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.util.zip.GZIPOutputStream

class LogFileSearchTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `finds the occurrences by their message in the current and the rolled files`() {
        val files = files(
            current = """
                2026-10-05 10:00:00.000 ERROR kai@1.2.3.4 o.p.business.fibu.RechnungDao            : Invoice 4711 of 'kai' failed
                2026-10-05 10:00:01.000 ERROR kai@1.2.3.4 o.p.business.fibu.RechnungDao            : Something else
                2026-10-05 10:00:02.000  WARN kai@1.2.3.4 o.p.business.fibu.RechnungDao            : Invoice 1 of 'x' failed
                """,
            rolled = mapOf(
                "2026-10-04" to """
                    ---2026-10-04 23:59:00.000 -ERROR @ o.p.business.fibu.RechnungDao            : Invoice 42 of 'anna' failed
                    ---2026-10-04 08:00:00.000 -ERROR @ o.p.business.fibu.RechnungDao            : Invoice 41 of 'anna' failed
                    """,
            ),
        )
        val result = LogFileSearch(problem(sampleMessage = "Invoice 1 of 'b' failed"), ZoneOffset.UTC).search(files)
        Assertions.assertEquals(listOf("ProjectForge.log", "ProjectForge.2026-10-04.log.gz"), result.searchedFiles)
        Assertions.assertEquals(
            listOf("Invoice 4711 of 'kai' failed", "Invoice 42 of 'anna' failed"),
            result.records.map { it.message },
            "Newest first, not the WARN, not before the first occurrence.",
        )
        Assertions.assertFalse(result.truncated)
        Assertions.assertFalse(result.formatWarning)
    }

    @Test
    fun `finds a request's exception by its root cause's frame and a problem without sample by its logger`() {
        val files = files(
            current = """
                2026-10-05 10:00:00.000 ERROR kai@1.2.3.4 o.p.r.c.GlobalDefaultExceptionHandler    : Exception while processing GET /rs/invoice
                java.lang.IllegalStateException: boom
                	at org.projectforge.business.fibu.RechnungDao.save(RechnungDao.kt:42)

                2026-10-05 10:00:01.000 ERROR kai@1.2.3.4 o.p.r.c.GlobalDefaultExceptionHandler    : Exception while processing GET /rs/order
                java.lang.IllegalStateException: boom
                	at org.projectforge.business.fibu.AuftragDao.save(AuftragDao.kt:42)
                """,
        )
        val request = problem(location = "RechnungDao:42", exceptionClass = "java.lang.IllegalStateException", sampleMessage = "boom")
        Assertions.assertEquals(
            listOf("Exception while processing GET /rs/invoice"),
            LogFileSearch(request, ZoneOffset.UTC).search(files).records.map { it.message },
        )
        val noSample = problem(location = "GlobalDefaultExceptionHandler:77", sampleMessage = null)
        Assertions.assertEquals(2, LogFileSearch(noSample, ZoneOffset.UTC).search(files).records.size)
    }

    @Test
    fun `stops at the limit of hits and warns about an unreadable format`() {
        val lines = (0 until 5).joinToString("\n") {
            "2026-10-05 10:00:0$it.000 ERROR @ o.p.business.fibu.RechnungDao            : Invoice $it failed"
        }
        val limited = LogFileSearch(problem(sampleMessage = "Invoice 1 failed"), ZoneOffset.UTC, maxHits = 3)
            .search(files(current = lines))
        Assertions.assertEquals(listOf("Invoice 4 failed", "Invoice 3 failed", "Invoice 2 failed"), limited.records.map { it.message })
        Assertions.assertTrue(limited.truncated)

        val other = files(current = "2026-10-05T10:00:00.000+00:00 ERROR 4711 --- [main] o.p.Foo : Spring's default")
        val result = LogFileSearch(problem(sampleMessage = "Spring's default"), ZoneOffset.UTC).search(other)
        Assertions.assertTrue(result.formatWarning)
        Assertions.assertEquals(1, result.unparsedLines)
        Assertions.assertEquals(LogFileParser.RECOMMENDED_PATTERN, result.recommendedPattern)
    }

    private fun problem(
        location: String? = "RechnungDao:12",
        exceptionClass: String? = null,
        sampleMessage: String?,
    ) = LogFileProblem(
        level = LogLevel.ERROR,
        location = location,
        exceptionClass = exceptionClass,
        sampleMessage = sampleMessage,
        byCode = false,
        firstSeen = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli(),
        lastSeen = Instant.parse("2026-10-05T10:00:05Z").toEpochMilli(),
    )

    private fun files(current: String, rolled: Map<String, String> = emptyMap()): LogFiles {
        val file = File(dir, "ProjectForge.log")
        file.writeText(current.trimIndent())
        rolled.forEach { (day, text) ->
            GZIPOutputStream(File(dir, "ProjectForge.$day.log.gz").outputStream()).bufferedWriter().use {
                it.write(text.trimIndent())
            }
        }
        return LogFiles(file, "${dir.absolutePath}/ProjectForge.%d{yyyy-MM-dd}.log.gz")
    }
}
