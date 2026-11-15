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


package org.projectforge.common.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.core.spi.FilterReply
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FlywayNoDataWarningFilterTest {
    private val context = LoggerContext()
    private val filter = FlywayNoDataWarningFilter()
    private val flywayLogger = context.getLogger(FlywayNoDataWarningFilter.FLYWAY_SQL_SCRIPT_EXECUTOR_LOGGER)

    @Test
    fun `no data warnings of Flyway are dropped`() {
        assertEquals(FilterReply.DENY, filter.decide(null, flywayLogger, Level.WARN, NO_DATA_WARNING, null, null))
    }

    @Test
    fun `other warnings of Flyway are kept`() {
        assertEquals(
            FilterReply.NEUTRAL,
            filter.decide(
                null, flywayLogger, Level.WARN,
                "DB: identifier truncated (SQL State: 01004 - Error Code: 0)", null, null,
            ),
        )
        assertEquals(FilterReply.NEUTRAL, filter.decide(null, flywayLogger, Level.WARN, null, null, null))
    }

    @Test
    fun `other loggers are not affected`() {
        assertEquals(
            FilterReply.NEUTRAL,
            filter.decide(null, context.getLogger("org.projectforge.Test"), Level.WARN, NO_DATA_WARNING, null, null),
        )
    }

    companion object {
        private const val NO_DATA_WARNING = "DB: no data (SQL State: 02000 - Error Code: -1100)"
    }
}
