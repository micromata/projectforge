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

class LogFileQueryTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun `search terms are all required, a leading minus excludes, quotes keep a phrase`() {
        val record = record(message = "Invoice 4711 of 'kai' failed", stackTrace = "java.lang.IllegalStateException: boom")
        Assertions.assertTrue(SearchTerms.parse("invoice FAILED").matches(record))
        Assertions.assertTrue(SearchTerms.parse("IllegalStateException kai@").matches(record), "Stack trace and user.")
        Assertions.assertFalse(SearchTerms.parse("invoice -4711").matches(record))
        Assertions.assertTrue(SearchTerms.parse("\"of 'kai'\" -\"of 'anna'\"").matches(record))
        Assertions.assertFalse(SearchTerms.parse("\"failed of\"").matches(record))
        Assertions.assertTrue(SearchTerms.parse("  - \"\" ").matches(record), "Nothing to search for.")
        Assertions.assertTrue(SearchTerms.parse(null).matches(record))
    }

    @Test
    fun `logger patterns match the abbreviated loggers of the log file`() {
        fun matches(pattern: String, logger: String) = LoggerPattern.of(pattern)!!.matches(logger)
        Assertions.assertTrue(matches("org.projectforge.business.fibu", "o.p.business.fibu.RechnungDao"))
        Assertions.assertTrue(matches("org.projectforge.business.fibu", "o.p.b.fibu.kost.KostCache"))
        Assertions.assertFalse(matches("org.projectforge.business.fibu", "o.p.business.user.UserDao"))
        Assertions.assertTrue(matches("o.h", "org.hibernate.SQL"), "An abbreviation entered.")
        Assertions.assertTrue(matches("org.hibernate", "o.h.e.j.s.SqlExceptionHelper"))
        Assertions.assertFalse(matches("org.hibernate", "o.p.f.p.HibernateUtils"))
        Assertions.assertTrue(matches("hibernate", "org.hibernate.SQL"), "A word: a package segment.")
        Assertions.assertTrue(matches("rechnung", "o.p.business.fibu.RechnungDao"), "A word: part of the class.")
        Assertions.assertFalse(matches("fibu.RechnungDao.foo", "o.p.business.fibu.RechnungDao"))
        Assertions.assertNull(LoggerPattern.of(" . "))
    }

    @Test
    fun `finds the records of the period, level, loggers and terms in the current and rolled files`() {
        val files = files(
            current = """
                2026-10-05 10:00:00.000 ERROR kai@1.2.3.4 o.p.business.fibu.RechnungDao            : Invoice 4711 failed
                2026-10-05 10:00:01.000  INFO kai@1.2.3.4 o.p.business.fibu.RechnungDao            : Invoice 4712 failed
                2026-10-05 10:00:02.000  WARN @ o.h.e.j.s.SqlExceptionHelper             : Invoice 4713 failed
                2026-10-05 10:00:03.000  WARN anna@1.2.3.5 o.p.business.user.UserDao                : Invoice 4714 failed
                """,
            rolled = mapOf(
                "2026-10-04" to """
                    2026-10-04 23:59:00.000 ERROR @ o.p.business.fibu.RechnungDao            : Invoice 42 failed
                    2026-10-04 08:00:00.000 ERROR @ o.p.business.fibu.RechnungDao            : Invoice 41 failed
                    """,
            ),
        )
        fun search(
            search: String? = "invoice",
            threshold: LogLevel = LogLevel.WARN,
            from: String = "2026-10-04T12:00:00Z",
            include: List<String> = emptyList(),
            exclude: List<String> = emptyList(),
        ) = LogFileSearch(
            LogFileQuery(search, threshold, millis(from), millis("2026-10-05T23:00:00Z"), include, exclude),
            ZoneOffset.UTC,
        ).search(files).records.map { it.message.substringAfter("Invoice ").substringBefore(' ') }
        Assertions.assertEquals(listOf("4714", "4713", "4711", "42"), search(), "Newest first, not INFO, not before.")
        Assertions.assertEquals(listOf("4714", "4713", "4712", "4711"), search(threshold = LogLevel.INFO, from = "2026-10-05T00:00:00Z"))
        Assertions.assertEquals(listOf("4714", "4711", "42"), search(exclude = listOf("org.hibernate")))
        Assertions.assertEquals(listOf("4711", "42"), search(include = listOf("org.projectforge.business.fibu")))
        Assertions.assertEquals(listOf("4711", "42"), search(threshold = LogLevel.FATAL), "FATAL is written as ERROR.")
        Assertions.assertEquals(listOf("4714"), search(search = "invoice anna"))
    }

    @Test
    fun `reads the context of a hit within its file, by scope and without excluded loggers`() {
        val files = files(
            current = """
                2026-10-05 10:00:00.000  INFO kai@1.2.3.4 sessions=[http=a] o.p.business.fibu.RechnungDao            : kai 1
                2026-10-05 10:00:01.000  INFO anna@1.2.3.5 o.p.business.user.UserDao                : anna 2
                2026-10-05 10:00:02.000  INFO @ o.h.e.j.s.SqlExceptionHelper             : noise 3
                2026-10-05 10:00:03.000  INFO kai@1.2.3.4 sessions=[http=b] o.p.business.user.UserDao                : kai 4
                2026-10-05 10:00:04.000 ERROR kai@1.2.3.4 o.p.business.fibu.RechnungDao            : the hit
                java.lang.IllegalStateException: boom
                	at org.projectforge.business.fibu.RechnungDao.save(RechnungDao.kt:42)
                2026-10-05 10:00:05.000  INFO anna@1.2.3.5 o.p.business.user.UserDao                : anna 6
                2026-10-05 10:00:06.000  INFO kai@1.2.3.4 o.p.business.fibu.RechnungDao            : kai 7
                """,
        )
        fun context(
            before: Int = 2,
            after: Int = 1,
            scope: LogFileContextScope = LogFileContextScope.ALL,
            exclude: List<String> = emptyList(),
            line: Int = 5,
            timestamp: String = "2026-10-05T10:00:04Z",
        ) = LogFileContext(
            line, millis(timestamp), "kai@1.2.3.4", "o.p.business.fibu.RechnungDao", before, after, scope, exclude,
            ZoneOffset.UTC,
        ).read(files, "ProjectForge.log")!!

        fun messages(records: List<LogFileRecord>) = records.map { it.message }

        context().let {
            Assertions.assertEquals("the hit", it.hit!!.message)
            Assertions.assertEquals(listOf("noise 3", "kai 4"), messages(it.before))
            Assertions.assertEquals(listOf("anna 6"), messages(it.after))
            Assertions.assertTrue(it.moreBefore)
            Assertions.assertTrue(it.moreAfter)
        }
        context(exclude = listOf("org.hibernate")).let {
            Assertions.assertEquals(listOf("anna 2", "kai 4"), messages(it.before))
        }
        context(before = 5, after = 5, scope = LogFileContextScope.USER).let {
            Assertions.assertEquals(listOf("kai 1", "kai 4"), messages(it.before), "Sessions aside, the same user.")
            Assertions.assertEquals(listOf("kai 7"), messages(it.after))
            Assertions.assertFalse(it.moreBefore, "The beginning of the file.")
            Assertions.assertFalse(it.moreAfter, "The end of the file.")
        }
        context(before = 5, after = 0, scope = LogFileContextScope.LOGGER).let {
            Assertions.assertEquals(listOf("kai 1"), messages(it.before))
            Assertions.assertTrue(it.after.isEmpty())
            Assertions.assertTrue(it.moreAfter)
        }
        Assertions.assertNull(context(timestamp = "2026-10-05T10:00:03Z").hit, "Another record at the line.")
        Assertions.assertNull(context(line = 6).hit, "No record starts at the line.")
        val other = LogFileContext(5, 0, "", "", 1, 1, zone = ZoneOffset.UTC)
        Assertions.assertNull(other.read(files, "../ProjectForge.log"), "Only the configured files.")
        Assertions.assertNull(other.read(files, "ProjectForge.2026-10-01.log.gz"), "No such file.")
    }

    @Test
    fun `resolves the configured files by their names only`() {
        val files = files(current = "", rolled = mapOf("2026-10-04" to ""))
        Assertions.assertEquals(File(dir, "ProjectForge.log"), files.byName("ProjectForge.log"))
        Assertions.assertEquals(File(dir, "ProjectForge.2026-10-04.log.gz"), files.byName("ProjectForge.2026-10-04.log.gz"))
        Assertions.assertNull(files.byName("ProjectForge.2026-10-03.log.gz"))
        Assertions.assertNull(files.byName("ProjectForge.2026-13-04.log.gz"))
        Assertions.assertNull(files.byName("../x/ProjectForge.2026-10-04.log.gz"))
    }

    private fun millis(iso: String) = Instant.parse(iso).toEpochMilli()

    private fun record(message: String, stackTrace: String? = null) = LogFileRecord(
        timestampMillis = 0,
        level = LogLevel.ERROR,
        user = "kai@1.2.3.4",
        logger = "o.p.business.fibu.RechnungDao",
        message = message,
        stackTrace = stackTrace,
        file = "ProjectForge.log",
        line = 1,
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
