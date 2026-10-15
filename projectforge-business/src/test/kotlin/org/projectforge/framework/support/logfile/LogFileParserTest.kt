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
import org.projectforge.common.logging.LogLevel
import java.time.Instant
import java.time.ZoneOffset

class LogFileParserTest {
    @Test
    fun `reads the legacy format with its stray dashes`() {
        val (records, parser) = parse(
            """
            ---2026-10-05 02:47:46.946 -ERROR @ org.projectforge.mail.SendMail           : While sending message
            -
            org.eclipse.angus.mail.util.MailConnectException: Couldn't connect to host, port: mail, 25;
              nested exception is:
            	java.net.ConnectException: Connection refused
            	at org.projectforge.mail.SendMail.sendIt(SendMail.kt:293)
            Caused by: java.net.ConnectException: Connection refused
            	at java.base/sun.nio.ch.Net.pollConnect(Native Method)
            	... 15 common frames omitted

            --2026-10-05 02:47:47.072 - INFO @ o.p.f.p.database.IndexProgressMonitor    : Indexing 80%
            ---2026-10-05 02:48:00.001 - WARN kai@127.0.0.1 sessions=[http=ab12] o.p.web.rest.RestAuthenticationUtils     : User: kai calls RestURL: /rs/menu
            """
        )
        Assertions.assertEquals(3, records.size)
        Assertions.assertEquals(0, parser.unparsedStarts)
        records[0].let {
            Assertions.assertEquals(LogLevel.ERROR, it.level)
            Assertions.assertEquals("org.projectforge.mail.SendMail", it.logger)
            Assertions.assertEquals("@", it.user)
            Assertions.assertEquals("While sending message", it.message)
            Assertions.assertTrue(it.stackTrace!!.startsWith("org.eclipse.angus.mail.util.MailConnectException: Couldn't"))
            Assertions.assertTrue(it.stackTrace.endsWith("... 15 common frames omitted"))
            Assertions.assertEquals(1, it.line)
            Assertions.assertEquals(Instant.parse("2026-10-05T02:47:46.946Z").toEpochMilli(), it.timestampMillis)
        }
        records[1].let {
            Assertions.assertEquals(LogLevel.INFO, it.level)
            Assertions.assertEquals("o.p.f.p.database.IndexProgressMonitor", it.logger)
            Assertions.assertNull(it.stackTrace)
        }
        records[2].let {
            Assertions.assertEquals(LogLevel.WARN, it.level)
            Assertions.assertEquals("kai@127.0.0.1 sessions=[http=ab12]", it.user)
            Assertions.assertEquals("o.p.web.rest.RestAuthenticationUtils", it.logger)
            Assertions.assertEquals("User: kai calls RestURL: /rs/menu", it.message)
        }
    }

    @Test
    fun `reads the recommended format with multi-line messages`() {
        val (records, parser) = parse(
            """
            2026-10-05 10:00:00.000 ERROR kai@10.0.0.1 o.p.business.fibu.RechnungDao            : Booking failed:
            first detail
            second detail
            java.lang.IllegalStateException: boom
            	at org.projectforge.business.fibu.RechnungDao.save(RechnungDao.kt:42)

            2026-10-05 10:00:01.000  INFO @ o.p.Foo                                  : done
            """
        )
        Assertions.assertEquals(2, records.size)
        Assertions.assertEquals(0, parser.unparsedStarts)
        Assertions.assertEquals("Booking failed:\nfirst detail\nsecond detail", records[0].message)
        Assertions.assertTrue(records[0].stackTrace!!.startsWith("java.lang.IllegalStateException: boom\n\tat "))
        Assertions.assertEquals(LogLevel.INFO, records[1].level)
        Assertions.assertEquals(7, records[1].line)
    }

    @Test
    fun `counts the lines of another format`() {
        val (records, parser) = parse(
            """
            2026-10-05T10:00:00.000+00:00 ERROR 4711 --- [main] o.p.Foo : Spring's default format
            2026-10-05T10:00:01.000+00:00  INFO 4711 --- [main] o.p.Foo : again
            """
        )
        Assertions.assertEquals(0, records.size)
        Assertions.assertEquals(2, parser.unparsedStarts)
    }

    private fun parse(text: String): Pair<List<LogFileRecord>, LogFileParser> {
        val records = mutableListOf<LogFileRecord>()
        val parser = LogFileParser("test.log", ZoneOffset.UTC) { records.add(it) }
        text.trimIndent().lines().forEach { parser.accept(it) }
        parser.finish()
        return records to parser
    }
}
